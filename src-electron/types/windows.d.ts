import { BrowserWindow } from 'electron'
import { BrowserWindowConstructorOptions } from 'electron/common'

export interface windowsOptions {
  url: string
  width?: number
  height?: number
  preload?: boolean | string
  devTools?: boolean
  frame?: boolean
  transparent?: boolean
  proportion?: proportionSize
  parent?: BrowserWindow | string | null | undefined
  options?: BrowserWindowConstructorOptions
  min?: windowsSize
  max?: windowsSize
}
export interface proportionSize {
  fixedWidth?: number
  fixedHeiht?: number
  minWidth?: number
  maxWidth?: number
}
export interface windowsSize {
  width: number
  height: number
}
