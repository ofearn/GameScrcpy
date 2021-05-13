package main

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"os"

	"github.com/buger/jsonparser"
	"github.com/gorilla/websocket"
)

var upgrader = websocket.Upgrader{
	CheckOrigin: func(r *http.Request) bool {
		return true
	},
}

const (
	TYPE_START_UP = 1000 + iota
	TYPE_SHUT_OFF
	TYPE_INJECT_KEYCODE
	TYPE_INJECT_TEXT
	TYPE_INJECT_TOUCH_EVENT
	TYPE_INJECT_SCROLL_EVENT
	TYPE_BACK_OR_SCREEN_ON
	TYPE_EXPAND_NOTIFICATION_PANEL
	TYPE_COLLAPSE_NOTIFICATION_PANEL
	TYPE_GET_CLIPBOARD
	TYPE_SET_CLIPBOARD
	TYPE_SET_SCREEN_POWER_MODE
	TYPE_ROTATE_DEVICE
	TYPE_SCREEN_SHOT
	TYPE_SWITCH_VIDEO
)

/**
 * 创建websocket
 */
func createWebsocketServer() {
	http.HandleFunc("/devices", devicesFunc)
	http.HandleFunc("/control", controlFunction)
	http.HandleFunc("/video", videoFunc)
	fmt.Println("start websocket server listen on:", *socketPort)
	err := http.ListenAndServe(fmt.Sprintf("127.0.0.1:%d", *socketPort), nil)
	if err != nil {
		fmt.Println("create websocket err:", err)
		os.Exit(1)
	}
}

/**
 * 获取设备列表
 */
func devicesFunc(writer http.ResponseWriter, request *http.Request) {
	getDevice()
	data, _ := json.Marshal(devices)
	writer.Write(data)
}

/**
 * 注册 video
 */
func videoFunc(writer http.ResponseWriter, request *http.Request) {
	conn, deviceName, err := getWebsocketConn(writer, request)
	if err != nil || deviceName == "" || !devices[deviceName] {
		conn.Close()
	}
	if connectionDevices[deviceName] != nil && connectionDevices[deviceName].height > 0 {
		// 发送之前 i 帧数据
		for _, data := range connectionDevices[deviceName].head {
			conn.WriteMessage(websocket.BinaryMessage, data)
		}
	}
	videoConn[deviceName] = append(videoConn[deviceName], conn)
	for {
		_, _, err = conn.ReadMessage()
		if err != nil {
			break
		}
	}
	for i, vConn := range videoConn[deviceName] {
		if vConn.RemoteAddr().String() == conn.RemoteAddr().String() {
			videoConn[deviceName] = append(videoConn[deviceName][:i], videoConn[deviceName][i+1:]...)
		}
	}
	fmt.Println("websocket 关闭连接:", conn.RemoteAddr())
	conn.Close()
}

/**
 * 注册 control
 */
func controlFunction(writer http.ResponseWriter, request *http.Request) {
	conn, deviceName, err := getWebsocketConn(writer, request)
	if err != nil || deviceName == "" || !devices[deviceName] {
		conn.Close()
	}
	controlDevices[conn.RemoteAddr().String()] = deviceName
	var message []byte
	for {
		_, message, err = conn.ReadMessage()
		if err != nil {
			// 发生错误断开连接
			break
		}
		// 处理消息
		parserControlMessage(deviceName, message, conn)
	}
	fmt.Println("websocket 关闭连接:", conn.RemoteAddr())
	delete(controlDevices, conn.RemoteAddr().String())
	conn.Close()
}

/**
 * 获取websocket连接了
 */
func getWebsocketConn(writer http.ResponseWriter, request *http.Request) (conn *websocket.Conn, deviceName string, err error) {
	conn, err = upgrader.Upgrade(writer, request, nil)
	if err != nil {
		return
	}
	deviceName = request.URL.Query().Get("device")
	return
}

