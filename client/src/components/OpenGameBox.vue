<script setup>
import { reformatDateTime } from '@/util';

defineProps({
  openGame: {
    type: Object,
    required: true
  },
  disabled: Boolean
})
</script>

<template>
  <div class="card mt-4" style="width: 400px; height: 200px;">
    <div class="card-body">
      <div style="height: 130px;">
        <p>Players: {{ openGame.playerNames.join(", ") }}</p>
        <p>Created: {{ reformatDateTime(openGame.openTimeStr) }}</p>
      </div>
      <div class="d-flex justify-content-center">
        <button type="button" class="btn btn-secondary" :disabled @click="$emit('joinGame')">Join</button>
      </div>
    </div>
  </div>
</template>