/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.almatron.httpendpoint;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;

/**
 *
 * @author macvek
 */
public class MultipartFormDataReaderTest {
    
    private MultipartFormDataReader reader;
    
    @Before
    public void setup() {
        flag = false;
        reader = new MultipartFormDataReader("XXXXXXXX");
    }

    private boolean flag;
    
    @Test
    public void shouldLoadHeaders() throws IOException {
        InputStream stream = new ByteArrayInputStream(
                        ("--XXXXXXXX\r\n" +
                        "Content-Disposition: form-data; name=\"fieldname\"\r\n" +
                        "\r\n" +
                        "Submit MULTIPART FORM DATA\r\n" + 
                        "--XXXXXXXX--\r\n").getBytes());
        
        
        final MultipartFormDataReader.OnFieldHandler onFieldHandler = (List<String> headers, InputStream value) -> {
            if (headers.contains("Content-Disposition: form-data; name=\"fieldname\"")) {
                flag = true;
            }
            byte[] bytes = new byte[128];
            value.read(bytes);
            
        };
        
        reader.withOnFieldHandler(onFieldHandler).readFromStream(stream);
        
        assertTrue(flag);
    }
    
    @Test
    public void shouldLoadContent() throws IOException {
        InputStream stream = new ByteArrayInputStream(
                        ("--XXXXXXXX\r\n" +
                        "Content-Disposition: form-data; name=\"fieldname\"\r\n" +
                        "\r\n" +
                        "Submit MULTIPART FORM DATA\r\n" + 
                        "--XXXXXXXX--\r\n").getBytes());
        
        
        final MultipartFormDataReader.OnFieldHandler onFieldHandler = (List<String> headers, InputStream value) -> {
            byte[] bytes = new byte[128];
            int size = value.read(bytes);

            assertEquals("Submit MULTIPART FORM DATA\r\n", new String(bytes,0,size));
            
        };
        
        reader.withOnFieldHandler(onFieldHandler).readFromStream(stream);
    }
    
    @Test
    public void shouldLoadContentFirstOnlyPartialBoundary() throws IOException {
        InputStream stream = new ByteArrayInputStream(
                        ("--XXXXXXXX\r\n" +
                        "Content-Disposition: form-data; name=\"fieldname\"\r\n" +
                        "\r\n" +
                        "--XXXXXXX--\r\n" + 
                        "--XXXXXXXX--\r\n").getBytes());
        
        
        final MultipartFormDataReader.OnFieldHandler onFieldHandler = (List<String> headers, InputStream value) -> {
            byte[] bytes = new byte[128];
            int size = value.read(bytes);

            assertEquals("--XXXXXXX--\r\n", new String(bytes,0,size));
            
        };
        
        reader.withOnFieldHandler(onFieldHandler).readFromStream(stream);
    }
    
    @Test
    public void shouldLoadContentFirstOnlyPartialBoundaryWithLimit() throws IOException {
        InputStream stream = new ByteArrayInputStream(
                        ("--XXXXXXXX\r\n" +
                        "Content-Disposition: form-data; name=\"fieldname\"\r\n" +
                        "\r\n" +
                        "-----X\r\n" + 
                        "--XXXXXXXX--\r\n").getBytes());
        
        
        final MultipartFormDataReader.OnFieldHandler onFieldHandler = (List<String> headers, InputStream value) -> {
            byte[] bytes = new byte[128];
            int size = 0;
            while(0 != value.read(bytes,size, 1)) size++;

            assertEquals("-----X\r\n", new String(bytes,0,size));
            
        };
        
        reader.withOnFieldHandler(onFieldHandler).readFromStream(stream);
    }
    
    
}
