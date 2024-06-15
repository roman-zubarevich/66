<script setup>
import { computed } from 'vue'
import { reformatDateTime } from '@/util';

const props = defineProps({
  gameInfo: {
    type: Object,
    required: true
  },
  disabled: Boolean,
})

const players = computed(() => {
  return props.gameInfo.playerNames.map((playerName, playerIndex) => playerName + (props.gameInfo.playerOnlineStatuses[playerIndex] ? "" : " (away)"))
})
</script>

<template>
  <div class="card" style="width: 400px; height: 200px;">
    <div class="card-body">
      <div style="height: 130px;">
        <p>Players: {{ players.join(", ") }}</p>
        <p>Started: {{ reformatDateTime(gameInfo.startTimeStr) }}</p>
        <p>Suspended: {{ reformatDateTime(gameInfo.suspendTimeStr) }}</p>
      </div>
      <div class="d-flex justify-content-center">
        <button type="button" class="btn btn-secondary" :disabled @click="$emit('rejoinGame')">Rejoin</button>
      </div>
    </div>
  </div>
</template>