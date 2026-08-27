<template>
  <div class="csp-main">
    <template v-if="session">
      <div class="csp-main-header">
        <span>与 {{ getUserLabel(session) }} 对话中</span>
        <el-button size="small" type="danger" plain @click="$emit('close-session')">结束会话</el-button>
      </div>
      <div class="csp-messages" ref="msgRef">
        <div
          v-for="(item, idx) in timedMessages"
          :key="idx"
        >
          <div v-if="item.type === 'time'" class="csp-time-separator">{{ item.label }}</div>
          <div v-else :class="['csp-msg', item.sendType === 2 ? 'self' : 'other']">
            <div class="csp-msg-avatar">{{ item.sendType === 2 ? '👩‍💼' : '👤' }}</div>
            <div class="csp-msg-body">
              <div class="csp-msg-bubble">{{ item.content }}</div>
            </div>
          </div>
        </div>
        <div v-if="messages.length === 0" class="csp-empty-msg">暂无消息</div>
      </div>
    </template>
    <div v-else class="csp-no-session">请选择会话</div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick } from 'vue'

interface SessionVO { id: number; userId: number; nickname: string; status: number; createTime: string; lastMessage?: string; unreadCount?: number }
interface MsgVO { id: number; sessionId: number; sendType: number; content: string; createTime: string }

const props = defineProps<{
  messages: MsgVO[]
  session: SessionVO | null
  wsConnected: boolean
}>()

defineEmits<{
  (e: 'close-session'): void
}>()

const msgRef = ref<HTMLElement | null>(null)

const timedMessages = computed(() => {
  const result: any[] = []
  for (let i = 0; i < props.messages.length; i++) {
    const msg = props.messages[i]
    const shouldShowTime = i === 0 || (() => {
      const prev = props.messages[i - 1]
      if (!prev.createTime || !msg.createTime) return true
      try {
        return new Date(msg.createTime).getTime() - new Date(prev.createTime).getTime() > 5 * 60 * 1000
      } catch { return true }
    })()
    if (shouldShowTime && msg.createTime) {
      result.push({ type: 'time', label: formatFullTime(msg.createTime) })
    }
    result.push({ type: 'msg', sendType: msg.sendType, content: msg.content })
  }
  return result
})

function getUserLabel(s: SessionVO): string {
  return s.nickname || '用户' + s.userId
}

function scrollToBottom() { nextTick(() => { if (msgRef.value) msgRef.value.scrollTop = msgRef.value.scrollHeight }) }

function formatFullTime(t: string): string {
  try {
    const d = new Date(t)
    if (isNaN(d.getTime())) return ''
    const pad = (n: number) => String(n).padStart(2, '0')
    const now = new Date()
    const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
    const date = new Date(d.getFullYear(), d.getMonth(), d.getDate())
    const diffDays = (today.getTime() - date.getTime()) / (24 * 60 * 60 * 1000)
    const time = `${pad(d.getHours())}:${pad(d.getMinutes())}`
    if (diffDays < 1) return time
    if (diffDays < 2) return `昨天 ${time}`
    if (diffDays < 7) return `${pad(d.getMonth() + 1)}/${pad(d.getDate())} ${time}`
    return `${d.getFullYear()}/${pad(d.getMonth() + 1)}/${pad(d.getDate())} ${time}`
  } catch { return '' }
}

defineExpose({ scrollToBottom })
</script>

<style scoped>
.csp-main { flex: 1 1 auto; display: flex; flex-direction: column; min-width: 0; min-height: 0; }
.csp-main-header { padding: 12px 20px; border-bottom: 1px solid #eee; display: flex; justify-content: space-between; align-items: center; font-size: 14px; font-weight: 600; flex-shrink: 0; }
.csp-messages { flex: 1 1 auto; overflow-y: auto; padding: 16px 20px; background: #f7f8fa; min-height: 0; }
.csp-msg { display: flex; width: 100%; gap: 8px; margin-bottom: 16px; align-items: flex-start; box-sizing: border-box; }
.csp-msg.other { justify-content: flex-start; flex-direction: row; }
.csp-msg.self { justify-content: flex-start; flex-direction: row-reverse; }
.csp-msg-avatar {
  width: 36px; height: 36px;
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 16px; flex-shrink: 0; background: #e8eaed;
  border: 1px solid #ddd; box-sizing: border-box;
}
.csp-msg-body { max-width: 70%; display: flex; flex-direction: column; min-width: 0; }
.csp-msg.self .csp-msg-body { align-items: flex-end; }
.csp-msg.other .csp-msg-body { align-items: flex-start; }

/* ── QQ 风格气泡 ── */
.csp-msg-bubble {
  position: relative; display: inline-block;
  padding: 10px 14px; border-radius: 12px;
  font-size: 13px; line-height: 1.5; word-break: break-word; white-space: pre-wrap;
  max-width: 100%; box-sizing: border-box;
}
.csp-msg.self .csp-msg-bubble {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  border-radius: 12px 4px 12px 12px;
}
.csp-msg.self .csp-msg-bubble::after {
  content: '';
  position: absolute; top: 12px; right: -6px;
  width: 0; height: 0;
  border: 6px solid transparent;
  border-left-color: #764ba2; border-right: 0;
}
.csp-msg.other .csp-msg-bubble {
  background: #fff; color: #333;
  border: 1px solid #e8e8e8;
  border-radius: 4px 12px 12px 12px;
}
.csp-msg.other .csp-msg-bubble::before {
  content: '';
  position: absolute; top: 12px; left: -7px;
  width: 0; height: 0;
  border: 6px solid transparent;
  border-right-color: #e8e8e8; border-left: 0;
}
.csp-msg.other .csp-msg-bubble::after {
  content: '';
  position: absolute; top: 13px; left: -5px;
  width: 0; height: 0;
  border: 5px solid transparent;
  border-right-color: #fff; border-left: 0;
}
.csp-msg-time { display: none; }
.csp-time-separator {
  text-align: center;
  padding: 8px 0;
  margin: 4px 0 8px;
  font-size: 11px;
  color: #b2b2b2;
  user-select: none;
}
.csp-empty-msg { text-align: center; padding: 40px; color: #999; }
.csp-no-session { flex: 1; display: flex; align-items: center; justify-content: center; color: #999; }
</style>