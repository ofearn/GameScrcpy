package main

import (
	"encoding/binary"
	"fmt"
	"net"
	"os"

	"github.com/gorilla/websocket"
)

func createTcpServer() {
	listener, err := net.Listen("tcp", fmt.Sprintf("127.0.0.1:%d", *tcpPort))
	if err != nil {
		fmt.Println("create tpc err:", err)
		os.Exit(1)
	}
	fmt.Println("start tcp server listen on:", *tcpPort)
	for {
		connect, _ := listener.Accept()
		if waitConnectDevice == "" {
			connect.Close()
		}
		if connectionDevices[waitConnectDevice] == nil {
			connectionDevices[waitConnectDevice] = &connection{video: connect}
			go receiveVideoMessage(connect, waitConnectDevice)
		} else {
			connectionDevices[waitConnectDevice].control = connect
			go receiveControlMessage(connect, waitConnectDevice)
			waitConnectDevice = ""
		}
	}
}
func receiveVideoMessage(connect net.Conn, deviceName string) {
	buffer := make([]byte, 1024)
	for {
		n, err := connect.Read(buffer)
		if err != nil {
			break
		}
		data := connectionDevices[deviceName].videData
		if connectionDevices[deviceName].width == 0 {
			// 设备暂未初始化
			if n >= 68 {
				connectionDevices[deviceName].name = string(buffer[:64])
				connectionDevices[deviceName].width = int(binary.BigEndian.Uint16(buffer[64:66]))
				connectionDevices[deviceName].height = int(binary.BigEndian.Uint16(buffer[66:68]))
			}
			connectionDevices[deviceName].videData = append(data, buffer[68:n]...)
		} else {
			connectionDevices[deviceName].videData = append(data, buffer[:n]...)
		}
		getFrame(deviceName)
	}
	closeDevice(deviceName)
	connect.Close()
}
func receiveControlMessage(connect net.Conn, deviceName string) {
	buffer := make([]byte, 1024)
	for {
		n, err := connect.Read(buffer)
		if err != nil {
			break
		}
		data := connectionDevices[deviceName].controlData
		connectionDevices[deviceName].controlData = append(data, buffer[:n]...)
		getData(deviceName)
	}
	closeDevice(deviceName)
	connect.Close()
}
func closeDevice(deviceName string) {
	cd := connectionDevices[deviceName]
	if cd == nil {
		return
	}
	cd.control.Close()
	cd.video.Close()
	delete(connectionDevices, deviceName)
	// 连接video 需要断开
	for _, conn := range videoConn[deviceName] {
		conn.Close()
	}
	delete(videoConn, deviceName)
}
func getFrame(deviceName string) {
	data := connectionDevices[deviceName].videData
	if len(data) < 12 {
		return
	}
	length := int(binary.BigEndian.Uint32(data[8:12])) + 12
	if len(data) < length {
		return
	}
	// 已经完整了
	frame := data[0:length]
	connectionDevices[deviceName].videData = data[length:]
	if len(connectionDevices[deviceName].head) < 3 {
		connectionDevices[deviceName].head = append(connectionDevices[deviceName].head, frame)
	}
	for _, conn := range videoConn[deviceName] {
		conn.WriteMessage(websocket.BinaryMessage, frame)
	}
}
func getData(deviceName string) {
	data := connectionDevices[deviceName].controlData
	dataType := int(data[0])
	var length = 0
	switch dataType {
	case 0:
		if len(data) < 3 {
			return
		}
		length = int(binary.BigEndian.Uint16(data[1:3]))
		if len(data) >= length+3 {
			connectionDevices[deviceName].clipboard = string(data[3 : length+3])
			connectionDevices[deviceName].controlData = data[length+3:]
		}
	case 1:
		if len(data) < 5 {
			return
		}
		length = int(binary.BigEndian.Uint32(data[1:5]))
		if len(data) >= length+5 {
			connectionDevices[deviceName].screenshot = data[5 : length+5]
			connectionDevices[deviceName].controlData = data[length+5:]
		}
	}
}
