let handlerByType = {}
let webSocket = null

export function initWsClient(openHandler, closeHandler) {
  webSocket = new WebSocket(import.meta.env.VITE_SERVER_URL)
  webSocket.onmessage = handleMessage
  webSocket.onopen = openHandler
  webSocket.onclose = closeHandler
}

export function addWsHandler(msgType, handler) {
  handlerByType[msgType] = handler
}

export function removeWsHandlers(...msgTypes) {
  msgTypes.forEach(msgType => delete handlerByType[msgType])
}

export function sendWsMessage(msgType, msgObj = {}) {
  msgObj.type = msgType
  console.debug("sending", msgObj)
  webSocket.send(JSON.stringify(msgObj))
}

function handleMessage(event) {
  const message = JSON.parse(event.data)
  console.debug("received", message)
  const handler = handlerByType[message.type]
  handler(message)
}

export function closeWsClient() {
  handlerByType = {}
  if (webSocket) {
    webSocket.close()
  }
}