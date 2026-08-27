import { ref } from 'vue'

/**
 * 通用 WebSocket 连接管理 Composable
 *
 * 封装连接/断开/重连/发送/消息回调，每次调用返回独立实例，
 * 多个组件各自维护自己的 WebSocket 连接，互不干扰。
 *
 * @example
 * const { connected, connect, disconnect, send, onMessage } = useWebSocket()
 * onMessage((data) => { ... })
 * connect('ws://localhost:5173/api/ws/merchant?uid=1&type=user&targetMerchantId=5')
 */
export function useWebSocket() {
  const connected = ref(false)
  const connecting = ref(false)

  let ws: WebSocket | null = null
  let messageHandler: ((data: any) => void) | null = null
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null
  let reconnectAttempts = 0
  let manualDisconnect = false
  let currentUrl = ''

  const MAX_RECONNECT = 20
  const BASE_DELAY = 3000
  const MAX_DELAY = 30000

  /** 建立 WebSocket 连接，url 可选（首次必须传，后续重连可省略） */
  function connect(url?: string) {
    if (url) currentUrl = url
    if (!currentUrl) {
      console.warn('[WS] 无法连接：未提供 URL')
      return
    }
    if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) {
      console.log('[WS] 已连接或连接中，跳过')
      return
    }

    manualDisconnect = false
    connecting.value = true

    console.log('[WS] 正在连接:', currentUrl)

    try {
      ws = new WebSocket(currentUrl)

      ws.onopen = () => {
        console.log('[WS] 连接成功')
        connected.value = true
        connecting.value = false
        reconnectAttempts = 0
      }

      ws.onmessage = (event: MessageEvent) => {
        try {
          const data = JSON.parse(event.data)
          messageHandler?.(data)
        } catch {
          // 忽略非 JSON 消息
        }
      }

      ws.onclose = (ev) => {
        console.log('[WS] 连接关闭 code:', ev.code)
        connected.value = false
        connecting.value = false
        if (!manualDisconnect) {
          scheduleReconnect()
        }
      }

      ws.onerror = (ev) => {
        console.error('[WS] 连接出错:', ev)
        connected.value = false
        connecting.value = false
      }
    } catch (e) {
      console.error('[WS] 创建 WebSocket 异常:', e)
      connected.value = false
      connecting.value = false
    }
  }

  /** 主动断开 */
  function disconnect() {
    console.log('[WS] 主动断开')
    manualDisconnect = true
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
    if (ws) {
      ws.onclose = null
      ws.close()
      ws = null
    }
    connected.value = false
    connecting.value = false
  }

  /** 发送消息 */
  function send(data: string) {
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(data)
    } else {
      console.warn('[WS] 无法发送，连接未就绪')
    }
  }

  /** 注册消息回调 */
  function onMessage(handler: (data: any) => void) {
    messageHandler = handler
  }

  /** 定时重连 */
  function scheduleReconnect() {
    if (reconnectTimer) return
    if (reconnectAttempts >= MAX_RECONNECT) {
      console.log('[WS] 已达最大重试次数，停止重连')
      return
    }
    reconnectAttempts++
    const delay = Math.min(BASE_DELAY * Math.pow(1.5, reconnectAttempts - 1), MAX_DELAY)
    console.log(`[WS] 将在 ${delay}ms 后重连 (第 ${reconnectAttempts} 次)`)
    reconnectTimer = setTimeout(() => {
      reconnectTimer = null
      if (currentUrl) connect()
    }, delay)
  }

  return {
    connected,
    connecting,
    connect,
    disconnect,
    send,
    onMessage,
  }
}

// ============================================================
// 商家端共享单例（MerchantLayout 与 CustomerServicePanel 共用）
// ============================================================
const merchantInstance = useWebSocket()

/**
 * 商家端 WebSocket 单例
 *
 * MerchantLayout 负责连接生命周期，CustomerServicePanel 负责收发消息，
 * 两者通过此单例共享同一个 WebSocket 连接。
 */
export function useMerchantWebSocket() {
  return merchantInstance
}