/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.almatron.httpendpoint;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author macvek
 */
public class CutHeadersFromInputStreamTest {
    
    @Test
    public void testShouldReadInputBufferToEndOfHeaders() throws IOException {
        String textInput = "GET /query HTTP/1.1\r\n";
        textInput+="Header: headerValue\r\n\r\n";
        InputStream input = new ByteArrayInputStream(textInput.getBytes());
        
        InputStream headers = new CutHeadersFromInputStream(input).asInputStream();
        byte[] buffer = new byte[128];
        int length = headers.read(buffer);
        
        final String read = new String(buffer,0,length);
        assertEquals(textInput.length(), read.length());
        assertEquals(textInput, read);
    }
    
}
