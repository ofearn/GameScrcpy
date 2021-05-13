import { contextBridge } from 'electron'
import { create, getSize, setSize, openUrl, hide, show, setProportion, close, minimize, maximize, getList, call, exitApp } from './preload/windows'
import { exec, run, kill } from './preload/shell'
import path from 'path'
contextBridge.exposeInMainWorld('xw', {
  windows: { create, getSize, setSize, openUrl, hide, show, setProportion, close, minimize, maximize, getList, call },
  shell: { exec, run, kill },
  path: path.resolve(__dirname),
  exitApp,
})
