let ws = null
let handlers = []
let reconnectTimer = null
let currentUserId = null

const WS_BASE = import.meta.env.VITE_WS_BASE_URL || ''

export function connectWS(userId) {
  if (ws && ws.readyState === WebSocket.OPEN && currentUserId === userId) {
    return ws
  }

  disconnect()
  currentUserId = userId

  ws = new WebSocket(`${WS_BASE}/ws/${userId}`)

  ws.onopen = () => console.log('WS 已连接', userId)

  ws.onmessage = (e) => {
    let data
    try {
      data = JSON.parse(e.data)
    } catch {
      return
    }
    handlers.forEach((h) => {
      try { h(data) } catch (err) { console.error('WS handler error', err) }
    })
  }

  ws.onclose = () => {
    console.log('WS 断开，3秒后自动重连')
    if (currentUserId) {
      reconnectTimer = setTimeout(() => {
        connectWS(currentUserId)
      }, 3000)
    }
  }

  ws.onerror = (e) => console.error('WS 错误', e)

  return ws
}

export function addHandler(handler) {
  if (!handlers.includes(handler)) {
    handlers.push(handler)
  }
}

export function removeHandler(handler) {
  handlers = handlers.filter((h) => h !== handler)
}

export function send(data) {
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify(data))
  }
}

export function sendRaw(text) {
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(text)
  }
}

export function disconnect() {
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
  if (ws) {
    ws.onclose = null
    ws.close()
    ws = null
  }
  currentUserId = null
}

export function isConnected() {
  return ws && ws.readyState === WebSocket.OPEN
}
