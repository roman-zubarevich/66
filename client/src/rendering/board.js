import { Button } from './button'
import { Card } from './card'
import { Cat, CatPosition, LEFT } from './cat'
import { initRendering, renderer, setScaleFactor } from './common'
import { Deck } from './deck'
import { Hand } from './hand'
import { Label } from './label'
import { ANGLE_90, ANGLE_180, ANGLE_270, CLOSER_LOOK_SCALE, HALF_CARD_HEIGHT, Position } from './position'
import { Sign } from './sign'
import { reformatDateTime } from '@/util'


let inverseScaleFactor = 1

let canvasLeft = 0
let canvasTop = 0


export class Board {
  static deckBounds = { left: 500, top: 300, right: 800, bottom: 500 }
  static handPositions = [
    new Position(800, 925),
    new Position(75, 500, ANGLE_90),
    new Position(400, 75, ANGLE_180),
    new Position(800, 75, ANGLE_180),
    new Position(1200, 75, ANGLE_180),
    new Position(1525, 500, ANGLE_270),
  ]
  static catPositions = [
    new CatPosition(1250, 925, LEFT),
    new CatPosition(75, 800),
    new CatPosition(100, 75),
    new CatPosition(500, 75),
    new CatPosition(900, 75, LEFT),
    new CatPosition(1525, 200, LEFT),
  ]
  static deckCardPosition = new Position(800, 600, 0, CLOSER_LOOK_SCALE)
  static discardedCardPosition = new Position(1100, 500)

  discarded = []
  actions = []

  get discardedCard() {
    return this.discarded.at(-1)
  }

  constructor(gameInfo, suspendInfo, canvas, actionHandler, exitHandler) {
    this.playerIndex = gameInfo.playerIndex

    const discardedLabelPosition = Board.discardedCardPosition.copy()
    discardedLabelPosition.y += HALF_CARD_HEIGHT + 20
    this.discardedLabel = new Label(discardedLabelPosition, "discarded")
    this.turnLabel = new Label(new Position(20, 925, 0, 1.2), "", false)
    this.infoLabel = new Label(new Position(800, 500, 0, 1.5), "")

    this.discardButton = new Button(980, 590, "DiscardButton")
    this.showCardsButton = new Button(1050, 880, "ShowCardsButton")
    this.okButton = new Button(1050, 880, "OkButton")
    this.stopButton = new Button(1050, 880, "StopButton")
    this.nextRoundButton = new Button(260, 880, "NextRoundButton")
    this.exitButton = new Button(120, 880, "ExitButton")
    this.buttons = [
      this.discardButton,
      this.showCardsButton,
      this.okButton,
      this.stopButton,
      this.nextRoundButton,
      this.exitButton,
    ]

    this.stopSign = new Sign()

    this.hiddenCards = Array.from(Array(52), () => new Card())

    this.deck = new Deck(Board.deckBounds)

    const playerNames = gameInfo.playerNames
    const hands = Array(playerNames.length)
    const cats = Array(playerNames.length)
    let index = this.playerIndex

    function addHand(positionIndex, nameSuffix = "") {
      hands[index] = new Hand(Board.handPositions[positionIndex], playerNames[index] + nameSuffix)
      cats[index] = new Cat(Board.catPositions[positionIndex])
      index = (index + 1) % playerNames.length
    }

    addHand(0, " (you)")
    switch (playerNames.length) {
      case 2:
        addHand(3)
        break
      case 3:
        addHand(1)
        addHand(5)
        break
      case 4:
        addHand(1)
        addHand(3)
        addHand(5)
        break
      case 5:
        addHand(1)
        addHand(2)
        addHand(4)
        addHand(5)
        break
      default:
        console.error(`Unexpected number of players: ${playerNames.length}`)
    }
    this.hands = hands
    this.cats = cats

    canvas.onmousedown = (event) => this.click(event)
    canvas.onmousemove = (event) => this.mouseMove(event)
    this.canvas = canvas

    this.exitHandler = exitHandler
    this.gameActionHandler = actionHandler

    if (suspendInfo) {
      this.setSuspendInfo(suspendInfo)
    }
    this.clearActions()
  }

