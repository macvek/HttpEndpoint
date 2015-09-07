package pl.almatron.httpendpoint;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * HttpEndpoint
 *
 * @author macvek
 */
public class HttpResponseBuffer {

    private static final Charset usAscii = Charset.forName("US-ASCII");
    private static final List<byte[]> baseHeaders = Arrays.asList(
            "Connection: close\r\n".getBytes(usAscii),
            "Server: HttpEndpoint\r\n".getBytes(usAscii)
    );
    private final OutputStream outputStream;
    private final ArrayList<byte[]> headers = new ArrayList<>(baseHeaders);
    private byte[] responseBody;
    private InputStream responseBodyStream;
    private int status = 200;
    
    private int contentLength;

    public HttpResponseBuffer(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public void setResponseBody(byte[] responseBody) {
        contentLength = responseBody.length;
        this.responseBody = responseBody;
    }
    
    public void setResponseBodyAsStream(InputStream stream) {
        try {
            contentLength = stream.available();
        } catch (IOException ex) {
            throw new RuntimeException("failed on stream.available", ex);
        }
        responseBodyStream = stream;
    }

    public void setStatus(int status) {
        this.status = status;
    }
    
    public void setContentType(String contentType) {
        addHeader("Content-Type: " + contentType);
    }

    public void addHeader(String header) {
        headers.add((header + "\r\n").getBytes(usAscii));
    }

    public void send() {
        try {
            sendStatus();
            sendHeaders();

            sendResponseBody();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void sendHeaders() throws IOException {
        for (byte[] header : headers) {
            outputStream.write(header);
        }
        outputStream.write(("Content-Length: " + contentLength + "\r\n\r\n").getBytes(usAscii));
    }

    private void sendStatus() throws IOException {
        outputStream.write(("HTTP/1.1 "+status+" OK\r\n").getBytes(usAscii));
    }

    private void sendResponseBody() throws IOException {
        if (responseBody != null) {
            outputStream.write(responseBody);
        }
        else if (responseBodyStream != null) {
            byte[] buffer = new byte[1024];
            while(responseBodyStream.available() > 0) {
                int read = responseBodyStream.read(buffer);
                outputStream.write(buffer,0,read);
            }
        }
    }
}
