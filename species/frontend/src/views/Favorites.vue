<template>
  <div class="favorites">
    <h1 class="page-title">我的收藏</h1>

    <el-card shadow="never">
      <el-table
        :data="favorites"
        v-loading="loading"
        stripe
        style="width: 100%"
        @row-click="goToDetail"
      >
        <el-table-column prop="nameZh" label="中文名" min-width="140" />
        <el-table-column prop="nameEn" label="英文名" min-width="160" />
        <el-table-column prop="nameScientific" label="学名" min-width="180">
          <template #default="{ row }">
            <span class="scientific-name">{{ row.nameScientific }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="conservationStatus" label="保护级别" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.conservationStatus)" size="small">
              {{ row.conservationStatus || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button
              type="danger"
              size="small"
              link
              @click.stop="removeFavorite(row.id)"
            >
              取消收藏
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && favorites.length === 0" description="还没有收藏任何物种" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { favoriteApi } from '../api/favorite'

const router = useRouter()
const favorites = ref([])
const loading = ref(true)

onMounted(fetchData)

async function fetchData() {
  loading.value = true
  try {
    const res = await favoriteApi.list()
    favorites.value = res.data || []
  } catch {
    favorites.value = []
  } finally {
    loading.value = false
  }
}

async function removeFavorite(speciesId) {
  try {
    await favoriteApi.remove(speciesId)
    favorites.value = favorites.value.filter(f => f.id !== speciesId)
    ElMessage.success('已取消收藏')
  } catch {
    // 错误已在 request.js 中处理
  }
}

function goToDetail(row) {
  router.push(`/species/${row.id}`)
}

function statusTagType(status) {
  const map = { CR: 'danger', EN: 'danger', VU: 'warning', NT: 'info', LC: 'success' }
  return map[status] || 'info'
}
</script>

<style scoped>
.page-title {
  font-size: 22px;
  margin-bottom: 16px;
  color: #303133;
}

.scientific-name {
  font-style: italic;
  color: #606266;
}

.el-table {
  cursor: pointer;
}
</style>
