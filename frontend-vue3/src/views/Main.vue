<template>
  <div id="app">
    <EmergencyBanner :broadcast="emergencyBroadcast" @close="emergencyBroadcast = null" />
    
    <div class="card" :style="{ marginTop: emergencyBroadcast ? '60px' : '0' }">
      <div class="title">欢迎，{{ userStore.user?.name || userStore.user?.username }}</div>
      <div class="subtitle">
        当前身份：{{ userStore.user?.role === 'admin' ? '管理员' : userStore.user?.role === 'worker' ? '工作人员' : '居民' }}
        <span v-if="userStore.user?.role === 'worker'" class="tag">
          工作状态：{{ userStore.user?.employmentStatus || userStore.user?.employment_status || '未知' }}
        </span>
      </div>
      <button class="btn secondary" @click="handleLogout">退出登录</button>
    </div>
    
    <!-- 待审批员工显示提示信息 -->
    <div v-if="isPendingApproval" class="card" style="margin-top: 20px; text-align: center; padding: 40px;">
      <div style="font-size: 24px; color: #d33; margin-bottom: 20px;">⚠️</div>
      <div style="font-size: 18px; color: #666; font-weight: 600;">暂无权限！请等待管理员的审批！</div>
    </div>
    
    <!-- 正常用户显示功能菜单和内容 -->
    <div v-else class="layout">
      <!-- 侧边栏菜单 -->
      <div class="sidebar">
        <div class="sidebar-title">功能菜单</div>
        <div 
          v-for="menu in menus" 
          :key="menu.key"
          class="menu-item"
          :class="{ active: currentMenu === menu.key }"
          @click="currentMenu = menu.key"
        >
          {{ menu.label }}
        </div>
      </div>

      <!-- 主内容区 -->
      <div class="content">
        <!-- 管理员/工作人员页面 -->
        <WorkerAdmin v-if="currentMenu === 'staff' && userStore.user?.role === 'admin'" />
        <WorkerSelf v-else-if="currentMenu === 'staff' && userStore.user?.role === 'worker'" />
        <Announcements v-else-if="currentMenu === 'announcement'" />
        <TicketsWork v-else-if="currentMenu === 'tickets-admin'" />
        <Rail v-else-if="currentMenu === 'rail'" />
        <Residents v-else-if="currentMenu === 'residents'" />
        <MallAdmin v-else-if="currentMenu === 'mall-admin'" />
        <OrderAdmin v-else-if="currentMenu === 'order-admin'" />
        <Sales v-else-if="currentMenu === 'sales'" />
        
        <!-- 居民页面 -->
        <Complain v-else-if="currentMenu === 'complain'" />
        <Star v-else-if="currentMenu === 'star'" />
        <MyOrders v-else-if="currentMenu === 'my-orders'" />
        <Shop v-else-if="currentMenu === 'shop'" />
        <ShopOrders v-else-if="currentMenu === 'shop-orders'" />
        <Settings v-else-if="currentMenu === 'settings'" />
        
        <div v-else>请选择功能菜单</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import WorkerAdmin from './components/WorkerAdmin.vue'
import WorkerSelf from './components/WorkerSelf.vue'
import Announcements from './components/Announcements.vue'
import TicketsWork from './components/TicketsWork.vue'
import Rail from './components/Rail.vue'
import Residents from './components/Residents.vue'
import MallAdmin from './components/MallAdmin.vue'
import OrderAdmin from './components/OrderAdmin.vue'
import Sales from './components/Sales.vue'
import Complain from './components/Complain.vue'
import Star from './components/Star.vue'
import MyOrders from './components/MyOrders.vue'
import Shop from './components/Shop.vue'
import ShopOrders from './components/ShopOrders.vue'
import Settings from './components/Settings.vue'
import EmergencyBanner from '@/components/EmergencyBanner.vue'
import api from '@/utils/api'
import { bufferToString } from '@/utils/constants'

const router = useRouter()
const userStore = useUserStore()

const currentMenu = ref('')
const emergencyBroadcast = ref(null)

// 检查是否是待审批状态
const isPendingApproval = computed(() => {
  if (userStore.user?.role !== 'worker') {
    return false
  }
  const status = userStore.user?.employmentStatus || userStore.user?.employment_status
  return status === '待审批'
})

onMounted(async () => {
  // 刷新用户信息，确保状态是最新的
  await userStore.refreshUser()
  // 默认显示第一个菜单
  if (menus.value.length > 0) {
    currentMenu.value = menus.value[0].key
  }
  // 检查紧急广播
  await checkEmergencyBroadcast()
  
  // 监听页面可见性变化，当页面重新可见时刷新用户信息
  document.addEventListener('visibilitychange', async () => {
    if (!document.hidden) {
      await userStore.refreshUser()
    }
  })
  
  // 监听窗口焦点变化，当窗口重新获得焦点时刷新用户信息
  window.addEventListener('focus', async () => {
    await userStore.refreshUser()
  })
})

async function checkEmergencyBroadcast() {
  try {
    const res = await api.get('/announcements')
    const announcements = res.data.data || []
    const emergency = announcements.find(a => a.emergency && !a.expired)
    if (emergency) {
      emergencyBroadcast.value = {
        title: bufferToString(emergency.title),
        content: bufferToString(emergency.content),
        expired: false
      }
    }
  } catch (e) {
    console.error('检查紧急广播失败:', e)
  }
}

// 根据用户角色显示不同菜单
const menus = computed(() => {
  const role = userStore.user?.role
  if (role === 'admin' || role === 'worker') {
    return [
      { key: 'staff', label: '员工管理' },
      { key: 'announcement', label: '市政公告' },
      { key: 'tickets-admin', label: '工单系统' },
      { key: 'rail', label: '铁路32106' },
      { key: 'residents', label: '居民管理' },
      { key: 'mall-admin', label: '商城管理' },
      { key: 'order-admin', label: '订单管理' },
      ...(role === 'admin' ? [{ key: 'sales', label: '销售额分析' }] : [])
    ]
  } else {
    return [
      { key: 'announcement', label: '市政公告' },
      { key: 'complain', label: '接诉即办' },
      { key: 'star', label: '动物城之星' },
      { key: 'rail', label: '铁路32106' },
      { key: 'my-orders', label: '我的车票' },
      { key: 'shop', label: '动物商城' },
      { key: 'shop-orders', label: '我的订单' },
      { key: 'settings', label: '设置' }
    ]
  }
})


function handleLogout() {
  userStore.clearAuth()
  router.push('/login')
}
</script>

