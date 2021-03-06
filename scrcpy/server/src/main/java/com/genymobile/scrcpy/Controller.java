package com.genymobile.scrcpy;

import android.graphics.Bitmap;
import android.os.Build;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Controller {

    private static final int DEVICE_ID_VIRTUAL = -1;

    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();

    private final Device device;
    private final DesktopConnection connection;
    private final DeviceMessageSender sender;

    private final KeyCharacterMap charMap = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD);

    private long lastTouchDown;
    private final PointersState pointersState = new PointersState();
    private final MotionEvent.PointerProperties[] pointerProperties = new MotionEvent.PointerProperties[PointersState.MAX_POINTERS];
    private final MotionEvent.PointerCoords[] pointerCoords = new MotionEvent.PointerCoords[PointersState.MAX_POINTERS];

    private boolean keepPowerModeOff;

    private ScreenEncoder screenEncoder;

    public Controller(Device device, DesktopConnection connection, ScreenEncoder screenEncoder) {
        this.device = device;
        this.connection = connection;
        this.screenEncoder = screenEncoder;
        initPointers();
        sender = new DeviceMessageSender(connection);
    }

    private void initPointers() {
        for (int i = 0; i < PointersState.MAX_POINTERS; ++i) {
            MotionEvent.PointerProperties props = new MotionEvent.PointerProperties();
            props.toolType = MotionEvent.TOOL_TYPE_FINGER;

            MotionEvent.PointerCoords coords = new MotionEvent.PointerCoords();
            coords.orientation = 0;
            coords.size = 1;

            pointerProperties[i] = props;
            pointerCoords[i] = coords;
        }
    }

    public void control() throws IOException {
        // on start, power on the device
        if (!Device.isScreenOn()) {
            device.injectKeycode(KeyEvent.KEYCODE_POWER);

            // dirty hack
            // After POWER is injected, the device is powered on asynchronously.
            // To turn the device screen off while mirroring, the client will send a message that
            // would be handled before the device is actually powered on, so its effect would
            // be "canceled" once the device is turned back on.
            // Adding this delay prevents to handle the message before the device is actually
            // powered on.
            SystemClock.sleep(500);
        }

        while (true) {
            handleEvent();
        }
    }

    public DeviceMessageSender getSender() {
        return sender;
    }

    private void handleEvent() throws IOException {
        ControlMessage msg = connection.receiveControlMessage();
        switch (msg.getType()) {
            case ControlMessage.TYPE_INJECT_KEYCODE:
                if (device.supportsInputEvents()) {
                    injectKeycode(msg.getAction(), msg.getKeycode(), msg.getRepeat(), msg.getMetaState());
                }
                break;
            case ControlMessage.TYPE_INJECT_TEXT:
                if (device.supportsInputEvents()) {
                    injectText(msg.getText());
                }
                break;
            case ControlMessage.TYPE_INJECT_TOUCH_EVENT:
                if (device.supportsInputEvents()) {
                    injectTouch(msg.getAction(), msg.getPointerId(), msg.getPosition(), msg.getPressure(), msg.getButtons());
                }
                break;
            case ControlMessage.TYPE_INJECT_SCROLL_EVENT:
                if (device.supportsInputEvents()) {
                    injectScroll(msg.getPosition(), msg.getHScroll(), msg.getVScroll());
                }
                break;
            case ControlMessage.TYPE_BACK_OR_SCREEN_ON:
                if (device.supportsInputEvents()) {
                    pressBackOrTurnScreenOn();
                }
                break;
            case ControlMessage.TYPE_EXPAND_NOTIFICATION_PANEL:
                Device.expandNotificationPanel();
                break;
            case ControlMessage.TYPE_COLLAPSE_NOTIFICATION_PANEL:
                Device.collapsePanels();
                break;
            case ControlMessage.TYPE_GET_CLIPBOARD:
                String clipboardText = Device.getClipboardText();
                if (clipboardText != null) {
                    sender.pushClipboardText(clipboardText);
                }
                break;
            case ControlMessage.TYPE_SET_CLIPBOARD:
                setClipboard(msg.getText(), msg.getPaste());
                break;
            case ControlMessage.TYPE_SET_SCREEN_POWER_MODE:
                if (device.supportsInputEvents()) {
                    int mode = msg.getAction();
                    boolean setPowerModeOk = Device.setScreenPowerMode(mode);
                    if (setPowerModeOk) {
                        keepPowerModeOff = mode == Device.POWER_MODE_OFF;
                        Ln.i("Device screen turned " + (mode == Device.POWER_MODE_OFF ? "off" : "on"));
                    }
                }
                break;
            case ControlMessage.TYPE_ROTATE_DEVICE:
                Device.rotateDevice();
                break;
            case ControlMessage.TYPE_SCREEN_SORT:
                Bitmap screenSortBitmap = device.getScreenSort();
                if (screenSortBitmap != null) {
                    DeviceMessage event = DeviceMessage.createScreenShot(screenSortBitmap);
                    connection.sendDeviceMessage(event);
                }
                break;
            case ControlMessage.TYPE_SWITCH_SEND_VIDEO:
                screenEncoder.switchSendVideo();
                break;
            default:
                // do nothing
        }
    }

    private boolean injectKeycode(int action, int keycode, int repeat, int metaState) {
        if (keepPowerModeOff && action == KeyEvent.ACTION_UP && (keycode == KeyEvent.KEYCODE_POWER || keycode == KeyEvent.KEYCODE_WAKEUP)) {
            schedulePowerModeOff();
        }
        return device.injectKeyEvent(action, keycode, repeat, metaState);
    }

    private boolean injectChar(char c) {
        String decomposed = KeyComposition.decompose(c);
        char[] chars = decomposed != null ? decomposed.toCharArray() : new char[]{c};
        KeyEvent[] events = charMap.getEvents(chars);
        if (events == null) {
            return false;
        }
        for (KeyEvent event : events) {
            if (!device.injectEvent(event)) {
                return false;
            }
        }
        return true;
    }

    private int injectText(String text) {
        int successCount = 0;
        for (char c : text.toCharArray()) {
            if (!injectChar(c)) {
                Ln.w("Could not inject char u+" + String.format("%04x", (int) c));
                continue;
            }
            successCount++;
        }
        return successCount;
    }

    private boolean injectTouch(int action, long pointerId, Position position, float pressure, int buttons) {
        long now = SystemClock.uptimeMillis();

        Point point = device.getPhysicalPoint(position);
        if (point == null) {
            Ln.w("Ignore touch event, it was generated for a different device size");
            return false;
        }

        int pointerIndex = pointersState.getPointerIndex(pointerId);
        if (pointerIndex == -1) {
            Ln.w("Too many pointers for touch event");
            return false;
        }
        Pointer pointer = pointersState.get(pointerIndex);
        pointer.setPoint(point);
        pointer.setPressure(pressure);
        pointer.setUp(action == MotionEvent.ACTION_UP);

        int pointerCount = pointersState.update(pointerProperties, pointerCoords);

        if (pointerCount == 1) {
            if (action == MotionEvent.ACTION_DOWN) {
                lastTouchDown = now;
            }
        } else {
            // secondary pointers must use ACTION_POINTER_* ORed with the pointerIndex
            if (action == MotionEvent.ACTION_UP) {
                action = MotionEvent.ACTION_POINTER_UP | (pointerIndex << MotionEvent.ACTION_POINTER_INDEX_SHIFT);
            } else if (action == MotionEvent.ACTION_DOWN) {
                action = MotionEvent.ACTION_POINTER_DOWN | (pointerIndex << MotionEvent.ACTION_POINTER_INDEX_SHIFT);
            }
        }

        // Right-click and middle-click only work if the source is a mouse
        boolean nonPrimaryButtonPressed = (buttons & ~MotionEvent.BUTTON_PRIMARY) != 0;
        int source = nonPrimaryButtonPressed ? InputDevice.SOURCE_MOUSE : InputDevice.SOURCE_TOUCHSCREEN;

        MotionEvent event = MotionEvent
                .obtain(lastTouchDown, now, action, pointerCount, pointerProperties, pointerCoords, 0, buttons, 1f, 1f, DEVICE_ID_VIRTUAL, 0, source,
                        0);
        return device.injectEvent(event);
    }

    private boolean injectScroll(Position position, int hScroll, int vScroll) {
        long now = SystemClock.uptimeMillis();
        Point point = device.getPhysicalPoint(position);
        if (point == null) {
            // ignore event
            return false;
        }

        MotionEvent.PointerProperties props = pointerProperties[0];
        props.id = 0;

        MotionEvent.PointerCoords coords = pointerCoords[0];
        coords.x = point.getX();
        coords.y = point.getY();
        coords.setAxisValue(MotionEvent.AXIS_HSCROLL, hScroll);
        coords.setAxisValue(MotionEvent.AXIS_VSCROLL, vScroll);

        MotionEvent event = MotionEvent
                .obtain(lastTouchDown, now, MotionEvent.ACTION_SCROLL, 1, pointerProperties, pointerCoords, 0, 0, 1f, 1f, DEVICE_ID_VIRTUAL, 0,
                        InputDevice.SOURCE_TOUCHSCREEN, 0);
        return device.injectEvent(event);
    }

    /**
     * Schedule a call to set power mode to off after a small delay.
     */
    private static void schedulePowerModeOff() {
        EXECUTOR.schedule(new Runnable() {
            @Override
            public void run() {
                Ln.i("Forcing screen off");
                Device.setScreenPowerMode(Device.POWER_MODE_OFF);
            }
        }, 200, TimeUnit.MILLISECONDS);
    }

    private boolean pressBackOrTurnScreenOn() {
        int keycode = Device.isScreenOn() ? KeyEvent.KEYCODE_BACK : KeyEvent.KEYCODE_POWER;
        if (keepPowerModeOff && keycode == KeyEvent.KEYCODE_POWER) {
            schedulePowerModeOff();
        }
        return device.injectKeycode(keycode);
    }

    private boolean setClipboard(String text, boolean paste) {
        boolean ok = device.setClipboardText(text);
        if (ok) {
            Ln.i("Device clipboard set");
        }

        // On Android >= 7, also press the PASTE key if requested
        if (paste && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && device.supportsInputEvents()) {
            device.injectKeycode(KeyEvent.KEYCODE_PASTE);
        }

        return ok;
    }
}
