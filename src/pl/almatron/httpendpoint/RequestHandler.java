package pl.almatron.httpendpoint;

import java.io.IOException;
import java.io.InputStream;

/**
 * HttpEndpoint
 *
 * @author macvek
 */
public class RequestHandler {
    private final static String CONTENT_LENGTH = "Content-Length";
    private final static String CONTENT_TYPE = "Content-Type";
    private final static String BOUNDARY = "boundary=";
    
    void handleRequest(HttpRequestBuffer requestBuffer, HttpResponseBuffer responseBuffer) throws IOException {

        requestBuffer.readFirstLine();
        System.out.println("Method:" + requestBuffer.getMethod());
        System.out.println("Query:" + requestBuffer.getQuery());
        System.out.println("Protocol:" + requestBuffer.getProtocol());

        String contentLength = null;
        String contentType = null;
        String boundary = null;
        
        for (String header : requestBuffer.readHeaders()) {
            
            String[] keyAndValue = splitHeader(header);
            if (CONTENT_LENGTH.equals(keyAndValue[0])) {
                contentLength = keyAndValue[1];
            }
            else if (CONTENT_TYPE.equals(keyAndValue[0])) {
                contentType = parseContentType(keyAndValue[1]);
                if ("multipart/form-data".equals(contentType)) {
                    boundary = parseBoundary(keyAndValue[1]);
                }
            }
            
            
            System.out.println(header);
        }

        if ("POST".equals(requestBuffer.getMethod())) {
            if (false && boundary != null) {
                MultipartFormDataReader multipartFormDataReader = new MultipartFormDataReader(boundary);
                
            }
            else
            if (contentLength != null) {
                requestBuffer.setupBodyLength(Integer.parseInt(contentLength));
                System.out.println(new String(requestBuffer.readBody()));
            } else {
                throw new RuntimeException("Pusty contentLength");
            }
        }
        
        if ("/image.png".equals(requestBuffer.getQuery())) {
            responseBuffer.setContentType("image/png");
            responseBuffer.setResponseBody(readFromStream("/image.png"));
        }
        else {
            responseBuffer.setContentType("text/html");
            responseBuffer.setResponseBody(readFromStream("/helloworld.html"));
        }
        responseBuffer.send();
        System.out.println("END OF RESPONSE!");
    }

    private byte[] readFromStream(String request) throws IOException {
        byte[] buffer = new byte[1024*1024];
        int readCount;
        try (InputStream stream = getClass().getResourceAsStream(request)) {
            readCount = stream.read(buffer);
        }
        byte[] readBuffer = new byte[readCount];
        System.arraycopy(buffer, 0, readBuffer, 0, readCount);
        return readBuffer;
    }

    private String[] splitHeader(String header) {
        String[] keyAndValue = header.split(":");
        if (keyAndValue.length >= 2) {
            return new String[] {keyAndValue[0].trim(), keyAndValue[1].trim()};
        }
        else {
            throw new IllegalArgumentException("Header line doesn't contain semicolor: "+header);
        }
    }
    
    private String parseContentType(String contentType) {
        int indexOf = contentType.indexOf(";");
        if (indexOf == -1) {
            return contentType;
        }
        else {
            return contentType.substring(0, indexOf);
        }
    }
    
    private String parseBoundary(String contentType) {
        int indexOf = contentType.indexOf(BOUNDARY);
        if (indexOf == -1) {
            throw new IllegalArgumentException("boundary not found in "+contentType);
        }
        
        return contentType.substring(indexOf+BOUNDARY.length());
    }
}
