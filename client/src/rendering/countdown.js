import { assets, movementPromise, renderer, rive, scaleFactor } from "./common"


export class Countdown {
    constructor(position) {
      const artboard = assets.artboardByName("Countdown")
      this.artboard = artboard
      this.position = position
  
      const machine = new rive.StateMachineInstance(artboard.stateMachineByIndex(0), artboard)
      this.machine = machine
      this.startTrigger = machine.input(0).asTrigger()
      this.instantHideTrigger = machine.input(1).asTrigger()
    }
  
    async start() {
      if (!this.isShown) {
        this.startTrigger.fire()
        this.animation = { name: "show" }
        this.isShown = true
        await movementPromise(this.animation)
        this.isShown = false
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
      renderer.translate(this.position.x * scaleFactor, this.position.y * scaleFactor)
      const resultingScale = this.position.scale * scaleFactor
      renderer.scale(resultingScale, resultingScale)
      this.artboard.draw(renderer)
      renderer.restore()
    }
  
    delete() {
      this.machine.delete()
      this.artboard.delete()
    }
  }
  