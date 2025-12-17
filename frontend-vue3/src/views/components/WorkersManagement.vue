<template>
  <div>
    <h2>员工管理</h2>
    <el-table :data="workers" style="width: 100%">
      <el-table-column prop="name" label="姓名" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="workArea" label="工作区域" />
      <el-table-column prop="department" label="部门" />
      <el-table-column prop="salary" label="薪资" />
      <el-table-column prop="employmentStatus" label="状态" />
      <el-table-column label="操作">
        <template #default="scope">
          <el-button size="small" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleFire(scope.row)">开除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '@/utils/api'
import { ElMessage } from 'element-plus'

const workers = ref([])

onMounted(async () => {
  try {
    const res = await api.get('/admin/workers')
    workers.value = res.data.data
  } catch (error) {
    ElMessage.error('加载失败')
  }
})

function handleEdit(row) {
  // 编辑功能
}

function handleFire(row) {
  // 开除功能
}
</script>

