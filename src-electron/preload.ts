import { contextBridge } from 'electron'
import { create, getSize, setSize, openUrl, hide, show, setProportion, close, minimize, maximize, getList, call, exitApp } from './preload/windows'
import { exec, run, kill } from './preload/shell'
import { read, write, exists, mkdir, ls, add, del, watch } from './preload/file'
import path from 'path'

contextBridge.exposeInMainWorld('__dirname', __dirname)
contextBridge.exposeInMainWorld('path', path)
contextBridge.exposeInMainWorld('exitApp', exitApp)
contextBridge.exposeInMainWorld('windows', {
  create,
  getSize,
  setSize,
  openUrl,
  hide,
  show,
  setProportion,
  close,
  minimize,
  maximize,
  getList,
  call,
})
contextBridge.exposeInMainWorld('shell', {
  exec,
  run,
  kill,
})
contextBridge.exposeInMainWorld('file', {
  read,
  write,
  exists,
  mkdir,
  ls,
  add,
  del,
  watch,
})
