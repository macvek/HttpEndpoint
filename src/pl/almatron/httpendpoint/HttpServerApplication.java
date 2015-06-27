package pl.almatron.httpendpoint;

import java.lang.management.ManagementFactory;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

/**
 * HttpEndpoint
 *
 * @author macvek
 */
public class HttpServerApplication {

    private HttpEndpoint endpoint;

    public void start() throws Exception {

        registerMBeanController();
        
        endpoint = new HttpEndpoint();
        endpoint.setHostname("localhost");
        endpoint.setPort(10000);
        endpoint.start();
        endpoint.waitForAcceptor();
    }

    private void registerMBeanController() throws NotCompliantMBeanException, MBeanRegistrationException, InstanceAlreadyExistsException, MalformedObjectNameException {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = new ObjectName("pl.almatron:type=HttpServerApplicationController");
        HttpServerApplicationController mbean = new HttpServerApplicationController();
        mbean.setApplication(this);
        mbs.registerMBean(mbean, name);
    }

    public static void main(String[] args) throws Exception {
        new HttpServerApplication().start();
    }

    public void shutdown() {
        endpoint.stop();
    }
}
