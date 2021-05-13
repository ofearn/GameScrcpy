package com.genymobile.scrcpy;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class DeviceMessageWriter {

    private static final int MESSAGE_MAX_SIZE = 1 << 18; // 256k
    public static final int CLIPBOARD_TEXT_MAX_LENGTH = MESSAGE_MAX_SIZE - 5; // type: 1 byte; length: 4 bytes

    private final byte[] rawBuffer = new byte[MESSAGE_MAX_SIZE];
    private final ByteBuffer buffer = ByteBuffer.wrap(rawBuffer);

    public void writeTo(DeviceMessage msg, OutputStream output) throws IOException {
        switch (msg.getType()) {
            case DeviceMessage.TYPE_CLIPBOARD:
                buffer.clear();
                buffer.put((byte) DeviceMessage.TYPE_CLIPBOARD);
                byte[] raw = msg.getData();
                int len = StringUtils.getUtf8TruncationIndex(raw, CLIPBOARD_TEXT_MAX_LENGTH);
                buffer.putInt(len);
                buffer.put(raw, 0, len);
                output.write(rawBuffer, 0, buffer.position());
                break;
            case DeviceMessage.TYPE_SCREEN_SHOT:
                byte[] bitmapData = msg.getData();
                int bitmapLen = bitmapData.length;
                byte[] picBuffer = new byte[bitmapLen + 5];
                final ByteBuffer buf = ByteBuffer.wrap(picBuffer);
                buf.put((byte) DeviceMessage.TYPE_SCREEN_SHOT);
                buf.putInt(bitmapLen);
                buf.put(bitmapData);
                output.write(picBuffer, 0, buf.position());
                buf.clear();
                break;
            default:
                Ln.w("Unknown device message: " + msg.getType());
                break;
        }
    }
}
