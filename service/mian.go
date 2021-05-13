package main

import (
	"flag"
)

var tcpPort = flag.Int("t", 8023, "tpc port")
var socketPort = flag.Int("w", 8024, "websocket port")

func main() {
	flag.Parse()
	go createWebsocketServer()
	createTcpServer()
	//test()
}
