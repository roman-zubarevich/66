<script setup>
import RiveCanvas from "@rive-app/canvas-advanced-single"
import Competitors from '@/components/Competitors.vue'
import Game from '@/components/Game.vue'
import GameLobby from '@/components/GameLobby.vue'
import Info from "@/components/Info.vue"
import NewGameBox from "@/components/NewGameBox.vue"
import OpenGameBox from '@/components/OpenGameBox.vue'
import PlayerBox from '@/components/PlayerBox.vue'
import SuspendedGameBox from '@/components/SuspendedGameBox.vue'
import { addWsHandler, closeWsClient, initWsClient, sendWsMessage } from '@/wsClient'
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

const MAX_PLAYERS = 5
const PLAYER_NAME_KEY = "playerName"
const SECRET_KEY = "secret"
const State = Object.freeze({
  WorldLobby: 0,
  GameLobby: 1,
  Game: 2,
})

const playerName = ref(window.localStorage.getItem(PLAYER_NAME_KEY))
const playerSecret = ref(window.localStorage.getItem(SECRET_KEY))

const initialized = ref(false)
const connected = ref(false)
const waitingForResponse = ref(false)
const openGameById = ref({})  // Includes available and fully booked
const suspendedGameById = ref({})
const playerNameById = ref({})
const offlinePlayerIds = []

const state = ref(State.WorldLobby)
const gameInfo = ref(null)

const suspendedGamesExist = computed(() => {
  for (var key in suspendedGameById.value) return true
  return false
})


onMounted(() => {
  setupMessageHandlers()
  if (playerName.value) {
    initMessaging()
  }
})

onBeforeUnmount(() => {
  closeWsClient()
})


let riveData = null
async function initRive() {
  const rive = await RiveCanvas()
  const assetBytes = await (
    await fetch(new Request("/cards.riv"))
  ).arrayBuffer()
  const assets = await rive.load(new Uint8Array(assetBytes))
  return { rive, assets }
}
initRive().then(data => {
  initialized.value = true
  riveData = data
})

function setPlayerName(value) {
  playerName.value = value
  window.localStorage.setItem(PLAYER_NAME_KEY, value)
}

function setSecret(value) {
  playerSecret.value = value
  window.localStorage.setItem(SECRET_KEY, value)
}

function initMessaging() {
  initWsClient(onConnect, onDisconnect)
}

function onConnect() {
  connected.value = true
    if (playerSecret.value) {
      act("ListSuspendedGames", { playerSecret: playerSecret.value })
    } else if (playerName.value) {
      registerPlayer()
    }
}

function onDisconnect() {
  connected.value = false
    waitingForResponse.value = false
    playerNameById.value = {}
    openGameById.value = {}
    suspendedGameById.value = {}
    gameInfo.value = null
    offlinePlayerIds.length = 0
    state.value = State.WorldLobby
    setTimeout(initMessaging, 10000)
}

