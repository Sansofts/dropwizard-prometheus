package io.prometheus.reporter;

import io.prometheus.client.exporter.MetricsServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.*;

/**
 * Author santhosh.ct .
 */
public class PrometheusReporter {

    private static final Logger logger = getLogger(PrometheusReporter.class);
    private Server server;

    /**
     * Start the prometheus server
     * @param prometheusListenerPort
     */
    public void start(Integer prometheusListenerPort){
        if (server == null) {
            startServer(prometheusListenerPort);
        }
    }

    /**
     *
     * @param prometheusListenerPort
     */
    private void startServer(Integer prometheusListenerPort){
        server = new Server(prometheusListenerPort);
        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        server.setHandler(context);
        context.addServlet(new ServletHolder(new MetricsServlet()), "/metrics");
        try {
            server.setStopAtShutdown(true);
            server.start();
        } catch (Exception e) {
            logger.error("Error starting prometheus reporter. {}", e.getCause());
        }
    }

    /**
     * Stop the prometheus server
     */
    public void stop(){
        if (server!=null && !server.isStopped()) {
            try {
                server.stop();
            } catch (Exception e) {
                logger.error("Could not stop prometheus reporter. {}", e.getCause());
            }
        }
    }
}
