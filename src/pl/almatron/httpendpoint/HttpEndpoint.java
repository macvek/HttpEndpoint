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
            Logger.getLogger(HttpEndpoint.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void stop() {
        try {
            shutdownExecutor();
            shutdownSockets();

            serverSocket.close();
        } catch (InterruptedException | IOException ex) {
            Logger.getLogger(HttpEndpoint.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void startWithExceptions() throws Exception {
        serverSocket = new ServerSocket();
        serverSocket.bind(new InetSocketAddress(hostname, port));
        acceptor = new Acceptor();
        acceptorStatus = executorService.submit(acceptor);
    }

    private void shutdownExecutor() throws InterruptedException {
        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.SECONDS);
    }

    private int watchdog = 2;

    private synchronized void shutdownSockets() {
        clientSocketDelegates.stream().forEach(
                (ClientSocketDelegate delegate) -> {
                    delegate.closeConnection();
                }
        );
    }

    private class Acceptor implements Runnable {

        @Override
        public void run() {
            try {
                for (;;) {
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
                    Logger.getGlobal().log(Level.SEVERE, "Trying to close already closed socket", e);
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
