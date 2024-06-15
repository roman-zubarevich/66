const PI = 3.14
export const ANGLE_90 = PI * 0.5
export const ANGLE_180 = PI
export const ANGLE_270 = PI * 1.5
export const ANGLE_360 = PI * 2

export const MOTION_FRAMES = 30
const MOTION_FRAMES_INVERSE = 1 / MOTION_FRAMES

export const CARD_WIDTH = 80
export const CARD_HEIGHT = 100
export const HALF_CARD_WIDTH = CARD_WIDTH / 2
export const HALF_CARD_HEIGHT = CARD_HEIGHT / 2

export const CLOSER_LOOK_SCALE = 3

export const PEEK_TIME = 2000


export function interpolate(start, end, progress) {
  return start + (end - start) * progress
}

export class Position {
  constructor(x, y, angle = 0, scale = 1) {
    this.x = x
    this.y = y
    this.angle = angle
    this.scale = scale
  }

  copy() {
    return new Position(this.x, this.y, this.angle, this.scale)
  }

  interpolate(motion) {
    const progress = motion.frame * MOTION_FRAMES_INVERSE
    this.x = interpolate(motion.start.x, motion.end.x, progress)
    this.y = interpolate(motion.start.y, motion.end.y, progress)
    this.angle = interpolate(motion.start.angle, motion.end.angle, progress)
    this.scale = interpolate(motion.start.scale, motion.end.scale, progress)
  }
}
