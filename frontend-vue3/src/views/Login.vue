<template>
  <div class="auth-wrapper">
    <div class="card">
      <div class="title">动物城管理系统</div>
      <div class="subtitle">欢迎来到疯狂动物城，请选择登录或注册身份进入系统</div>
      
      <div class="nav-tabs">
        <button 
          class="nav-tab" 
          :class="{ active: activeTab === 'login' }"
          @click="activeTab = 'login'"
        >
          登录
        </button>
        <button 
          class="nav-tab" 
          :class="{ active: activeTab === 'register-resident' }"
          @click="activeTab = 'register-resident'"
        >
          注册居民
        </button>
        <button 
          class="nav-tab" 
          :class="{ active: activeTab === 'register-worker' }"
          @click="activeTab = 'register-worker'"
        >
          注册工作人员
        </button>
      </div>

      <!-- 登录表单 -->
      <div v-if="activeTab === 'login'">
        <div id="auth-alert"></div>
        <div class="form-field">
          <label>账号</label>
          <input v-model="loginForm.username" id="login-username" />
        </div>
        <div class="form-field">
          <label>密码</label>
          <input v-model="loginForm.password" id="login-password" type="password" @keyup.enter="handleLogin" />
        </div>
        <div class="btn-group">
          <button class="btn" @click="handleLogin" :disabled="loading">登录</button>
          <button class="btn secondary" @click="fillAdmin">使用管理员账号</button>
        </div>
      </div>

      <!-- 注册表单 -->
      <div v-if="activeTab === 'register-resident' || activeTab === 'register-worker'">
        <div id="auth-alert"></div>
        <div class="form-row">
          <div class="form-field">
            <label>账号</label>
            <input v-model="registerForm.username" id="reg-username" />
          </div>
          <div class="form-field">
            <label>密码</label>
            <input v-model="registerForm.password" id="reg-password" type="password" />
          </div>
        </div>
        <div class="form-row">
          <div class="form-field">
            <label>姓名</label>
            <input v-model="registerForm.name" id="reg-name" />
          </div>
          <div class="form-field">
            <label>年龄</label>
            <input v-model.number="registerForm.age" id="reg-age" type="number" />
          </div>
        </div>
        <div class="form-row">
          <div class="form-field">
            <label>性别</label>
            <select v-model="registerForm.gender" id="reg-gender">
              <option value="">请选择</option>
              <option value="男">男</option>
              <option value="女">女</option>
            </select>
          </div>
          <div class="form-field">
            <label>动物种类</label>
            <select v-model="registerForm.animalType" id="reg-animal">
              <option value="">请选择</option>
              <option v-for="type in animalTypes" :key="type" :value="type">{{ type }}</option>
            </select>
          </div>
        </div>
        <!-- 居民特有字段 -->
        <template v-if="activeTab === 'register-resident'">
          <div class="form-row">
            <div class="form-field">
              <label>居住区域</label>
              <select v-model="registerForm.liveArea" id="reg-live-area">
                <option value="">请选择</option>
                <option v-for="area in areas" :key="area" :value="area">{{ area }}</option>
              </select>
            </div>
            <div class="form-field">
              <label>户籍所在地</label>
              <select v-model="registerForm.household" id="reg-household">
                <option value="">请选择</option>
                <option v-for="h in households" :key="h" :value="h">{{ h }}</option>
              </select>
            </div>
          </div>
        </template>

        <!-- 工作人员特有字段 -->
        <template v-if="activeTab === 'register-worker'">
          <div class="form-row">
            <div class="form-field">
              <label>工作区域</label>
              <select v-model="registerForm.workArea" id="reg-work-area">
                <option value="">请选择</option>
                <option v-for="area in areas" :key="area" :value="area">{{ area }}</option>
              </select>
            </div>
            <div class="form-field">
              <label>所属部门</label>
              <select v-model="registerForm.department" id="reg-dept">
                <option value="">请选择</option>
                <option v-for="dept in departments" :key="dept" :value="dept">{{ dept }}</option>
              </select>
            </div>
          </div>
        </template>

        <button class="btn" @click="handleRegister" :disabled="loading">
          {{ activeTab === 'register-worker' ? '提交工作人员注册（待管理员审批）' : '注册为居民' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { ANIMAL_TYPES, AREAS, HOUSEHOLDS, DEPARTMENTS } from '@/utils/constants'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('login')
const loading = ref(false)

const loginForm = ref({
  username: '',
  password: ''
})

const registerForm = ref({
  username: '',
  password: '',
  name: '',
  age: null,
  gender: '',
  animalType: '',
  liveArea: '',
  household: '',
  workArea: '',
  department: ''
})

const animalTypes = ANIMAL_TYPES
const areas = AREAS
const households = HOUSEHOLDS
const departments = DEPARTMENTS

function fillAdmin() {
  loginForm.value.username = 'admin'
  loginForm.value.password = '123456'
}

async function handleLogin() {
  const alertEl = document.getElementById('auth-alert')
  if (alertEl) alertEl.innerHTML = ''
  
  if (!loginForm.value.username || !loginForm.value.password) {
    if (alertEl) {
      alertEl.innerHTML = '<div class="alert alert-error">请输入用户名和密码</div>'
    } else {
      ElMessage.error('请输入用户名和密码')
    }
    return
  }
  
  loading.value = true
  try {
    await userStore.login(loginForm.value.username, loginForm.value.password)
    router.push('/main')
  } catch (error) {
    if (alertEl) {
      alertEl.innerHTML = `<div class="alert alert-error">${error.message || '登录失败'}</div>`
    } else {
      ElMessage.error(error.message || '登录失败')
    }
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  const alertEl = document.getElementById('auth-alert')
  if (alertEl) alertEl.innerHTML = ''
  
  if (!registerForm.value.username || !registerForm.value.password || !registerForm.value.name) {
    if (alertEl) {
      alertEl.innerHTML = '<div class="alert alert-error">请填写必填项</div>'
    } else {
      ElMessage.error('请填写必填项')
    }
    return
  }
  
  const payload = {
    ...registerForm.value,
    role: activeTab.value === 'register-worker' ? 'worker' : 'resident'
  }
  
  loading.value = true
  try {
    await userStore.register(payload)
    if (alertEl) {
      alertEl.innerHTML = `<div class="alert alert-success">${activeTab.value === 'register-worker' ? '注册成功，请等待管理员审批' : '注册成功，请登录'}</div>`
    } else {
      ElMessage.success(activeTab.value === 'register-worker' ? '注册成功，请等待管理员审批' : '注册成功，请登录')
    }
    activeTab.value = 'login'
    loginForm.value.username = registerForm.value.username
  } catch (error) {
    if (alertEl) {
      alertEl.innerHTML = `<div class="alert alert-error">${error.message || '注册失败'}</div>`
    } else {
      ElMessage.error(error.message || '注册失败')
    }
  } finally {
    loading.value = false
  }
}
</script>

