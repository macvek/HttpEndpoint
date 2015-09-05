package pl.almatron.httpendpoint;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * HttpEndpoint
 * @author macvek
 */
public class MultipartFormDataReader {
    private final int bufferSize;
    private final byte[] boundaryBytes;
    private final byte[] boundaryReadBuffer;
    private int boundaryOverloadedSize;
    private int boundaryOverloadedLimit;
    private OnFieldHandler onFieldHandler;
    private InputStream inputStream;
    private static final CompareByteArrays ENDLINE = new CompareByteArrays(new byte[] {'\r','\n' });
    private static final CompareByteArrays TWOHYPHENS = new CompareByteArrays(new byte[] {'-','-' });
    
    private final CompareByteArrays boundaryComparator;
    private BufferWithLookup bufferWithLookup;

    public static interface OnFieldHandler {
        public void onField(List<String> headers, InputStream content) throws IOException;
    }

    public MultipartFormDataReader(String boundary) {
        this.boundaryBytes = ("--"+boundary).getBytes();
        bufferSize  = boundaryBytes.length;
        boundaryReadBuffer = new byte[bufferSize];
        boundaryComparator = new CompareByteArrays(boundaryBytes);
    }
    
    public MultipartFormDataReader readFromStream(InputStream stream) throws IOException {
        bufferWithLookup = new BufferWithLookup(stream);
        inputStream = stream;
        readUntilBoundary();
        for (;;) {
            if (false == isEndOfStream()) {
                final InputStream headersInputStream = new CutHeadersFromInputStream(stream).asInputStream();
                final Scanner scanner = new Scanner(headersInputStream);
                scanner.useDelimiter("\r\n");
                ArrayList<String> headers = new ArrayList<>();
                while(scanner.hasNext()) {
                    headers.add(scanner.next());
                }
                
                onFieldHandler.onField(headers, toNextBoundaryInputStream());
            }
            else {
                break;
            }
            
        }
        
        return this;
    }
    
    private InputStream toNextBoundaryInputStream() {
        return new UpToBoundaryInputStream();
    }
    
    public MultipartFormDataReader withOnFieldHandler(OnFieldHandler handler) {
        onFieldHandler = handler;
        return this;
    }
    
    private void readUntilBoundary() throws IOException {
        for(;;) {
            if (fillBoundaryReadBuffer() < bufferSize) {
                break;
            }
        }
    }
    
    private int fillBoundaryReadBuffer() throws IOException {
       int bytesRead = bufferWithLookup.read(boundaryReadBuffer);
       if (bytesRead < boundaryReadBuffer.length) {
           throw new RuntimeException("boundary not found");
       }
       
       int offset = boundaryComparator.findOffset(boundaryReadBuffer);
       if (offset == 0) {
           return 0;
       }
       else if (offset == bytesRead) {
           return bytesRead;
       }
       else {
           byte[] backingBuffer = new byte[offset];
           byte[] storage = new byte[bytesRead];
           bufferWithLookup.read(backingBuffer);
           System.arraycopy(boundaryReadBuffer, offset, storage, 0, bytesRead - offset);
           System.arraycopy(backingBuffer, 0, storage, bytesRead - offset, offset);
           
           if (boundaryComparator.findOffset(storage) == 0) {
               return offset;
           }
           else {
               bufferWithLookup.setLookupBuffer(backingBuffer);
               return bytesRead;
           }
       }
       
    }

    
    private boolean isEndOfStream() throws IOException {
        byte[] twoBytes = new byte[2];
        if (2 == inputStream.read(twoBytes)) {
            if (0 == ENDLINE.findOffset(twoBytes)) {
                return false;
            }
            else if (0 == TWOHYPHENS.findOffset(twoBytes)) {
                return true;
            }
            else {
                throw new RuntimeException("expected end line or two colons, but got "+new String(twoBytes));
            }
        }
        else {
            throw new RuntimeException("Expected two bytes, but got less");
        }
    }
    
    public class UpToBoundaryInputStream extends FilterInputStream {
        private boolean isEmpty;
        
        public UpToBoundaryInputStream() {
            super(null);
        }

        @Override
        public int read(byte[] bytes, int offset, int length) throws IOException {
            int newOffset = offset;
            int maxOffset = offset + length;
            if (boundaryOverloadedSize > 0) {
                int bytesToReadFromOverload = Math.min(boundaryOverloadedSize, length);
                System.arraycopy(boundaryReadBuffer, boundaryOverloadedLimit - boundaryOverloadedSize, bytes, newOffset, bytesToReadFromOverload);
                boundaryOverloadedSize -= bytesToReadFromOverload;
                newOffset += bytesToReadFromOverload;
                if (maxOffset == newOffset) {
                    return bytesToReadFromOverload;
                }
            }
            
            while (!isEmpty) {
                int bytesRead = fillBoundaryReadBuffer();
                isEmpty = bytesRead < bufferSize;
                
                if (newOffset + bytesRead > maxOffset) {
                    int bytesSizeToCopyToOutput = maxOffset - newOffset;
                    boundaryOverloadedLimit = bytesRead;
                    boundaryOverloadedSize = bytesRead - bytesSizeToCopyToOutput;
                    System.arraycopy(boundaryReadBuffer, 0, bytes, newOffset, bytesSizeToCopyToOutput);
                    newOffset = maxOffset;
                    break;
                }
                else {
                    System.arraycopy(boundaryReadBuffer, 0, bytes, newOffset, bytesRead);
                    newOffset += bytesRead; 
                }

            }
            return newOffset - offset;
        }

        @Override
        public int read() throws IOException {
            throw new UnsupportedOperationException("UpToBoundaryInputStream supports only reading to given buffer");
        }
        
        
    }
    
}
