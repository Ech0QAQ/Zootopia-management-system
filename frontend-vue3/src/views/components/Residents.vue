<template>
  <div>
    <div class="card">
      <div class="title">居民管理</div>
      <div class="subtitle">仅可查看居民信息，支持按条件筛选和名字搜索。</div>
      <div class="form-row">
        <div class="form-field">
          <label>动物种类</label>
          <select v-model="filters.animalType">
            <option value="">全部</option>
            <option v-for="type in animalTypes" :key="type" :value="type">{{ type }}</option>
          </select>
        </div>
        <div class="form-field">
          <label>居住区域</label>
          <select v-model="filters.liveArea">
            <option value="">全部</option>
            <option v-for="area in areas" :key="area" :value="area">{{ area }}</option>
          </select>
        </div>
        <div class="form-field">
          <label>户籍所在地</label>
          <select v-model="filters.household">
            <option value="">全部</option>
            <option v-for="h in households" :key="h" :value="h">{{ h }}</option>
          </select>
        </div>
        <div class="form-field">
          <label>按姓名搜索</label>
          <input v-model="filters.name" placeholder="输入姓名..." />
        </div>
      </div>
      <button class="btn" @click="filterResidents">筛选</button>
      
      <div style="margin-top:16px;">
        <table style="width:100%;font-size:13px;border-collapse:collapse;text-align:center;">
          <thead>
            <tr>
              <th style="padding:8px;border-bottom:2px solid #e0e7f5;">姓名</th>
              <th style="padding:8px;border-bottom:2px solid #e0e7f5;">账号</th>
              <th style="padding:8px;border-bottom:2px solid #e0e7f5;">动物种类</th>
              <th style="padding:8px;border-bottom:2px solid #e0e7f5;">居住区域</th>
              <th style="padding:8px;border-bottom:2px solid #e0e7f5;">户籍所在地</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="r in filteredResidents" :key="r.id" style="border-bottom:1px solid #f1f1f1;">
              <td style="padding:8px;">{{ r.name }}</td>
              <td style="padding:8px;">{{ r.username }}</td>
              <td style="padding:8px;">{{ r.animalType }}</td>
              <td style="padding:8px;">{{ r.liveArea }}</td>
              <td style="padding:8px;">{{ r.household }}</td>
            </tr>
          </tbody>
        </table>
        <div v-if="!filteredResidents.length" style="font-size:12px;color:#999;padding:20px;text-align:center;">暂无符合条件的居民</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '@/utils/api'
import { ANIMAL_TYPES, AREAS, HOUSEHOLDS } from '@/utils/constants'

const animalTypes = ANIMAL_TYPES
const areas = AREAS
const households = HOUSEHOLDS
const residents = ref([])
const filters = ref({
  animalType: '',
  liveArea: '',
  household: '',
  name: ''
})

const filteredResidents = computed(() => {
  return residents.value.filter(r => {
    const matchAnimal = !filters.value.animalType || r.animalType === filters.value.animalType
    const matchArea = !filters.value.liveArea || r.liveArea === filters.value.liveArea
    const matchHousehold = !filters.value.household || r.household === filters.value.household
    const matchName = !filters.value.name || (r.name || '').toLowerCase().includes(filters.value.name.toLowerCase())
    return matchAnimal && matchArea && matchHousehold && matchName
  })
})

onMounted(async () => {
  await loadResidents()
})

async function loadResidents() {
  try {
    const res = await api.get('/admin/users/residents')
    residents.value = res.data.data
  } catch (error) {
    console.error('加载失败:', error)
  }
}

function filterResidents() {
  // 计算属性自动处理筛选
}
</script>
