import { assets, movementPromise, renderer, rive, scaleFactor } from "./common"
import { ANGLE_180, ANGLE_270, ANGLE_90, interpolate } from "./position"


export const LEFT = -1
export const RIGHT = 1

const MOTION_FRAMES = 60
const MOTION_FRAMES_INVERSE = 1 / MOTION_FRAMES

const FAR_AWAY = 500

const SIGN_X_OFFSET = 67
const SIGN_Y_OFFSET = -22


export class CatPosition {
  constructor(x, y, direction = RIGHT) {
    this.x = x
    this.y = y
    this.direction = direction
  }

  copy() {
    return new CatPosition(this.x, this.y, this.direction)
  }

  interpolate(motion) {
    const progress = motion.frame * MOTION_FRAMES_INVERSE
    this.x = interpolate(motion.start.x, motion.end.x, progress)
    this.y = interpolate(motion.start.y, motion.end.y, progress)
  }

  turnIfNeeded(x) {
    this.direction = x > this.x ? RIGHT : x < this.x ? LEFT : this.direction
  }
}

export class Cat {
  static count = 0

  constructor(position) {
    this.id = Cat.count++

    this.position = position.copy()
    this.initialPosition = position.copy()

    const artboard = assets.artboardByName("Cat")
    this.artboard = artboard

    const machine = new rive.StateMachineInstance(artboard.stateMachineByIndex(0), artboard)
    this.machine = machine
    this.sleepTrigger = machine.input(0).asTrigger()
    this.wakeUpTrigger = machine.input(1).asTrigger()
    this.instantSleepTrigger = machine.input(2).asTrigger()
    this.instantWakeUpTrigger = machine.input(3).asTrigger()
    this.walkFlag = machine.input(4).asBool()
    this.jumpTrigger = machine.input(5).asTrigger()
    this.landTrigger = machine.input(6).asTrigger()
    this.instantLandTrigger = machine.input(7).asTrigger()
    this.stopTrigger = machine.input(8).asTrigger()
    this.crownTrigger = machine.input(9).asTrigger()

    this.look = artboard.node("Look")
    this.lookForward()
  }

  lookTo(angle) {
    let x = 0, y = 0
    switch (angle) {
      case 0:
        y = FAR_AWAY
        break
      case ANGLE_90:
        x = FAR_AWAY
        this.position.direction = LEFT
        break
      case ANGLE_180:
        y = -FAR_AWAY
        break
      case ANGLE_270:
        x = FAR_AWAY
        this.position.direction = RIGHT
        break
      default:
        console.error(`Unexpected direction for cat ${this.id}: ${angle}`)
    }
    this.look.x = x
    this.look.y = y
  }

  lookForward() {
    this.lookTo(this.position.direction === LEFT ? ANGLE_90 : ANGLE_270)
  }

  async sleep(instantly = false) {
    if (!this.isSleeping) {
      this.isSleeping = true
      instantly ? this.instantSleepTrigger.fire() : this.sleepTrigger.fire()
      this.animation = { name: "sleep" }
      await movementPromise(this.animation)
    }
  }

  async wakeUp(instantly = false) {
    if (this.isSleeping) {
      instantly ? this.instantWakeUpTrigger.fire() : this.wakeUpTrigger.fire()
      this.animation = { name: "wakeUp" }
      await movementPromise(this.animation)
      this.isSleeping = false
    }
  }

  async walkTo(x, y) {
    this.position.turnIfNeeded(x)
    this.walkFlag.value = true
    this.motion = { start: this.position.copy(), end: new CatPosition(x, y), frame: 0 }
    await movementPromise(this.motion)
    this.walkFlag.value = false
    this.animation = { name: "stop" }
    await movementPromise(this.animation)
  }

  async jumpTo(x, y) {
    this.position.turnIfNeeded(x)
    this.jumpTrigger.fire()
    this.animation = { name: "jump" }
    this.isJumping = true
    await movementPromise(this.animation)

    this.motion = { start: this.position.copy(), end: new CatPosition(x, y), frame: 0 }
    await movementPromise(this.motion)

    this.landTrigger.fire()
    this.animation = { name: "land" }
    await movementPromise(this.animation)
    this.isJumping = false
  }

  async jumpBack() {
    const { x, y } = this.initialPosition
    await this.jumpTo(x, y)
    this.position.direction = this.initialPosition.direction
  }

  async walkBack() {
    const { x, y } = this.initialPosition
    await this.walkTo(x, y)
    this.position.direction = this.initialPosition.direction
  }

  crown() {
    this.crownTrigger.fire()
  }

  requestStop(sign, instantly = false) {
    const signPromise = sign.show(this.position.x + this.position.direction * SIGN_X_OFFSET, this.position.y + SIGN_Y_OFFSET, instantly)
    if (instantly) {
      return signPromise
    }
    this.stopTrigger.fire()
    this.animation = { name: "requestStop" }
    return Promise.all([signPromise, movementPromise(this.animation)])
  }

  reset() {
    this.position = this.initialPosition.copy()
    if (this.motion) {
      const motionCancelled = this.motion.cancelled
      delete this.motion
      motionCancelled()
    }

    if (this.walkFlag.value) {
      this.walkFlag.value = false
    } else if (this.isJumping) {
      this.instantLandTrigger.fire()
      this.isJumping = false
    } else if (this.isSleeping) {
      this.instantWakeUpTrigger.fire()
      this.isSleeping = false
    }
    
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
    renderer.scale(this.position.direction * scaleFactor, scaleFactor)
    this.artboard.draw(renderer)
    renderer.restore()
  }

  delete() {
    this.machine.delete()
    this.artboard.delete()
  }
}
