import { sendWsMessage } from '@/wsClient'

export function sendAck() {
  sendWsMessage("Ack")
}

export function reformatDateTime(dateTimeStr) {
  const date = new Date(dateTimeStr)
  return date.toUTCString()
}

export function random(min, max) {
  return Math.random() * (max - min) + min
}
