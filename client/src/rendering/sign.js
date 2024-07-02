import { assets, movementPromise, renderer, rive, scaleFactor } from "./common"


export class Sign {
  constructor() {
    const artboard = assets.artboardByName("StopSign")
    this.artboard = artboard

    const machine = new rive.StateMachineInstance(artboard.stateMachineByIndex(0), artboard)
    this.machine = machine
    this.showTrigger = machine.input(0).asTrigger()
    this.instantShowTrigger = machine.input(1).asTrigger()
    this.instantHideTrigger = machine.input(2).asTrigger()
  }

  async show(x, y, instantly = false) {
    if (!this.isShown) {
      this.x = x
      this.y = y
      instantly ? this.instantShowTrigger.fire() : this.showTrigger.fire()
      this.animation = { name: "show" }
      this.isShown = true
      await movementPromise(this.animation)
    }
  }

  async hide() {
    if (this.isShown) {
      this.isShown = false
      this.instantHideTrigger.fire()
      this.animation = { name: "hide" }
      await movementPromise(this.animation)
    }
  }

  reset() {
    if (this.isShown) {
      this.instantHideTrigger.fire()
      this.isShown = false
    }

    if (this.animation) {
      const animationCancelled = this.animation.cancelled
      delete this.animation
      animationCancelled()
    }
  }

  draw(elapsedTimeSec) {
    if (this.x !== undefined) {
      this.machine.advance(elapsedTimeSec)
      const stateChanges = this.machine.stateChangedCount()
      for (let i = 0; i < stateChanges; i++) {
        if (this.machine.stateChangedNameByIndex(i) === "exit" && this.animation) {
          const animationDone = this.animation.done
          delete this.animation
          animationDone(this)
        }
      }

      this.artboard.advance(elapsedTimeSec)
      renderer.save()
      renderer.translate(this.x * scaleFactor, this.y * scaleFactor)
      renderer.scale(scaleFactor, scaleFactor)
      this.artboard.draw(renderer)
      renderer.restore()
    }
  }

  delete() {
    this.machine.delete()
    this.artboard.delete()
  }
}
