<template>
  <div class="csp-input-area">
    <el-input v-model="inputText" placeholder="输入回复..." @keyup.enter="handleSend" :disabled="!wsConnected || disabled" />
    <el-button type="primary" @click="handleSend" :disabled="!wsConnected || disabled || !inputText.trim()">发送</el-button>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const props = defineProps<{
  wsConnected: boolean
  disabled: boolean
}>()

const emit = defineEmits<{
  (e: 'send-message', text: string): void
}>()

const inputText = ref('')

function handleSend() {
  const text = inputText.value.trim()
  if (!text || !props.wsConnected) return
  emit('send-message', text)
  inputText.value = ''
}
</script>

<style scoped>
.csp-input-area { display: flex; gap: 10px; padding: 12px 20px; border-top: 1px solid #eee; flex-shrink: 0; }
.csp-input-area .el-input { flex: 1; }
</style>