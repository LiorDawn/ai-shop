<template>
  <div class="csp-sidebar">
    <div class="csp-sidebar-header">
      <h3>店铺客服</h3>
      <span :class="['csp-ws-dot', wsConnected ? 'online' : 'offline']"></span>
    </div>
    <div class="csp-session-list">
      <div
        v-for="s in sessions"
        :key="s.id"
        :class="['csp-session-item', { active: activeSessionId === s.id }]"
        @click="$emit('select-session', s)"
      >
        <div class="csp-session-info">
          <span class="csp-session-user">{{ getUserLabel(s) }}</span>
          <span class="csp-session-time">{{ formatTime(s.createTime) }}</span>
        </div>
        <div class="csp-session-preview">{{ s.lastMessage || '暂无消息' }}</div>
        <span v-if="s.unreadCount && s.unreadCount > 0 && activeSessionId !== s.id" class="csp-unread">{{ s.unreadCount > 99 ? '99+' : s.unreadCount }}</span>
      </div>
      <div v-if="sessions.length === 0" class="csp-empty">暂无客户咨询</div>
    </div>
  </div>
</template>

<script setup lang="ts">
interface SessionVO { id: number; userId: number; nickname: string; status: number; createTime: string; lastMessage?: string; unreadCount?: number }

defineProps<{
  sessions: SessionVO[]
  activeSessionId: number | null
  wsConnected: boolean
}>()

defineEmits<{
  (e: 'select-session', session: SessionVO): void
}>()

function getUserLabel(s: SessionVO): string {
  return s.nickname || '用户' + s.userId
}

function formatTime(t: string | undefined | null): string {
  if (!t) return ''
  try {
    const d = new Date(t)
    if (isNaN(d.getTime())) return ''
    const pad = (n: number) => String(n).padStart(2, '0')
    const now = new Date()
    if (d.toDateString() === now.toDateString()) return `${pad(d.getHours())}:${pad(d.getMinutes())}`
    return `${pad(d.getMonth() + 1)}/${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
  } catch { return '' }
}
</script>

<style scoped>
.csp-sidebar { width: 280px; border-right: 1px solid #eee; display: flex; flex-direction: column; min-height: 0; }
.csp-sidebar-header { padding: 14px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #eee; flex-shrink: 0; }
.csp-sidebar-header h3 { margin: 0; font-size: 15px; }
.csp-ws-dot { width: 8px; height: 8px; border-radius: 50%; }
.csp-ws-dot.online { background: #67c23a; }
.csp-ws-dot.offline { background: #909399; }
.csp-session-list { flex: 1 1 auto; overflow-y: auto; min-height: 0; }
.csp-session-item { padding: 12px 14px; border-bottom: 1px solid #f5f5f5; cursor: pointer; transition: background 0.2s; }
.csp-session-item:hover { background: #f7f8fa; }
.csp-session-item.active { background: #eef1ff; border-left: 3px solid #67c23a; }
.csp-session-info { display: flex; justify-content: space-between; margin-bottom: 4px; }
.csp-session-user { font-size: 14px; font-weight: 600; color: #333; }
.csp-session-time { font-size: 11px; color: #999; }
.csp-session-preview { font-size: 12px; color: #999; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.csp-unread { min-width: 18px; height: 18px; padding: 0 5px; background: #e4393c; color: #fff; font-size: 11px; line-height: 18px; text-align: center; border-radius: 9px; display: inline-block; float: right; }
.csp-empty { padding: 40px; text-align: center; color: #999; font-size: 13px; }
</style>