package main

import (
	"github.com/gorilla/websocket"
	"net"
)

/**
  设备列表
*/
type connection struct {
	name           string
	width, height  int
	head           [][]byte
	videData,controlData,screenshot     []byte
	clipboard string
	video, control net.Conn
}

var devices map[string]bool
var videoConn map[string][]*websocket.Conn
var controlDevices map[string]string
var connectionDevices map[string]*connection
var waitConnectDevice = ""
var touchPoints = 0

func init() {
	if devices == nil {
		devices = make(map[string]bool)
	}
	if videoConn == nil {
		videoConn = make(map[string][]*websocket.Conn)
	}
	if controlDevices == nil {
		controlDevices = make(map[string]string)
	}
	if connectionDevices == nil {
		connectionDevices = make(map[string]*connection)
	}

}
