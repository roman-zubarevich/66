<script setup>
import { computed } from 'vue';

const props = defineProps({
  playerNameById: Object,
})

const onlinePlayers = computed(() => {
  const countByName = {}
  for (const name of Object.values(props.playerNameById)) {
    const count = countByName[name] || 0
    countByName[name] = count + 1
  }
  return Object.entries(countByName).toSorted(([a], [b]) => a < b ? -1 : a > b ? 1 : 0)
})
</script>

<template>
  <div class="text-center">
    Other players online: {{ onlinePlayers.length }}
    <button type="button" class="btn btn-secondary ms-2" :disabled="onlinePlayers.length === 0" data-bs-toggle="modal" data-bs-target="#competitorsModal">See</button>
  </div>

  <div class="modal fade" id="competitorsModal" data-bs-keyboard="true" tabindex="-1">
    <div class="modal-dialog modal-dialog-scrollable modal-m">
      <div class="modal-content">
        <div class="modal-header">
          <h1 class="modal-title fs-4">Other players</h1>
          <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
        </div>
        <div class="modal-body">
          <p v-for="[name, count] in onlinePlayers">{{ name }}<span v-if="count > 1"> &times; {{ count }}</span></p>
        </div>
      </div>
    </div>
  </div>
</template>