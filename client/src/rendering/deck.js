import { random } from '@/util'
import { Label } from './label'
import { ANGLE_360, CARD_HEIGHT, CARD_WIDTH, HALF_CARD_HEIGHT, Position } from './position'


const CARD_RADIUS = Math.sqrt(CARD_WIDTH * CARD_WIDTH + CARD_HEIGHT * CARD_HEIGHT)
const EPSILON = CARD_RADIUS + 0.5


function distance(x, y, card) {
  const deltaX = x - card.position.x
  const deltaY = y - card.position.y
  return deltaX * deltaX + deltaY * deltaY
}


export class Deck {
  cards = []

  constructor(bounds) {
    this.bounds = bounds
    this.label = new Label(new Position((bounds.left + bounds.right) * 0.5, bounds.bottom + HALF_CARD_HEIGHT + 15), "deck")
  }

  computeArea() {
    if (this.cards.length === 0) {
      return
    }
    const firstCardPos = this.cards[0].position
    let minX = firstCardPos.x, minY = firstCardPos.y, maxX = minX, maxY = minY
    this.cards.forEach(card => {
      const { x, y } = card.position
      if (x < minX) {
        minX = x
      } else if (x > maxX) {
        maxX = x
      }
      if (y < minY) {
        minY = y
      } else if (y > maxY) {
        maxY = y
      }
    })
    this.area = { left: minX - CARD_RADIUS, top: minY - CARD_RADIUS, right: maxX + CARD_RADIUS, bottom: maxY + CARD_RADIUS }
  }

  resize(size, cardPool) {
    const cards = this.cards
    if (cards.length > size) {
      cardPool.splice(0, 0, ...cards.splice(size))
    } else if (cards.length < size) {
      const { left, top, right, bottom } = this.bounds
      for (let i = cards.length; i < size; i++) {
        const card = cardPool.pop()
        card.setPosition(new Position(random(left, right), random(top, bottom), random(0, ANGLE_360)))
        cards.push(card)
      }
    }
  }

  async shuffle() {
    const { left, top, right, bottom } = this.bounds
    await Promise.all(this.cards.map(card => card.moveTo(new Position(random(left, right), random(top, bottom), random(0, ANGLE_360)))))
    this.computeArea()
  }

  addCard(card) {
    this.cards.push(card)
  }

  takeCard(recalculateArea = true) {
    const card = this.cards.pop()
    if (recalculateArea) {
      const { x, y } = card.position
      const { left, top, right, bottom } = this.area
      if (x - left < EPSILON || y - top < EPSILON || right - x < EPSILON || bottom - y < EPSILON) {
        this.computeArea()
      }
    }
    return card
  }

  enableClick(value = true) {
    this.cards.forEach(card => card.enableClick(value))
  }

  hover(value = true, cardIndex) {
    this.cards[cardIndex].hover(value)
    if (value) {
      this.hoverCard = this.cards[cardIndex]
    } else {
      delete this.hoverCard
    }
  }

  click(cardIndex) {
    const [hoverCard] = this.cards.splice(cardIndex, 1)
    this.cards.push(hoverCard)
    return this.cards.at(-1).click()
  }

  reset() {
    this.cards.forEach(card => card.reset())
  }

  draw(elapsedTimeSec) {
    this.label.draw(elapsedTimeSec)
    this.cards.forEach(card => {
      if (card !== this.hoverCard) card.draw(elapsedTimeSec)
    })
    this.hoverCard?.draw(elapsedTimeSec)
  }

  delete() {
    this.label.delete()
    this.cards.forEach(card => card.delete())
  }

  elementIndexAt(pointX, pointY) {
    const { left, top, right, bottom } = this.area
    if (pointX < left || pointX > right || pointY < top || pointY > bottom) {
      return -1
    }
    let cardIndex = 0
    let minDistance = distance(pointX, pointY, this.cards[0])
    for (let i = 1; i < this.cards.length; i++) {
      const card = this.cards[i]
      const currentDistance = distance(pointX, pointY, card)
      if (currentDistance < minDistance) {
        cardIndex = i
        minDistance = currentDistance
      }
    }
    return cardIndex
  }
}