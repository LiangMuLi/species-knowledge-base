<template>
  <div class="species-detail">
    <div class="detail-nav">
      <el-button text @click="router.back()">
        ← 返回列表
      </el-button>
      <el-button
        :type="isFavorited ? 'warning' : 'default'"
        :icon="isFavorited ? StarFilled : Star"
        :loading="favLoading"
        @click="toggleFavorite"
      >
        {{ isFavorited ? '已收藏' : '收藏' }}
      </el-button>
    </div>

    <div v-loading="loading">
      <!-- 基本信息卡片 -->
      <el-card shadow="never" v-if="species">
        <div class="detail-header">
          <h1>{{ species.nameZh }}</h1>
          <el-tag v-if="species.conservationStatus" :type="statusTagType">
            {{ statusLabel }}
          </el-tag>
        </div>

        <div class="scientific-line">
          <span v-if="species.nameEn" class="en-name">{{ species.nameEn }}</span>
          <span v-if="species.nameScientific" class="sciname">
            {{ species.nameScientific }}
          </span>
        </div>

        <el-divider />

        <el-descriptions :column="2" border>
          <el-descriptions-item label="别名" v-if="species.alias">
            {{ species.alias }}
          </el-descriptions-item>
          <el-descriptions-item label="分类">
            {{ categoryName }}
          </el-descriptions-item>
          <el-descriptions-item label="栖息地">
            {{ species.habitat || '未知' }}
          </el-descriptions-item>
          <el-descriptions-item label="分布区域">
            {{ species.distribution || '未知' }}
          </el-descriptions-item>
          <el-descriptions-item label="体重">
            {{ species.weight || '未知' }}
          </el-descriptions-item>
          <el-descriptions-item label="寿命">
            {{ species.lifespan || '未知' }}
          </el-descriptions-item>
          <el-descriptions-item label="食性">
            {{ species.diet || '未知' }}
          </el-descriptions-item>
          <el-descriptions-item label="繁殖方式">
            {{ species.reproduction || '未知' }}
          </el-descriptions-item>
          <el-descriptions-item label="特有物种" v-if="species.isEndemic">
            <el-tag size="small" type="warning">是</el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <!-- 描述 -->
        <el-divider />
        <h3>物种描述</h3>
        <p class="description-text">{{ species.description || '暂无描述' }}</p>

        <!-- 趣味知识 -->
        <div v-if="species.funFacts" class="fun-facts">
          <el-divider />
          <h3>💡 趣味知识</h3>
          <p>{{ species.funFacts }}</p>
        </div>

        <!-- 评论区 -->
        <el-divider />
        <div class="comments-section">
          <h3>💬 评论 ({{ comments.length }})</h3>

          <!-- 发表评论 -->
          <div class="comment-form">
            <el-input
              v-model="newComment"
              type="textarea"
              :rows="2"
              placeholder="写下你的想法..."
              maxlength="500"
              show-word-limit
            />
            <div class="comment-form-actions">
              <div class="rating-wrapper">
                <span class="rating-label">评分：</span>
                <el-rate v-model="newRating" :max="5" />
              </div>
              <el-button
                type="primary"
                size="small"
                :loading="commentLoading"
                @click="submitComment"
              >发表评论</el-button>
            </div>
          </div>

          <!-- 评论列表 -->
          <div v-if="comments.length > 0" class="comments-list">
            <div v-for="c in comments" :key="c.id" class="comment-item">
              <div class="comment-header">
                <strong>{{ c.nickname }}</strong>
                <span class="comment-time">{{ formatTime(c.createdAt) }}</span>
                <el-button
                  v-if="c.canDelete"
                  type="danger"
                  size="small"
                  link
                  @click="deleteComment(c.id)"
                >删除</el-button>
              </div>
              <div class="comment-body">{{ c.content }}</div>
              <div v-if="c.rating" class="comment-rating">
                <el-rate v-model="c.rating" disabled :max="5" size="small" />
              </div>
            </div>
          </div>
          <div v-else class="no-comments">暂无评论，来发表第一条吧</div>
        </div>
      </el-card>

      <el-empty v-if="!loading && !species" description="物种不存在" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Star, StarFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { speciesApi } from '../api/species'
import { favoriteApi } from '../api/favorite'
import { commentApi } from '../api/comment'

const route = useRoute()
const router = useRouter()

const species = ref(null)
const loading = ref(true)
const categoryName = ref('')
const isFavorited = ref(false)
const favLoading = ref(false)

