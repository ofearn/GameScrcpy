import { FSWatcher, unwatchFile } from 'fs'
import { existsSync, lstatSync, mkdirSync, readdirSync, readFileSync, rmdirSync, statSync, unlinkSync, watch as watchFile, WriteFileOptions, writeFileSync } from 'fs'
import path from 'path'
export const read = (path: string, format?: string) => {
  const result = readFileSync(path)
  if (!format) {
    return result
  }
  try {
    const decoder = new TextDecoder(format)
    return decoder.decode(result)
  } catch (_) {
    return result
  }
}
export const exists = (path: string) => {
  return existsSync(path)
}

export const write = (path: string, content: string, options?: WriteFileOptions) => {
  writeFileSync(path, content, options)
}

export const mkdir = (filePath: string) => {
  if (existsSync(filePath)) {
    return true
  }
  if (mkdir(path.dirname(filePath))) {
    mkdirSync(filePath)
  }
  return true
}

export const add = (filePath: string) => {
  if (!exists(filePath)) {
    mkdir(path.dirname(filePath))
    write(filePath, '')
  }
}

export const del = (filePath: string) => {
  const file = lstatSync(filePath)
  if (file.isFile()) {
    unlinkSync(filePath)
    return true
  }
  const files = readdirSync(filePath)
  files.forEach((f, _) => {
    const curPath = path.join(filePath, f)
    if (statSync(curPath).isDirectory()) {
      del(curPath)
    } else {
      unlinkSync(curPath)
    }
  })
  rmdirSync(filePath)
  return true
}

export const ls = (filePath: string) => {
  const file = lstatSync(filePath)
  if (!file.isDirectory()) {
    return false
  }
  return readdirSync(filePath)
}

export const watch = (filePath: string, callback: (eventType?: string) => boolean | void) => {
  const file = lstatSync(filePath)
  if (!file.isFile()) {
    return
  }
  const watch = watchFile(filePath, (e) => {
    if (callback && callback(e)) {
      console.log('watch closed')
      watch.close()
    }
  })
}
