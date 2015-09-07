package pl.almatron.httpendpoint;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

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

        String contentLength = null;
        String boundary = null;
        
        for (String header : requestBuffer.readHeaders()) {
            
            String[] keyAndValue = splitHeader(header);
            if (CONTENT_LENGTH.equals(keyAndValue[0])) {
                contentLength = keyAndValue[1];
            }
            else if (CONTENT_TYPE.equals(keyAndValue[0])) {
                if ("multipart/form-data".equals(parseContentType(keyAndValue[1]))) {
                    boundary = parseBoundary(keyAndValue[1]);
                }
            }
        }

        if ("POST".equals(requestBuffer.getMethod())) {
            if (boundary != null) {
                MultipartFormDataReader multipartFormDataReader = new MultipartFormDataReader(boundary);
                
                multipartFormDataReader.withOnFieldHandler((List<String> headers, InputStream content) -> {
                    String fileName = "";
                    for (String header : headers) {
                        String fileInvocation = "Content-Disposition: form-data; name=\"filefield\"; filename=";
                        if (header.contains(fileInvocation)) {
                            fileName = header.replace(fileInvocation, "").replace("\"","").replace("\\","").replace("/", "");
                        }
                    }
                    byte[] contentBytes = new byte[1024*1024];
                    
                    int read = content.read(contentBytes);
                    if (read == contentBytes.length) {
                        throw new RuntimeException("Upload limit exceeded");
                    }
                    
                    if (!fileName.isEmpty()) {
                        FileOutputStream uploaded = new FileOutputStream(fileName);
                        uploaded.write(contentBytes, 0, read);
                    }
                    
                });
                
                multipartFormDataReader.readFromStream(requestBuffer.getRequestBufferedInputStream());
            }
            else
            if (contentLength != null) {
                final int contentLengthVal = Integer.parseInt(contentLength);
                if (contentLengthVal > 4096) {
                    throw new RuntimeException("ContentLength exceeds 4096 bytes");
                }
                requestBuffer.setupBodyLength(contentLengthVal);
            } else {
                throw new RuntimeException("Empty ContentLength");
            }
        }
        
        String queryFile = requestBuffer.getQuery();
        if (queryFile.isEmpty() || "/".equals(queryFile)) {
            queryFile = "welcome.html";
        }
        
        URL resourceURL = getClass().getResource("/public/"+queryFile);
        final String mime;
        final InputStream responseStream;
        if (resourceURL != null) {
            responseStream = resourceURL.openStream();
            mime = MIMEType.forType(fileExtension(queryFile));
        }
        else {
            responseStream = status404Stream();
            responseBuffer.setStatus(404);
            mime = MIMEType.forType("html");
        }
        
        responseBuffer.setContentType(mime);
        responseBuffer.setResponseBodyAsStream(responseStream);

        responseBuffer.send();
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

    private String fileExtension(String queryFile) {
        String[] split = queryFile.split("\\.");
        return split[split.length-1];
    }

    private InputStream status404Stream() {
        return getClass().getResourceAsStream("/special/status404.html");
    }
}
