import { BrowserWindow } from 'electron'
import { BrowserWindowConstructorOptions, Event, Rectangle } from 'electron/main'
import { windowsOptions, windowsSize, proportionSize } from '../types/windows'
import merge from 'lodash/merge'
import startsWith from 'lodash/startsWith'

class windows {
  id: number = 0
  private name: string
  instance: BrowserWindow | null = null
  private options: BrowserWindowConstructorOptions = { webPreferences: {} }
  private url: string = ''
  private devTools: boolean = false
  private proportion: boolean = false
  private proportionSize: proportionSize = {}
  private min: boolean = false
  private minSize: windowsSize = { width: 0, height: 0 }
  private max: boolean = false
  private maxSize: windowsSize = { width: 0, height: 0 }
  private preload: string = ''
  private windowsOption: windowsOptions
  constructor(name: string, options: windowsOptions) {
    this.name = name
    this.windowsOption = options
  }
  private formatOptions(options: windowsOptions) {
    this.url = options.url
    if (options.width) {
      this.options.width = options.width
    }
    if (options.height) {
      this.options.height = options.height
    }
    if (options.preload && this.preload != '') {
      this.options = merge(this.options, {
        webPreferences: {
          preload: this.preload,
        },
      })
    }
    if (options.devTools) {
      this.devTools = true
    }
    if (options.frame) {
      this.options.frame = false
    }
    if (options.transparent) {
      this.options.transparent = true
    }
    if (options.parent instanceof BrowserWindow) {
      this.options.parent = options.parent
    }
    if (options.options) {
      this.options = merge(this.options, options.options)
    }
    if (options.proportion) {
      this.formatProportion(options.proportion)
    }
    if (!this.proportion && options.min) {
      const { width = 0, height = 0 } = options.min
      if (width > 0 && height > 0) {
        this.min = true
        this.minSize = options.min
      }
    }
    if (!this.proportion && options.max) {
      const { width = 0, height = 0 } = options.max
      if (width > 0 && height > 0) {
        this.max = true
        this.maxSize = options.max
      }
      this.options.maximizable = false
    }
  }
  private formatProportion(proportionSize: proportionSize) {
    if (this.options.width && this.options.height) {
      let { fixedWidth = 0, fixedHeiht = 0, minWidth = 0, maxWidth = 0 } = proportionSize
      this.proportion = true
      if (minWidth === 0) {
        minWidth = Math.round((this.options.width - fixedWidth) / 2)
      }
      if (maxWidth == 0) {
        maxWidth = (this.options.width - fixedWidth) * 2
      }
      this.proportionSize = {
        fixedWidth,
        fixedHeiht,
        minWidth,
        maxWidth,
      }
      this.options.maximizable = false
    }
  }
  create(): boolean {
    if (this.instance) {
      return false
    }
    this.formatOptions(this.windowsOption)
    this.instance = new BrowserWindow(this.options)
    this.openUrl(this.url)
    if (this.devTools) {
      this.openDevTools()
    }
    this.registerEvent()
    this.id = this.instance.webContents.id
    return true
  }
  setPreload(preload: string): windows {
    this.preload = preload
    return this
  }
  reload(): boolean {
    this.instance?.reload()
    return true
  }
  close(): boolean {
    this.instance?.destroy()
    this.instance = null
    return true
  }
  openUrl(url: string) {
    if (startsWith(url, 'http://') || startsWith(url, 'https://') || startsWith(url, 'file://')) {
      this.instance?.loadURL(url)
    } else {
      this.instance?.loadFile(url)
    }
    return true
  }
  openDevTools() {
    this.instance?.webContents.openDevTools()
  }
  closeDevTools() {
    this.instance?.webContents.closeDevTools()
  }
  getSize() {
    return this.instance?.getContentSize()
  }
  setSize(size: windowsSize): boolean {
    this.instance?.setContentSize(size.width, size.height)
    return true
  }
  hide(): boolean {
    this.instance?.hide()
    return true
  }
  show(): boolean {
    this.instance?.show()
    return true
  }
  minimize(): boolean {
    this.instance?.minimize()
    return true
  }
  maximize(): boolean {
    this.instance?.maximize()
    return true
  }
  setProportion(proportionSize: proportionSize): boolean {
    if (this.proportion) {
      this.instance?.off('will-resize', this.proportionEvent.bind(this))
    }
    this.formatProportion(proportionSize)
    return true
  }
  callFunction(method: string, ...args: any): any {
    if (!this.instance || typeof this.instance[method as keyof BrowserWindow] !== 'function') {
      return false
    }
    try {
      // @ts-ignore
      return this.instance[method as keyof BrowserWindow](...args)
    } catch (_) {
      return false
    }
  }
  private registerEvent() {
    this.instance?.on('closed', () => {
      this.instance = null
    })
    if (this.proportion) {
      this.instance?.on('will-resize', this.proportionEvent.bind(this))
    }
    if (this.min || this.max) {
      this.instance?.on('will-resize', this.minOrMaxEvent.bind(this))
    }
  }
  private proportionEvent(event: Event, newBounds: Rectangle) {
    event.preventDefault()
    const { width } = newBounds
    const { fixedWidth = 0, fixedHeiht = 0, minWidth = 0, maxWidth = 0 } = this.proportionSize
    if (minWidth && width < minWidth) {
      return
    }
    if (maxWidth && width > maxWidth) {
      return
    }
    const ratio = (width - fixedWidth) / <number>this.options.width
    const nowHeight = Math.round(ratio * (<number>this.options.height - fixedHeiht) + fixedHeiht)
    this.instance?.setContentSize(width, nowHeight)
  }
  private minOrMaxEvent(event: Event, newBounds: Rectangle) {
    const { width, height } = newBounds
    if (this.min && (width < this.minSize.width || height < this.minSize.height)) {
      event.preventDefault()
    }
    if (this.max && (width > this.maxSize.width || height > this.maxSize.height)) {
      event.preventDefault()
    }
  }
}

export default windows
