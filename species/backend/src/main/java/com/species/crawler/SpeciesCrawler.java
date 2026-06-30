package com.species.crawler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.species.entity.SpeciesInfo;
import com.species.mapper.SpeciesInfoMapper;
import org.jsoup.Jsoup;
import org.springframework.jdbc.core.JdbcTemplate;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 物种爬虫
 *
 * 从 Wikipedia 中文版爬取动物数据：
 * 1. REST API 拿摘要描述
 * 2. HTML 页面解析信息框（学名、保护级别、栖息地等）
 *
 * 速度：每个请求间隔 300ms，10 条约 15-20 秒完成
 * 如遇网络问题只保存基本信息，保证数据不丢
 */
@Component
public class SpeciesCrawler {

    private static final Logger log = LoggerFactory.getLogger(SpeciesCrawler.class);
    private static final String UA = "SpeciesKB/1.0 (https://github.com/species-kb)";
    private static final String WIKI_SUMMARY = "https://zh.wikipedia.org/api/rest_v1/page/summary/";
    private static final String WIKI_HTML = "https://zh.wikipedia.org/api/rest_v1/page/html/";

    private final SpeciesInfoMapper speciesInfoMapper;
    private final JdbcTemplate jdbcTemplate;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "crawler-worker");
        t.setDaemon(true);
        return t;
    });
    private final Map<String, CrawlJob> jobs = new ConcurrentHashMap<>();

    public SpeciesCrawler(SpeciesInfoMapper speciesInfoMapper, JdbcTemplate jdbcTemplate) {
        this.speciesInfoMapper = speciesInfoMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public CrawlJob startCrawl() {
        String jobId = UUID.randomUUID().toString().substring(0, 8);
        CrawlJob job = new CrawlJob(jobId);
        job.setTotal(SEEDS.size());
        jobs.put(jobId, job);
        executor.submit(() -> doCrawl(job));
        return job;
    }

    public CrawlJob getJob(String jobId) { return jobs.get(jobId); }

    public Map<Long, Long> getSeedStats() {
        return Map.of(
            1L, SEEDS.stream().filter(s -> s.categoryId == 1L).count(),
            2L, SEEDS.stream().filter(s -> s.categoryId == 2L).count(),
            3L, SEEDS.stream().filter(s -> s.categoryId == 3L).count()
        );
    }

    public List<String> getSeedNames() {
        return SEEDS.stream().map(SeedAnimal::nameZh).toList();
    }

    public int clearCrawlerData() {
        List<String> names = getSeedNames();
        if (names.isEmpty()) return 0;
        int deleted = speciesInfoMapper.delete(
                new LambdaQueryWrapper<SpeciesInfo>()
                        .in(SpeciesInfo::getNameZh, names)
                        .gt(SpeciesInfo::getId, 15)
        );
        // 重置自增 ID，保证新数据从 16 开始
        jdbcTemplate.execute("ALTER TABLE species_info AUTO_INCREMENT = 16");
        return deleted;
    }

    // ==================== 爬取逻辑 ====================

    private static final int CRAWL_TARGET = 10;
    /** 请求间隔（毫秒），Wikipedia 对 API 请求限制较宽松 */
    private static final int REQUEST_DELAY = 300;

    private void doCrawl(CrawlJob job) {
        log.info("爬虫任务 {} 启动，目标 {} 条", job.getJobId(), CRAWL_TARGET);
        int processed = 0;

        for (SeedAnimal seed : SEEDS) {
            if (!"running".equals(job.getStatus())) break;
            if (processed >= CRAWL_TARGET) {
                job.addLog("📊 已达目标（" + CRAWL_TARGET + " 条），停止");
                break;
            }

            // 去重
            Long count = speciesInfoMapper.selectCount(
                    new LambdaQueryWrapper<SpeciesInfo>()
                            .eq(SpeciesInfo::getNameZh, seed.nameZh));
            if (count != null && count > 0) {
                job.addLog("⏭️ 跳过 【" + seed.nameZh + "】— 已存在");
                job.incrementSkipped();
                continue;
            }

            try {
                SpeciesInfo info = fetchFromWikipedia(seed);
                if (info != null) {
                    speciesInfoMapper.insert(info);
                    job.addLog("✅ 成功 【" + seed.nameZh + "】");
                    job.incrementSuccess();
                } else {
                    saveBasic(seed);
                    job.addLog("⚠️ 网络失败 【" + seed.nameZh + "】— 已存基本信息");
                    job.incrementFailed();
                }
            } catch (Exception e) {
                log.warn("爬取失败 [{}]: {}", seed.nameZh, e.getMessage());
                saveBasic(seed);
                job.addLog("⚠️ 爬取失败 【" + seed.nameZh + "】— " + e.getMessage());
                job.incrementFailed();
            }

            processed++;
            try { Thread.sleep(REQUEST_DELAY); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
        }

        job.complete();
        log.info("爬虫任务 {} 完成: 成功{} / 跳过{} / 失败{}",
                job.getJobId(), job.getSuccess(), job.getSkipped(), job.getFailed());
    }

    /**
     * 从 Wikipedia 爬取完整物种数据
     */
    private SpeciesInfo fetchFromWikipedia(SeedAnimal seed) {
        SpeciesInfo info = new SpeciesInfo();
        info.setCategoryId(seed.categoryId);
        info.setNameZh(seed.nameZh);
        info.setConservationStatus(seed.conservationStatus);
        info.setStatus(1);
        info.setCreatedBy(1L);

        String encoded = URLEncoder.encode(seed.nameZh, StandardCharsets.UTF_8);

        // 1. REST API → 摘要描述
        try {
            Map<String, Object> summary = fetchJson(WIKI_SUMMARY + encoded);
            if (summary != null) {
                String extract = (String) summary.get("extract");
                if (extract != null && !extract.isEmpty()) {
                    info.setDescription(truncate(extract, 5000));
                }
                // 从摘要提取学名
                if (extract != null) {
                    Pattern p = Pattern.compile("学名[：:]\\s*(\\S+)");
                    Matcher m = p.matcher(extract);
                    if (m.find()) {
                        info.setNameScientific(m.group(1).replaceAll("[）)]", "").trim());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("摘要API失败 [{}]: {}", seed.nameZh, e.getMessage());
        }

        // 2. HTML 页面 → 信息框
        try {
            Document doc = Jsoup.connect(WIKI_HTML + encoded)
                    .userAgent(UA)
                    .timeout(8000)
                    .get();

            Element infobox = doc.selectFirst("table.infobox");
            if (infobox != null) {
                for (Element row : infobox.select("tr")) {
                    Element th = row.selectFirst("th");
                    Element td = row.selectFirst("td");
                    if (th == null || td == null) continue;

                    String label = th.text().trim().replaceAll("\\s+", "");
                    String value = td.text().trim();

                    if (containsAny(label, "拉丁学名", "学名")) {
                        if (info.getNameScientific() == null) info.setNameScientific(value);
                    } else if (containsAny(label, "别名")) {
                        info.setAlias(truncate(value, 500));
                    } else if (containsAny(label, "保护级别", "保护现状", "濒危")) {
                        String p = parseConservation(value);
                        if (p != null) info.setConservationStatus(p);
                    } else if (containsAny(label, "栖息地", "栖息")) {
                        info.setHabitat(truncate(value, 1000));
                    } else if (containsAny(label, "分布")) {
                        info.setDistribution(truncate(value, 1000));
                    } else if (containsAny(label, "体重", "重量")) {
                        info.setWeight(truncate(value, 100));
                    } else if (containsAny(label, "食性", "饮食")) {
                        info.setDiet(truncate(value, 500));
                    } else if (containsAny(label, "寿命")) {
                        info.setLifespan(truncate(value, 100));
                    } else if (containsAny(label, "繁殖")) {
                        info.setReproduction(truncate(value, 500));
                    }
                }
            }

            // 正文段落作为趣味知识
            StringBuilder extra = new StringBuilder();
            for (Element p : doc.select("p")) {
                String text = p.text().trim();
                if (text.length() > 40 && !text.startsWith("[")) {
                    extra.append(text).append("\n\n");
                    if (extra.length() > 5000) break;
                }
            }
            if (!extra.isEmpty()) {
                info.setFunFacts(truncate(extra.toString().trim(), 5000));
            }
        } catch (Exception e) {
            log.warn("HTML解析失败 [{}]: {}", seed.nameZh, e.getMessage());
        }

        return info;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchJson(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", UA)
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(8))
                .GET().build();
        HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() == 200) {
            return objectMapper.readValue(resp.body(), new TypeReference<Map<String, Object>>() {});
        }
        return null;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) if (text.contains(kw)) return true;
        return false;
    }

    private String parseConservation(String text) {
        String u = text.toUpperCase();
        if (text.contains("极危") || u.contains("CR")) return "CR";
        if (text.contains("濒危") || u.contains("EN")) return "EN";
        if (text.contains("易危") || u.contains("VU")) return "VU";
        if (text.contains("近危") || u.contains("NT")) return "NT";
        if (text.contains("无危") || u.contains("LC")) return "LC";
        if (text.contains("未评估") || u.contains("NE")) return "NE";
        if (text.contains("数据缺乏") || u.contains("DD")) return "DD";
        return null;
    }

    private void saveBasic(SeedAnimal seed) {
        try {
            SpeciesInfo info = new SpeciesInfo();
            info.setNameZh(seed.nameZh);
            info.setCategoryId(seed.categoryId);
            info.setConservationStatus(seed.conservationStatus);
            info.setStatus(1);
            info.setCreatedBy(1L);
            speciesInfoMapper.insert(info);
        } catch (Exception e) {
            log.error("保存基本信息失败 [{}]", seed.nameZh, e);
        }
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() > maxLen ? s.substring(0, maxLen) : s;
    }

    // ==================== 种子数据 ====================

    private record SeedAnimal(String nameZh, Long categoryId, String conservationStatus) {}

    private static final List<SeedAnimal> SEEDS = List.of(
            new SeedAnimal("大熊猫", 1L, "EN"),
            new SeedAnimal("东北虎", 1L, "EN"),
            new SeedAnimal("金丝猴", 1L, "EN"),
            new SeedAnimal("藏羚羊", 1L, "NT"),
            new SeedAnimal("亚洲象", 1L, "EN"),
            new SeedAnimal("雪豹", 1L, "VU"),
            new SeedAnimal("华南虎", 1L, "CR"),
            new SeedAnimal("麋鹿", 1L, "NT"),
            new SeedAnimal("中华穿山甲", 1L, "CR"),
            new SeedAnimal("江豚", 1L, "EN"),
            new SeedAnimal("亚洲黑熊", 1L, "VU"),
            new SeedAnimal("狼", 1L, "LC"),
            new SeedAnimal("赤狐", 1L, "LC"),
            new SeedAnimal("兔狲", 1L, "LC"),
            new SeedAnimal("猞猁", 1L, "LC"),
            new SeedAnimal("云豹", 1L, "VU"),
            new SeedAnimal("藏狐", 1L, "LC"),
            new SeedAnimal("紫貂", 1L, "LC"),
            new SeedAnimal("驼鹿", 1L, "LC"),
            new SeedAnimal("白唇鹿", 1L, "VU"),
            new SeedAnimal("荒漠猫", 1L, "VU"),
            new SeedAnimal("貂熊", 1L, "VU"),
            new SeedAnimal("丹顶鹤", 2L, "EN"),
            new SeedAnimal("黑颈鹤", 2L, "VU"),
            new SeedAnimal("朱鹮", 2L, "EN"),
            new SeedAnimal("中华秋沙鸭", 2L, "EN"),
            new SeedAnimal("金雕", 2L, "LC"),
            new SeedAnimal("白尾海雕", 2L, "LC"),
            new SeedAnimal("绿孔雀", 2L, "EN"),
            new SeedAnimal("褐马鸡", 2L, "VU"),
            new SeedAnimal("红腹锦鸡", 2L, "LC"),
            new SeedAnimal("大鸨", 2L, "VU"),
            new SeedAnimal("东方白鹳", 2L, "EN"),
            new SeedAnimal("白鹤", 2L, "CR"),
            new SeedAnimal("白头鹤", 2L, "VU"),
            new SeedAnimal("大天鹅", 2L, "LC"),
            new SeedAnimal("鸳鸯", 2L, "LC"),
            new SeedAnimal("雕鸮", 2L, "LC"),
            new SeedAnimal("雪鸮", 2L, "VU"),
            new SeedAnimal("蓝孔雀", 2L, "LC"),
            new SeedAnimal("扬子鳄", 3L, "CR"),
            new SeedAnimal("蟒蛇", 3L, "VU"),
            new SeedAnimal("玳瑁", 3L, "CR"),
            new SeedAnimal("棱皮龟", 3L, "VU"),
            new SeedAnimal("绿海龟", 3L, "EN"),
            new SeedAnimal("鳄蜥", 3L, "CR"),
            new SeedAnimal("巨蜥", 3L, "VU"),
            new SeedAnimal("四爪陆龟", 3L, "CR"),
            new SeedAnimal("鼋", 3L, "CR"),
            new SeedAnimal("大壁虎", 3L, "LC")
    );
}
