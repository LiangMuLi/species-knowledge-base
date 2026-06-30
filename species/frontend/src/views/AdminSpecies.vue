<template>
  <div class="admin-species">
    <div class="page-header">
      <h1 class="page-title">物种管理</h1>
      <div class="header-actions">
        <el-button
          v-if="selectedIds.length > 0"
          type="danger"
          :disabled="loading"
          @click="handleBatchDelete"
        >
          批量删除（{{ selectedIds.length }}）
        </el-button>
        <el-button type="primary" @click="openDialog()">+ 新增物种</el-button>
      </div>
    </div>

    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-bar">
      <el-form :inline="true" :model="searchForm" @keyup.enter="handleSearch">
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="搜索中文名、学名" clearable style="width: 240px" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="searchForm.categoryId" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 物种表格 -->
    <el-card shadow="never" style="margin-top: 16px">
      <el-table
        :data="speciesList" v-loading="loading" stripe style="width: 100%"
        @selection-change="onSelectionChange"
      >
        <el-table-column type="selection" width="45" />
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="nameZh" label="中文名" min-width="120" />
        <el-table-column prop="nameEn" label="英文名" min-width="130" />
        <el-table-column prop="nameScientific" label="学名" min-width="150">
          <template #default="{ row }">
            <span class="scientific-name">{{ row.nameScientific }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="conservationStatus" label="保护级别" width="90">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.conservationStatus)" size="small">{{ row.conservationStatus || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="70">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑物种' : '新增物种'"
      width="700px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" label-position="top">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="中文名" prop="nameZh">
              <el-input v-model="form.nameZh" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="英文名" prop="nameEn">
              <el-input v-model="form.nameEn" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="学名" prop="nameScientific">
              <el-input v-model="form.nameScientific" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="分类" prop="categoryId">
              <el-select v-model="form.categoryId" placeholder="选择分类" style="width: 100%">
                <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="保护级别" prop="conservationStatus">
              <el-select v-model="form.conservationStatus" placeholder="选择" style="width: 100%" clearable>
                <el-option label="极危 CR" value="CR" />
                <el-option label="濒危 EN" value="EN" />
                <el-option label="易危 VU" value="VU" />
                <el-option label="近危 NT" value="NT" />
                <el-option label="无危 LC" value="LC" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio :value="1">已发布</el-radio>
                <el-radio :value="0">草稿</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="别名">
          <el-input v-model="form.alias" placeholder="逗号分隔多个别名" />
        </el-form-item>

        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="栖息地">
              <el-input v-model="form.habitat" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分布区域">
              <el-input v-model="form.distribution" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="体重">
              <el-input v-model="form.weight" placeholder="如: 100-200kg" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="寿命">
              <el-input v-model="form.lifespan" placeholder="如: 20-30年" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="食性">
              <el-input v-model="form.diet" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="繁殖方式">
          <el-input v-model="form.reproduction" />
        </el-form-item>

        <el-form-item label="趣味知识">
          <el-input v-model="form.funFacts" type="textarea" :rows="2" />
        </el-form-item>

        <el-form-item label="特有物种">
          <el-switch v-model="form.isEndemic" :active-value="1" :inactive-value="0" />
        </el-form-item>

        <!-- 图片上传 -->
        <el-form-item label="图片">
          <div class="upload-wrapper">
            <el-upload
              :action="uploadUrl"
              :headers="uploadHeaders"
              :show-file-list="false"
              :on-success="handleUploadSuccess"
              :before-upload="beforeUpload"
              accept="image/*"
            >
              <el-button type="primary" :loading="uploading">上传图片</el-button>
              <template #tip>
                <span class="upload-tip">支持 JPG/PNG/GIF，最大 10MB</span>
              </template>
            </el-upload>
            <img v-if="form.imageUrl" :src="form.imageUrl" class="preview-img" @click="form.imageUrl = ''" title="点击移除" />
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { speciesApi } from '../api/species'
import { useUserStore } from '../store/user'

