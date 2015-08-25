/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.almatron.httpendpoint;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author macvek
 */
public class HttpRequestBufferTest {
    
    public HttpRequestBufferTest() {
    }

    @Test
    public void shouldProcessSimpleGetQueryWithHeaders() {
        String textInput = "GET /query HTTP/1.1\r\n";
        textInput+="Header: headerValue\r\n\r\n";
        InputStream input = new ByteArrayInputStream(textInput.getBytes());
        HttpRequestBuffer buffer = new HttpRequestBuffer(input);
        
        buffer.readFirstLine();
        assertEquals("GET", buffer.getMethod());
        assertEquals("HTTP/1.1", buffer.getProtocol());
        assertEquals("/query", buffer.getQuery());

        List<String> headers = buffer.readHeaders();
        assertEquals("Header: headerValue", headers.get(0));
    }
    
    @Test
    public void shouldProcessSimplePostQueryWithHeaders() {
        String postData = "SomeDataThatIsPosted";
        String textInput = "POST /query HTTP/1.1\r\n";
        textInput+="Header: headerValue\r\n";
        textInput+="Content-Length: "+postData.length()+"\r\n";
        textInput+="\r\n";
        InputStream input = new ByteArrayInputStream(textInput.getBytes());
        HttpRequestBuffer buffer = new HttpRequestBuffer(input);
        
        buffer.readFirstLine();
        assertEquals("POST", buffer.getMethod());
        assertEquals("HTTP/1.1", buffer.getProtocol());
        assertEquals("/query", buffer.getQuery());

        List<String> headers = buffer.readHeaders();
        assertEquals("Header: headerValue", headers.get(0));
        
        buffer.readBody();
    }
    
}
