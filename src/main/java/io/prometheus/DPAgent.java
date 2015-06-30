package io.prometheus;

import io.prometheus.enhancer.ResourceClassTransformer;
import io.prometheus.reporter.PrometheusReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.Instrumentation;
import java.util.HashMap;
import java.util.Map;

/**
 * Author santhosh.ct .
 */
public class DPAgent {

    private static final Logger logger = LoggerFactory.getLogger(DPAgent.class);
    private static final PrometheusReporter prometheusReporter = new PrometheusReporter();

    /**
     * @param agentArguments
     * @param instrumentation
     */
    public static void premain(String agentArguments, Instrumentation instrumentation) {
        logger.info("Loading prometheus agent...");

        if (agentArguments != null) {
            Map<String, String> properties = new HashMap<String, String>();
            for (String propertyAndValue : agentArguments.split(",")) {
                String[] tokens = propertyAndValue.split(":", 2);
                if (tokens.length != 2) {
                    continue;
                }
                properties.put(tokens[0], tokens[1]);
            }

            String prometheusListenerPort = properties.get("prometheus.listener.port");
            if (prometheusListenerPort != null){
                startAgent(instrumentation, Integer.valueOf(prometheusListenerPort));
            }else
                throw new RuntimeException("Error. No listener port given for prometheus server in the agent arguments");
        }
    }

    /**
     *  @param instrumentation
     * @param prometheusListenerPort
     */
    private static void startAgent(Instrumentation instrumentation, Integer prometheusListenerPort) {
        //add transformer
        instrumentation.addTransformer(new ResourceClassTransformer());

        //start the prometheus reporter
        startPrometheusReporter(prometheusReporter, prometheusListenerPort);

    }

    /**
     *
     * @param prometheusReporter
     * @param prometheusListenerPort
     */
    private static void startPrometheusReporter(final PrometheusReporter prometheusReporter, Integer prometheusListenerPort) {
        prometheusReporter.start(prometheusListenerPort);
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                prometheusReporter.stop();
            }
        }));
    }
}
