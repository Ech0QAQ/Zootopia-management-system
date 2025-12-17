<template>
  <div>
    <div class="card">
      <div class="title">工单系统</div>
      <div class="subtitle">处理居民在"接诉即办"中的留言。</div>
      <div>
        <div v-for="msg in messages" :key="msg.id" style="border-bottom:1px solid #f1f1f1;padding:6px 0;font-size:13px;">
          <div><strong>{{ msg.userName }}</strong>：{{ msg.content }}</div>
          <div style="color:#999;">当前状态：{{ msg.status }}{{ msg.status === '已解决' && msg.score == null ? '（待居民评价）' : '' }}{{ msg.score != null ? ` | 评分：${'★'.repeat(msg.score)}` : '' }}</div>
          <div class="form-field" style="margin-top:12px;">
            <label>回复内容（保存不提交，仅内部草稿）</label>
            <textarea
              v-model="replyInputs[msg.id]"
              rows="3"
              :placeholder="'请输入回复内容...'"
              :disabled="msg.status === '已解决'"
            ></textarea>
          </div>
          <div class="btn-group">
            <button
              v-if="msg.status === '待受理'"
              class="btn"
              @click="acceptMessage(msg.id)"
            >
              受理
            </button>
            <template v-if="msg.status === '处理中'">
              <button class="btn secondary" @click="saveReply(msg.id)">保存答复</button>
              <button class="btn" @click="submitReply(msg.id)">提交答复（标记已解决）</button>
            </template>
          </div>
        </div>
        <div v-if="!messages.length" style="font-size:12px;color:#999;">暂无工单</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '@/utils/api'
import { bufferToString } from '@/utils/constants'

const messages = ref([])
const replyInputs = ref({})

onMounted(async () => {
  await loadMessages()
})

async function loadMessages() {
  try {
    const res = await api.get('/messages/all')
    messages.value = res.data.data.map(m => ({
      ...m,
      userName: bufferToString(m.userName),
      content: bufferToString(m.content),
      reply: bufferToString(m.reply)
    }))
    messages.value.forEach(m => {
      replyInputs.value[m.id] = m.reply || ''
    })
  } catch (error) {
    console.error('加载失败:', error)
  }
}

async function acceptMessage(id) {
  try {
    await api.post(`/messages/${id}/accept`)
    await loadMessages()
  } catch (error) {
    alert(error.response?.data?.message || '操作失败')
  }
}

async function saveReply(id) {
  try {
    await api.post(`/messages/${id}/save`, { reply: replyInputs.value[id] || '' })
    alert('已保存草稿')
  } catch (error) {
    alert(error.response?.data?.message || '保存失败')
  }
}

async function submitReply(id) {
  try {
    await api.post(`/messages/${id}/submit`, { reply: replyInputs.value[id] || '' })
    await loadMessages()
  } catch (error) {
    alert(error.response?.data?.message || '提交失败')
  }
}
</script>
