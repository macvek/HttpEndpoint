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
    private final byte[] secondaryBuffer;
    private int secondaryBufferPointer;
    private OnFieldHandler onFieldHandler;
    private InputStream inputStream;
    private static final CompareByteArrays ENDLINE = new CompareByteArrays(new byte[] {'\r','\n' });
    private static final CompareByteArrays TWOHYPHENS = new CompareByteArrays(new byte[] {'-','-' });
    
    private final CompareByteArrays boundaryComparator;

    public static interface OnFieldHandler {
        public void onField(List<String> headers, InputStream content) throws IOException;
    }

    public MultipartFormDataReader(String boundary) {
        this.boundaryBytes = ("--"+boundary).getBytes();
        bufferSize  = boundaryBytes.length;
        boundaryReadBuffer = new byte[bufferSize];
        secondaryBufferPointer = bufferSize;
        secondaryBuffer = new byte[bufferSize];
        boundaryComparator = new CompareByteArrays(boundaryBytes);
    }
    
    public MultipartFormDataReader readFromStream(InputStream stream) throws IOException {
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
            if (fillBoundaryReadBuffer(bufferSize) < bufferSize) {
                break;
            }
        }
    }
    
    private int fillBoundaryReadBuffer(int limit) throws IOException {
        int rotatedBytes = rotateBuffers();
        int bytesToRead = bufferSize - rotatedBytes;
        boolean readingNotRequired = false;
        
        if (readingNotRequired || bytesToRead == inputStream.read(boundaryReadBuffer, rotatedBytes, bytesToRead)) {
            int offset = boundaryComparator.findOffset(boundaryReadBuffer);
            if (offset >= limit) {
                cutToSecondaryBuffer(limit, rotatedBytes);
                return limit;
            }
            else
            if (offset > 0 && offset < boundaryBytes.length) {
                return fillSecondaryBuffer(offset);
            }
            else {
                return offset;
            }
        }
        else {
            throw new RuntimeException("Not enough bytes read, aborting");
        }
    }

    private int rotateBuffers() {
        int rotatedBytes = bufferSize-secondaryBufferPointer;
        if (rotatedBytes > 0) {
            System.arraycopy(secondaryBuffer, secondaryBufferPointer, boundaryReadBuffer, 0, rotatedBytes);
        }
        return rotatedBytes;
    }

    private int fillSecondaryBuffer(int offset) throws RuntimeException, IOException {
        int partialBoundaryLength = bufferSize - offset;
        System.arraycopy(boundaryReadBuffer, offset, secondaryBuffer, 0, partialBoundaryLength);
        if (offset == inputStream.read(secondaryBuffer,partialBoundaryLength, offset)) {
            if (0 == boundaryComparator.findOffset(secondaryBuffer)) {
                secondaryBufferPointer = bufferSize;
                return offset;
            }
            else {
                secondaryBufferPointer = offset;                
                return bufferSize;
            }
        }
        else {
            throw new RuntimeException("Boundary not found in stream");
        }
    }
    
    private void cutToSecondaryBuffer(int from, int to) {
        int length = to - from;
        secondaryBufferPointer = bufferSize - length;
        System.arraycopy(boundaryReadBuffer, from, secondaryBuffer, secondaryBufferPointer,  length);
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

        public UpToBoundaryInputStream() {
            super(null);
        }

        @Override
        public int read(byte[] bytes, int offset, int length) throws IOException {
            int newOffset = offset;
            int maxOffset = length - offset;
            for(;;) {
                int bytesRead = fillBoundaryReadBuffer(length);
                System.arraycopy(boundaryReadBuffer, 0, bytes, newOffset, bytesRead);
                newOffset += bytesRead; 
                
                if (bytesRead < bufferSize) {
                    break;
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
