<template>
  <div v-if="broadcast" class="emergency-banner" :class="{ expired: broadcast.expired }">
    <div class="banner-content">
      <div class="banner-title">
        <span class="banner-icon">🚨</span>
        <strong>紧急广播：{{ broadcast.title }}</strong>
        <el-button
          v-if="!broadcast.expired"
          type="text"
          size="small"
          @click="$emit('close')"
          style="margin-left: auto; color: #fff"
        >
          ✕
        </el-button>
      </div>
      <div class="banner-text">{{ broadcast.content }}</div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  broadcast: {
    type: Object,
    default: null
  }
})

defineEmits(['close'])
</script>

<style scoped>
.emergency-banner {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  background: linear-gradient(135deg, #ff4d4f, #ff7875);
  color: #fff;
  z-index: 10000;
  box-shadow: 0 4px 12px rgba(255, 77, 79, 0.3);
  animation: slideDown 0.3s ease-out;
}

.emergency-banner.expired {
  background: linear-gradient(135deg, #999, #bbb);
}

.banner-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 12px 20px;
}

.banner-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 4px;
}

.banner-icon {
  font-size: 20px;
}

.banner-text {
  font-size: 14px;
  line-height: 1.5;
  white-space: pre-wrap;
}

@keyframes slideDown {
  from {
    transform: translateY(-100%);
  }
  to {
    transform: translateY(0);
  }
}
</style>

