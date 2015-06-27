package pl.almatron.httpendpoint;

/**
 * HttpEndpoint
 * @author macvek
 */
public class HttpServerApplicationController implements HttpServerApplicationControllerMBean {

    private HttpServerApplication application;
    
    @Override
    public void shutdown() {
        application.shutdown();
    }

    public void setApplication(HttpServerApplication application) {
        this.application = application;
    }
    
    

}