  async setState(boardState) {
    Object.assign(this, boardState)
    this.isPlaying = true

    this.clearActions()

    this.stopSign.hide()

    const deck = this.deck
    if (this.finishedRound) {
      // Starting new round
      const promises = this.cats.map(cat => cat.wakeUp())

      if (this.discarded.length > 0) {
        const discardedCard = this.discarded.pop()
        promises.push(discardedCard.hide())
        deck.addCard(discardedCard)

        // Make sure we have enough spare cards to fill deck and hands
        this.discarded.forEach(card => card.hide(true))
        this.hiddenCards.push(...this.discarded)
        this.discarded.length = 0
      }

      this.hands.forEach((hand, index) => {
        hand.setScore(this.totalScores[index])
        promises.push(hand.hideCards())
        hand.takeAllCards().forEach(card => deck.addCard(card))
      })
      await Promise.all(promises)
      
      deck.resize(52, this.hiddenCards)
      deck.reset()
      await deck.shuffle()
      
      await Promise.all(this.hands.map(hand => hand.populate(Array.from(Array(4), () => deck.takeCard(false)))))
      deck.computeArea()
    } else {
      // Resuming suspended game
      for (let i = 0; i < this.cats.length; i++) {
        if (i !== this.activePlayerIndex) {
          this.cats[i].sleep(true)
        }
      }
  
      deck.resize(this.deckSize, this.hiddenCards)

      this.handSizes.forEach((handSize, index) => {
        const hand = this.hands[index]
        hand.setScore(this.totalScores[index])
        hand.resize(handSize, this.hiddenCards)
      })
  
      if (this.discardedValue !== undefined) {
        const discardedCard = this.hiddenCards.pop()
        discardedCard.setPosition(Board.discardedCardPosition.copy())
        discardedCard.setValue(this.discardedValue)
        discardedCard.show(true)
        this.discarded.push(discardedCard)
      }
  
      if (this.stopperIndex !== undefined) {
        this.stopRequested(this.stopperIndex, true)
      }
    }

    deck.computeArea()

    this.turnLabel.setValue(`round ${this.round}\nturn ${this.turn}`)
    this.infoLabel.setValue()
  }

  reset() {
    if (!this.isPlaying) {
      return
    }

    this.stopSign.reset()
    this.cats.forEach(cat => cat.reset())
    this.deck.reset()
    if (this.movingCard) {
      this.movingCard.reset()
      this.hiddenCards.push(this.movingCard)
      delete this.movingCard
    }
    if (this.deckCard) {
      this.deckCard.reset()
      this.hiddenCards.push(this.deckCard)
      delete this.deckCard
    }
    this.hands.forEach(hand => {
      hand.reset()
      hand.takeAllCards().forEach(card => this.hiddenCards.push(card))
    })
    this.discarded.forEach(card => card.reset())
    this.hiddenCards.push(...this.discarded)
    this.discarded.length = 0

    this.clearActions()
    this.finishedRound = false
    this.isPlaying = false
  }

  setSuspendInfo({ playerOnlineStatuses, startTimeStr, suspendTimeStr }) {
    if (!playerOnlineStatuses.includes(false)) {
      return
    }

    this.hands.forEach((hand, index) => hand.setSuffix(playerOnlineStatuses[index] ? "" : " (away)"))
    this.infoLabel.setValue(`Game started at ${reformatDateTime(startTimeStr)},\nsuspended at ${reformatDateTime(suspendTimeStr)}`)
    this.reset()
  }

  async update(deckCard, discardedValue, actions) {
    if (deckCard !== undefined) {
      this.deckCard = this.deck.takeCard()
      this.deckCard.setValue(deckCard)
      await this.deckCard.moveTo(Board.deckCardPosition)
      await this.deckCard.show()
    }

    if (discardedValue !== undefined) {
      if (this.deckCard) {
        // Discard our deck card
        this.movingCard = this.deckCard
        delete this.deckCard
      } else {
        // Another player discards a new card from deck
        this.movingCard = this.deck.takeCard()
        this.movingCard.setValue(discardedValue)
        await this.movingCard.show()
      }
      await this.movingCard.moveTo(Board.discardedCardPosition)
      this.discarded.push(this.movingCard)
      delete this.movingCard
    }

    this.setupActions(actions)
  }

  async startTurn(activePlayerIndex, turn, actions) {
    if (this.activePlayerIndex !== activePlayerIndex) {
      if (this.activePlayerIndex >= 0) {
        this.cats[this.activePlayerIndex].sleep()
      }
      await this.cats[activePlayerIndex].wakeUp()
      this.activePlayerIndex = activePlayerIndex
    }

    this.turnLabel.setValue(`round ${this.round}\nturn ${turn}`)

    this.setupActions(actions)
  }

