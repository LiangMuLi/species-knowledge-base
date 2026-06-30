<template>
  <div class="layout">
    <!-- 顶部导航栏 -->
    <nav class="navbar">
      <div class="nav-left">
        <router-link to="/" class="nav-logo">🌿 物种知识库</router-link>
        <router-link to="/species" class="nav-link">物种列表</router-link>
        <router-link to="/favorites" class="nav-link">我的收藏</router-link>
        <el-dropdown v-if="userStore.isAdmin" class="admin-dropdown">
          <span class="nav-link admin-link">
            管理后台<el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="router.push('/admin/species')">物种管理</el-dropdown-item>
              <el-dropdown-item @click="router.push('/admin/users')">用户管理</el-dropdown-item>
              <el-dropdown-item @click="router.push('/admin/crawler')">数据爬虫</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
      <div class="nav-right">
        <span class="nav-user">
          {{ userStore.userInfo?.nickname || userStore.userInfo?.username }}
        </span>
        <el-button type="danger" size="small" plain @click="handleLogout">
          退出登录
        </el-button>
      </div>
    </nav>

    <!-- 主体内容 -->
    <main class="main-content">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { ArrowDown } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../store/user'

const router = useRouter()
const userStore = useUserStore()

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定退出登录吗？', '提示')
    userStore.logout()
    ElMessage.success('已退出')
    router.push('/login')
  } catch {
    // 取消操作
  }
}
</script>

<style scoped>
.layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.navbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  height: 60px;
  background-color: #fff;
  border-bottom: 1px solid #e4e7ed;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
}

.nav-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.nav-logo {
  font-size: 18px;
  font-weight: bold;
  color: #409eff;
  text-decoration: none;
}

.nav-link {
  font-size: 14px;
  color: #606266;
  text-decoration: none;
}

.nav-link:hover {
  color: #409eff;
}

.admin-link {
  color: #e6a23c;
  font-weight: 500;
  cursor: pointer;
}

.admin-link:hover {
  color: #f56c6c;
}

.admin-dropdown {
  height: 60px;
  display: flex;
  align-items: center;
}

.nav-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.nav-user {
  color: #606266;
  font-size: 14px;
}

.main-content {
  flex: 1;
  padding: 20px;
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
}
</style>
