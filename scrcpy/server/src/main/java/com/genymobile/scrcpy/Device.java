package com.genymobile.scrcpy;

import com.genymobile.scrcpy.wrappers.ClipboardManager;
import com.genymobile.scrcpy.wrappers.ContentProvider;
import com.genymobile.scrcpy.wrappers.InputManager;
import com.genymobile.scrcpy.wrappers.ServiceManager;
import com.genymobile.scrcpy.wrappers.SurfaceControl;
import com.genymobile.scrcpy.wrappers.WindowManager;

import android.content.IOnPrimaryClipChangedListener;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.view.IRotationWatcher;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

public final class Device {

    public static final int POWER_MODE_OFF = SurfaceControl.POWER_MODE_OFF;
    public static final int POWER_MODE_NORMAL = SurfaceControl.POWER_MODE_NORMAL;

    private static final ServiceManager SERVICE_MANAGER = new ServiceManager();

    public interface RotationListener {
        void onRotationChanged(int rotation);
    }

    public interface ClipboardListener {
        void onClipboardTextChanged(String text);
    }

    private ScreenInfo screenInfo;
    private RotationListener rotationListener;
    private ClipboardListener clipboardListener;
    private final AtomicBoolean isSettingClipboard = new AtomicBoolean();

    /**
     * Logical display identifier
     */
    private final int displayId;

    /**
     * The surface flinger layer stack associated with this logical display
     */
    private final int layerStack;

    private final boolean supportsInputEvents;

    public Device(Options options) {
        displayId = options.getDisplayId();
        DisplayInfo displayInfo = SERVICE_MANAGER.getDisplayManager().getDisplayInfo(displayId);
        if (displayInfo == null) {
            int[] displayIds = SERVICE_MANAGER.getDisplayManager().getDisplayIds();
            throw new InvalidDisplayIdException(displayId, displayIds);
        }

        int displayInfoFlags = displayInfo.getFlags();

        screenInfo = ScreenInfo.computeScreenInfo(displayInfo, options.getCrop(), options.getMaxSize(), options.getLockedVideoOrientation());
        layerStack = displayInfo.getLayerStack();

        SERVICE_MANAGER.getWindowManager().registerRotationWatcher(new IRotationWatcher.Stub() {
            @Override
            public void onRotationChanged(int rotation) {
                synchronized (Device.this) {
                    screenInfo = screenInfo.withDeviceRotation(rotation);

                    // notify
                    if (rotationListener != null) {
                        rotationListener.onRotationChanged(rotation);
                    }
                }
            }
        }, displayId);

        if (options.getControl()) {
            // If control is enabled, synchronize Android clipboard to the computer automatically
            ClipboardManager clipboardManager = SERVICE_MANAGER.getClipboardManager();
            if (clipboardManager != null) {
                clipboardManager.addPrimaryClipChangedListener(new IOnPrimaryClipChangedListener.Stub() {
                    @Override
                    public void dispatchPrimaryClipChanged() {
                        if (isSettingClipboard.get()) {
                            // This is a notification for the change we are currently applying, ignore it
                            return;
                        }
                        synchronized (Device.this) {
                            if (clipboardListener != null) {
                                String text = getClipboardText();
                                if (text != null) {
                                    clipboardListener.onClipboardTextChanged(text);
                                }
                            }
                        }
                    }
                });
            } else {
                Ln.w("No clipboard manager, copy-paste between device and computer will not work");
            }
        }

        if ((displayInfoFlags & DisplayInfo.FLAG_SUPPORTS_PROTECTED_BUFFERS) == 0) {
            Ln.w("Display doesn't have FLAG_SUPPORTS_PROTECTED_BUFFERS flag, mirroring can be restricted");
        }

        // main display or any display on Android >= Q
        supportsInputEvents = displayId == 0 || Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
        if (!supportsInputEvents) {
            Ln.w("Input events are not supported for secondary displays before Android 10");
        }
    }

    public synchronized ScreenInfo getScreenInfo() {
        return screenInfo;
    }

    public int getLayerStack() {
        return layerStack;
    }

    public Point getPhysicalPoint(Position position) {
        // it hides the field on purpose, to read it with a lock
        @SuppressWarnings("checkstyle:HiddenField")
        ScreenInfo screenInfo = getScreenInfo(); // read with synchronization

        // ignore the locked video orientation, the events will apply in coordinates considered in the physical device orientation
        Size unlockedVideoSize = screenInfo.getUnlockedVideoSize();

        int reverseVideoRotation = screenInfo.getReverseVideoRotation();
        // reverse the video rotation to apply the events
        Position devicePosition = position.rotate(reverseVideoRotation);

        Size clientVideoSize = devicePosition.getScreenSize();
        if (!unlockedVideoSize.equals(clientVideoSize)) {
            // The client sends a click relative to a video with wrong dimensions,
            // the device may have been rotated since the event was generated, so ignore the event
            return null;
        }
        Rect contentRect = screenInfo.getContentRect();
        Point point = devicePosition.getPoint();
        int convertedX = contentRect.left + point.getX() * contentRect.width() / unlockedVideoSize.getWidth();
        int convertedY = contentRect.top + point.getY() * contentRect.height() / unlockedVideoSize.getHeight();
        return new Point(convertedX, convertedY);
    }

    public static String getDeviceName() {
        return Build.MODEL;
    }

    public boolean supportsInputEvents() {
        return supportsInputEvents;
    }

