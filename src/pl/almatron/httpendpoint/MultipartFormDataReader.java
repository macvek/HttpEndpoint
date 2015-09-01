package pl.almatron.httpendpoint;

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
    
    private final byte[] boundaryBytes;
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
        boundaryComparator = new CompareByteArrays(boundaryBytes);
    }
    
    public MultipartFormDataReader readFromStream(InputStream stream) throws IOException {
        inputStream = stream;
        for (;;) {
            readUntilBoundary();
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
        return inputStream;
    }
    
    public MultipartFormDataReader withOnFieldHandler(OnFieldHandler handler) {
        onFieldHandler = handler;
        return this;
    }
    
    private void readUntilBoundary() throws IOException {
        byte[] buffer = new byte[boundaryBytes.length];
        int bytesRead = inputStream.read(buffer);
        if (bytesRead == buffer.length) {
            int offset = boundaryComparator.findOffset(buffer);
            while (offset > 0) {
                System.arraycopy(buffer, offset, buffer, 0, buffer.length - offset);
                if (offset == inputStream.read(buffer,buffer.length - offset, offset)) {
                    offset = boundaryComparator.findOffset(buffer);
                }
                else {
                    throw new RuntimeException("Boundary not found in stream");
                }
            }
        }
        else {
            throw new RuntimeException("Not enough bytes read, aborting");
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
    
}
