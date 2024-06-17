import { assets, cancelTimeout, movementPromise, renderer, rive, scaleFactor, timeoutPromise } from './common'
import { ANGLE_180, HALF_CARD_WIDTH, HALF_CARD_HEIGHT, MOTION_FRAMES, PEEK_TIME } from './position'


export class Card {
  static count = 0

  constructor() {
    this.id = Card.count++

    const artboard = assets.artboardByIndex(0)
    this.artboard = artboard

    const machine = new rive.StateMachineInstance(artboard.stateMachineByIndex(0), artboard)
    this.machine = machine
    this.showTrigger = machine.input(0).asTrigger()
    this.hideTrigger = machine.input(1).asTrigger()
    this.instantShowTrigger = machine.input(2).asTrigger()
    this.instantHideTrigger = machine.input(3).asTrigger()
    this.highlightFlag = machine.input(4).asBool()
    this.hoverFlag = machine.input(5).asBool()
    this.clickTrigger = machine.input(6).asTrigger()
    this.peekTrigger = machine.input(7).asTrigger()
    this.unpeekTrigger = machine.input(8).asTrigger()
    this.instantUnpeekTrigger = machine.input(9).asTrigger()
    this.markTrigger = machine.input(10).asTrigger()
  }

  setPosition(position) {
    this.position = position
    if (position.angle === 0 || position.angle === ANGLE_180) {
      this.xOffset = HALF_CARD_WIDTH
      this.yOffset = HALF_CARD_HEIGHT
    } else {
      this.xOffset = HALF_CARD_HEIGHT
      this.yOffset = HALF_CARD_WIDTH
    }
  }

  setValue(value) {
    this.artboard.textRun("Value").text = value.toString()
  }

  moveTo(endPosition) {
    this.motion = { start: this.position.copy(), end: endPosition, frame: 0 }
    return movementPromise(this.motion)
  }

  show(instantly = false) {
    this.isShown = true
    instantly ? this.instantShowTrigger.fire() : this.showTrigger.fire()
    this.animation = { name: "show" }
    return movementPromise(this.animation)
  }

  async hide(instantly = false) {
    instantly ? this.instantHideTrigger.fire() : this.hideTrigger.fire()
    this.animation = { name: "hide" }
    await movementPromise(this.animation)
    this.isShown = false
    return this
  }

  async showToOthers() {
    this.isPeeked = true
    this.peekTrigger.fire()
    this.animation = { name: "peek" }
    await movementPromise(this.animation)
    await timeoutPromise(this, async () => {
      this.unpeekTrigger.fire()
      this.animation = { name: "unpeek" }
      await movementPromise(this.animation)
      this.isPeeked = false
    }, PEEK_TIME)
  }

  enableClick(value = true) {
    this.highlightFlag.value = value
  }

  hover(value = true) {
    this.hoverFlag.value = value
  }

  click() {
    this.clickTrigger.fire()
    this.animation = { name: "click" }
    return movementPromise(this.animation)
  }

  mark() {
    this.markTrigger.fire()
  }

  reset() {
    cancelTimeout(this)

    if (this.motion) {
      const motionCancelled = this.motion.cancelled
      delete this.motion
      motionCancelled()
    }

    if (this.isPeeked) {
      this.instantUnpeekTrigger.fire()
      this.isPeeked = false
    } else if (this.isShown) {
      this.instantHideTrigger.fire()
      this.isShown = false
    }
    this.highlightFlag.value = false
    this.hoverFlag.value = false
    // TODO: cancel click

    if (this.animation) {
      const animationCancelled = this.animation.cancelled
      delete this.animation
      animationCancelled()
    }
  }

  draw(elapsedTimeSec) {
    if (this.motion) {
      if (this.motion.frame++ < MOTION_FRAMES) {
        this.position.interpolate(this.motion)
      } else {
        const motionDone = this.motion.done
        delete this.motion
        motionDone(this)
      }
    }

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
    renderer.rotate(this.position.angle)
    const resultingScale = this.position.scale * scaleFactor
    renderer.scale(resultingScale, resultingScale)
    this.artboard.draw(renderer)
    renderer.restore()
  }

  delete() {
    this.machine.delete()
    this.artboard.delete()
  }

  elementIndexAt(pointX, pointY) {
    const { x, y } = this.position
    return pointX >= x - this.xOffset && pointX <= x + this.xOffset && pointY >= y - this.yOffset && pointY <= y + this.yOffset ? 0 : -1
  }
}
