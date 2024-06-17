import { cancelTimeout, timeoutPromise } from './common'
import { Label } from './label'
import { ANGLE_90, ANGLE_180, ANGLE_270, CARD_WIDTH, CARD_HEIGHT, CLOSER_LOOK_SCALE, HALF_CARD_HEIGHT, HALF_CARD_WIDTH, PEEK_TIME, Position } from './position'


const CARD_INTERVAL = 30
const CARD_STEP = CARD_WIDTH + CARD_INTERVAL

const CLOSER_LOOK_X_OFFSET = (CARD_WIDTH * CLOSER_LOOK_SCALE - CARD_HEIGHT) * 0.5 + 8
const CLOSER_LOOK_Y_OFFSET = (CARD_HEIGHT * CLOSER_LOOK_SCALE - CARD_HEIGHT) * 0.5 + 10
const CLOSER_LOOK_FIRST_OWN_CARD_X_OFFSET = CARD_WIDTH * (CLOSER_LOOK_SCALE - 1)

const LABEL_OFFSET = HALF_CARD_HEIGHT + 20
const CAT_ROW_X_OFFSET = HALF_CARD_HEIGHT + 100
const CAT_ROW_Y_OFFSET = HALF_CARD_HEIGHT + 70
const CAT_CLOSER_LOOK_X_OFFSET = HALF_CARD_WIDTH * CLOSER_LOOK_SCALE + 70
const CAT_CLOSER_LOOK_Y_OFFSET = HALF_CARD_HEIGHT * CLOSER_LOOK_SCALE + 70


export class Hand {
  static count = 0

  cards = []
  revealedCardByIndex = {}

  constructor(position, name) {
    this.id = Hand.count++
    this.center = position

    const catOffset = { x: 0, y: 0 }
    const labelPosition = position.copy()
    labelPosition.scale = 1.2
    switch (position.angle) {
      case 0:
        labelPosition.y -= LABEL_OFFSET
        catOffset.y = -CAT_ROW_Y_OFFSET
        break;
      case ANGLE_90:
        labelPosition.x += LABEL_OFFSET
        catOffset.x = CAT_ROW_X_OFFSET
        break;
      case ANGLE_180:
        labelPosition.y += LABEL_OFFSET
        labelPosition.angle = 0
        catOffset.y = CAT_ROW_Y_OFFSET
        break;
      case ANGLE_270:
        labelPosition.x -= LABEL_OFFSET
        catOffset.x = -CAT_ROW_X_OFFSET
        break;
    }
    this.label = new Label(labelPosition, name)
    this.name = name
    this.catOffset = catOffset
  }

  async resize(size, cardPool) {
    const cards = this.cards
    if (cards.length < size) {
      for (let i = cards.length; i < size; i++) {
        const card = cardPool.pop()
        card.setPosition(this.center.copy())
        cards.push(card)
      }
      await this.align(true)
    }
  }

  takeAllCards() {
    const allCards = [...this.cards]
    this.cards.length = 0
    return allCards
  }

  reset() {
    cancelTimeout(this)

    const revealedCardByIndex = this.revealedCardByIndex
    for (let index in revealedCardByIndex) {
      this.cards[index] = revealedCardByIndex[index]
      delete revealedCardByIndex[index]
    }

    this.cards.forEach(card => card.reset())
  }

  setScore(totalScore, roundScore) {
    let roundSuffix = ""
    if (roundScore !== undefined) {
      const winner = roundScore === 0 ? " | round winner!" : ""
      roundSuffix = ` | round score: ${roundScore}${winner}`
    }
    this.setSuffix(` | total score: ${totalScore}${roundSuffix}`)
  }

  setSuffix(suffix = "") {
    this.label.setValue(`${this.name}${suffix}`)
  }

  getAlignData(x, y, count, step) {
    const startOffset = (count - 1) * step * 0.5
    let stepX = 0, stepY = 0
    switch (this.center.angle) {
      case 0:
        x -= startOffset
        stepX = step
        break
      case ANGLE_90:
        y -= startOffset
        stepY = step
        break
      case ANGLE_180:
        x += startOffset
        stepX = -step
        break
      case ANGLE_270:
        y += startOffset
        stepY = -step
        break
      default:
        console.error(`Unexpected angle for hand ${this.id}: ${this.center.angle}`)
    }
    return { x, y, stepX, stepY }
  }

  async align(instantly = false) {
    const angle = this.center.angle
    let { x, y, stepX, stepY } = this.getAlignData(this.center.x, this.center.y, this.cards.length, CARD_STEP)
    if (instantly) {
      this.cards.forEach(card => {
        card.position.x = x
        card.position.y = y
        card.position.angle = angle
        x += stepX
        y += stepY
      })
    } else {
      await Promise.all(this.cards.map(card => {
        const alignedPosition = new Position(x, y, angle)
        x += stepX
        y += stepY
        return card.moveTo(alignedPosition)
      }))
    }
  }

  getCloserLookingCatPosition(lastCardIndex) {
    let { x, y, angle } = this.cards[lastCardIndex].position
    switch (this.center.angle) {
      case 0:
        x += CAT_CLOSER_LOOK_X_OFFSET
        y -= CAT_ROW_Y_OFFSET
        angle = ANGLE_90
        break
      case ANGLE_90:
        x += CAT_ROW_X_OFFSET
        y += CAT_CLOSER_LOOK_Y_OFFSET
        angle = ANGLE_180
        break
      case ANGLE_180:
        x -= CAT_CLOSER_LOOK_X_OFFSET
        y += CAT_ROW_Y_OFFSET
        angle = ANGLE_270
        break
      case ANGLE_270:
        x -= CAT_ROW_X_OFFSET
        y -= CAT_CLOSER_LOOK_Y_OFFSET
        angle = 0
        break
      default:
        console.error(`Unexpected angle for hand ${this.id}: ${this.center.angle}`)
    }
    return { x, y, angle }
  }

