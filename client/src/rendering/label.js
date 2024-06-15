import { assets, renderer, scaleFactor } from "./common"


export class Label {
  constructor(position, value = "", isCentered = true) {
    this.position = position
    const artboard = assets.artboardByName(isCentered ? "CenteredLabel" : "Label")
    this.artboard = artboard
    this.textRun = artboard.textRun("Value")
    this.setValue(value)
  }

  setValue(value = "") {
    this.textRun.text = value
  }

  draw(elapsedTimeSec) {
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
    this.artboard.delete()
  }
}
