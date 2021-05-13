import { execSync, spawn } from 'child_process'
import process from 'process'
import { join } from 'path'
const decoder = new TextDecoder('gbk')
export const exec = (cmd: string, formatPath: boolean = false) => {
  if (formatPath) {
    cmd = join(__dirname, cmd)
  }
  return decoder.decode(execSync(cmd))
}
export const run = (path: string, args: string[], formatPath = false, callback: Function) => {
  if (formatPath) {
    path = join(__dirname, path)
  }
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
  return process.kill(pid, 'SIGTERM')
}
