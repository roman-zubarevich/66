<script setup>
import { addWsHandler, removeWsHandlers, sendWsMessage } from '@/wsClient'
import { sendAck } from '@/util'
import { createBoard, setCanvasBounds, setGlobalScale } from '@/rendering/board'
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'


const props = defineProps({
  gameInfo: {
    type: Object,
    required: true
  },
  suspendedInfo: {
    type: Object,
  },
  riveData: {
    type: Object,
    required: true,
  },
})

const emit = defineEmits(["exit", "resume"])


const canvasRef = ref(null)
const canvasSize = ref({ x: 0, y: 0 })

let board = null


function resizeCanvas() {
  const windowWidth = Math.max(window.innerWidth, 320)
  const windowHeight = Math.max(window.innerHeight, 200)
  const maxWidth = windowHeight * 1.6
  if (windowWidth > maxWidth) {
    canvasSize.value = { x: maxWidth, y: windowHeight }
    setGlobalScale(maxWidth * 0.000625)
  } else {
    canvasSize.value = { x: windowWidth, y: windowWidth * 0.625 }
    setGlobalScale(windowWidth * 0.000625)
  }
  nextTick().then(() => {
    const { left, top } = canvasRef.value.getBoundingClientRect()
    setCanvasBounds(left, top)
  })
}

watch(() => props.suspendedInfo, info => {
  if (info) {
    board.setSuspendInfo(info)
  }
}, { deep: true })

onMounted(() => {
  board = createBoard(
    props.riveData,
    canvasRef.value,
    props.gameInfo,
    props.suspendedInfo,
    (msgType, msgObj) => sendWsMessage(msgType, msgObj),
    () => emit('exit')
  )
  resizeCanvas()

  window.onresize = resizeCanvas
  window.onbeforeunload = () => board.destroy()

  addWsHandler("BoardInitialized", boardState => {
    board.setState(boardState).then(() => {
      sendAck()
      if (props.suspendedInfo) {
        emit("resume", props.gameInfo.gameId)
      }
    }).catch(promiseRejected)
  })
  addWsHandler("BoardUpdated", ({ deckCard, discardedValue, actions }) => {
    board.update(deckCard, discardedValue, actions).then(() => sendAck()).catch(promiseRejected)
  })
  addWsHandler("CardExchanged", ({ playerIndex, cardIndex, anotherPlayerIndex, anotherPlayerCardIndex }) => {
    board.exchangeCards(playerIndex, cardIndex, anotherPlayerIndex, anotherPlayerCardIndex).then(() => sendAck()).catch(promiseRejected)
  })
  addWsHandler("CardsReplaced", ({ playerIndex, cardIndexes, fromDeck, discardedValue }) => {
    board.replaceCards(playerIndex, cardIndexes, fromDeck, discardedValue).then(() => sendAck()).catch(promiseRejected)
  })
  addWsHandler("CardsRevealed", ({ cardInfos }) => {
    board.revealCards(cardInfos).then(() => sendAck()).catch(promiseRejected)
  })
  addWsHandler("RoundFinished", result => {
    board.finishRound(result).then(() => sendAck()).catch(promiseRejected)
  })
  addWsHandler("StopRequested", ({ stopperIndex }) => {
    board.stopRequested(stopperIndex).then(() => sendAck()).catch(promiseRejected)
  })
  addWsHandler("TurnStarted", ({ activePlayerIndex, turn, actions }) => {
    board.startTurn(activePlayerIndex, turn, actions).then(() => sendAck()).catch(promiseRejected)
  })

  // Ack GameStarted or RejoinedGame message
  sendAck()
})

onBeforeUnmount(() => {
  board.destroy()
  board = null
  window.onresize = null
  window.onbeforeunload = null
  removeWsHandlers("BoardInitialized", "BoardUpdated", "CardExchanged", "CardsReplaced", "CardsRevealed", "RoundFinished", "StopRequested", "TurnStarted")
})

function promiseRejected() {
  // Rejected due to asynchronous suspension, ignore
}
</script>

<template>
  <div class="in-game">
    <canvas id="canvas" ref="canvasRef" :width="canvasSize.x" :height="canvasSize.y"></canvas>
  </div>
</template>

<style scoped>
.in-game {
  background-color: #1A3035;
  height: 100vh;
}
</style>