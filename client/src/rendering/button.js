import { assets, renderer, rive, scaleFactor } from "./common"


export class Button {
  constructor(left, top, artboardName, hoverCallback) {
    const artboard = assets.artboardByName(artboardName)
    this.artboard = artboard

    const machine = new rive.StateMachineInstance(artboard.stateMachineByIndex(0), artboard)
    this.machine = machine
    this.hoverFlag = machine.input(0).asBool()
    this.clickTrigger = machine.input(1).asTrigger()

    this.left = left
    this.top = top
    this.right = left + artboard.bounds.maxX
    this.bottom = top + artboard.bounds.maxY

    this.enabled = false
    this.hoverCallback = hoverCallback
  }

  enableClick(value = true) {
    this.enabled = value
  }

  hover(value = true) {
    this.hoverFlag.value = value
    if (this.hoverCallback) {
      this.hoverCallback(value)
    }
  }

  click() {
    this.clickTrigger.fire()
    return new Promise(resolve => this.animationDone = resolve)
  }

  draw(elapsedTimeSec) {
    if (this.enabled) {
      this.machine.advance(elapsedTimeSec)
      const stateChanges = this.machine.stateChangedCount()
      for (let i = 0; i < stateChanges; i++) {
        if (this.machine.stateChangedNameByIndex(i) === "exit") {
          const animationDone = this.animationDone
          delete this.animationDone
          animationDone(this)
        }
      }
      
      this.artboard.advance(elapsedTimeSec)
      renderer.save()
      renderer.translate(this.left * scaleFactor, this.top * scaleFactor)
      renderer.scale(scaleFactor, scaleFactor)
      this.artboard.draw(renderer)
      renderer.restore()
    }
  }

  delete() {
    this.machine.delete()
    this.artboard.delete()
  }

  elementIndexAt(pointX, pointY) {
    return pointX >= this.left && pointX <= this.right && pointY >= this.top && pointY <= this.bottom ? 0 : -1
  }
}
