import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'ThreadPool',
    component: () => import('@/views/ThreadPool.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
