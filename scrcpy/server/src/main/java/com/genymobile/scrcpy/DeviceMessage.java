package com.genymobile.scrcpy;

import android.graphics.Bitmap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class DeviceMessage {

    public static final int TYPE_CLIPBOARD = 0;
    public static final int TYPE_SCREEN_SHOT = 1;

    private int type;
    private byte[] data;

    private DeviceMessage() {
    }

    public static DeviceMessage createClipboard(String text) {
        DeviceMessage event = new DeviceMessage();
        event.type = TYPE_CLIPBOARD;
        event.data = text.getBytes(StandardCharsets.UTF_8);
        return event;
    }

    public static DeviceMessage createScreenShot(Bitmap image) throws IOException {
        DeviceMessage event = new DeviceMessage();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
        byteArrayOutputStream.flush();
        byteArrayOutputStream.close();
        byte[] raw = byteArrayOutputStream.toByteArray();
        event.type = TYPE_SCREEN_SHOT;
        event.data = raw;
        return event;
    }

    public int getType() {
        return type;
    }

    public byte[] getData() {
        return data;
    }
}