    public boolean injectEvent(InputEvent inputEvent, int mode) {
        if (!supportsInputEvents()) {
            throw new AssertionError("Could not inject input event if !supportsInputEvents()");
        }

        if (displayId != 0 && !InputManager.setDisplayId(inputEvent, displayId)) {
            return false;
        }

        return SERVICE_MANAGER.getInputManager().injectInputEvent(inputEvent, mode);
    }

    public boolean injectEvent(InputEvent event) {
        return injectEvent(event, InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
    }

    public boolean injectKeyEvent(int action, int keyCode, int repeat, int metaState) {
        long now = SystemClock.uptimeMillis();
        KeyEvent event = new KeyEvent(now, now, action, keyCode, repeat, metaState, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0,
                InputDevice.SOURCE_KEYBOARD);
        return injectEvent(event);
    }

    public boolean injectKeycode(int keyCode) {
        return injectKeyEvent(KeyEvent.ACTION_DOWN, keyCode, 0, 0) && injectKeyEvent(KeyEvent.ACTION_UP, keyCode, 0, 0);
    }

    public static boolean isScreenOn() {
        return SERVICE_MANAGER.getPowerManager().isScreenOn();
    }

    public synchronized void setRotationListener(RotationListener rotationListener) {
        this.rotationListener = rotationListener;
    }

    public synchronized void setClipboardListener(ClipboardListener clipboardListener) {
        this.clipboardListener = clipboardListener;
    }

    public static void expandNotificationPanel() {
        SERVICE_MANAGER.getStatusBarManager().expandNotificationsPanel();
    }

    public static void collapsePanels() {
        SERVICE_MANAGER.getStatusBarManager().collapsePanels();
    }

    public static String getClipboardText() {
        ClipboardManager clipboardManager = SERVICE_MANAGER.getClipboardManager();
        if (clipboardManager == null) {
            return null;
        }
        CharSequence s = clipboardManager.getText();
        if (s == null) {
            return null;
        }
        return s.toString();
    }

    public boolean setClipboardText(String text) {
        ClipboardManager clipboardManager = SERVICE_MANAGER.getClipboardManager();
        if (clipboardManager == null) {
            return false;
        }

        String currentClipboard = getClipboardText();
        if (currentClipboard != null && currentClipboard.equals(text)) {
            // The clipboard already contains the requested text.
            // Since pasting text from the computer involves setting the device clipboard, it could be set twice on a copy-paste. This would cause
            // the clipboard listeners to be notified twice, and that would flood the Android keyboard clipboard history. To workaround this
            // problem, do not explicitly set the clipboard text if it already contains the expected content.
            return false;
        }

        isSettingClipboard.set(true);
        boolean ok = clipboardManager.setText(text);
        isSettingClipboard.set(false);
        return ok;
    }

    /**
     * @param mode one of the {@code POWER_MODE_*} constants
     */
    public static boolean setScreenPowerMode(int mode) {
        IBinder d = SurfaceControl.getBuiltInDisplay();
        if (d == null) {
            Ln.e("Could not get built-in display");
            return false;
        }
        return SurfaceControl.setDisplayPowerMode(d, mode);
    }

    /**
     * Disable auto-rotation (if enabled), set the screen rotation and re-enable auto-rotation (if it was enabled).
     */
    public static void rotateDevice() {
        WindowManager wm = SERVICE_MANAGER.getWindowManager();

        boolean accelerometerRotation = !wm.isRotationFrozen();

        int currentRotation = wm.getRotation();
        int newRotation = (currentRotation & 1) ^ 1; // 0->1, 1->0, 2->1, 3->0
        String newRotationString = newRotation == 0 ? "portrait" : "landscape";

        Ln.i("Device rotation requested: " + newRotationString);
        wm.freezeRotation(newRotation);
        // restore auto-rotate if necessary
        if (accelerometerRotation) {
            wm.thawRotation();
        }
    }

    public static ContentProvider createSettingsProvider() {
        return SERVICE_MANAGER.getActivityManager().createSettingsProvider();
    }

    public Bitmap getScreenSort() {
        Size videoSize = getScreenInfo().getVideoSize();
        int rotation = getScreenInfo().getDeviceRotation();
        boolean isHorizontal = rotation == 0 ? true : false;
        int width = isHorizontal ? videoSize.getWidth() : videoSize.getHeight();
        int height = isHorizontal ? videoSize.getHeight() : videoSize.getWidth();
        String surfaceClassName;
        if (Build.VERSION.SDK_INT < 17) {
            surfaceClassName = "android.view.Surface";
        } else {
            surfaceClassName = "android.view.SurfaceControl";
        }
        Bitmap mScreenBitmap = null;
        try {
            Class<?> classname;
            classname = Class.forName(surfaceClassName);
            if (Build.VERSION.SDK_INT <= 28) {
                Method method = classname.getDeclaredMethod("screenshot", new Class[]{Integer.TYPE, Integer.TYPE});
                mScreenBitmap = (Bitmap) method.invoke(null, new Object[]{Integer.valueOf(width), Integer.valueOf(height)});
            } else {
                Method method = classname.getDeclaredMethod("screenshot", new Class[]{Rect.class, int.class, int.class, int.class});
                Rect rect = new Rect();
                rect.left = 0;
                rect.top = 0;
                mScreenBitmap = (Bitmap) method.invoke(null, new Object[]{rect, Integer.valueOf(width), Integer.valueOf(height), rotation});
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mScreenBitmap;
    }
}
