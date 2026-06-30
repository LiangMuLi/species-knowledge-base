<template>
  <div class="species-list">
    <h1 class="page-title">物种数据库</h1>

    <!-- 搜索栏（关键词自动补全） -->
    <el-card class="search-bar" shadow="never">
      <el-form :inline="true" :model="searchForm" @keyup.enter="handleSearch">
        <el-form-item label="关键词">
          <el-autocomplete
            v-model="searchForm.keyword"
            :fetch-suggestions="querySuggestions"
            :trigger-on-focus="false"
            placeholder="搜索中文名、英文名、学名"
            clearable
            style="width: 280px"
            @select="handleSelectSuggestion"
          >
            <template #default="{ item }">
              <div class="suggestion-item">
                <span class="sug-name">{{ item.nameZh }}</span>
                <span class="sug-en">{{ item.nameEn }}</span>
                <el-tag v-if="item.conservationStatus" size="small" style="margin-left: auto">
                  {{ item.conservationStatus }}
                </el-tag>
              </div>
            </template>
          </el-autocomplete>
        </el-form-item>

        <el-form-item label="分类">
          <el-select v-model="searchForm.categoryId" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>

        <!-- 高级筛选切换 -->
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button text @click="showAdvanced = !showAdvanced">
            {{ showAdvanced ? '收起' : '高级' }}筛选
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 高级筛选面板 -->
      <el-collapse-transition>
        <div v-show="showAdvanced" class="advanced-filters">
          <el-form :inline="true" :model="searchForm">
            <el-form-item label="保护级别">
              <el-select v-model="searchForm.conservationStatus" placeholder="全部" clearable style="width: 120px">
                <el-option label="极危 CR" value="CR" />
                <el-option label="濒危 EN" value="EN" />
                <el-option label="易危 VU" value="VU" />
                <el-option label="近危 NT" value="NT" />
                <el-option label="无危 LC" value="LC" />
              </el-select>
            </el-form-item>
            <el-form-item label="栖息地">
              <el-input v-model="searchForm.habitat" placeholder="如: 森林、湿地" style="width: 160px" clearable />
            </el-form-item>
          </el-form>
        </div>
      </el-collapse-transition>
    </el-card>

    <!-- 物种列表 -->
    <el-card shadow="never" style="margin-top: 16px">
      <el-table :data="speciesList" v-loading="loading" stripe style="width: 100%" @row-click="goToDetail">
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
        <el-table-column prop="habitat" label="栖息地" min-width="160" show-overflow-tooltip />
        <el-table-column label="图片" width="80" v-if="hasImages">
          <template #default="{ row }">
            <el-image v-if="row.imageUrl" :src="row.imageUrl" style="width: 40px; height: 40px; border-radius: 4px" fit="cover" />
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" width="100">
          <template #default="{ row }">
            {{ row.updatedAt ? row.updatedAt.substring(0, 10) : '-' }}
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="page" v-model:page-size="size"
          :total="total" :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="fetchData" @current-change="fetchData"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { speciesApi } from '../api/species'

const router = useRouter()

const speciesList = ref([])
const categories = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const showAdvanced = ref(false)

const searchForm = ref({
  keyword: '',
  categoryId: null,
  conservationStatus: null,
  habitat: ''
})

onMounted(() => {
  fetchCategories()
  fetchData()
})

async function fetchData() {
  loading.value = true
  try {
    const params = {
      page: page.value,
      size: size.value,
      keyword: searchForm.value.keyword || undefined,
      categoryId: searchForm.value.categoryId || undefined,
      conservationStatus: searchForm.value.conservationStatus || undefined,
      habitat: searchForm.value.habitat || undefined
    }
    const res = await speciesApi.list(params)
    speciesList.value = res.data.records || []
    total.value = res.data.total || 0
  } catch {
    speciesList.value = []
  } finally {
    loading.value = false
  }
}

async function fetchCategories() {
  try {
    const res = await speciesApi.getCategories()
    categories.value = res.data || []
  } catch {
    categories.value = []
  }
}

// 搜索建议
const suggestTimer = ref(null)

async function querySuggestions(query, cb) {
  if (!query || query.length < 1) {
    cb([])
    return
  }
  try {
    const res = await speciesApi.suggestions(query)
    cb((res.data || []).map(item => ({ ...item, value: item.nameZh })))
  } catch {
    cb([])
  }
}

function handleSelectSuggestion(item) {
  searchForm.value.keyword = item.nameZh
  handleSearch()
}

function handleSearch() {
  page.value = 1
  fetchData()
}

function handleReset() {
  searchForm.value = {
    keyword: '',
    categoryId: null,
    conservationStatus: null,
    habitat: ''
  }
  page.value = 1
  fetchData()
}

function goToDetail(row) {
  router.push(`/species/${row.id}`)
}

function statusTagType(status) {
  const map = { CR: 'danger', EN: 'danger', VU: 'warning', NT: 'info', LC: 'success' }
  return map[status] || 'info'
}

const hasImages = computed(() => speciesList.value.some(s => !!s.imageUrl))
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

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.el-table {
  cursor: pointer;
}

.advanced-filters {
  padding-top: 8px;
  border-top: 1px solid #ebeef5;
  margin-top: 4px;
}

.suggestion-item {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.sug-name {
  font-weight: 500;
  color: #303133;
}

.sug-en {
  font-size: 12px;
  color: #909399;
}
</style>
