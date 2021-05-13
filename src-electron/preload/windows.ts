import { ipcRenderer } from 'electron'
import { proportionSize, windowsOptions, windowsSize } from '../types/windows'
import EventType from '../windows/eventType'

/**
 * 创建window窗口
 * @param name
 * @param options
 * @returns
 */
export const create = (name: string = '', options: windowsOptions) => {
  return ipcRenderer.sendSync(EventType.CREATE_WINDOWS, name, options)
}
/**
 * 获取窗口大小
 * @param name
 * @returns
 */
export const getSize = (name: string = '') => {
  return ipcRenderer.sendSync(EventType.GET_WINDOWS_SIZE, name)
}
/**
 * 设置窗口大小
 * @param name
 * @param size
 * @returns
 */
export const setSize = (name: string = '', size: windowsSize) => {
  return ipcRenderer.sendSync(EventType.SET_WINDOWS_SIZE, name, size)
}
/**
 * 打开url网页
 * @param name
 * @param url
 * @returns
 */
export const openUrl = (name: string = '', url: string) => {
  return ipcRenderer.sendSync(EventType.WINDOWS_OPEN_URL, name, url)
}
/**
 * 隐藏窗口
 * @param name
 * @returns
 */
export const hide = (name: string = '') => {
  return ipcRenderer.sendSync(EventType.WINDOWS_HIDE, name)
}
/**
 * 显示窗口
 * @param name
 * @returns
 */
export const show = (name: string = '') => {
  return ipcRenderer.sendSync(EventType.WINDOWS_SHOW, name)
}
/**
 * 设置窗口等比缩放
 * @param name
 * @param proportion
 * @returns
 */
export const setProportion = (name: string = '', proportion: proportionSize) => {
  return ipcRenderer.sendSync(EventType.SET_WINDOWS_PROPORTION, name, proportion)
}
/**
 * 获取窗口别表
 * @returns
 */
export const getList = () => {
  return ipcRenderer.sendSync(EventType.GET_WINDOWS_LIST)
}

/**
 * 关闭窗口
 * @param {*} name
 * @returns
 */
export const close = (name: string = '') => {
  return ipcRenderer.sendSync(EventType.CLOSE_WINDOWS, name)
}

/**
 * 最小化窗口
 * @param name
 * @returns
 */
export const minimize = (name: string = '') => {
  return ipcRenderer.sendSync(EventType.MINIMIZE_WINDOWS, name)
}
/**
 * 最大化窗口
 * @param name
 * @returns
 */
export const maximize = (name: string = '') => {
  return ipcRenderer.sendSync(EventType.MAXIMIZE_WINDOWS, name)
}
/**
 * 动态调用窗口方法
 * @param name
 * @param method
 * @param args
 * @returns
 */
export const call = (name: string = '', method: string, ...args: any[]) => {
  return ipcRenderer.sendSync(EventType.CALL_WINDOWS_FUNCTION, name, method, ...args)
}

export const exitApp = () => {
  return ipcRenderer.sendSync(EventType.EXIT_APP)
}
