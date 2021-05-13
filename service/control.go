package main

import "encoding/binary"

const (
	TOUCH_IN = iota
	TOUCH_ON
	TOUCH_MOVE
)

func startUpDevice(deviceName string, size int, bit int, fps int, lock int, video bool) bool {
	if !pushFile(deviceName, "scrcpy-server", "/data/local/tmp/scrcpy-server.jar") {
		return false
	}
	if !reverse(deviceName, *tcpPort) {
		return false
	}
	go startServer(deviceName, "/data/local/tmp/scrcpy-server.jar", "com.genymobile.scrcpy.Server", 1.17, "info", size, bit, fps, lock, video)
	return true
}
func keycode(actionType, keycode, repeat, metaState int) []byte {
	buffer := make([]byte, 14)
	buffer[0] = 0
	binary.BigEndian.PutUint32(buffer[2:6], uint32(keycode))
	binary.BigEndian.PutUint32(buffer[6:10], uint32(repeat))
	binary.BigEndian.PutUint32(buffer[10:14], uint32(metaState))
	return buffer
}

func text(str string) []byte {
	buffer := make([]byte, 5)
	buffer[0] = 1
	binary.BigEndian.PutUint32(buffer[1:5], uint32(len(str)))
	buffer = append(buffer, []byte(str)...)
	return buffer
}

func touch(actionType, x, y, width, height int) []byte {
	if actionType == 0 {
		touchPoints++
	}
	buffer := make([]byte, 28)
	buffer[0] = 2
	binary.BigEndian.PutUint16(buffer[22:24], 1<<16-1)
	switch actionType {
	case TOUCH_IN:
		buffer[1] = TOUCH_IN
	case TOUCH_ON:
		buffer[1] = TOUCH_ON
		binary.BigEndian.PutUint16(buffer[22:24], 0)
	default:
		buffer[1] = TOUCH_MOVE
	}
	binary.BigEndian.PutUint64(buffer[2:10], 1<<64-1)
	binary.BigEndian.PutUint32(buffer[10:14], uint32(x))
	binary.BigEndian.PutUint32(buffer[14:18], uint32(y))
	binary.BigEndian.PutUint16(buffer[18:20], uint16(width))
	binary.BigEndian.PutUint16(buffer[20:22], uint16(height))
	binary.BigEndian.PutUint32(buffer[24:28], uint32(touchPoints))
	if actionType == 1 {
		touchPoints--
	}
	return buffer[:]
}

func scroll(x, y, width, height, hScroll, vScroll int) []byte {
	buffer := make([]byte, 21)
	buffer[0] = 3
	binary.BigEndian.PutUint32(buffer[1:5], uint32(x))
	binary.BigEndian.PutUint32(buffer[5:9], uint32(y))
	binary.BigEndian.PutUint16(buffer[7:11], uint16(width))
	binary.BigEndian.PutUint16(buffer[11:13], uint16(height))
	binary.BigEndian.PutUint32(buffer[13:17], uint32(hScroll))
	binary.BigEndian.PutUint32(buffer[17:21], uint32(vScroll))
	return buffer
}
func backOrScreenOn() []byte {
	buffer := make([]byte, 1)
	buffer[0] = 4
	return buffer
}

func expandNotificationPanel() []byte {
	buffer := make([]byte, 1)
	buffer[0] = 5
	return buffer
}
func collapseNotificationPanel() []byte {
	buffer := make([]byte, 1)
	buffer[0] = 6
	return buffer
}

func getClipboard() []byte {
	buffer := make([]byte, 1)
	buffer[0] = 7
	return buffer
}
func setClipboard(text string) []byte {
	buffer := make([]byte, 6)
	buffer[0] = 8
	buffer[1] = 0
	binary.BigEndian.PutUint32(buffer[2:6], uint32(len(text)))
	buffer = append(buffer, []byte(text)...)
	return buffer
}
func setScreenPowerMode() []byte {
	buffer := make([]byte, 2)
	buffer[0] = 9
	buffer[1] = 0
	return buffer
}
func rotateDevice() []byte {
	buffer := make([]byte, 1)
	buffer[0] = 10
	return buffer
}
func screenshot() []byte {
	buffer := make([]byte, 1)
	buffer[0] = 11
	return buffer
}
func switchVideo() []byte {
	buffer := make([]byte, 1)
	buffer[0] = 12
	return buffer
}
