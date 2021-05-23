import { execSync, spawn } from 'child_process'
import process from 'process'

export const exec = (cmd: string, format?: string) => {
  const result = execSync(cmd)
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
export const run = (path: string, args: string[], callback?: Function) => {
  const shell = spawn(path, args)
  shell.stdout.on('data', function (data) {
    callback && callback(1, data)
  })
  shell.stderr.on('data', function (data) {
    callback && callback(-1, data)
  })
  shell.on('close', function (code) {
    callback && callback(0, code)
  })
  return shell
}
export const kill = (pid: number) => {
  process.kill(pid, 'SIGTERM')
}