  getCatAlignData(count, step) {
    const { x, y } = this.center
    const alignData = this.getAlignData(x + this.catOffset.x, y + this.catOffset.y, count, step)
    alignData.angle = this.center.angle
    return alignData
  }

  async showCard(index, value) {
    const card = this.cards[index]
    card.setValue(value)
    if (card.position.angle !== 0) {
      const endPosition = card.position.copy()
      endPosition.angle = 0
      await card.moveTo(endPosition)
    }
    await card.show()
  }

  async hideCards() {
    await Promise.all(this.cards.map(card => card.hide()))
  }

  async populate(cards) {
    this.cards = cards
    await this.align()
  }

  async peekCards(indexes, values, isPrivate) {
    let xOffset = 0, yOffset = 0, scale = 1, isXOffsetOneTime = false
    if (isPrivate) {
      scale = CLOSER_LOOK_SCALE
      switch (this.center.angle) {
        case 0:
          // The only case of privately peeking multiple cards is revealing 2 own cards on round start
          if (indexes.length == 2) {
            xOffset = -CLOSER_LOOK_FIRST_OWN_CARD_X_OFFSET
            isXOffsetOneTime = true
          }
          yOffset = -CLOSER_LOOK_Y_OFFSET
          break
        case ANGLE_90:
          xOffset = CLOSER_LOOK_X_OFFSET
          break
        case ANGLE_180:
          yOffset = CLOSER_LOOK_Y_OFFSET
          break
        case ANGLE_270:
          xOffset = -CLOSER_LOOK_X_OFFSET
          break
        default:
          console.error(`Unexpected angle for hand ${this.id}: ${this.center.angle}`)
      }
    }

    const cards = this.cards
    const revealedCardByIndex = this.revealedCardByIndex
    const promises = []
    const initialPositionByIndex = {}
    for (let i in indexes) {
      const index = indexes[i]
      const card = cards[index]
      card.setValue(values[i])

      const initialPosition = card.position.copy()
      initialPositionByIndex[index] = initialPosition
      const viewingPosition = new Position(initialPosition.x + xOffset, initialPosition.y + yOffset, 0, scale)
      if (isXOffsetOneTime) {
        xOffset = 0
      }

      // Need to separate peeked cards to draw them above others
      revealedCardByIndex[index] = card
      cards[index] = undefined

      promises.push(card.moveTo(viewingPosition).then(() => card.show()))
    }
    await Promise.all(promises)

    async function returnPeekedCard(index) {
      const card = revealedCardByIndex[index]
      await card.hide()
      await card.moveTo(initialPositionByIndex[index])
      cards[index] = card
      delete revealedCardByIndex[index]
    }

    promises.length = 0
    await timeoutPromise(this, async () => {
      for (let index in revealedCardByIndex) {
        promises.push(returnPeekedCard(index))
      }
      await Promise.all(promises)
    }, PEEK_TIME)
  }

  async showCards(indexes, values) {
    const promises = []
    for (let i in indexes) {
      const card = this.cards[indexes[i]]
      card.setValue(values[i])
      const viewingPosition = new Position(card.position.x, card.position.y)
      promises.push(card.moveTo(viewingPosition).then(() => card.show()))
    }
    await Promise.all(promises)
  }

  showCardsToOthers(indexes) {
    return Promise.all(indexes.map(index => this.cards[index].showToOthers()))
  }

  moveCardTo(index, position) {
    return this.cards[index].moveTo(position)
  }

  async exchangeCardWith(index, anotherHand, anotherCardIndex) {
    const card = this.cards[index]
    const anotherCard = anotherHand.cards[anotherCardIndex]
    await Promise.all([
      card.moveTo(anotherCard.position.copy()),
      anotherCard.moveTo(card.position.copy())
    ])
    this.cards[index] = anotherCard
    anotherHand.cards[anotherCardIndex] = card
    card.mark()
    anotherCard.mark()
  }

  // Note: replaced cards are removed from the hand but not deleted
  async replaceCardsBy(indexes, newCard, hideNewCard = false) {
    const endPosition = this.cards[indexes[0]].position.copy()
    endPosition.angle = this.center.angle
    await newCard.moveTo(endPosition)
    if (hideNewCard) {
      await newCard.hide()
    }
    this.cards[indexes[0]] = newCard
    if (indexes.length > 1) {
      indexes.reverse()
      indexes.pop()
      indexes.forEach(index => this.cards.splice(index, 1))
      await this.align()
    }
    newCard.mark()
  }

  enableClick(value = true) {
    this.cards.forEach(card => card.enableClick(value))
  }

  hover(value = true, index) {
    this.cards[index].hover(value)
  }

  click(index) {
    return this.cards[index].click()
  }

  select(index, value = true) {
    const card = this.cards[index]
    const endPosition = card.position.copy()
    endPosition.y += value ? -20 : 20
    return card.moveTo(endPosition)
  }

  draw(elapsedTimeSec) {
    this.label.draw(elapsedTimeSec)
    this.cards.forEach(card => card?.draw(elapsedTimeSec))
  }

  drawRevealed(elapsedTimeSec) {
    Object.values(this.revealedCardByIndex).forEach(card => card.draw(elapsedTimeSec))
  }

  delete() {
    this.label.delete()
    this.cards.forEach(card => card?.delete())
    Object.values(this.revealedCardByIndex).forEach(card => card.delete())
  }

  elementIndexAt(pointX, pointY) {
    return this.cards.findIndex(card => card.elementIndexAt(pointX, pointY) >= 0)
  }
}