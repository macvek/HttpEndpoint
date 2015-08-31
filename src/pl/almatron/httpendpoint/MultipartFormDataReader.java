package pl.almatron.httpendpoint;

import java.io.InputStream;

/**
 * HttpEndpoint
 * @author macvek
 */
public class MultipartFormDataReader {
    
    private final String boundary;
    private OnFieldHandler onFieldHandler;
    
    public static interface OnFieldHandler {
        public void onField(String fieldName, InputStream value);
    }

    public MultipartFormDataReader(String boundary) {
        this.boundary = boundary;
    }
    
    public MultipartFormDataReader readFromStream(InputStream stream) {
        
        
        return this;
    }
    
    public MultipartFormDataReader withOnFieldHandler(OnFieldHandler handler) {
        onFieldHandler = handler;
        return this;
    }
    
}
