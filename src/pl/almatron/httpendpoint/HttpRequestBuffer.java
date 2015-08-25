package pl.almatron.httpendpoint;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * HttpEndpoint
 *
 * @author macvek
 */
public class HttpRequestBuffer {

    private int bodyLength;

    private String method;
    private String query;
    private String protocol;

    private final BufferedInputStream bufferedInputStream;
    
    private final Scanner scanner;
    private static final String END_OF_LINE = "\r\n";
    
    public HttpRequestBuffer(InputStream inputStream) {
        bufferedInputStream = new BufferedInputStream(inputStream);
        scanner = new Scanner(bufferedInputStream);
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
