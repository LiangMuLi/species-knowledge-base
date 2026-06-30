<template>
  <div class="admin-users">
    <h1 class="page-title">用户管理</h1>

    <el-card shadow="never">
      <el-table :data="users" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="nickname" label="昵称" min-width="120" />
        <el-table-column prop="role" label="角色" width="80">
          <template #default="{ row }">
            <el-tag :type="row.role === 'admin' ? 'danger' : 'info'" size="small">
              {{ row.role === 'admin' ? '管理员' : '用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="注册时间" width="110">
          <template #default="{ row }">
            {{ row.createdAt ? row.createdAt.substring(0, 10) : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openResetDialog(row)">重置密码</el-button>
            <el-button
              :type="row.status === 1 ? 'warning' : 'success'"
              size="small"
              @click="toggleStatus(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 重置密码弹窗 -->
    <el-dialog v-model="dialogVisible" title="重置密码" width="400px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="0">
        <p style="margin-bottom: 12px; color: #606266;">
          重置用户 <strong>{{ targetUser?.username }}</strong> 的密码：
        </p>
        <el-form-item prop="newPassword">
          <el-input
            v-model="form.newPassword"
            type="password"
            placeholder="输入新密码（至少 6 位）"
            show-password
          />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="确认新密码"
            show-password
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="resetting" @click="handleResetPassword">确认重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '../api/admin'

const users = ref([])
const loading = ref(true)
const dialogVisible = ref(false)
const resetting = ref(false)
const targetUser = ref(null)
const formRef = ref(null)

const form = ref({ newPassword: '', confirmPassword: '' })

const rules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== form.value.newPassword) {
          callback(new Error('两次密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

onMounted(fetchUsers)

async function fetchUsers() {
  loading.value = true
  try {
    const res = await adminApi.getUsers()
    users.value = res.data || []
  } catch {
    users.value = []
  } finally {
    loading.value = false
  }
}

function openResetDialog(user) {
  targetUser.value = user
  form.value = { newPassword: '', confirmPassword: '' }
  dialogVisible.value = true
}

async function handleResetPassword() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  resetting.value = true
  try {
    await adminApi.resetPassword(targetUser.value.id, form.value.newPassword)
    ElMessage.success('密码已重置')
    dialogVisible.value = false
  } catch {
    // 错误已在 request.js 处理
  } finally {
    resetting.value = false
  }
}

async function toggleStatus(row) {
  const action = row.status === 1 ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确定${action}用户「${row.username}」吗？`, '提示', {
      type: 'warning'
    })
    const newStatus = row.status === 1 ? 0 : 1
    await adminApi.toggleStatus(row.id, newStatus)
    ElMessage.success(`${action}成功`)
    fetchUsers()
  } catch {
    // 取消或错误
  }
}
</script>

<style scoped>
.page-title {
  font-size: 22px;
  margin-bottom: 16px;
  color: #303133;
}
</style>
