<script setup>
defineProps({
  disabled: Boolean
})

defineEmits(["create"])
</script>

<template>
  <div class="card" style="width: 400px; height: 200px;">
    <div class="card-body">
      <div class="d-flex justify-content-center center-vertically">
        <button type="button" class="btn btn-secondary" :disabled @click="$emit('create')">Create game</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.center-vertically {
  position: relative;
  top: 50%;
  transform: translateY(-50%);
}
</style>