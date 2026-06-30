package com.species.controller;

import com.species.crawler.CrawlJob;
import com.species.crawler.SpeciesCrawler;
import com.species.util.Result;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 爬虫管理 Controller
 *
 * 提供爬虫的启动和状态查询接口（仅管理员可用）
 */
@RestController
@RequestMapping("/api/crawler")
public class CrawlerController {

    private final SpeciesCrawler speciesCrawler;

    public CrawlerController(SpeciesCrawler speciesCrawler) {
        this.speciesCrawler = speciesCrawler;
    }

    /**
     * 启动爬取任务
     * POST /api/crawler/start
     * 异步执行，返回 jobId，前端轮询 status 接口获取进度
     */
    @PostMapping("/start")
    public Result<?> start() {
        CrawlJob job = speciesCrawler.startCrawl();
        return Result.success(Map.of(
                "jobId", job.getJobId(),
                "total", job.getTotal()
        ));
    }

    /**
     * 查询爬取任务状态
     * GET /api/crawler/status/{jobId}
     */
    @GetMapping("/status/{jobId}")
    public Result<?> status(@PathVariable String jobId) {
        CrawlJob job = speciesCrawler.getJob(jobId);
        if (job == null) {
            return Result.error(404, "任务不存在");
        }
        return Result.success(Map.of(
                "jobId", job.getJobId(),
                "status", job.getStatus(),
                "total", job.getTotal(),
                "success", job.getSuccess(),
                "failed", job.getFailed(),
                "skipped", job.getSkipped(),
                "logs", job.getLogs()
        ));
    }

    /**
     * 获取种子数据统计
     * GET /api/crawler/seeds
     */
    @GetMapping("/seeds")
    public Result<?> seeds() {
        return Result.success(speciesCrawler.getSeedStats());
    }

    /**
     * 批量清除爬虫添加的数据
     * DELETE /api/crawler/clear
     * 只删除种子列表中的物种且 id > 15（保留原始 15 条手写数据）
     */
    @DeleteMapping("/clear")
    public Result<?> clear() {
        int deleted = speciesCrawler.clearCrawlerData();
        return Result.success("已删除 " + deleted + " 条爬取数据");
    }
}
