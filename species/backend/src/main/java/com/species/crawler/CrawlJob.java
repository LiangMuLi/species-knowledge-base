package com.species.crawler;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 爬虫任务状态
 *
 * 记录一次爬取任务的进度和结果，线程安全
 */
public class CrawlJob {

    private final String jobId;
    private volatile String status = "running"; // running / completed / failed
    private int total;
    private int success;
    private int failed;
    private int skipped;
    private final List<String> logs = new ArrayList<>();
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public CrawlJob(String jobId) {
        this.jobId = jobId;
        this.startTime = LocalDateTime.now();
    }

    public synchronized void addLog(String message) {
        logs.add(message);
    }

    public void complete() {
        this.status = "completed";
        this.endTime = LocalDateTime.now();
    }

    public void fail(String reason) {
        this.status = "failed";
        this.endTime = LocalDateTime.now();
        addLog("❌ 任务失败: " + reason);
    }

    // === getters & setters ===

    public String getJobId() { return jobId; }
    public String getStatus() { return status; }
    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    public int getSuccess() { return success; }
    public synchronized void incrementSuccess() { this.success++; }
    public int getFailed() { return failed; }
    public synchronized void incrementFailed() { this.failed++; }
    public int getSkipped() { return skipped; }
    public synchronized void incrementSkipped() { this.skipped++; }

    public synchronized List<String> getLogs() {
        return new ArrayList<>(logs);
    }

    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
}