// 评论相关
const comments = ref([])
const newComment = ref('')
const newRating = ref(0)
const commentLoading = ref(false)

onMounted(async () => {
  try {
    const res = await speciesApi.getById(route.params.id)
    species.value = res.data

    // 获取分类名称
    if (res.data?.categoryId) {
      try {
        const catRes = await speciesApi.getCategories()
        const cat = (catRes.data || []).find(c => c.id === res.data.categoryId)
        categoryName.value = cat?.name || '未知'
      } catch {
        categoryName.value = '未知'
      }
    }

    // 检查是否已收藏
    checkFavorite()
    // 加载评论
    loadComments()
  } catch {
    species.value = null
  } finally {
    loading.value = false
  }
})

async function checkFavorite() {
  try {
    const res = await favoriteApi.getIds()
    isFavorited.value = (res.data || []).includes(Number(route.params.id))
  } catch {
    // 未登录时忽略
  }
}

async function toggleFavorite() {
  favLoading.value = true
  try {
    if (isFavorited.value) {
      await favoriteApi.remove(route.params.id)
      isFavorited.value = false
      ElMessage.success('已取消收藏')
    } else {
      await favoriteApi.add(route.params.id)
      isFavorited.value = true
      ElMessage.success('收藏成功')
    }
  } catch {
    // 错误已在 request.js 中处理
  } finally {
    favLoading.value = false
  }
}

// ===== 评论功能 =====
async function loadComments() {
  try {
    const res = await commentApi.list(route.params.id)
    comments.value = (res.data?.comments || []).map(c => ({
      ...c,
      canDelete: true  // 前端简化处理，后端会校验权限
    }))
  } catch {
    comments.value = []
  }
}

async function submitComment() {
  const text = newComment.value.trim()
  if (!text) {
    ElMessage.warning('请输入评论内容')
    return
  }

  commentLoading.value = true
  try {
    await commentApi.add(route.params.id, text, newRating.value || null)
    ElMessage.success('评论成功')
    newComment.value = ''
    newRating.value = 0
    loadComments()
  } catch {
    // 错误已在 request.js 中处理
  } finally {
    commentLoading.value = false
  }
}

async function deleteComment(id) {
  try {
    await commentApi.delete(id)
    ElMessage.success('删除成功')
    loadComments()
  } catch {
    // 错误已在 request.js 中处理
  }
}

function formatTime(t) {
  if (!t) return ''
  return t.substring(0, 16).replace('T', ' ')
}

const statusLabel = computed(() => {
  const map = {
    CR: '极危 CR',
    EN: '濒危 EN',
    VU: '易危 VU',
    NT: '近危 NT',
    LC: '无危 LC'
  }
  return map[species.value?.conservationStatus] || species.value?.conservationStatus
})

const statusTagType = computed(() => {
  const map = { CR: 'danger', EN: 'danger', VU: 'warning', NT: 'info', LC: 'success' }
  return map[species.value?.conservationStatus] || 'info'
})
</script>

<style scoped>
.species-detail {
  max-width: 900px;
  margin: 0 auto;
}

.detail-nav {
  display: flex;
  justify-content: space-between;
  margin-bottom: 16px;
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 8px;
}

.detail-header h1 {
  font-size: 28px;
  color: #303133;
}

.scientific-line {
  display: flex;
  gap: 16px;
  color: #909399;
  font-size: 14px;
}

.en-name {
  color: #606266;
}

.sciname {
  font-style: italic;
}

.description-text {
  line-height: 1.8;
  color: #606266;
  font-size: 15px;
}

.fun-facts {
  background: #fdf6ec;
  border-radius: 8px;
  padding: 4px 16px 16px;
  margin-top: 8px;
}

/* 评论区 */
.comments-section {
  margin-top: 16px;
}

.comment-form {
  margin: 16px 0;
}

.comment-form-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}

.rating-wrapper {
  display: flex;
  align-items: center;
  gap: 4px;
}

.rating-label {
  font-size: 13px;
  color: #606266;
}

.comments-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.comment-item {
  padding: 12px 16px;
  background: #f8f9fa;
  border-radius: 8px;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.comment-time {
  font-size: 12px;
  color: #909399;
}

.comment-body {
  font-size: 14px;
  color: #303133;
  line-height: 1.6;
}

.no-comments {
  text-align: center;
  color: #909399;
  padding: 20px;
  font-size: 14px;
}
</style>
