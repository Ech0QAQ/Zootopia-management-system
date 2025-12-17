<template>
  <div>
    <div class="card">
      <div class="title">接诉即办</div>
      <div class="subtitle">您可以在这里留言建议或投诉，工作人员会尽快处理。</div>
      <div class="form-field">
        <label>留言内容</label>
        <textarea v-model="messageForm.content" rows="3"></textarea>
      </div>
      <button class="btn" @click="submitMessage">提交留言</button>
    </div>
    <div class="card">
      <div class="subtitle">我的留言</div>
      <div>
        <div v-for="msg in messages" :key="msg.id" style="border-bottom:1px solid #f1f1f1;padding:6px 0;font-size:13px;">
          <div>{{ msg.content }}</div>
          <div style="color:#999;">状态：{{ msg.status }}{{ msg.status === '已解决' && msg.score == null ? '（待评价）' : '' }}</div>
          <div v-if="msg.reply" style="color:#555;">回复：{{ msg.reply }}</div>
          <div v-if="msg.status === '已解决' && msg.score == null" class="star-rating" :data-id="msg.id">
            评分：
            <span
              v-for="i in 5"
              :key="i"
              class="star inactive"
              :class="{ inactive: hoveredScore[msg.id] !== i && selectedScore[msg.id] !== i }"
              :data-score="i"
              :data-id="msg.id"
              @mouseenter="handleStarHover(msg.id, i)"
              @mouseleave="handleStarLeave(msg.id)"
              @click="submitScore(msg.id, i)"
            >★</span>
          </div>
          <div v-if="msg.score != null">我的评分：{{ '★'.repeat(msg.score) }}</div>
        </div>
        <div v-if="!messages.length" style="font-size:12px;color:#999;">暂无留言</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '@/utils/api'
import { bufferToString } from '@/utils/constants'

const messages = ref([])
const messageForm = ref({ content: '' })
const hoveredScore = ref({})
const selectedScore = ref({})

onMounted(async () => {
  await loadMessages()
})

async function loadMessages() {
  try {
    const res = await api.get('/messages/my')
    messages.value = res.data.data.map(m => ({
      ...m,
      content: bufferToString(m.content),
      reply: bufferToString(m.reply)
    }))
  } catch (error) {
    console.error('加载失败:', error)
  }
}

async function submitMessage() {
  const text = messageForm.value.content?.trim()
  if (!text) return
  try {
    await api.post('/messages', { content: text })
    messageForm.value.content = ''
    await loadMessages()
  } catch (error) {
    alert(error.response?.data?.message || '提交失败')
  }
}

function handleStarHover(msgId, score) {
  hoveredScore.value[msgId] = score
}

function handleStarLeave(msgId) {
  delete hoveredScore.value[msgId]
}

async function submitScore(msgId, score) {
  try {
    await api.post(`/messages/${msgId}/score`, { score })
    selectedScore.value[msgId] = score
    await loadMessages()
  } catch (error) {
    alert(error.response?.data?.message || '评分失败')
  }
}
</script>

