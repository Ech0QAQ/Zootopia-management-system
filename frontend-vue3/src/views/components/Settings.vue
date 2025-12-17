<template>
  <div>
    <div class="card">
      <div class="title">个人信息</div>
      <div class="subtitle">以下信息为只读，不可修改</div>
      <div style="margin-top:16px;">
        <div style="padding:8px 0;border-bottom:1px solid #f1f1f1;">
          <div style="font-size:13px;color:#666;">用户名</div>
          <div style="font-size:15px;margin-top:4px;">{{ profile.username || '-' }}</div>
        </div>
        <div style="padding:8px 0;border-bottom:1px solid #f1f1f1;">
          <div style="font-size:13px;color:#666;">姓名</div>
          <div style="font-size:15px;margin-top:4px;">{{ profile.name || '-' }}</div>
        </div>
        <div style="padding:8px 0;border-bottom:1px solid #f1f1f1;">
          <div style="font-size:13px;color:#666;">年龄</div>
          <div style="font-size:15px;margin-top:4px;">{{ profile.age || '-' }}</div>
        </div>
        <div style="padding:8px 0;border-bottom:1px solid #f1f1f1;">
          <div style="font-size:13px;color:#666;">性别</div>
          <div style="font-size:15px;margin-top:4px;">{{ profile.gender || '-' }}</div>
        </div>
        <div style="padding:8px 0;border-bottom:1px solid #f1f1f1;">
          <div style="font-size:13px;color:#666;">物种</div>
          <div style="font-size:15px;margin-top:4px;">{{ profile.animalType || profile.animal_type || '-' }}</div>
        </div>
        <div style="padding:8px 0;border-bottom:1px solid #f1f1f1;">
          <div style="font-size:13px;color:#666;">居住区域</div>
          <div style="font-size:15px;margin-top:4px;">{{ profile.liveArea || profile.live_area || '-' }}</div>
        </div>
        <div style="padding:8px 0;">
          <div style="font-size:13px;color:#666;">户籍所在地</div>
          <div style="font-size:15px;margin-top:4px;">{{ profile.household || '-' }}</div>
        </div>
      </div>
    </div>

    <div class="card">
      <div class="title">修改信息</div>
      <div class="subtitle">可以修改居住区域和户籍所在地</div>
      <div v-if="editAlert" :class="'alert ' + editAlert.type">{{ editAlert.message }}</div>
      <div class="form-row" style="margin-top:16px;">
        <div class="form-field">
          <label>居住区域</label>
          <select v-model="editForm.liveArea">
            <option value="">请选择</option>
            <option v-for="area in areas" :key="area" :value="area">{{ area }}</option>
          </select>
        </div>
        <div class="form-field">
          <label>户籍所在地</label>
          <select v-model="editForm.household">
            <option value="">请选择</option>
            <option v-for="h in households" :key="h" :value="h">{{ h }}</option>
          </select>
        </div>
      </div>
      <button class="btn" style="margin-top:16px;" @click="saveProfile">保存修改</button>
    </div>

    <div class="card">
      <div class="title">修改密码</div>
      <div class="subtitle">修改密码需要先验证当前密码</div>
      <div v-if="passwordAlert" :class="'alert ' + passwordAlert.type">{{ passwordAlert.message }}</div>
      <div class="form-field" style="margin-top:16px;">
        <label>当前密码</label>
        <input v-model="passwordForm.currentPassword" type="password" />
      </div>
      <div class="form-field">
        <label>新密码</label>
        <input v-model="passwordForm.newPassword" type="password" />
      </div>
      <div class="form-field">
        <label>确认新密码</label>
        <input v-model="passwordForm.confirmPassword" type="password" />
      </div>
      <button class="btn" style="margin-top:16px;" @click="changePassword">修改密码</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '@/utils/api'
import { AREAS, HOUSEHOLDS } from '@/utils/constants'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const areas = AREAS
const households = HOUSEHOLDS
const profile = ref({})
const editForm = ref({
  liveArea: '',
  household: ''
})
const passwordForm = ref({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})
const editAlert = ref(null)
const passwordAlert = ref(null)

onMounted(async () => {
  await loadProfile()
})

async function loadProfile() {
  try {
    const res = await api.get('/me')
    profile.value = res.data.data
    editForm.value.liveArea = profile.value.liveArea || profile.value.live_area || ''
    editForm.value.household = profile.value.household || ''
  } catch (error) {
    console.error('加载失败:', error)
  }
}

async function saveProfile() {
  editAlert.value = null
  try {
    await api.put('/resident/profile', {
      live_area: editForm.value.liveArea || null,
      household: editForm.value.household || null
    })
    editAlert.value = { type: 'alert-success', message: '修改成功！' }
    profile.value.liveArea = editForm.value.liveArea
    profile.value.live_area = editForm.value.liveArea
    profile.value.household = editForm.value.household
    // 刷新用户信息
    await userStore.refreshUser()
  } catch (error) {
    editAlert.value = { type: 'alert-error', message: error.response?.data?.message || '修改失败' }
  }
}

async function changePassword() {
  passwordAlert.value = null
  const { currentPassword, newPassword, confirmPassword } = passwordForm.value
  
  if (!currentPassword || !newPassword || !confirmPassword) {
    passwordAlert.value = { type: 'alert-error', message: '请填写所有密码字段' }
    return
  }
  if (newPassword !== confirmPassword) {
    passwordAlert.value = { type: 'alert-error', message: '两次输入的新密码不一致' }
    return
  }
  
  try {
    await api.put('/resident/password', { currentPassword, newPassword })
    passwordAlert.value = { type: 'alert-success', message: '密码修改成功！' }
    passwordForm.value = {
      currentPassword: '',
      newPassword: '',
      confirmPassword: ''
    }
  } catch (error) {
    passwordAlert.value = { type: 'alert-error', message: error.response?.data?.message || '修改失败' }
  }
}
</script>
