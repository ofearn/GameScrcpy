import { ChildProcess } from 'child_process'

export = Shell
export as namespace shell
declare namespace Shell {
  function exec(cmd: string, format?: string): any
  function run(path: string, args: string[], callback?: () => void): ChildProcess
  function kill(pid: number): void
}