function setupMessageHandlers() {
  addWsHandler("OnlinePlayers", ({ nameById }) => {
    Object.assign(playerNameById.value, nameById)
    for (const id of offlinePlayerIds) {
      delete playerNameById.value[id]
    }
    offlinePlayerIds.length = 0
  })
  addWsHandler("PlayerStatus", ({ id, name, isOnline }) => {
    if (isOnline) {
      playerNameById.value[id] =name
    } else {
      if (playerNameById.value[id]) {
        delete playerNameById.value[id]
      } else {
        offlinePlayerIds.push(id)
      }
    }
  })
  addWsHandler("OpenGames", ({ games }) => {
    games.forEach(game => openGameById.value[game.gameId] = game)
  })
  addResponseHandler("SuspendedGames", ({ games }) => {
    games.forEach(game => suspendedGameById.value[game.gameId] = game)
  })
  addResponseHandler("PlayerInfo", ({ name, secret }) => {
    setPlayerName(name)
    if (!playerSecret.value) {
      setSecret(secret)
    }
  })
  addResponseHandler("JoinedGame", ({ gameId }) => {
    let game = openGameById.value[gameId]
    if (!game) {
      game = { gameId, playerNames: [] }
      openGameById.value[gameId] = game
    }
    gameInfo.value = { gameId, playerNames: game.playerNames, playerIndex: game.playerNames.length }
    game.playerNames.push(playerName.value)
    state.value = State.GameLobby
  })
  addResponseHandler("GameDeleted", ({ gameId }) => {
    delete openGameById.value[gameId]
    if (state.value === State.GameLobby && gameInfo.value.gameId === gameId) {
      gameInfo.value = null
      state.value = State.WorldLobby
    }
  })
  addResponseHandler("NewPlayer", ({ gameId, name }) => {
    openGameById.value[gameId].playerNames.push(name)
  })
  addResponseHandler("PlayerRemoved", ({ gameId, playerIndex }) => {
    openGameById.value[gameId].playerNames.splice(playerIndex, 1)
    if (state.value === State.GameLobby && gameInfo.value.gameId === gameId) {
      if (gameInfo.value.playerIndex > playerIndex) {
        gameInfo.value.playerIndex--
      } else if (gameInfo.value.playerIndex === playerIndex) {
        gameInfo.value = null
        state.value = State.WorldLobby
      }
    }
  })
  addResponseHandler("GameStarted", ({ gameId }) => {
    delete openGameById.value[gameId]
    state.value = State.Game
  })
  addResponseHandler("PlayerLeft", ({ gameId, playerIndex }) => {
    const game = suspendedGameById.value[gameId]
    if (game) {
      game.playerOnlineStatuses[playerIndex] = false
      if (gameId === gameInfo.value?.gameId && playerIndex === gameInfo.value?.playerIndex) {
        state.value = State.WorldLobby
      }
    }
  })
  addResponseHandler("GameSuspended", ({ suspendedGame, playerIndex }) => {
    suspendedGameById.value[suspendedGame.gameId] = suspendedGame
    if (playerIndex === gameInfo.value.playerIndex) {
      state.value = State.WorldLobby
    }
  })
  addResponseHandler("PlayerRejoined", ({ gameId, playerIndex }) => {
    const game = suspendedGameById.value[gameId]
    if (game) {
      game.playerOnlineStatuses[playerIndex] = true
      if (game.playerIndex === playerIndex) {
        gameInfo.value = { gameId, playerNames: game.playerNames, playerIndex: game.playerIndex }
        state.value = State.Game
      }
    }
  })
  addResponseHandler("Failure", ({ command, message, errorReason }) => {
    console.error(`${command} failed: ${message}`)
    if (errorReason === "PLAYER_NOT_FOUND") {
      playerSecret.value = null
      registerPlayer()
    }
  })
}

function registerPlayer() {
  act("RegisterPlayer", { name: playerName.value })
}

function updatePlayerName(newName) {
  if (playerName.value) {
    act("RenamePlayer", { secret: playerSecret.value, name: newName })
  } else {
    setPlayerName(newName)
    initMessaging()
  }
}

function resumeGame(gameId) {
  delete suspendedGameById.value[gameId]
}

function endGame() {
  gameInfo.value = null
  state.value = State.WorldLobby
}

function act(msgType, msgObj) {
  waitingForResponse.value = true
  sendWsMessage(msgType, msgObj)
}

function addResponseHandler(msgType, handler) {
  addWsHandler(msgType, msgObj => {
    waitingForResponse.value = false
    handler(msgObj)
  })
}
</script>

<template>
  <Info />
  <template v-if="state === State.WorldLobby">
    <PlayerBox :name="playerName" :disabled="waitingForResponse || !connected || !playerSecret" @renamed="updatePlayerName" />
    <Competitors :playerNameById/>
    <div v-if="playerName">
      <div v-if="initialized && connected && playerSecret" class="d-flex m-4 justify-content-center">
        <div>
          <NewGameBox :disabled="waitingForResponse" @create="act('CreateGame', { playerSecret })"/>
          <template v-for="(openGame, gameId) in openGameById">
            <OpenGameBox v-if="openGame.playerNames.length < MAX_PLAYERS"
                :openGame
                :disabled="waitingForResponse"
                @joinGame="act('JoinGame', { gameId, playerSecret })"
            />
          </template>
        </div>
        <template v-if="suspendedGamesExist">
          <div class="vr ms-4 me-4"></div>
          <div>
            <SuspendedGameBox v-for="(gameInfo, gameId, index) in suspendedGameById"
                :gameInfo
                :disabled="waitingForResponse"
                :class="{'mt-4': index !== 0}" @rejoinGame="act('RejoinGame', { gameId, playerSecret })"
            />
          </div>
        </template>
      </div>
      <div v-else class="d-flex mt-4 justify-content-center">
        <div class="spinner-grow"></div>
      </div>
    </div>
  </template>
  <template v-else-if="state == State.GameLobby">
    <GameLobby :gameInfo :disabled="waitingForResponse" @leaveGame="act('LeaveGame')" @startGame="act('StartGame')" />
  </template>
  <template v-else>
    <Game :gameInfo :suspendedInfo="suspendedGameById[gameInfo.gameId]" :riveData @exit="endGame" @resume="resumeGame" />
  </template>
</template>

<style scoped></style>