  setupActions(actions) {
    this.clearActions()
    if (actions) {
      actions.forEach(action => {
        switch (action) {
          case "StartNextRound":
            this.addAction(this.nextRoundButton, () => this.act(action))
            break
          case "TakeCardFromDeck":
            this.addAction(this.deck, () => this.act(action))
            break
          case "TakeDiscardedCard":
            this.addAction(this.discardedCard, () => this.act(action))
            break
          case "Discard":
            this.addAction(this.discardButton, () => this.act(action))
            break
          case "PickOwnCard":
            this.addAction(this.hands[this.playerIndex], cardIndex => this.act(action, { cardIndex }))
            break
          case "PickAnothersCard":
            this.hands.forEach((hand, playerIndex) => {
              if (playerIndex !== this.playerIndex) {
                this.addAction(hand, cardIndex => this.act(action, { playerIndex, cardIndex }))
              }
            })
            break
          case "ShowCards":
            this.addAction(this.showCardsButton, () => {
              this.clearActions()

              const hand = this.hands[this.playerIndex]
              const cardIndexesSet = new Set()
              this.addAction(hand, cardIndex => {
                if (cardIndexesSet.has(cardIndex)) {
                  cardIndexesSet.delete(cardIndex)
                  hand.select(cardIndex, false)
                  if (cardIndexesSet.size === 1) {
                    // Disable "ok" button
                    this.okButton.enableClick(false)
                    this.actions.pop()
                  }
                } else {
                  cardIndexesSet.add(cardIndex)
                  hand.select(cardIndex)
                  if (cardIndexesSet.size === 2) {
                    // Enable "ok" button
                    this.addAction(this.okButton, () => {
                      const cardIndexes = Array.from(cardIndexesSet)
                      cardIndexes.forEach(index => hand.select(index, false))
                      this.act(action, { cardIndexes })
                    })
                  }
                }
              })
            })
            break
          case "StopRound":
            this.addAction(this.stopButton, () => this.act(action))
            break
          default:
            console.error("Unexpected action", action)
        }
      })

      if (this.lastMouseEvent) {
        this.mouseMove(this.lastMouseEvent)
      }
    }
  }

  addAction(item, handler) {
    item.enableClick()
    this.actions.push({ item, handler })
  }

  clearActions(allowSuspend = true) {
    this.actions.forEach(action => action.item.enableClick(false))
    this.actions.length = 0
    this.addAction(this.exitButton, () => {
      if (allowSuspend) {
        this.act("SuspendGame")
      } else {
        this.exitHandler()
      }
    })
    if (!!this.hoverItem && this.hoverItem !== this.exitButton) {
      this.hoverItem.hover(false, this.hoverElementIndex)
      delete this.hoverItem
      this.canvas.style.cursor = "auto"
    }
  }

  act(action, params = {}) {
    this.clearActions()
    this.gameActionHandler(action, params)
  }

  async exchangeCards(playerIndex, cardIndex, anotherPlayerIndex, anotherPlayerCardIndex) {
    await this.hands[playerIndex].exchangeCardWith(cardIndex, this.hands[anotherPlayerIndex], anotherPlayerCardIndex)
  }

  async replaceCards(playerIndex, cardIndexes, fromDeck, discardedValue) {
    const hand = this.hands[playerIndex]

    if (cardIndexes.length == 1 || playerIndex === this.playerIndex) {
      // Reveal the hand card(s) that will be replaced
      await Promise.all(cardIndexes.map(cardIndex => hand.showCard(cardIndex, discardedValue)))
    }

    let hideMovingCard = false
    if (fromDeck) {
      if (this.deckCard) {
        // Replace by our deck card
        this.movingCard = this.deckCard
        await this.movingCard.hide()
        delete this.deckCard
      } else {
        // Replace by a new card from deck
        this.movingCard = this.deck.takeCard()
      }
    } else {
      // Replace by discarded card
      this.movingCard = this.discarded.pop()
      hideMovingCard = true
    }

    // Discard cards from player's hand
    const promises = cardIndexes.map(cardIndex => hand.moveCardTo(cardIndex, Board.discardedCardPosition).then(card => {
      this.discarded.push(card)
    }))

    promises.push(hand.replaceCardsBy(cardIndexes, this.movingCard, hideMovingCard).then(() => delete this.movingCard))
    await Promise.all(promises)
  }

