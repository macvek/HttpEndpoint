package pl.almatron.httpendpoint;

import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * HttpEndpoint
 *
 * @author macvek
 */
public class HttpRequestBuffer {

    private RotateBuffer rotateBuffer;
    private int endOfFirstLine;
    private int headersTermination;
    private int bodyTermination;

    private String method;
    private String query;
    private String protocol;

    private final int[] headerIndexes;
    private final static int maxHeaders = 32;
    private int headerPointer = 0;

    private static final Charset usAscii = Charset.forName("US-ASCII");

    public HttpRequestBuffer(InputStream inputStream) {
        headerIndexes = new int[maxHeaders + 1];
        rotateBuffer = new RotateBuffer();
        rotateBuffer.setInputStream(inputStream);
    }

    public void readFirstLine() {
        do {
            rotateBuffer.readChunk();
            markEndOfFirstLine();
        } while (endOfFirstLine == 0);

        parseFirstLine();

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
    
    public void saveHeaders() {
        headerIndexes[headerPointer] = endOfFirstLine;
        headerPointer = 1;
        for (;;) {
            int lastIndex = headerIndexes[headerPointer - 1];
            int nextLinePointer = rotateBuffer.readToNextLine(lastIndex);
            if (nextLinePointer == -1) {
                rotateBuffer.readChunk();
            } else {
                if (isHeaderListFull()) {
                    throw new RuntimeException("Headers limit exceeded");
                }

                if (isEmptyHeaderLine(nextLinePointer)) {
                    headersTermination = nextLinePointer;
                    rotateBuffer.setAllowedEndOfStream(true);
                    return;
                }

                addHeaderPointer(nextLinePointer);
            }
        }
    }
    
    public String[] readHeaders() {
        String[] headers = new String[headerPointer - 1];
        for (int i = 1; i < headerPointer; i++) {
            headers[i - 1] = new String(rotateBuffer.array(),
                    headerIndexes[i - 1],
                    headerIndexes[i] - headerIndexes[i - 1] - 2,
                    usAscii);

        }

        return headers;
    }

    private void parseFirstLine() throws RuntimeException {
        int methodPos = 0;
        int queryPos = 0;
        byte[] buffer = rotateBuffer.array();
        for (int i = 0; i < endOfFirstLine - 2; i++) {
            if (buffer[i] == ' ') {
                if (methodPos == 0) {
                    methodPos = i;
                } else {
                    queryPos = i;
                }
            }
        }

        if (methodPos > 0
                && methodPos < queryPos
                && queryPos < endOfFirstLine - 2) {
            method = new String(buffer, 0, methodPos, usAscii);
            query = new String(buffer, methodPos + 1, queryPos - methodPos-1, usAscii);
            protocol = new String(buffer, queryPos + 1, endOfFirstLine - queryPos - 2, usAscii);
        } else {
            throw new RuntimeException("Malformed query");
        }
    }

    private void markEndOfFirstLine() {
        assert endOfFirstLine == 0;

        int pointer = rotateBuffer.readToNextLine(0);
        if (pointer != -1) {
            endOfFirstLine = pointer;
        }
    }
    

    private int bodyLength() {
        return bodyTermination - bodyStart();
    }

    private int bodyStart() {
        return headersTermination;
    }

    private void addHeaderPointer(int nextLinePointer) {
        headerIndexes[headerPointer] = nextLinePointer;
        headerPointer++;
    }

    private boolean isHeaderListFull() {
        return headerPointer == headerIndexes.length;
    }

    private boolean isEmptyHeaderLine(int nextLinePointer) {
        return nextLinePointer - headerIndexes[headerPointer - 1] == 2;
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
