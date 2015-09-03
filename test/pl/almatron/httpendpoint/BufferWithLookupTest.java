/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.almatron.httpendpoint;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;

/**
 *
 * @author macvek
 */
public class BufferWithLookupTest {
    
    private BufferWithLookup bufferWithLookup;
    private byte[] bytes;
    @Before
    public void setup() {
        bufferWithLookup = new BufferWithLookup(new ByteArrayInputStream(new byte[] {1,2,3,4,5}));
        bytes = new byte[4];
    }
    
    @Test
    public void shouldReadFromBuffer() throws IOException {
        bufferWithLookup.read(bytes);
        
        assertArrayEquals(new byte[] {1,2,3,4}, bytes);
    }
    
    @Test
    public void shouldReadFromFrontBuffer() throws IOException {
        bufferWithLookup.setLookupBuffer(new byte[]{7,7,7,7});
        bufferWithLookup.read(bytes);
        assertArrayEquals(new byte[] {7,7,7,7}, bytes);
        bufferWithLookup.read(bytes);
        assertArrayEquals(new byte[] {1,2,3,4}, bytes);
    }
    
}
