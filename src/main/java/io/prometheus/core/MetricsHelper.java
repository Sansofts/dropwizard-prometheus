package io.prometheus.core;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;
import io.prometheus.client.Summary;

/**
 * Author santhosh.ct .
 */
public class MetricsHelper {

    /**
     * called by instrumented method
     * @param summaryName
     * @return
     */
    public static Summary getSummary(String summaryName){
        summaryName = replaceString(summaryName);
        return Summary.build().name(summaryName).help(summaryName).register();
    }

    /**
     * called by instrumented method
     * @param histogramName
     * @return
     */
    public static Histogram getHistogram(String histogramName){
        histogramName = replaceString(histogramName);
        return Histogram.build().name(histogramName).help(histogramName).register();
    }

    /**
     * called by instrumented method
     * @param counterName
     * @return
     */
    public static Counter getCounter(String counterName){
        counterName = replaceString(counterName);
        return Counter.build().name(counterName).help(counterName).register();
    }

    /**
     * called by instrumented method
     * @param gaugeName
     * @return
     */
    public static Gauge getGauge(String gaugeName){
        gaugeName = replaceString(gaugeName);
        return Gauge.build().name(gaugeName).help(gaugeName).labelNames("methods").register();
    }

    /**
     * Replace the dots with '-' for prometheus naming convention
     * @param metricName
     * @return
     */
    private static String replaceString(String metricName) {
        return metricName.replace(".", "_");
    }
}
