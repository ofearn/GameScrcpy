package main

import (
	"bufio"
	"fmt"
	"io"
	"os/exec"
	"runtime"
	"strings"
	"sync"
)

func getDevice() {
	adb, _ := execShell("adb devices")
	split := strings.Split(adb, "\r\n")
	start := false
	for _, line := range split {
		if start && line != "" {
			deviceInfo := strings.Split(line, "\t")
			if len(deviceInfo) == 2 {
				devices[deviceInfo[0]] = getDeviceStatus(deviceInfo[1])
			}
		}
		if line == "List of devices attached" {
			start = true
		}
	}
}
func pushFile(device string, filename string, path string) bool {
	if !devices[device] {
		return false
	}
	info, err := execShell(fmt.Sprintf("adb -s %s push %s %s", device, filename, path))
	if err != nil {
		return false
	}
	if strings.Index(info, "1 file pushed") == 1 {
		return false
	}
	return true
}

func reverse(device string, port int) bool {
	if !devices[device] {
		return false
	}
	_, err := execShell(fmt.Sprintf("adb -s %s  reverse localabstract:scrcpy tcp:%d", device, port))
	return err == nil
}
func startServer(device string, path string, class string, version float32, level string, size int, bit int, fps int, lock int, video bool) bool {
	if !devices[device] {
		return false
	}
	cmd := fmt.Sprintf("adb -s %v  shell CLASSPATH=%v app_process / %v %v %v %v %v %v %v false - true true 0 false false - - %v", device, path, class, version, level, size, bit, fps, lock, video)
	err := execKeepShell(cmd)
	return err == nil
}
func checkDeviceStatus(device string) bool {
	getDevice()
	return devices[device]
}
func getDeviceStatus(status string) bool {
	return status == "device"
}
func execShell(cmd string) (string, error) {
	var c *exec.Cmd
	if runtime.GOOS == "windows" {
		c = exec.Command("cmd", "/C", cmd) // windows
	} else {
		c = exec.Command("bash", "-c", cmd) // mac or linux
	}
	bytes, err := c.Output()
	return string(bytes), err
}

func execKeepShell(cmd string) error {
	var c *exec.Cmd
	if runtime.GOOS == "windows" {
		c = exec.Command("cmd", "/C", cmd) // windows
	} else {
		c = exec.Command("bash", "-c", cmd) // mac or linux
	}
	stdout, err := c.StdoutPipe()
	if err != nil {
		return err
	}
	var waitGroup sync.WaitGroup
	waitGroup.Add(1)
	go func() {
		defer waitGroup.Done()
		reader := bufio.NewReader(stdout)
		for {
			readString, err := reader.ReadString('\n')
			if err != nil || err == io.EOF {
				return
			}
			fmt.Print(readString)
		}
	}()
	err = c.Start()
	waitGroup.Wait()
	return err
}
