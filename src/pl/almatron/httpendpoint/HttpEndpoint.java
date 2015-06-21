/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.almatron.httpendpoint;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author macvek
 */
public class HttpEndpoint {
    
    private ExecutorService executorService = Executors.newCachedThreadPool();
    private ServerSocket serverSocket;
    
    private String hostname;
    private int port;
    
    private Acceptor acceptor;
    private Future<?> acceptorStatus;
    
    public void start() {
        try {
            startWithExceptions();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    
    private void startWithExceptions() throws Exception {
        serverSocket = new ServerSocket();
        serverSocket.bind(new InetSocketAddress(hostname, port));
        acceptor = new Acceptor();
        acceptorStatus = executorService.submit(acceptor);
    }

    private void stop() {
        try {
            executorService.shutdown();
            executorService.awaitTermination(5, TimeUnit.SECONDS);
            serverSocket.close();
        } catch (InterruptedException | IOException ex) {
            Logger.getLogger(HttpEndpoint.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void waitForAcceptor() {
        try {
            acceptorStatus.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(HttpEndpoint.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    
    
    private class Acceptor implements Runnable{

        @Override
        public void run() {
            try {

                acceptConnectionsAndDelegate();
                
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        
        private void acceptConnectionsAndDelegate() throws IOException {
            executorService.execute(
                    new ClientSocketDelegate(serverSocket.accept()
            ));
            
        }
    }
    
    private static class ClientSocketDelegate extends RunnableWithThrow{
        private Socket socket;

        public ClientSocketDelegate(Socket socket) {
            this.socket = socket;
        }
                
        @Override
        public void runWithThrow() throws IOException {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String line = reader.readLine();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            writer.write(line);
            writer.flush();
            socket.close();
        }
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public void setPort(int port) {
        this.port = port;
    }
    
    public static void main(String[] args) {
        HttpEndpoint endpoint = new HttpEndpoint();
        endpoint.setHostname("localhost");
        endpoint.setPort(10000);
        endpoint.start();
        endpoint.waitForAcceptor();
        endpoint.stop();
    }

        
}
