package pl.almatron.httpendpoint;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
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

    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private ServerSocket serverSocket;

    private String hostname;
    private int port;

    private Acceptor acceptor;
    private Future<?> acceptorStatus;
    
    private boolean stopTriggered;

    public void start() {
        try {
            startWithExceptions();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public void waitForAcceptor() {
        try {
            acceptorStatus.get();
        } catch (InterruptedException | ExecutionException ex) {
            if (!stopTriggered) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Unexpected accepting socket termination", ex);
            }
        }
    }

    public void stop() {
        Logger.getLogger(getClass().getName()).log(Level.INFO, "Stop triggered, shutting down server and client sockets");
        
        try {
            stopTriggered = true;
            acceptor.shutdown();
            shutdownExecutor();
            shutdownSockets();
            
        } catch (InterruptedException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        }
        Logger.getLogger(getClass().getName()).log(Level.INFO, "HttpEndpoint is stopped");
    }

    private void startWithExceptions() throws Exception {
        Logger.getLogger(getClass().getName()).log(Level.INFO, "Starting HttpEndpoint");
        serverSocket = new ServerSocket();
        serverSocket.bind(new InetSocketAddress(hostname, port));
        acceptor = new Acceptor();
        acceptorStatus = executorService.submit(acceptor);
        Logger.getLogger(getClass().getName()).log(Level.INFO, "Started HttpEndpoint");
    }

    private void shutdownExecutor() throws InterruptedException {
        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.SECONDS);
    }

    private synchronized void shutdownSockets() {
        clientSocketDelegates.stream().forEach(
                (ClientSocketDelegate delegate) -> {
                    delegate.closeConnection();
                }
        );
    }

    private class Acceptor implements Runnable {
        private boolean online;
        
        @Override
        public void run() {
            try {
                online = true;
                Logger.getLogger(getClass().getName()).log(Level.INFO, "Waiting for connections...");
                while(online) {
                    acceptConnectionsAndDelegate();
                }

            } catch (IOException ex) {
                if (!online) {
                    throw new RuntimeException(ex);
                }
            }
        }

        private void acceptConnectionsAndDelegate() throws IOException {
            final ClientSocketDelegate clientSocketDelegate = new ClientSocketDelegate(serverSocket.accept());
            executorService.execute(clientSocketDelegate);

        }
        
        private void shutdown() {
            if (online) {
                online = false;
                try {
                    serverSocket.close();
                }
                catch(IOException ex) {
                     Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Unexpected exception while closing server socket", ex);
                }
            }
        }
    }

    private final HashSet<ClientSocketDelegate> clientSocketDelegates = new HashSet<>();

    private synchronized void addClientSocketDelegate(ClientSocketDelegate delegate) {
        clientSocketDelegates.add(delegate);
    }

    private synchronized void removeClientSocketDelegate(ClientSocketDelegate delegate) {
        clientSocketDelegates.remove(delegate);
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
            } catch (IOException e) {
                if (!connectionCloseForced) {
                    throw new RuntimeException(e);
                }
            } finally {
                removeClientSocketDelegate(this);
                done = true;
            }

        }

        public void closeConnection() {
            if (!done) {
                try {
                    connectionCloseForced = true;
                    socket.close();
                } catch (IOException e) {
                    Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Trying to close already closed socket", e);
                }
            }
        }

        private void handleConnection() throws IOException {
            HttpRequestBuffer requestBuffer = new HttpRequestBuffer(socket.getInputStream());
            HttpResponseBuffer responseBuffer = new HttpResponseBuffer(socket.getOutputStream());

            RequestHandler handler = new RequestHandler();
            handler.handleRequest(requestBuffer, responseBuffer);

            socket.close();
        }

    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public void setPort(int port) {
        this.port = port;
    }

}
