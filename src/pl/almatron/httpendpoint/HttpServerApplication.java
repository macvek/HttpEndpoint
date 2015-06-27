package pl.almatron.httpendpoint;

/**
 * HttpEndpoint
 * @author macvek
 */
public class HttpServerApplication {
    public static void main(String[] args) {
        HttpEndpoint endpoint = new HttpEndpoint();
        endpoint.setHostname("localhost");
        endpoint.setPort(10000);
        endpoint.start();
        endpoint.waitForAcceptor();
        endpoint.stop();
    }
}
