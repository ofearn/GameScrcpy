export = Windows
export as namespace windows
declare namespace Windows {
  function create(name: string): boolean
  function close(name?: string): boolean
  function minimize(name?: string): boolean
}
