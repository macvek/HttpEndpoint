package pl.almatron.httpendpoint;

import java.io.IOException;
import java.io.InputStream;

/**
 * HttpEndpoint
 *
 * @author macvek
 */
class RotateBuffer {

    private byte[] buffer;
    private int size;
    private InputStream inputStream;
    private final static int rotateSize = 512;
    private final static int rotateTimesLimit = 3;
    private int rotateTimesCounter = rotateTimesLimit;
    private boolean allowedEndOfStream;

    public RotateBuffer() {
        buffer = new byte[rotateSize];
    }

    public void readChunk() {
        boolean endOfStream = false;
        try {
            int bytesRead = inputStream.read(buffer, size, buffer.length - size);
            if (bytesRead == -1) {
                endOfStream = true;
            } else {
                size += bytesRead;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (endOfStream && !allowedEndOfStream) {
            throw new RuntimeException("Unexpected end of stream");
        }

        if (size == buffer.length) {
            rotateBuffer();
        }
    }

    public int readToNextLine(int from) {
        if (size - from > 1) {
            for (int i = from; i < size - 1; i++) {
                if (buffer[i] == '\r' && buffer[i + 1] == '\n') {
                    return i + 2;
                }
            }
        }

        return -1;
    }

    private void rotateBuffer() {
        if (rotateTimesCounter == 0) {
            throw new RuntimeException("RotateBuffer times exceeded");
        }

        rotateTimesCounter -= 1;

        byte[] newBuffer = new byte[buffer.length + buffer.length];
        System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
        buffer = newBuffer;
    }

    public int getSize() {
        return size;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public byte[] array() {
        return buffer;
    }

    public void setAllowedEndOfStream(boolean allowedEndOfStream) {
        this.allowedEndOfStream = allowedEndOfStream;
    }

}
