package pl.almatron.httpendpoint;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * HttpEndpoint
 *
 * @author macvek
 */
public class HttpRequestBuffer {
    private static final String END_OF_LINE = "\r\n";
    private static final byte[] END_OF_HEADERS_PATTERN = new byte[] {'\r','\n','\r','\n'};
    private int bodyLength;

    private String method;
    private String query;
    private String protocol;
   
    private Scanner invocationScanner;
    private final BufferedInputStream bufferedInputStream;
    
    private ByteBuffer headersReadBuffer;
    
    public HttpRequestBuffer(InputStream inputStream) {
        bufferedInputStream = new BufferedInputStream(inputStream);
        
    }

    public void readFirstLine() {
        bufferUntilHeadersEnd();
        initializeInvocationScanner();
        final String firstLine = invocationScanner.next();
        String[] parts = firstLine.split(" ",3);
        if (parts.length == 3) {
            method = parts[0];
            query = parts[1];
            protocol = parts[2];
        }
        else {
            throw new RuntimeException("Cannot parse readFirstLine: " + firstLine);
        }
    }
    
    
    public void setupBodySize(int bodyLength) {
        this.bodyLength = bodyLength;
    }

    public byte[] readBody() {
        byte[] body = new byte[bodyLength];
        try {
            bufferedInputStream.read(body, 0, body.length);
        } catch (IOException ex) {
            throw new RuntimeException("Error while readBody", ex);
        }
        return body;
    }
   
    public List<String> readHeaders() {
        ArrayList<String> headers = new ArrayList<>();
        boolean foundLast = false;
        while(invocationScanner.hasNext()) {
            String line = invocationScanner.next();

            if (line.isEmpty()) {
                foundLast = true;
                break;
            }
            else {
                headers.add(line);
            }
        }
        
        if (!foundLast) {
            throw new RuntimeException("Header parsing error, no trailing endline");
        }
        
        return headers;
    }
    
    private void bufferUntilHeadersEnd() {
        final CompareByteArrays compareByteArrays= new CompareByteArrays(END_OF_HEADERS_PATTERN);
        byte[] expectedBytes = new byte[END_OF_HEADERS_PATTERN.length];
        initializeHeadersReadBuffer();
        int expectedToRead = expectedBytes.length;
        for (;;) {
            try {
                if ( bufferedInputStream.read(expectedBytes, expectedBytes.length - expectedToRead, expectedToRead) !=  expectedToRead) {
                    throw new RuntimeException("Unexpected end of stream");
                }
                putIntoHeadersReadBuffer(expectedBytes);
                
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

    private void putIntoHeadersReadBuffer(byte[] expectedEndLine) {
        headersReadBuffer.put(expectedEndLine);
    }

    private void initializeHeadersReadBuffer() {
        headersReadBuffer = ByteBuffer.allocate(1024);
    }
    
    private void initializeInvocationScanner() {
        invocationScanner = new Scanner(new ByteArrayInputStream(headersReadBuffer.array(), 0, headersReadBuffer.position()));
        invocationScanner.useDelimiter(END_OF_LINE);
    }
    
    public String getMethod() {
        return method;
    }

    public String getQuery() {
        return query;
    }

    public String getProtocol() {
        return protocol;
    }
    
    

}
