package pl.almatron.httpendpoint;

/**
 * HttpEndpoint
 * @author macvek
 */
public class HeadersParser {
    private byte[] buffer;
    private int[] headerIndexes = new int[maxHeaders + 1];
    private final static int maxHeaders = 32;

    public void setBuffer(byte[] buffer) {
        this.buffer = buffer;
    }
    
    public int findEndOfHeaders() {
      
        return -1;
    }
    
    public String[] getHeaders() {
        return null;
    }
    
}
