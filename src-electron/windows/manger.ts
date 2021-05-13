import windows from '.'
import { windowsOptions } from '../types/windows'
import event from './event'
class manger {
  private preload: string
  private mainWindows: windows | null = null
  private mainWindowsOptions: windowsOptions
  windowsMap: Map<string, windows>
  windowsContrast: Map<number, string>
  constructor(preload: string = '', mainWindowsOptions: windowsOptions) {
    this.preload = preload
    this.mainWindowsOptions = mainWindowsOptions
    this.windowsMap = new Map<string, windows>()
    this.windowsContrast = new Map<number, string>()
    this.registerEvent()
  }
  private registerEvent() {
    const windowsEvent = new event(this)
    windowsEvent.init()
  }
  createMainWindows(): boolean {
    return this.createWindows('main', this.mainWindowsOptions)
  }
  createWindows(name: string, options: windowsOptions): boolean {
    if (this.windowsMap.has(name)) {
      return false
    }
    const browserWindows = new windows(name, options)
    if (!browserWindows.setPreload(this.preload).create()) {
      return false
    }
    const closeEvent = () => {
      this.windowsMap.delete(name)
      this.windowsContrast.delete(browserWindows.id)
      browserWindows.instance?.off('close', closeEvent)
      browserWindows.close()
    }
    browserWindows.instance?.on('close', closeEvent)
    this.windowsMap.set(name, browserWindows)
    this.windowsContrast.set(browserWindows.id, name)
    if (name == 'main') {
      this.mainWindows = browserWindows
    }
    return true
  }
  getWindows(name: string): windows | undefined {
    return this.windowsMap.get(name)
  }
}
export default manger
