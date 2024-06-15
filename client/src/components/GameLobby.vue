<script setup>
defineProps({
  gameInfo: {
    type: Object,
    required: true
  },
  disabled: Boolean
})
</script>

<template>
  <div class="p-4 d-flex justify-content-center">
    <div class="card">
      <div class="card-header text-center fs-5">
        Game created by {{ gameInfo.playerIndex === 0 ? "you" : gameInfo.playerNames[0] }}
      </div>
      <div class="card-body">
        <p>Players: {{ gameInfo.playerNames.join(", ") }}</p>
        <p v-if="gameInfo.playerNames.length === 1">Waiting for other players to join</p>
        <div class="mt-4 text-center">
          <button type="button" class="btn btn-secondary" :disabled @click="$emit('leaveGame')">
            {{ gameInfo.playerIndex === 0 ? "Abandon" : "Leave" }}
          </button>
          <button v-if="gameInfo.playerIndex === 0 && gameInfo.playerNames.length > 1" type="button" class="btn btn-secondary ms-3" :disabled @click="$emit('startGame')">Start</button>
        </div>
      </div>
    </div>
  </div>
</template>