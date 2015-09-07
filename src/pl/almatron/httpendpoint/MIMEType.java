package pl.almatron.httpendpoint;

import java.util.HashMap;

/**
 * HttpEndpoint
 * @author macvek
 */
public class MIMEType {
    private static final HashMap<String,String> mimeTypes;
    static {
        mimeTypes = new HashMap<>();
        mimeTypes.put("html", "text/html");
        mimeTypes.put("png", "image/png");
    }
    
    public static String forType(String fileName) {
        String type = mimeTypes.get(fileName.toLowerCase());
        return type != null ? type : "application/octet-stream";
    }
    
}
