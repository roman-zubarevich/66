<script setup>
import { Modal } from 'bootstrap'
import { onBeforeUnmount, onMounted, ref } from 'vue';

const props = defineProps({
  name: String,
  disabled: Boolean,
})

const emit = defineEmits(["renamed"])

const newName = ref(null)
const nameModalRef = ref(null)
const nameInputRef = ref(null)
let nameModal = null


onMounted(() => {
  nameModal = new Modal("#nameModal", {})
  nameModalRef.value.addEventListener('shown.bs.modal', focusOnInput)
  if (!props.name) {
    editName()
  }
})

onBeforeUnmount(() => {
  nameModalRef.value.removeEventListener('shown.bs.modal', focusOnInput)
})


function focusOnInput() {
  nameInputRef.value.focus()
}

function editName() {
  newName.value = props.name
  nameModal.show()
}

function saveName() {
  if (newName.value) {
    if (newName.value !== props.name) {
      emit("renamed", newName.value)
    }
    nameModal.hide()
  }
}

function cancelNameEditing() {
  if (props.name) {
    nameModal.hide()
  }
}
</script>

<template>
  <div v-if="name" class="d-flex p-4 justify-content-center">
    <div class="card">
      <div class="card-header text-center fs-5">Player</div>
      <div class="card-body">
        {{ name }}
        <button type="button" class="btn btn-secondary ms-2" :disabled @click="editName">
          Change
        </button>
      </div>
    </div>
  </div>

  <!-- Player name modal -->
  <div class="modal fade" id="nameModal" ref="nameModalRef" data-bs-backdrop="static" data-bs-keyboard="false" tabindex="-1" @keydown.escape="cancelNameEditing">
    <div class="modal-dialog">
      <div class="modal-content">
        <div class="modal-header">
          <h1 class="modal-title fs-5">Enter player name</h1>
          <button v-if="name" type="button" class="btn-close" data-bs-dismiss="modal"></button>
        </div>
        <div class="modal-body">
          <input ref="nameInputRef" v-model.trim="newName" maxlength="40" placeholder="Player name" style="width: 100%;" @keydown.enter="saveName" />
        </div>
        <div class="modal-footer">
          <button v-if="name" type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
          <button type="button" :disabled="!newName" class="btn btn-secondary" @click="saveName">OK</button>
        </div>
      </div>
    </div>
  </div>
</template>