const userStore = useUserStore()

// 列表
const speciesList = ref([])
const categories = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const selectedIds = ref([])

const searchForm = ref({ keyword: '', categoryId: null })

// 弹窗
const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const formRef = ref(null)
const uploadUrl = '/api/upload/image'
const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${userStore.token}`
}))
const uploading = ref(false)

const defaultForm = {
  nameZh: '', nameEn: '', nameScientific: '', alias: '',
  categoryId: null, description: '', habitat: '', distribution: '',
  conservationStatus: null, weight: '', lifespan: '', diet: '',
  reproduction: '', funFacts: '', imageUrl: '',
  isEndemic: 0, status: 1
}

const form = reactive({ ...defaultForm })

const rules = {
  nameZh: [{ required: true, message: '请输入中文名', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }]
}

onMounted(() => {
  fetchCategories()
  fetchData()
})

async function fetchData() {
  loading.value = true
  try {
    // 管理员可以看到所有状态（包括草稿），默认按 ID 降序
    const res = await speciesApi.adminList({
      page: page.value,
      size: size.value,
      sortBy: 'id',
      sortOrder: 'desc',
      ...searchForm.value
    })
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

function handleSearch() { page.value = 1; fetchData() }
function handleReset() {
  searchForm.value = { keyword: '', categoryId: null }
  page.value = 1; fetchData()
}

function openDialog(row) {
  isEdit.value = !!row
  if (row) {
    Object.assign(form, { ...defaultForm, ...row })
  } else {
    Object.assign(form, { ...defaultForm })
  }
  dialogVisible.value = true
}

async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    if (isEdit.value) {
      await speciesApi.update(form.id, form)
      ElMessage.success('更新成功')
    } else {
      await speciesApi.create(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch {
    // 错误已在 request.js 中处理
  } finally {
    saving.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除「${row.nameZh}」吗？`, '确认删除', { type: 'warning' })
    await speciesApi.delete(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch {
    // 取消或错误
  }
}

function onSelectionChange(rows) {
  selectedIds.value = rows.map(r => r.id)
}

async function handleBatchDelete() {
  const count = selectedIds.value.length
  try {
    await ElMessageBox.confirm(
      `确定删除选中的 ${count} 个物种吗？此操作不可撤销。`,
      '批量删除',
      { confirmButtonText: '确认删除', confirmButtonClass: 'el-button--danger', type: 'warning' }
    )
    await speciesApi.batchDelete(selectedIds.value)
    ElMessage.success(`已删除 ${count} 条记录`)
    selectedIds.value = []
    fetchData()
  } catch {
    // 取消或错误
  }
}

function beforeUpload(file) {
  const validTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']
  if (!validTypes.includes(file.type)) {
    ElMessage.error('仅支持 JPG/PNG/GIF/WebP 格式')
    return false
  }
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.error('图片不能超过 10MB')
    return false
  }
  uploading.value = true
  return true
}

function handleUploadSuccess(res) {
  uploading.value = false
  if (res.code === 200) {
    form.imageUrl = res.data.url
    ElMessage.success('上传成功')
  } else {
    ElMessage.error(res.msg || '上传失败')
  }
}

function statusTagType(status) {
  const map = { CR: 'danger', EN: 'danger', VU: 'warning', NT: 'info', LC: 'success' }
  return map[status] || 'info'
}
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.page-title {
  font-size: 22px;
  color: #303133;
}

.header-actions {
  display: flex;
  gap: 8px;
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

.upload-wrapper {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.preview-img {
  width: 120px;
  height: 90px;
  object-fit: cover;
  border-radius: 4px;
  cursor: pointer;
  border: 1px solid #e4e7ed;
}

.upload-tip {
  font-size: 12px;
  color: #909399;
  margin-left: 8px;
}
</style>
