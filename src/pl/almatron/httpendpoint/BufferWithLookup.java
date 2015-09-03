package pl.almatron.httpendpoint;

import java.io.IOException;
import java.io.InputStream;

/**
 * HttpEndpoint
 * @author macvek
 */
public class BufferWithLookup {
    private InputStream inputStream;
    private byte[] lookupBuffer;
    
    public BufferWithLookup(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public void setLookupBuffer(byte[] buffer) {
        lookupBuffer = buffer;
    }
    
    public int read(byte[] outBuffer) throws IOException {
        int ptr = 0;
        if (lookupBuffer != null) {
            System.arraycopy(lookupBuffer, 0, outBuffer, 0, lookupBuffer.length);
            ptr=lookupBuffer.length;
            lookupBuffer = null;
        }
        
        return inputStream.read(outBuffer, ptr, outBuffer.length - ptr) + ptr;
    }

}
