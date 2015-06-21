/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.almatron.httpendpoint;

/**
 *
 * @author macvek
 */
public abstract class RunnableWithThrow implements Runnable {  

    @Override
    public void run() {
        try {
            runWithThrow();
        }catch(Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    abstract public void runWithThrow() throws Exception;
    
    
}