func parserControlMessage(deviceName string, message []byte, conn *websocket.Conn) {
	messageType, err := jsonparser.GetInt(message, "type")
	if err != nil {
		return
	}
	cd := connectionDevices[deviceName]
	if cd == nil && messageType > TYPE_SHUT_OFF {
		return
	}
	switch messageType {
	case TYPE_START_UP:
		if cd != nil {
			return
		}
		waitConnectDevice = deviceName
		size := getJsonInt(message, 0, "size")
		bit := getJsonInt(message, 2000000, "bit")
		fps := getJsonInt(message, 60, "fps")
		lock := getJsonInt(message, -1, "lock")
		video := getJsonBool(message, true, "video")
		if !startUpDevice(deviceName, size, bit, fps, lock, video) {
			waitConnectDevice = ""
		}
	case TYPE_SHUT_OFF:
		// 找到设备的所有socket连接 关闭
		if cd == nil {
			return
		}
		cd.video.Close()
	case TYPE_INJECT_KEYCODE:
		at := getJsonInt(message, 0, "actionType")
		kc := getJsonInt(message, 0, "keycode")
		repeat := getJsonInt(message, 0, "repeat")
		ms := getJsonInt(message, 0, "metaState")
		bytes := keycode(at, kc,repeat,ms)
		cd.control.Write(bytes)
	case TYPE_INJECT_TEXT:
		t := getJsonString(message, "", "text")
		bytes := text(t)
		cd.control.Write(bytes)
	case TYPE_INJECT_TOUCH_EVENT:
		x := getJsonInt(message, 0, "x")
		y := getJsonInt(message, 0, "y")
		at := getJsonInt(message, 0, "actionType")
		bytes := touch(at, x, y, cd.width, cd.height)
		cd.control.Write(bytes)
	case TYPE_INJECT_SCROLL_EVENT:
		x := getJsonInt(message, 0, "x")
		y := getJsonInt(message, 0, "y")
		hScroll := getJsonInt(message, 0, "hScroll")
		vScroll := getJsonInt(message, 0, "vScroll")
		bytes := scroll(x, y, cd.width, cd.height, hScroll, vScroll)
		cd.control.Write(bytes)
	case TYPE_BACK_OR_SCREEN_ON:
		bytes := backOrScreenOn()
		cd.control.Write(bytes)
	case TYPE_EXPAND_NOTIFICATION_PANEL:
		bytes := expandNotificationPanel()
		cd.control.Write(bytes)
	case TYPE_COLLAPSE_NOTIFICATION_PANEL:
		bytes := collapseNotificationPanel()
		cd.control.Write(bytes)
	case TYPE_GET_CLIPBOARD:
		cd.clipboard = ""
		bytes := getClipboard()
		cd.control.Write(bytes)
		go func() {
			for {
				if cd.clipboard != "" {
					// 发送
					conn.WriteMessage(websocket.TextMessage, []byte(cd.clipboard))
					break
				}
			}
		}()
	case TYPE_SET_CLIPBOARD:
		t := getJsonString(message, "", "text")
		bytes := setClipboard(t)
		cd.control.Write(bytes)
	case TYPE_SET_SCREEN_POWER_MODE:
		bytes := setScreenPowerMode()
		cd.control.Write(bytes)
	case TYPE_ROTATE_DEVICE:
		bytes := rotateDevice()
		cd.control.Write(bytes)
	case TYPE_SCREEN_SHOT:
		cd.screenshot = []byte("")
		bytes := screenshot()
		cd.control.Write(bytes)
		go func() {
			for {
				if len(cd.screenshot) > 0 {
					// 发送
					conn.WriteMessage(websocket.TextMessage, []byte(base64.StdEncoding.EncodeToString(cd.screenshot)))
					break
				}
			}
		}()
	case TYPE_SWITCH_VIDEO:
		bytes := switchVideo()
		cd.control.Write(bytes)
	}
}

func getJsonString(message []byte, defaultValue string, arr ...string) string {
	value, err := jsonparser.GetString(message, arr...)
	if err != nil {
		return defaultValue
	}
	return value
}
func getJsonInt(message []byte, defaultValue int, arr ...string) int {
	value, err := jsonparser.GetInt(message, arr...)
	if err != nil {
		return defaultValue
	}
	return int(value)
}
func getJsonBool(message []byte, defaultValue bool, arr ...string) bool {
	value, err := jsonparser.GetBoolean(message, arr...)
	if err != nil {
		return defaultValue
	}
	return value
}
func createWebsocketClient(url string) *websocket.Conn {
	ws, _, err := websocket.DefaultDialer.Dial(url, nil)
	if err != nil {
		log.Fatal(err)
	}
	go func() {
		err := sendMessage(ws, []byte("ping"))
		if err != nil {
			log.Fatal(err)
		}
	}()
	for {
		_, data, err := ws.ReadMessage()
		if err != nil {
			log.Fatal(err)
		}
		fmt.Println("receive: ", string(data))
	}
	return ws
}

func sendMessage(ws *websocket.Conn, message []byte) error {
	return ws.WriteMessage(websocket.BinaryMessage, message)
}
