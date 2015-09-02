package pl.almatron.httpendpoint;

import java.io.IOException;
import java.io.InputStream;

/**
 * HttpEndpoint
 * @author macvek
 */
public class OffsetCutBuffer {
    private byte[] buffer;
    private byte[] backingBuffer;
    private InputStream inputStream;
    private int chunkSize;
    private int offset;
    private boolean rotateBeforeRead;
    
    public OffsetCutBuffer(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public void resetWithChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
        buffer = new byte[chunkSize];
        backingBuffer = new byte[chunkSize];
    }
    
    public int read(int givenLength) throws IOException {
        int pos = 0;
        int length = givenLength;
        if (length > chunkSize) {
            throw new IllegalArgumentException("Length exceeds chunkSize="+chunkSize);
        }
        
        if (rotateBeforeRead) {
            System.arraycopy(backingBuffer, chunkSize-offset, buffer, 0, offset);
            rotateBeforeRead = false;
            length -= offset;
            pos = offset;
        }
        
        return inputStream.read(buffer, pos, length);
    }

    public byte[] getChunk() {
        return buffer;
    }

    public void markOffset(int offset) throws IOException {
        if (offset >= chunkSize) {
            throw new IllegalArgumentException("offset >= chunkSize , offset="+offset+" , chunkSize="+chunkSize);
        }
        this.offset = offset;
        int length = chunkSize - offset;
        System.arraycopy(buffer, offset, backingBuffer, 0, length);
        inputStream.read(backingBuffer, length, chunkSize - length);
    }

    public byte[] getMarkedChunk() {
        return backingBuffer;
    }

    public void discardOffset() {
        rotateBeforeRead = true;
    }

}
