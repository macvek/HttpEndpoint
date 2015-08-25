package pl.almatron.httpendpoint;

import java.io.IOException;
import java.io.InputStream;

/**
 * HttpEndpoint
 *
 * @author macvek
 */
public class RequestHandler {

    void handleRequest(HttpRequestBuffer requestBuffer, HttpResponseBuffer responseBuffer) throws IOException {

        requestBuffer.readFirstLine();
        System.out.println("Method:" + requestBuffer.getMethod());
        System.out.println("Query:" + requestBuffer.getQuery());
        System.out.println("Protocol:" + requestBuffer.getProtocol());

        requestBuffer.saveHeaders();

        String contentLength = null;

        for (String header : requestBuffer.readHeaders()) {
            final String CONTENT_LENGTH = "Content-Length: ";
            String valueOf = takeValueOf(header, CONTENT_LENGTH);
            if (valueOf != null) {
                contentLength = valueOf;
            }
            System.out.println(header);
        }

        if ("POST".equals(requestBuffer.getMethod())) {
            if (contentLength != null) {
                requestBuffer.setupBodySize(Integer.parseInt(contentLength));
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

    private String takeValueOf(String header, final String CONTENT_LENGTH) {
        if (header.startsWith(CONTENT_LENGTH)) {
            return header.substring(CONTENT_LENGTH.length());
        } else {
            return null;
        }
    }
}
