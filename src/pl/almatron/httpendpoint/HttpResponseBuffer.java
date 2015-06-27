package pl.almatron.httpendpoint;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    private int contentLength;

    public HttpResponseBuffer(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public void setResponseBody(byte[] responseBody) {
        contentLength = responseBody.length;
        this.responseBody = responseBody;
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
        outputStream.write("HTTP/1.1 200 OK\r\n".getBytes(usAscii));
    }

    private void sendResponseBody() throws IOException {
        if (responseBody != null) {
            outputStream.write(responseBody);
        }
    }
}