  async revealCards(cardInfos) {
    const promises = []
    // Wake up all looking cats
    for (let { lookingPlayerIndexes } of cardInfos) {
      for (let index of lookingPlayerIndexes) {
        promises.push(this.cats[index].wakeUp())
      }
    }
    await Promise.all(promises)
    promises.length = 0

    // Move looking cats to their targets
    for (let { lookingPlayerIndexes, targetPlayerIndex, cardIndexes, values } of cardInfos) {
      const targetHand = this.hands[targetPlayerIndex]
      if (values && (cardIndexes.length === 1 || lookingPlayerIndexes[0] === targetPlayerIndex)) {
        // Looking privately, so only one (our own) cat will move
        const lookingPlayerIndex = lookingPlayerIndexes[0]
        const { x, y, angle } = targetHand.getCloserLookingCatPosition(cardIndexes.at(-1))
        const cat = this.cats[lookingPlayerIndex]
        const movementMethod = lookingPlayerIndex === targetPlayerIndex ? "walkTo" : "jumpTo"
        promises.push(cat[movementMethod](x, y).then(() => cat.lookTo(angle)))
      } else {
        // Put card(s) owner's cat to sleep
        if (values) {
          promises.push(this.cats[targetPlayerIndex].sleep())
        }
        // Position cats based on their count
        let { x, y, stepX, stepY, angle } = targetHand.getCatAlignData(lookingPlayerIndexes.length, 150)
        for (let index of lookingPlayerIndexes) {
          const cat = this.cats[index]
          const movementMethod = index === targetPlayerIndex ? "walkTo" : "jumpTo"
          promises.push(cat[movementMethod](x, y).then(() => cat.lookTo(angle)))
          x += stepX
          y += stepY
        }
      }
    }
    await Promise.all(promises)
    promises.length = 0

    // Show cards
    for (let { lookingPlayerIndexes, targetPlayerIndex, cardIndexes, values } of cardInfos) {
      const targetHand = this.hands[targetPlayerIndex]
      if (values) {
        // Cards are revealed to this player, so show their values
        const isPrivate = cardIndexes.length === 1 || lookingPlayerIndexes[0] === targetPlayerIndex
        if (isPrivate || values.some(value => value !== values[0])) {
          promises.push(targetHand.peekCards(cardIndexes, values, isPrivate))
        } else {
          // Don't hide the cards if they are all equal and belong to a different player (successful "show multiple cards" scenario)
          promises.push(targetHand.showCards(cardIndexes, values))
        }
      } else {
        // Cards are revealed to some other players
        promises.push(targetHand.showCardsToOthers(cardIndexes))
      }
    }
    await Promise.all(promises)
    promises.length = 0

    // Return looking cats to their homes
    for (let { lookingPlayerIndexes, targetPlayerIndex } of cardInfos) {
      for (let index of lookingPlayerIndexes) {
        const cat = this.cats[index]
        const movementMethod = index === targetPlayerIndex ? "walkBack" : "jumpBack"
        promises.push(cat[movementMethod]().then(() => {
          cat.lookForward()
          const conditionMethod = index === this.activePlayerIndex ? "wakeUp" : "sleep"
          cat[conditionMethod]()
        }))
      }
    }
    await Promise.all(promises)
  }

  async stopRequested(stopperIndex, instantly = false) {
    await this.cats[stopperIndex].requestStop(this.stopSign, instantly)
  }

  async finishRound(result) {
    const promises = []
    result.hands.forEach((handValues, handIndex) => {
      const hand = this.hands[handIndex]
      handValues.forEach((value, index) => promises.push(hand.showCard(index, value)))
    })
    await Promise.all(promises)
    const totalScores = result.totalScores
    this.hands.forEach((hand, handIndex) => {
      hand.setScore(totalScores[handIndex], result.scores[handIndex])
    })
    if (result.isGameFinished) {
      await Promise.all(this.cats.map(cat => cat.wakeUp()))
      const minTotal = Math.min(...totalScores)
      totalScores.forEach((total, index) => {
        if (total === minTotal) {
          this.cats[index].crown()
        }
      })
      this.clearActions(false)
    } else {
      this.totalScores = totalScores
      this.finishedRound = true
    }
  }

