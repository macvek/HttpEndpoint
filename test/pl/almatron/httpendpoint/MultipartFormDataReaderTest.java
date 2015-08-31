/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.almatron.httpendpoint;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
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
        reader = new MultipartFormDataReader("X");
    }

    private boolean flag;
    
    @Test
    public void shouldLoadTextField() {
        InputStream stream = new ByteArrayInputStream(
                        ("--X\n" +
                        "Content-Disposition: form-data; name=\"fieldname\"\n" +
                        "\n" +
                        "Submit MULTIPART FORM DATA").getBytes());
        
        
        final MultipartFormDataReader.OnFieldHandler onFieldHandler = (String fieldName, InputStream value) -> {
            if ("fieldname".equals(fieldName)) {
                flag = true;
            }
            
        };
        
        reader.withOnFieldHandler(onFieldHandler).readFromStream(stream);
        
        assertTrue(flag);
    }
    
}
