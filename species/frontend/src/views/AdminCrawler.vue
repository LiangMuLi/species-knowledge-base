<template>
  <div class="admin-crawler">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="page-title">数据爬虫</h1>
      <div class="header-actions">
        <el-button
          v-if="currentJobId"
          @click="fetchStatus"
          :disabled="isRunning"
        >
          刷新状态
        </el-button>
        <el-button
          type="danger"
          plain
          @click="handleClear"
          :disabled="isRunning"
        >
          清空爬取数据
        </el-button>
        <el-button
          type="primary"
          @click="startCrawl"
          :loading="isRunning"
          :disabled="isRunning"
        >
          {{ isRunning ? '爬取中...' : '开始爬取' }}
        </el-button>
      </div>
    </div>

    <!-- 未开始时的介绍 -->
    <el-card v-if="!currentJobId" shadow="never" class="welcome-card">
      <div class="welcome-content">
        <el-empty description="还没有爬取任务">
          <p class="welcome-desc">
            点击「开始爬取」自动导入预置的完整物种数据。<br>
            每次导入 <strong>10 条</strong>新物种，覆盖哺乳动物、鸟类、爬行动物三大分类。
          </p>
          <p class="welcome-note">
            数据来源于预先编审的完整资料，每条都包含学名、描述、栖息地等全部字段。<br>
            已存在的物种会自动跳过，可多次点击直到导入全部 <strong>50+ 条</strong>。
          </p>
        </el-empty>
      </div>
    </el-card>

    <!-- 任务状态卡片 -->
    <el-row v-if="currentJobId" :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-item">
            <div class="stat-label">总计</div>
            <div class="stat-value">{{ jobData?.total ?? 0 }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-item">
            <div class="stat-label">成功</div>
            <div class="stat-value stat-success">{{ jobData?.success ?? 0 }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-item">
            <div class="stat-label">跳过（已存在）</div>
            <div class="stat-value stat-warning">{{ jobData?.skipped ?? 0 }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-item">
            <div class="stat-label">失败</div>
            <div class="stat-value stat-danger">{{ jobData?.failed ?? 0 }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 进度条 -->
    <el-card v-if="currentJobId" shadow="never" style="margin-top: 16px">
      <div class="progress-header">
        <span>爬取进度</span>
        <span class="progress-count">（{{ (jobData?.success || 0) + (jobData?.failed || 0) }} / {{ CRAWL_TARGET }} 条）</span>
        <el-tag v-if="isRunning" type="warning" size="small" effect="dark">运行中</el-tag>
        <el-tag v-else-if="jobData?.status === 'completed'" type="success" size="small" effect="dark">已完成</el-tag>
        <el-tag v-else-if="jobData?.status === 'failed'" type="danger" size="small" effect="dark">失败</el-tag>
      </div>
      <el-progress
        :percentage="progressPercent"
        :status="progressStatus"
        :stroke-width="16"
        :text-inside="true"
        style="margin-top: 12px"
      />
    </el-card>

    <!-- 运行日志 -->
    <el-card v-if="currentJobId" shadow="never" class="log-card">
      <template #header>
        <span>运行日志</span>
        <span class="log-count">（{{ jobData?.logs?.length ?? 0 }} 条）</span>
      </template>
      <div class="log-area" ref="logAreaRef">
        <div v-if="!jobData?.logs?.length" class="log-empty">
          等待爬虫启动...
        </div>
        <div
          v-for="(line, i) in jobData?.logs"
          :key="i"
          class="log-line"
          :class="logLineClass(line)"
        >
          {{ line }}
        </div>
      </div>
    </el-card>

    <!-- 完成提示 -->
    <el-alert
      v-if="jobData?.status === 'completed'"
      type="success"
      show-icon
      class="complete-alert"
    >
      <template #title>
        爬取完成！
        成功 <strong>{{ jobData.success }}</strong> 条，
        跳过 <strong>{{ jobData.skipped }}</strong> 条，
        失败 <strong>{{ jobData.failed }}</strong> 条。
        <el-button type="success" size="small" style="margin-left: 12px" @click="goToSpecies">
          查看物种列表
        </el-button>
      </template>
    </el-alert>
  </div>
</template>

<script setup>
import { ref, computed, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { crawlerApi } from '../api/crawler'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()

const currentJobId = ref('')
const jobData = ref(null)
const isRunning = ref(false)
const logAreaRef = ref(null)
let pollTimer = null

/** 单次爬取目标条数（与后端 CRAWL_TARGET 一致） */
const CRAWL_TARGET = 10

/** 计算进度百分比（基于成功+失败，不含跳过） */
const progressPercent = computed(() => {
  if (!jobData.value) return 0
  const done = (jobData.value.success || 0) + (jobData.value.failed || 0)
  return Math.min(Math.round((done / CRAWL_TARGET) * 100), 100)
})

/** 进度条状态 */
const progressStatus = computed(() => {
  if (jobData.value?.status === 'completed') return 'success'
  if (jobData.value?.status === 'failed') return 'exception'
  return ''
})

/** 启动爬取 */
async function startCrawl() {
  try {
    isRunning.value = true
    jobData.value = null
    const res = await crawlerApi.start()
    currentJobId.value = res.data.jobId

    // 立即拉一次状态
    await fetchStatus()
    // 启动轮询（每 2 秒）
    pollTimer = setInterval(fetchStatus, 2000)
  } catch (e) {
    isRunning.value = false
    ElMessage.error('启动爬取失败')
  }
}

/** 拉取任务状态 */
async function fetchStatus() {
  if (!currentJobId.value) return
  try {
    const res = await crawlerApi.status(currentJobId.value)
    jobData.value = res.data

    // 滚动日志到底部
    await nextTick()
    if (logAreaRef.value) {
      logAreaRef.value.scrollTop = logAreaRef.value.scrollHeight
    }

    // 任务结束 → 停止轮询
    if (res.data.status === 'completed' || res.data.status === 'failed') {
      isRunning.value = false
      stopPolling()
    }
  } catch {
    // 忽略单个请求失败
  }
}

/** 停止轮询 */
function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

/** 日志行样式类 */
function logLineClass(line) {
  if (!line) return ''
  if (line.startsWith('✅')) return 'log-success'
  if (line.startsWith('⏭️')) return 'log-skip'
  if (line.startsWith('⚠️')) return 'log-warning'
  if (line.startsWith('❌')) return 'log-error'
  return ''
}

/** 跳转到物种列表 */
function goToSpecies() {
  router.push('/admin/species')
}

/** 清空爬取数据 */
async function handleClear() {
  try {
    await ElMessageBox.confirm(
      '确定清空所有爬虫添加的物种数据吗？<br>原始 15 条数据不受影响，此操作不可撤销。',
      '确认清空',
      { confirmButtonText: '确认清空', cancelButtonText: '取消', type: 'warning', dangerouslyUseHTMLString: true }
    )
    const res = await crawlerApi.clear()
    ElMessage.success(res.msg || '已清空')
    // 重置页面状态
    currentJobId.value = ''
    jobData.value = null
  } catch {
    // 取消操作
  }
}

// 组件销毁时清理定时器
onUnmounted(() => {
  stopPolling()
})
</script>

<style scoped>
.admin-crawler {
  max-width: 960px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.page-title {
  font-size: 22px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 8px;
}

/* 欢迎/介绍卡片 */
.welcome-card {
  margin-top: 8px;
}

.welcome-content {
  text-align: center;
  padding: 20px 0;
}

.welcome-desc {
  color: var(--el-text-color-secondary);
  line-height: 1.8;
  margin: 12px 0;
}

.welcome-note {
  font-size: 13px;
  color: var(--el-text-color-placeholder);
  line-height: 1.8;
  margin: 8px 0;
}

/* 统计卡片 */
.stats-row {
  margin-bottom: 0 !important;
}

.stat-card {
  --el-card-padding: 16px;
}

.stat-item {
  text-align: center;
}

.stat-label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin-bottom: 6px;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: var(--el-text-color-primary);
}

.stat-value.stat-success {
  color: var(--el-color-success);
}

.stat-value.stat-warning {
  color: var(--el-color-warning);
}

.stat-value.stat-danger {
  color: var(--el-color-danger);
}

/* 进度条 */
.progress-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: var(--el-text-color-primary);
}

.progress-count {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}

/* 日志区域 */
.log-card {
  margin-top: 16px;
}

.log-count {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
  margin-left: 4px;
}

.log-area {
  height: 360px;
  overflow-y: auto;
  background: #1e1e1e;
  border-radius: 6px;
  padding: 12px 16px;
  font-family: 'Menlo', 'Monaco', 'Consolas', monospace;
  font-size: 13px;
  line-height: 1.7;
}

.log-empty {
  color: #888;
  text-align: center;
  padding: 60px 0;
  font-family: inherit;
}

.log-line {
  color: #d4d4d4;
  white-space: pre-wrap;
  word-break: break-all;
}

.log-line.log-success {
  color: #6a9955;
}

.log-line.log-skip {
  color: #dcdcaa;
}

.log-line.log-warning {
  color: #ce9178;
}

.log-line.log-error {
  color: #f44747;
}

/* 完成提示 */
.complete-alert {
  margin-top: 16px;
}

.complete-alert strong {
  font-weight: 700;
}
</style>
