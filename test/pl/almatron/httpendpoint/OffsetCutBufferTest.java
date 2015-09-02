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
public class OffsetCutBufferTest {
    
    public OffsetCutBufferTest() {
    }

    @Test
    public void shouldReadChunkFromInputStream() throws IOException {
        InputStream inputStream = new ByteArrayInputStream(new byte[] {
           0,0,0,0,1
        });
        
        OffsetCutBuffer offsetCutBuffer = new OffsetCutBuffer(inputStream);
        offsetCutBuffer.resetWithChunkSize(4);
        offsetCutBuffer.read(4);
        assertArrayEquals(new byte[] {0,0,0,0}, offsetCutBuffer.getChunk());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowWhenReadLengthExceedsChunkSize() throws IOException {
        
        InputStream inputStream = new ByteArrayInputStream(new byte[] {});
        OffsetCutBuffer offsetCutBuffer = new OffsetCutBuffer(inputStream);
        offsetCutBuffer.resetWithChunkSize(4);
        offsetCutBuffer.read(5);
        
    }
    
    @Test
    public void shouldMarkAndReturnChunk() throws IOException {
        InputStream inputStream = new ByteArrayInputStream(new byte[] {
           0,0,8,8,9,9,1,1
        });
        
        OffsetCutBuffer offsetCutBuffer = new OffsetCutBuffer(inputStream);
        offsetCutBuffer.resetWithChunkSize(4);
        offsetCutBuffer.read(4);
        offsetCutBuffer.markOffset(2);
        assertArrayEquals(new byte[] {8,8,9,9}, offsetCutBuffer.getMarkedChunk());
    }
    
    @Test
    public void shouldMarkAndDiscardChunk() throws IOException {
        InputStream inputStream = new ByteArrayInputStream(new byte[] {
           0,0,8,8,1,1,1,1
        });
        
        OffsetCutBuffer offsetCutBuffer = new OffsetCutBuffer(inputStream);
        offsetCutBuffer.resetWithChunkSize(4);
        offsetCutBuffer.read(4);
        offsetCutBuffer.markOffset(2);
        offsetCutBuffer.discardOffset();
        offsetCutBuffer.read(4);
        assertArrayEquals(new byte[] {1,1,1,1}, offsetCutBuffer.getChunk());
    }
    
    @Test
    public void shouldMarkCheckAnotherChunk() throws IOException {
        InputStream inputStream = new ByteArrayInputStream(new byte[] {
           0,0,8,8,8,8,1,1,1,1
        });
        
        OffsetCutBuffer offsetCutBuffer = new OffsetCutBuffer(inputStream);
        offsetCutBuffer.resetWithChunkSize(4);
        offsetCutBuffer.read(4);
        offsetCutBuffer.markOffset(2);
        offsetCutBuffer.read(4);
        assertArrayEquals(new byte[] {1,1,1,1}, offsetCutBuffer.getChunk());
    }
    
    @Test
    public void shouldReturnCorrectNumberOfBytesOnEndOfStream() throws IOException {
        InputStream inputStream = new ByteArrayInputStream(new byte[] {
           0,0,8
        });
        
        OffsetCutBuffer offsetCutBuffer = new OffsetCutBuffer(inputStream);
        offsetCutBuffer.resetWithChunkSize(4);
        assertEquals(3,offsetCutBuffer.read(4));
    }
    
    @Test
    public void shouldLimitReadToGivenBytes() throws IOException {
        InputStream inputStream = new ByteArrayInputStream(new byte[] {
           0,0,0,0
        });
        
        OffsetCutBuffer offsetCutBuffer = new OffsetCutBuffer(inputStream);
        offsetCutBuffer.resetWithChunkSize(4);
        assertEquals(3,offsetCutBuffer.read(3));
    }
    
    @Test
    public void shouldReadBufferedDataWithDifferentLimits() throws IOException {
        InputStream inputStream = new ByteArrayInputStream(new byte[] {
           0,0,8,8,4,4,1,1,1,1
        });
        
        OffsetCutBuffer offsetCutBuffer = new OffsetCutBuffer(inputStream);
        offsetCutBuffer.resetWithChunkSize(4);
        offsetCutBuffer.read(4); // 0,0,8,8
        offsetCutBuffer.markOffset(2); // in buffer extra 4,4
        offsetCutBuffer.discardOffset(); // next read rotates buffer
        offsetCutBuffer.read(1);
        
        byte[] twoBytes = new byte[2];
        System.arraycopy(offsetCutBuffer.getChunk(), 0, twoBytes, 0, 2);
        assertArrayEquals(new byte[] {4,4}, twoBytes);
        
        offsetCutBuffer.read(4);
        assertArrayEquals(new byte[] {1,1,1,1}, offsetCutBuffer.getChunk());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void shouldFailWithOffsetAtLeastChunkSize() throws IOException {
        InputStream inputStream = new ByteArrayInputStream(new byte[] {
           0,0,8,8,4,4,1,1,1,1
        });
        OffsetCutBuffer offsetCutBuffer = new OffsetCutBuffer(inputStream);
        offsetCutBuffer.resetWithChunkSize(4);
        offsetCutBuffer.markOffset(4);
    }
    
    @Test
    public void shouldReadBufferedDataFewTimesByOneByte() throws IOException {
        InputStream inputStream = new ByteArrayInputStream(new byte[] {
           0,0,8,8,4,4,1,1,1,1
        });
        
        OffsetCutBuffer offsetCutBuffer = new OffsetCutBuffer(inputStream);
        offsetCutBuffer.resetWithChunkSize(4);
        offsetCutBuffer.read(1); // 
        assertEquals(0, offsetCutBuffer.getChunk()[0]);
        
        offsetCutBuffer.markOffset(0);
        assertArrayEquals(new byte[] {0,0,8,8}, offsetCutBuffer.getMarkedChunk());

    }
    
}
