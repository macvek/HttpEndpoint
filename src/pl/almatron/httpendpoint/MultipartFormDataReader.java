package pl.almatron.httpendpoint;

import java.io.InputStream;

/**
 * HttpEndpoint
 * @author macvek
 */
public class MultipartFormDataReader {
    
    public interface FieldHandler {
        void onHeaders(String[] headers);
        void onData(byte[] buffer, boolean pending);
    }
    
    private byte[] boundary;
    private byte[] buffer;
    private FieldHandler fieldHandler;
    
    public MultipartFormDataReader() {
        
    }
    
    
}
