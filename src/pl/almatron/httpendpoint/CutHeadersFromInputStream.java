package pl.almatron.httpendpoint;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * HttpEndpoint
 *
 * @author macvek
 */
public class CutHeadersFromInputStream {

    private final InputStream inputStream;

    private static final byte[] END_OF_HEADERS_PATTERN = new byte[] {'\r','\n','\r','\n'};
    private static final int HEADERS_SIZE_CAPACITY = 4096;
    private static final int HEADERS_SIZE_INITIALSIZE = 256;
    
    private ByteBuffer headersReadBuffer;

    public CutHeadersFromInputStream(InputStream stream) {
        this.inputStream = stream;
    }

    public InputStream asInputStream() {
        initializeHeadersReadBuffer();
        bufferUntilHeadersEnd();
        return new ByteArrayInputStream(headersReadBuffer.array(), 0, headersReadBuffer.position() -2); // -2 == '\r\n'.length
    }
    
    private void initializeHeadersReadBuffer() {
        headersReadBuffer = ByteBuffer.allocate(HEADERS_SIZE_INITIALSIZE);
    }

    private void rotateHeadersReadBuffer() {
        int newLength = headersReadBuffer.capacity() + headersReadBuffer.capacity();
        if (newLength > HEADERS_SIZE_CAPACITY) {
            throw new RuntimeException("Headers size limit exceeded");
        }
        else {
            headersReadBuffer.limit(headersReadBuffer.position());
            headersReadBuffer.position(0);
            
            ByteBuffer newByteBuffer = ByteBuffer.allocate(newLength);
            newByteBuffer.put(headersReadBuffer);
            headersReadBuffer = newByteBuffer;
        }
        
    }
    
    private void bufferUntilHeadersEnd() {
        final CompareByteArrays compareByteArrays= new CompareByteArrays(END_OF_HEADERS_PATTERN);
        byte[] expectedBytes = new byte[END_OF_HEADERS_PATTERN.length];
        initializeHeadersReadBuffer();
        int expectedToRead = expectedBytes.length;
        for (;;) {
            try {
                if ( inputStream.read(expectedBytes, expectedBytes.length - expectedToRead, expectedToRead) !=  expectedToRead) {
                    throw new RuntimeException("Unexpected end of stream");
                }
                putIntoBufferCountingFromEnd(expectedBytes, expectedToRead);
                
            }catch(IOException e) {
                throw new RuntimeException("Error while end of headers scan", e);
            }
            
            expectedToRead = compareByteArrays.findOffset(expectedBytes);
            if (expectedToRead < expectedBytes.length) {
                if (expectedToRead == 0) {
                    break;
                }
                else {
                    System.arraycopy(expectedBytes, expectedToRead, expectedBytes, 0, expectedBytes.length - expectedToRead);
                }
            }
        } 
    }
    
    private void putIntoBufferCountingFromEnd(byte[] invocationContent, int lengthToPut) {
        
        if (headersReadBuffer.position() + lengthToPut > headersReadBuffer.capacity()) {
            rotateHeadersReadBuffer();
        }
        headersReadBuffer.put(invocationContent, invocationContent.length - lengthToPut, lengthToPut);
    }

}
