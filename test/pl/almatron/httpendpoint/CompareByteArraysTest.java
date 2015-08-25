/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.almatron.httpendpoint;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author macvek
 */
public class CompareByteArraysTest {
    
    public CompareByteArraysTest() {
    }

    @Test
    public void shouldReturnOffset3ForNoMatch() {
        assertEquals(3, new CompareByteArrays(new byte[] {1,2,3}).findOffset(new byte[] {0,0,0}));
    }
    
    @Test
    public void shouldReturnOffset0ForCompleteMatch() {
        assertEquals(0, new CompareByteArrays(new byte[] {1,2,3}).findOffset(new byte[] {1,2,3}));
    }
    
    @Test
    public void shouldReturnOffset1ForGivenPartialMatch() {
        assertEquals(1, new CompareByteArrays(new byte[] {1,2,3}).findOffset(new byte[] {1,1,2}));
    }
    
    @Test
    public void shouldNotFindMatchAndReturn3() {
        assertEquals(3, new CompareByteArrays(new byte[] {1,2,3}).findOffset(new byte[] {1,2,4}));
    }
    
}
