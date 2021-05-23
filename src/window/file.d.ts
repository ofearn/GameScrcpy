export = File
export as namespace file
declare namespace File {
  function read(path: string, format?: string): any
}
