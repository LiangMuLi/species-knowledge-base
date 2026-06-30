package com.species.config;

import com.species.entity.SpeciesCategory;
import com.species.entity.SpeciesInfo;
import com.species.entity.User;
import com.species.mapper.SpeciesCategoryMapper;
import com.species.mapper.SpeciesInfoMapper;
import com.species.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 数据库初始化器
 *
 * 1. 总是执行：创建缺失的表（如 species_comment）
 * 2. 首次执行（init-data=true）：创建管理员、分类、示例物种
 * 3. 每次启动：补充缺失的物种数据（如果不够 15 个）
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserMapper userMapper;
    private final SpeciesCategoryMapper categoryMapper;
    private final SpeciesInfoMapper speciesInfoMapper;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Value("${app.init-data:false}")
    private boolean initData;

    public DataInitializer(UserMapper userMapper,
                           SpeciesCategoryMapper categoryMapper,
                           SpeciesInfoMapper speciesInfoMapper,
                           PasswordEncoder passwordEncoder,
                           JdbcTemplate jdbcTemplate) {
        this.userMapper = userMapper;
        this.categoryMapper = categoryMapper;
        this.speciesInfoMapper = speciesInfoMapper;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        // 1. 总是执行：建缺失的表
        initSchema();

        // 2. 首次初始化
        if (initData && userMapper.selectCount(null) == 0) {
            initFullData();
        }

        // 3. 总是执行：补充分类和物种
        supplementData();
    }

    /** 创建没有的表（schema 迁移） */
    private void initSchema() {
        try {
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS species_comment (" +
                "  id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "  species_id BIGINT NOT NULL," +
                "  user_id BIGINT NOT NULL," +
                "  content TEXT NOT NULL," +
                "  rating TINYINT DEFAULT NULL COMMENT '评分 1-5'," +
                "  created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "  INDEX idx_species (species_id)," +
                "  FOREIGN KEY (species_id) REFERENCES species_info(id) ON DELETE CASCADE," +
                "  FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci " +
                "COMMENT='物种评论表'"
            );
            log.info("评论表已就绪");
        } catch (Exception e) {
            log.warn("建评论表跳过（可能已存在或外键问题）: {}", e.getMessage());
        }
    }

    /** 完整初始化（首次部署） */
    private void initFullData() {
        log.info("===== 开始完整初始化 =====");

        // 管理员
        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setNickname("管理员");
        admin.setRole("admin");
        admin.setStatus(1);
        userMapper.insert(admin);
        log.info("管理员已创建: admin / admin123");

        // 7 个分类
        insertCategory(1L, "哺乳动物", 1, "哺乳纲动物");
        insertCategory(2L, "鸟类", 2, "鸟纲动物");
        insertCategory(3L, "爬行动物", 3, "爬行纲动物");
        insertCategory(4L, "两栖动物", 4, "两栖纲动物");
        insertCategory(5L, "鱼类", 5, "鱼类");
        insertCategory(6L, "昆虫", 6, "昆虫纲");
        insertCategory(7L, "植物", 7, "植物界");
        log.info("7 个分类已创建");

        // 3 个初始物种（后面 supplement 会加更多）
        insertSpecies(1L, "大熊猫", "Giant Panda", "Ailuropoda melanoleuca",
            1L, "VU", 1,
            "大熊猫是中国特有物种，被誉为\"活化石\"和\"中国国宝\"。主要以竹子为食。",
            "海拔2600-3500米的竹林", "中国四川、陕西、甘肃",
            "大熊猫每天吃12-38公斤竹子，占体重的40%！");

        insertSpecies(2L, "东北虎", "Siberian Tiger", "Panthera tigris altaica",
            1L, "EN", 1,
            "东北虎是现存体型最大的虎亚种，分布于中国东北地区。",
            "针叶林、阔叶混交林", "中国东北、俄罗斯远东",
            "东北虎的条纹就像人类的指纹，每只都不一样。");

        insertSpecies(3L, "丹顶鹤", "Red-crowned Crane", "Grus japonensis",
            2L, "EN", 1,
            "丹顶鹤是东亚地区的珍稀鸟类，因头顶红色斑纹而得名。",
            "湿地、沼泽、湖泊", "中国东北、日本、韩国",
            "丹顶鹤在中国文化中象征长寿，常与松树并称\"松鹤延年\"");

        log.info("===== 完整初始化完成 =====");
    }

    /** 补充缺失的分类和物种 */
    private void supplementData() {
        // 补充分类（最多 7 个）
        long catCount = categoryMapper.selectCount(null);
        if (catCount < 7) {
            log.info("补充分类...");
            if (catCount < 4) insertCategory(4L, "两栖动物", 4, "两栖纲动物");
            if (catCount < 5) insertCategory(5L, "鱼类", 5, "鱼类");
            if (catCount < 6) insertCategory(6L, "昆虫", 6, "昆虫纲");
            if (catCount < 7) insertCategory(7L, "植物", 7, "植物界");
            log.info("分类补充完成");
        }

        // 补充物种到至少 15 个
        long speciesCount = speciesInfoMapper.selectCount(null);
        if (speciesCount >= 15) {
            return;  // 够了
        }

        log.info("当前 {} 个物种，补充到 15 个...", speciesCount);

        // 已有的物种 ID 列表 (3 + 新增的)
        // 从 4 开始插入 12 个
        insertSpeciesIfNotExists(4L, "金丝猴", "Golden Monkey", "Rhinopithecus roxellana",
            1L, "EN", 1,
            "金丝猴是中国特有的灵长类动物，因全身金色长毛而得名，生活在高海拔山林中。",
            "海拔1500-3300米的针阔混交林", "中国四川、甘肃、陕西、湖北",
            "金丝猴的鼻子向上翘，因此也被称为\"仰鼻猴\"。");

        insertSpeciesIfNotExists(5L, "藏羚羊", "Tibetan Antelope", "Pantholops hodgsonii",
            1L, "NT", 1,
            "藏羚羊栖息在青藏高原，其绒毛极为珍贵，曾因盗猎濒临灭绝。",
            "海拔3700-5500米的高寒草原", "中国西藏、青海、新疆",
            "藏羚羊奔跑速度可达每小时80公里，是高原上的短跑冠军。");

        insertSpeciesIfNotExists(6L, "亚洲象", "Asian Elephant", "Elephas maximus",
            1L, "EN", 1,
            "亚洲象是亚洲最大的陆地动物，在中国仅分布于云南西双版纳。",
            "热带雨林、季雨林", "中国云南、东南亚、南亚",
            "亚洲象的耳朵比非洲象小，且只有雄性有象牙。");

        insertSpeciesIfNotExists(7L, "朱鹮", "Crested Ibis", "Nipponia nippon",
            2L, "EN", 1,
            "朱鹮被誉为\"东方宝石\"，曾一度被认为已灭绝，后在陕西洋县被重新发现。",
            "湿地、水田、河流", "中国陕西洋县、日本",
            "朱鹮的羽毛在繁殖期会分泌一种色素，使身体变为灰黑色。");

        insertSpeciesIfNotExists(8L, "绿孔雀", "Green Peafowl", "Pavo muticus",
            2L, "EN", 1,
            "绿孔雀是国家一级保护动物，比蓝孔雀体型更大，羽毛更加绚丽。",
            "热带、亚热带常绿阔叶林", "中国云南、东南亚",
            "绿孔雀开屏时尾羽可达2米，上面有数百个闪耀的眼状斑纹。");

        insertSpeciesIfNotExists(9L, "雪鸮", "Snowy Owl", "Bubo scandiacus",
            2L, "VU", 1,
            "雪鸮生活在北极地区，全身雪白羽毛，以旅鼠为主要食物。",
            "北极冻原、开阔草地", "北极圈、加拿大、北欧",
            "雪鸮是《哈利·波特》中送信的猫头鹰海德薇的原型。");

        insertSpeciesIfNotExists(10L, "扬子鳄", "Chinese Alligator", "Alligator sinensis",
            3L, "CR", 1,
            "扬子鳄是中国特有的鳄鱼种类，也是世界上最濒危的鳄鱼之一。",
            "河流、湖泊、沼泽", "中国长江中下游、安徽、浙江",
            "扬子鳄是冷血动物，冬天会挖洞冬眠，长达半年不吃不喝。");

        insertSpeciesIfNotExists(11L, "绿海龟", "Green Sea Turtle", "Chelonia mydas",
            3L, "EN", 1,
            "绿海龟是最大的硬壳海龟，因体内脂肪呈绿色而得名。",
            "热带、亚热带海域", "全球热带海洋、中国南海",
            "绿海龟可以活到80岁以上，成年后每年会回到出生地沙滩产卵。");

        insertSpeciesIfNotExists(12L, "大鲵", "Chinese Giant Salamander", "Andrias davidianus",
            4L, "CR", 1,
            "大鲵俗称娃娃鱼，是现存最大的两栖动物，叫声似婴儿啼哭。",
            "山间溪流、岩洞", "中国中南部山区",
            "大鲵寿命可达50年以上，是世界上现存最大的两栖动物。");

        insertSpeciesIfNotExists(13L, "箭毒蛙", "Poison Dart Frog", "Dendrobatidae",
            4L, "LC", 1,
            "箭毒蛙是世界上最毒的蛙类之一，土著人用其毒素涂抹箭矢打猎。",
            "热带雨林", "中南美洲",
            "一只金色箭毒蛙的毒素足以杀死10个成年人。");

        insertSpeciesIfNotExists(14L, "中华鲟", "Chinese Sturgeon", "Acipenser sinensis",
            5L, "CR", 1,
            "中华鲟是洄游性鱼类，在长江上游产卵、大海中生长，被称为\"长江鱼王\"。",
            "长江中上游、沿海海域", "中国长江流域、东海、黄海",
            "中华鲟已在地球上生存了约1.4亿年，被称为\"水中大熊猫\"。");

        insertSpeciesIfNotExists(15L, "中华虎凤蝶", "Chinese Tiger Swallowtail", "Luehdorfia chinensis",
            6L, "VU", 1,
            "中华虎凤蝶是中国特有蝶类，翅膀上虎纹斑纹极具观赏价值。",
            "山区、丘陵林地", "中国中东部地区",
            "中华虎凤蝶一年只繁殖一代，幼虫以细辛植物为食。");

        log.info("物种已补充到 15 个");
    }

    // -- helper methods --

    private void insertCategory(Long id, String name, int sort, String desc) {
        SpeciesCategory c = new SpeciesCategory();
        c.setId(id); c.setName(name); c.setSortOrder(sort); c.setDescription(desc);
        categoryMapper.insert(c);
    }

    private void insertSpecies(Long id, String nameZh, String nameEn, String sci,
                               Long catId, String status, int pub,
                               String desc, String habitat, String dist, String facts) {
        SpeciesInfo s = new SpeciesInfo();
        s.setId(id); s.setNameZh(nameZh); s.setNameEn(nameEn);
        s.setNameScientific(sci); s.setCategoryId(catId);
        s.setConservationStatus(status); s.setStatus(pub);
        s.setDescription(desc); s.setHabitat(habitat);
        s.setDistribution(dist); s.setFunFacts(facts);
        speciesInfoMapper.insert(s);
    }

    private void insertSpeciesIfNotExists(Long id, String nameZh, String nameEn, String sci,
                               Long catId, String status, int pub,
                               String desc, String habitat, String dist, String facts) {
        if (speciesInfoMapper.selectById(id) == null) {
            insertSpecies(id, nameZh, nameEn, sci, catId, status, pub, desc, habitat, dist, facts);
            log.info("  + {}", nameZh);
        }
    }
}
