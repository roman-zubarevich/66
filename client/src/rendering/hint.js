import { assets, renderer, rive, scaleFactor } from "./common"


export class Hint {
    constructor(artboardName) {
      const artboard = assets.artboardByName(artboardName)
      this.artboard = artboard
  
      const machine = new rive.StateMachineInstance(artboard.stateMachineByIndex(0), artboard)
      this.machine = machine
      this.phase = machine.input(0).asNumber()
      this.phase.value = 0
    }
  
    inform(x, y) {
      this.x = x
      this.y = y
      this.phase.value = 1
    }

    showAction(x, y) {
      this.x = x
      this.y = y
      this.phase.value = 2
    }
  
    reset() {
      this.phase.value = 0
    }
  
    draw(elapsedTimeSec) {
      if (this.x !== undefined) {
        this.machine.advance(elapsedTimeSec)
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
  