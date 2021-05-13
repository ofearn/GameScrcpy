import { app, nativeTheme } from 'electron'
import path from 'path'
import manger from './windows/manger'

try {
  if (process.platform === 'win32' && nativeTheme.shouldUseDarkColors === true) {
    require('fs').unlinkSync(require('path').join(app.getPath('userData'), 'DevTools Extensions'))
  }
} catch (_) {}

const windwosManger: manger = new manger(path.resolve(__dirname, process.env.QUASAR_ELECTRON_PRELOAD as string), {
  url: process.env.APP_URL as string,
  preload: true,
  frame: true,
  width: 1200,
  height: 600,
  transparent: true,
  proportion: {},
})
function createWindow() {
  windwosManger.createMainWindows()
  if (process.env.DEBUGGING) {
    windwosManger.getWindows('main')?.openDevTools()
  }
}

app.on('ready', createWindow)
app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit()
  }
})
app.on('activate', () => {
  if (windwosManger.windowsMap.has('main')) {
    windwosManger.createMainWindows()
  }
})
