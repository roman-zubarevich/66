export let rive = null
export let assets = null
export let renderer = null

export let scaleFactor = 1

export function setScaleFactor(scale) {
  scaleFactor = scale
}

export function initRendering(riveData, canvas) {
  rive = riveData.rive
  assets = riveData.assets
  renderer = rive.makeRenderer(canvas)
}

export function movementPromise(holderObj) {
  return new Promise((resolve, reject) => {
    holderObj.done = resolve
    holderObj.cancelled = reject
  })
}

export function timeoutPromise(holderObj, callback, ms) {
  return new Promise((resolve, reject) => {
    holderObj.rejectTimeoutPromise = reject
    holderObj.timeoutId = setTimeout(async () => {
      try {
        delete holderObj.rejectTimeoutPromise
        await callback()
        resolve()
      } catch (error) {
        reject()
      }
    }, ms)
  })  
}

export function cancelTimeout(holderObj) {
  if (holderObj.rejectTimeoutPromise) {
    clearTimeout(holderObj.timeoutId)
    holderObj.rejectTimeoutPromise()
    delete holderObj.rejectTimeoutPromise
  }
}