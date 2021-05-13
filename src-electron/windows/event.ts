import { app, ipcMain } from 'electron'
import { IpcMainEvent } from 'electron/main'
import { proportionSize, windowsOptions, windowsSize } from '../types/windows'
import EventType from './eventType'
import manger from './manger'
import windows from '.'

class event {
  private manger: manger
  constructor(manger: manger) {
    this.manger = manger
  }
  init() {
    ipcMain.on(EventType.CREATE_WINDOWS, this.createWindow.bind(this))
    ipcMain.on(EventType.GET_WINDOWS_SIZE, this.getWindowSize.bind(this))
    ipcMain.on(EventType.SET_WINDOWS_SIZE, this.setWindowSize.bind(this))
    ipcMain.on(EventType.WINDOWS_OPEN_URL, this.openUrl.bind(this))
    ipcMain.on(EventType.WINDOWS_HIDE, this.hideWindow.bind(this))
    ipcMain.on(EventType.WINDOWS_SHOW, this.showWindow.bind(this))
    ipcMain.on(EventType.SET_WINDOWS_PROPORTION, this.setWindowProportion.bind(this))
    ipcMain.on(EventType.GET_WINDOWS_LIST, this.getWindowList.bind(this))
    ipcMain.on(EventType.CLOSE_WINDOWS, this.closeWindow.bind(this))
    ipcMain.on(EventType.MINIMIZE_WINDOWS, this.minimizeWindow.bind(this))
    ipcMain.on(EventType.MAXIMIZE_WINDOWS, this.maximizeWindow.bind(this))
    ipcMain.on(EventType.CALL_WINDOWS_FUNCTION, this.callWindowFunction.bind(this))
    ipcMain.on(EventType.EXIT_APP, this.exitApp.bind(this))
  }
  private getWindows(id: number, name: string): windows | undefined {
    if (name == '') {
      name = this.manger.windowsContrast.get(id) as string
    }
    return this.manger.getWindows(name)
  }
  private createWindow(event: IpcMainEvent, name: string, options: windowsOptions) {
    if (typeof options.parent == 'string') {
      options.parent = this.manger.getWindows(options.parent)?.instance
    }
    event.returnValue = this.manger.createWindows(name, options)
  }
  private getWindowSize(event: IpcMainEvent, name: string = '') {
    const browserWindows: windows | undefined = this.getWindows(event.sender.id, name)
    event.returnValue = browserWindows?.getSize()
  }
  private setWindowSize(event: IpcMainEvent, name: string, size: windowsSize) {
    const browserWindows: windows | undefined = this.getWindows(event.sender.id, name)
    event.returnValue = browserWindows?.setSize(size)
  }
  private openUrl(event: IpcMainEvent, name: string = '', url: string) {
    const browserWindows: windows | undefined = this.getWindows(event.sender.id, name)
    event.returnValue = browserWindows?.openUrl(url)
  }
  private hideWindow(event: IpcMainEvent, name: string = '') {
    const browserWindows: windows | undefined = this.getWindows(event.sender.id, name)
    event.returnValue = browserWindows?.hide()
  }
  private showWindow(event: IpcMainEvent, name: string = '') {
    const browserWindows: windows | undefined = this.getWindows(event.sender.id, name)
    event.returnValue = browserWindows?.show()
  }
  private setWindowProportion(event: IpcMainEvent, name: string, proportion: proportionSize) {
    const browserWindows: windows | undefined = this.getWindows(event.sender.id, name)
    event.returnValue = browserWindows?.setProportion(proportion)
  }
  private getWindowList(event: IpcMainEvent) {
    event.returnValue = Object.keys(this.manger.createWindows)
  }
  private closeWindow(event: IpcMainEvent, name: string = '') {
    const browserWindows: windows | undefined = this.getWindows(event.sender.id, name)
    event.returnValue = browserWindows?.close()
  }
  private minimizeWindow(event: IpcMainEvent, name: string = '') {
    const browserWindows: windows | undefined = this.getWindows(event.sender.id, name)
    event.returnValue = browserWindows?.minimize()
  }
  private maximizeWindow(event: IpcMainEvent, name: string = '') {
    const browserWindows: windows | undefined = this.getWindows(event.sender.id, name)
    event.returnValue = browserWindows?.maximize()
  }
  private callWindowFunction(event: IpcMainEvent, name: string = '', method: string, ...args: any[]) {
    const browserWindows: windows | undefined = this.getWindows(event.sender.id, name)
    event.returnValue = browserWindows?.callFunction(method, args)
  }
  private exitApp() {
    app.exit()
  }
}

export default event
