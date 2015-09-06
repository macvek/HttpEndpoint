package pl.almatron.httpendpoint;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

/**
 * HttpEndpoint
 *
 * @author macvek
 */
public class RequestHandler {
    private final static String CONTENT_LENGTH = "Content-Length";
    private final static String CONTENT_TYPE = "Content-Type";
    private final static String BOUNDARY = "boundary=";
    private static Transformer transformer;
    {
        try {
            initTransformer();
        } catch (TransformerFactoryConfigurationError ex) {
            Logger.getLogger(RequestHandler.class.getName()).log(Level.SEVERE, null, ex);
        } catch (TransformerConfigurationException ex) {
            Logger.getLogger(RequestHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    void handleRequest(HttpRequestBuffer requestBuffer, HttpResponseBuffer responseBuffer) throws IOException {
        long startTime = System.nanoTime();
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
            if (boundary != null) {
                MultipartFormDataReader multipartFormDataReader = new MultipartFormDataReader(boundary);
                
                multipartFormDataReader.withOnFieldHandler((List<String> headers, InputStream content) -> {
                    byte[] contentBytes = new byte[1024];
                    int read = content.read(contentBytes);
                    System.out.println("RECEIVED::");
                    System.out.println(new String(contentBytes,0,read));
                });
                
                multipartFormDataReader.readFromStream(requestBuffer.getRequestBufferedInputStream());
                
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
        else if ("/transform.html".equals(requestBuffer.getQuery())) {
            responseBuffer.setContentType("text/html");
            try {
                responseBuffer.setResponseBody(readXslt());
            } catch (TransformerException ex) {
                Logger.getLogger(RequestHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        else {
            responseBuffer.setContentType("text/html");
            responseBuffer.setResponseBody(readFromStream("/helloworld.html"));
        }
        responseBuffer.send();
        System.out.println("END OF RESPONSE!");
        System.out.println("Processing time : "+(System.nanoTime() - startTime)/1000000000.0);
    }

    private byte[] readXslt() throws TransformerException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(4096);
        
        StreamSource source = new StreamSource(getClass().getResourceAsStream("/source.xml"));
        StreamResult result = new StreamResult(byteArrayOutputStream);
        transformer.transform(source, result);
        return byteArrayOutputStream.toByteArray();
    }

    private void initTransformer() throws TransformerFactoryConfigurationError, TransformerConfigurationException {
        StreamSource stylesource = new StreamSource(getClass().getResourceAsStream("/stylesheet.xsl"));
        TransformerFactory tFactory = TransformerFactory.newInstance();
        transformer = tFactory.newTransformer(stylesource);
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
