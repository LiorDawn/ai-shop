<template>
  <div class="csp-panel">
    <div class="csp-layout">
      <SessionList
        :sessions="sessions"
        :active-session-id="activeSession?.id ?? null"
        :ws-connected="wsConnected"
        @select-session="selectSession"
      />
      <div class="csp-main-wrapper">
        <MessageList
          ref="messageListRef"
          :messages="currentMessages"
          :session="activeSession"
          :ws-connected="wsConnected"
          @close-session="closeSession"
        />
        <MessageInput
          v-if="activeSession"
          :ws-connected="wsConnected"
          :disabled="false"
          @send-message="onSendMessage"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessageBox } from 'element-plus'
import request from '../../api/request'
import { useMerchantWebSocket } from '../../composables/useWebSocket'
import SessionList from '../../components/merchant/SessionList.vue'
import MessageList from '../../components/merchant/MessageList.vue'
import MessageInput from '../../components/merchant/MessageInput.vue'

interface SessionVO { id: number; userId: number; nickname: string; status: number; createTime: string; lastMessage?: string; unreadCount?: number }
interface MsgVO { id: number; sessionId: number; sendType: number; content: string; createTime: string }

const sessions = ref<SessionVO[]>([])
const activeSession = ref<SessionVO | null>(null)
const currentMessages = ref<MsgVO[]>([])
const messageListRef = ref<InstanceType<typeof MessageList> | null>(null)

// 使用商家全局的单例 WebSocket（由 MerchantLayout 负责连接生命周期）
const { connected: wsConnected, connect, send, onMessage } = useMerchantWebSocket()

onMounted(() => {
  onMessage(handleWsMessage)
  connect()
  loadSessions()
})

onUnmounted(() => {
  // 连接由 MerchantLayout 管理，不在此断开
})

async function loadSessions() {
  try {
    const res = await request.get<any>('/merchant/chat/sessions')
    if (res.data) {
      sessions.value = res.data
      const unreadSession = res.data.find((s: any) => s.unreadCount && s.unreadCount > 0)
      if (unreadSession && !activeSession.value) {
        selectSession(unreadSession)
      } else if (!activeSession.value && res.data.length > 0) {
        selectSession(res.data[0])
      }
    }
  } catch { /* ignore */ }
}

async function selectSession(s: SessionVO) {
  activeSession.value = s
  try {
    const res = await request.get<any>(`/merchant/chat/messages/${s.id}`)
    if (res.data) currentMessages.value = res.data
    scrollToBottom()
    s.unreadCount = 0
  } catch { /* ignore */ }
}

function handleWsMessage(data: any) {
  if (data.type === 'connected') {
    wsConnected.value = true
    return
  }
  if (data.type === 'message') {
    if (activeSession.value?.id === data.sessionId) {
      currentMessages.value.push({ id: data.messageId || Date.now(), sessionId: data.sessionId, sendType: data.sendType || 1, content: data.content, createTime: data.createTime || new Date().toISOString() })
      scrollToBottom()
    }
    const s = sessions.value.find(x => x.id === data.sessionId)
    if (s) {
      s.lastMessage = data.content
      if (activeSession.value?.id !== data.sessionId) {
        s.unreadCount = (s.unreadCount || 0) + 1
      }
    } else {
      loadSessions()
    }
  }
}

function onSendMessage(text: string) {
  if (!wsConnected.value || !activeSession.value) return
  send(JSON.stringify({ type: 'message', sessionId: activeSession.value.id, toUid: activeSession.value.userId, content: text }))
  currentMessages.value.push({ id: Date.now(), sessionId: activeSession.value.id, sendType: 2, content: text, createTime: new Date().toISOString() })
  scrollToBottom()
}

async function closeSession() {
  if (!activeSession.value) return
  try {
    await ElMessageBox.confirm('确定结束会话吗？', '提示', { type: 'warning' })
    send(JSON.stringify({ type: 'close_session', sessionId: activeSession.value.id }))
    activeSession.value = null; currentMessages.value = []; loadSessions()
  } catch { /* cancel */ }
}

function scrollToBottom() { nextTick(() => { messageListRef.value?.scrollToBottom() }) }
</script>

<style scoped>
.csp-panel { height: calc(100vh - 120px); max-height: calc(100vh - 120px); display: flex; flex-direction: column; background: #fff; border-radius: 8px; overflow: hidden; }
.csp-layout { display: flex; flex: 1 1 auto; min-height: 0; }
.csp-main-wrapper { flex: 1 1 auto; display: flex; flex-direction: column; min-width: 0; min-height: 0; }
</style>