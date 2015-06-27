package pl.almatron.httpendpoint;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * HttpEndpoint
 *
 * @author macvek
 */
public class HttpRequestBuffer {

    private final InputStream inputStream;
    private byte[] buffer;
    private int endOfFirstLine;
    private int headersTermination;
    private int bodyTermination;
    private final static int rotateSize = 512;
    private final static int rotateTimesLimit = 3;
    private int rotateTimesCounter = rotateTimesLimit;
    private boolean endOfStream;
    private boolean allowedEndOfStream;

    private int readPointer;

    private String method;
    private String query;
    private String protocol;

    private final int[] headerIndexes;
    private final static int maxHeaders = 32;
    private int headerPointer = 0;

    private static final Charset usAscii = Charset.forName("US-ASCII");

    public HttpRequestBuffer(InputStream inputStream) {
        this.inputStream = inputStream;
        buffer = new byte[rotateSize];
        headerIndexes = new int[maxHeaders + 1];
    }

    public void readFirstLine() {
        do {
            readChunk();
            markEndOfFirstLine();
        } while (endOfFirstLine == 0);

        parseFirstLine();

    }
    
    
    public void saveBody(int lengthRequired) {
        bodyTermination = bodyStart() + lengthRequired;
        while (bodyTermination > readPointer) {
            readChunk();
        }
    }

    public byte[] readBody() {
        byte[] body = new byte[bodyLength()];
        System.arraycopy(buffer, bodyStart(), body, 0, bodyLength());
        return body;
    }
    
    public void saveHeaders() {
        headerIndexes[headerPointer] = endOfFirstLine;
        headerPointer = 1;
        for (;;) {
            int lastIndex = headerIndexes[headerPointer - 1];
            int nextLinePointer = readToNextLine(lastIndex);
            if (nextLinePointer == -1) {
                readChunk();
            } else {
                if (isHeaderListFull()) {
                    throw new RuntimeException("Headers limit exceeded");
                }

                if (isEmptyHeaderLine(nextLinePointer)) {
                    headersTermination = nextLinePointer;
                    allowedEndOfStream = true;
                    return;
                }

                addHeaderPointer(nextLinePointer);
            }
        }
    }
    
    public String[] readHeaders() {
        String[] headers = new String[headerPointer - 1];
        for (int i = 1; i < headerPointer; i++) {
            headers[i - 1] = new String(buffer,
                    headerIndexes[i - 1],
                    headerIndexes[i] - headerIndexes[i - 1] - 2,
                    usAscii);

        }

        return headers;
    }

    private void parseFirstLine() throws RuntimeException {
        int methodPos = 0;
        int queryPos = 0;

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
            query = new String(buffer, methodPos + 1, queryPos - methodPos, usAscii);
            protocol = new String(buffer, queryPos + 1, endOfFirstLine - queryPos - 2, usAscii);
        } else {
            throw new RuntimeException("Malformed query");
        }
    }

    private void markEndOfFirstLine() {
        assert endOfFirstLine == 0;

        int pointer = readToNextLine(0);
        if (pointer != -1) {
            endOfFirstLine = pointer;
        }
    }

    private int readToNextLine(int from) {
        if (readPointer - from > 1) {
            for (int i = from; i < readPointer - 1; i++) {
                if (buffer[i] == '\r' && buffer[i + 1] == '\n') {
                    return i + 2;
                }
            }
        }

        return -1;
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

    
    private void readChunk() {
        try {
            int bytesRead = inputStream.read(buffer, readPointer, buffer.length - readPointer);
            if (bytesRead == -1) {
                endOfStream = true;
            } else {
                readPointer += bytesRead;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (endOfStream && !allowedEndOfStream) {
            throw new RuntimeException("Unexpected end of stream");
        }

        if (readPointer == buffer.length) {
            rotateBuffer();
        }
    }

    private void rotateBuffer() {
        if (rotateTimesCounter == 0) {
            throw new RuntimeException("RotateBuffer times exceeded");
        }

        rotateTimesCounter -= 1;

        byte[] newBuffer = new byte[buffer.length + buffer.length];
        System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
        buffer = newBuffer;
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
