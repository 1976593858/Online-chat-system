// src/api/chat.js
let ws = null

export function connectWS(userId, onMessage) {
  ws = new WebSocket(`ws://localhost:8080/ws/group/${userId}`)

  ws.onopen = () => console.log('WS 已连接')
  ws.onmessage = (e) => onMessage(JSON.parse(e.data))
  ws.onclose = () => console.log('WS 断开')

  return ws
}

export function sendRaw(text) {
  ws && ws.readyState === WebSocket.OPEN && ws.send(text)
}