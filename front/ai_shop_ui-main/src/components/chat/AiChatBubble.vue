<template>
  <div class="ai-chat-bubble" @click="$emit('click')">
    <div class="bubble-icon">🤖</div>
    <div class="bubble-text"><slot /></div>
  </div>
</template>

<script setup lang="ts">
defineEmits<{ click: [] }>()
</script>

<style scoped>
.ai-chat-bubble {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  background: #e8f0fe;
  border-radius: 16px;
  cursor: pointer;
  font-size: 13px;
  color: #667eea;
  transition: all 0.2s;
}
.ai-chat-bubble:hover {
  background: #667eea;
  color: #fff;
}
.bubble-icon { font-size: 16px; }
.bubble-text { line-height: 1.4; }
</style>