  draw(elapsedTimeSec) {
    if (this.isDestroyed) {
      return false
    }
    renderer.clear()

    this.buttons.forEach(button => button.draw(elapsedTimeSec))

    if (this.isPlaying) {
      this.discardedLabel.draw(elapsedTimeSec)
      this.turnLabel.draw(elapsedTimeSec)

      this.deck.draw(elapsedTimeSec)
      this.hands.forEach(hand => hand.draw(elapsedTimeSec))
      this.deckCard?.draw(elapsedTimeSec)
      if (this.discarded.length > 0) {
        this.discardedCard.draw(elapsedTimeSec)
      }
      this.stopSign.draw(elapsedTimeSec)
      this.cats.forEach(cat => cat.draw(elapsedTimeSec))
      this.hands.forEach(hand => hand.drawRevealed(elapsedTimeSec))
      this.movingCard?.draw(elapsedTimeSec)
    } else {
      this.infoLabel.draw(elapsedTimeSec)
      this.hands.forEach(hand => hand.draw(elapsedTimeSec))
    }
    return true
  }

  destroy() {
    this.canvas.onmousedown = null
    this.canvas.onmousemove = null

    this.discardedLabel.delete()
    this.turnLabel.delete()
    this.infoLabel.delete()

    this.deck.delete()
    this.deckCard?.delete()
    this.hands.forEach(hand => hand.delete())
    this.movingCard?.delete()
    this.discarded.forEach(card => card.delete())

    this.buttons.forEach(button => button.delete())

    this.cats.forEach(cat => cat.delete())
    this.stopSign.delete()

    Hand.count = 0
    Card.count = 0
    Cat.count = 0
    this.isDestroyed = true
  }

  mouseMove(event) {
    this.lastMouseEvent = event
    const x = (event.x - canvasLeft) * inverseScaleFactor
    const y = (event.y - canvasTop) * inverseScaleFactor
    const hoverItem = this.hoverItem
    if (hoverItem) {
      const hoverElementIndex = this.hoverElementIndex
      const elementIndex = hoverItem.elementIndexAt(x, y)
      if (elementIndex < 0) {
        hoverItem.hover(false, hoverElementIndex)
        delete this.hoverItem

        this.actions.forEach(action => {
          const item = action.item
          if (item !== hoverItem) {
            const elementIndex = item.elementIndexAt(x, y)
            if (elementIndex >= 0) {
              item.hover(true, elementIndex)
              this.hoverItem = item
              this.hoverElementIndex = elementIndex
              this.actionHandler = action.handler
              return
            }
          }
        })
        this.canvas.style.cursor = "auto"
      } else if (elementIndex !== hoverElementIndex) {
        hoverItem.hover(false, hoverElementIndex)
        hoverItem.hover(true, elementIndex)
        this.hoverElementIndex = elementIndex
      }
    } else {
      this.actions.forEach(action => {
        const item = action.item
        const elementIndex = item.elementIndexAt(x, y)
        if (elementIndex >= 0) {
          item.hover(true, elementIndex)
          this.hoverItem = item
          this.hoverElementIndex = elementIndex
          this.actionHandler = action.handler
          this.canvas.style.cursor = "pointer"
          return
        }
      })
    }
  }

  click() {
    if (this.hoverItem && !this.isHandlingAction) {
      const elementIndex = this.hoverElementIndex
      const handler = this.actionHandler
      this.isHandlingAction = true
      this.hoverItem.click(elementIndex).then(() => {
        handler(elementIndex)
        this.isHandlingAction = false
      })
    }
  }
}

export function createBoard(riveData, canvas, gameInfo, suspendedInfo, actionHandler, exitHandler) {
  initRendering(riveData, canvas)
  const board = new Board(gameInfo, suspendedInfo, canvas, actionHandler, exitHandler)

  const rive = riveData.rive
  let lastTime = 0
  function renderLoop(time) {
    if (!lastTime) {
      lastTime = time;
    }
    const elapsedTimeMs = time - lastTime
    const elapsedTimeSec = elapsedTimeMs / 1000
    lastTime = time

    if (board.draw(elapsedTimeSec)) {
      rive.requestAnimationFrame(renderLoop)
    }
  }
  rive.requestAnimationFrame(renderLoop)
  return board
}

export function setGlobalScale(scale) {
  setScaleFactor(scale)
  inverseScaleFactor = 1 / scale
}

export function setCanvasBounds(left, top) {
  canvasLeft = left
  canvasTop = top
}
