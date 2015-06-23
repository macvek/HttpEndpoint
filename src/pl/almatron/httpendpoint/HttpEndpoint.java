/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.almatron.httpendpoint;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.HashSet;
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
            shutdownExecutor();
            shutdownSockets();
            
            serverSocket.close();
        } catch (InterruptedException | IOException ex) {
            Logger.getLogger(HttpEndpoint.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void shutdownExecutor() throws InterruptedException {
        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.SECONDS);
    }

    private void waitForAcceptor() {
        try {
            acceptorStatus.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(HttpEndpoint.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    
    
    private int watchdog = 1;

    private synchronized void shutdownSockets() {
        clientSocketDelegates.stream().forEach(
                (ClientSocketDelegate delegate) -> {
                    delegate.closeConnection();
                }
        );
    }
    
    private class Acceptor implements Runnable{

        @Override
        public void run() {
            try {
                for(;;) {
                    acceptConnectionsAndDelegate();
                    if (--watchdog == 0) {
                        break;
                    }
                }
                
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        
        private void acceptConnectionsAndDelegate() throws IOException {
            final ClientSocketDelegate clientSocketDelegate = new ClientSocketDelegate(serverSocket.accept());
            executorService.execute(clientSocketDelegate);
            
        }
    }
    
    private final HashSet<ClientSocketDelegate> clientSocketDelegates = new HashSet<>();
    
    private synchronized void addClientSocketDelegate(ClientSocketDelegate delegate) {
        clientSocketDelegates.add(delegate);
    }
    
    private synchronized void removeClientSocketDelegate(ClientSocketDelegate delegate) {
        clientSocketDelegates.remove(delegate);
    }
    
    static private class HttpRequestBuffer {
        private final InputStream inputStream;
        private byte[] buffer;
        private int endOfFirstLine;
        private int headersTermination;
        private static int rotateSize = 512;
        private static int rotateTimesLimit = 3;
        private int rotateTimesCounter = rotateTimesLimit;
        private boolean endOfStream;
        private boolean allowedEndOfStream;
        
        private int readPointer;
        
        private String method;
        private String query;
        private String protocol;
        
        private int[] headerIndexes;
        private static int maxHeaders = 32;
        private int headerPointer = 0;
        
        private static final Charset usAscii = Charset.forName("US-ASCII");
        
        public HttpRequestBuffer(InputStream inputStream) {
            this.inputStream = inputStream;
            buffer = new byte[rotateSize];
            headerIndexes = new int[maxHeaders+1];
        }
        
        private void readFirstLine() {
            do {
                readChunk();
                markEndOfFirstLine();
            } while(endOfFirstLine == 0);
            
            parseFirstLine();
            
            
        }

        private void parseFirstLine() throws RuntimeException {
            int methodPos = 0;
            int queryPos = 0;
            
            for (int i=0;i<endOfFirstLine-2;i++) {
                if (buffer[i] == ' ') {
                    if (methodPos == 0) {
                        methodPos = i;
                    }
                    else {
                        queryPos = i;
                    }
                }
            }
            
            if (methodPos > 0 &&
                    methodPos < queryPos &&
                    queryPos < endOfFirstLine - 2) {
                method = new String(buffer, 0, methodPos,usAscii);
                query = new String(buffer, methodPos+1, queryPos - methodPos, usAscii);
                protocol = new String(buffer, queryPos+1, endOfFirstLine- queryPos - 2, usAscii);
            }
            else {
                throw new RuntimeException("Malformed query");
            }
        }

        private void markEndOfFirstLine() {
            assert endOfFirstLine == 0;
            
            int pointer = readToNextLine(0);
            if (pointer != -1) {
                endOfFirstLine = pointer;
            }
        }

        private int readToNextLine(int from) {
            if (readPointer-from > 1) {
                for (int i=from;i<readPointer-1;i++) {
                    if (buffer[i] == '\r' && buffer[i+1] == '\n') {
                        return i+2;
                    }
                }
            }
            
            return -1;
        }
        
        private void saveHeaders() {
            headerIndexes[headerPointer] = endOfFirstLine;
            headerPointer = 1;
            for(;;) {
                int lastIndex = headerIndexes[headerPointer-1];
                int nextLinePointer = readToNextLine(lastIndex);
                if (nextLinePointer == -1) {
                    readChunk();
                }
                else {
                    if (isHeaderListFull()) {
                        throw new RuntimeException("Headers limit exceeded");
                    }
                    
                    if (isEmptyHeaderLine(nextLinePointer)) {
                        headersTermination = nextLinePointer;
                        allowedEndOfStream = true;
                        return;
                    }
                    
                    addHeaderPointer(nextLinePointer);
                }
            }
        }

        private void addHeaderPointer(int nextLinePointer) {
            headerIndexes[headerPointer] = nextLinePointer;
            headerPointer++;
        }

        private boolean isHeaderListFull() {
            return headerPointer == headerIndexes.length;
        }

        private boolean isEmptyHeaderLine(int nextLinePointer) {
            return nextLinePointer - headerIndexes[headerPointer-1] == 2;
        }
        
        private String[] readHeaders() {
            String[] headers = new String[headerPointer-1];
            for (int i=1;i<headerPointer;i++) {
                headers[i-1] = new String(buffer, 
                                        headerIndexes[i-1], 
                                        headerIndexes[i]-headerIndexes[i-1] -2, 
                                        usAscii);
                
            }
            
            return headers;
        }        
        
        
        private void readChunk() {
            try {
                int bytesRead = inputStream.read(buffer,readPointer, buffer.length - readPointer);
                if (bytesRead == -1) {
                    endOfStream = true;
                }
                else {
                    readPointer += bytesRead;
                }
            }catch(IOException e) {
                throw new RuntimeException(e);
            }
            
            if (endOfStream && !allowedEndOfStream) {
                throw new RuntimeException("Unexpected end of stream");
            }
            
            if (readPointer == buffer.length) {
                rotateBuffer();
            }
        }
        
        private void rotateBuffer() {
            if (rotateTimesCounter == 0) {
                throw new RuntimeException("RotateBuffer times exceeded");
            }
            
            rotateTimesCounter-=1;
            
            byte[] newBuffer = new byte[buffer.length + buffer.length];
            System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
            buffer = newBuffer;
        }

        public String getMethod() {
            return method;
        }

        public String getQuery() {
            return query;
        }

        public String getProtocol() {
            return protocol;
        }

    }
    
    private class ClientSocketDelegate implements Runnable {
        private final Socket socket;
        private boolean done;
        private boolean connectionCloseForced;
        
        
        public ClientSocketDelegate(Socket socket) {
            this.socket = socket;
        }
        
        @Override
        public void run() {
            try {
                addClientSocketDelegate(this);
                handleConnection();
            }
            catch (IOException e) {
                if (!connectionCloseForced) {
                    throw new RuntimeException(e);
                }
            }
            finally {
                removeClientSocketDelegate(this);
                done = true;
            }
            
        }
        
        public void closeConnection() {
            if (!done) {
                try {
                    connectionCloseForced = true;
                    socket.close();
                }
                catch(IOException e) {
                    System.out.println("Trying to close already closed socket");
                    e.printStackTrace();
                }
            }
        }

        private void handleConnection() throws IOException {
            HttpRequestBuffer buffer = new HttpRequestBuffer(socket.getInputStream());
            buffer.readFirstLine();
            System.out.println("Method:"+buffer.getMethod());
            System.out.println("Query:"+buffer.getQuery());
            System.out.println("Protocol:"+buffer.getProtocol());
            
            buffer.saveHeaders();
            
            for (String header : buffer.readHeaders()) {
                System.out.println(header);
            }
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
