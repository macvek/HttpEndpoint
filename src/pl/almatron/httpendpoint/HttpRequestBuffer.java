package pl.almatron.httpendpoint;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * HttpEndpoint
 *
 * @author macvek
 */
public class HttpRequestBuffer {

    private RotateBuffer rotateBuffer;
    private int headersTermination;
    private int bodyTermination;

    private String method;
    private String query;
    private String protocol;


    private final Scanner scanner;
    private static final String END_OF_LINE = "\r\n";
    
    public HttpRequestBuffer(InputStream inputStream) {
        scanner = new Scanner(new BufferedInputStream(inputStream));
        scanner.useDelimiter(END_OF_LINE);
    }

    public void readFirstLine() {
        final String firstLine = scanner.next();
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
    
    
    public void setupBodySize(int lengthRequired) {
        bodyTermination = bodyStart() + lengthRequired;
    }

    public byte[] readBody() {
        while (bodyTermination > rotateBuffer.getSize()) {
            rotateBuffer.readChunk();
        }
        
        byte[] body = new byte[bodyLength()];
        System.arraycopy(rotateBuffer.array(), bodyStart(), body, 0, bodyLength());
        return body;
    }
   
    public List<String> readHeaders() {
        ArrayList<String> headers = new ArrayList<>();
        boolean foundLast = false;
        while(scanner.hasNext()) {
            String line = scanner.next();
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

    private int bodyLength() {
        return bodyTermination - bodyStart();
    }

    private int bodyStart() {
        return headersTermination;
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
