import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../store/user'

/**
 * 路由配置
 *
 * 路由守卫逻辑：
 * 1. 用户访问 /login → 如果已登录则跳转首页
 * 2. 用户访问其他页 → 如果未登录则跳转登录页
 */
const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('../views/Layout.vue'),
    meta: { requiresAuth: true },
    redirect: '/species',
    children: [
      {
        path: 'species',
        name: 'SpeciesList',
        component: () => import('../views/SpeciesList.vue'),
        meta: { title: '物种列表' }
      },
      {
        path: 'species/:id',
        name: 'SpeciesDetail',
        component: () => import('../views/SpeciesDetail.vue'),
        meta: { title: '物种详情' }
      },
      {
        path: 'favorites',
        name: 'Favorites',
        component: () => import('../views/Favorites.vue'),
        meta: { title: '我的收藏' }
      },
      {
        path: 'admin/species',
        name: 'AdminSpecies',
        component: () => import('../views/AdminSpecies.vue'),
        meta: { title: '物种管理', requiresAdmin: true }
      },
      {
        path: 'admin/users',
        name: 'AdminUsers',
        component: () => import('../views/AdminUsers.vue'),
        meta: { title: '用户管理', requiresAdmin: true }
      },
      {
        path: 'admin/crawler',
        name: 'AdminCrawler',
        component: () => import('../views/AdminCrawler.vue'),
        meta: { title: '数据爬虫', requiresAdmin: true }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

/**
 * 路由守卫：检查登录状态 + 管理员权限
 * 每次路由切换前执行
 */
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()

  if (to.path === '/login' || to.path === '/register') {
    // 已登录用户访问登录/注册页 → 跳首页
    if (userStore.isLoggedIn) {
      next('/species')
    } else {
      next()
    }
  } else {
    // 未登录访问其他页 → 跳登录页
    if (!userStore.isLoggedIn) {
      next('/login')
    } else {
      // 检查管理员权限
      if (to.meta.requiresAdmin && !userStore.isAdmin) {
        next('/species')
      } else {
        next()
      }
    }
  }
})

export default router
