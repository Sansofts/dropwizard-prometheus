package io.prometheus.enhancer;

import com.codahale.metrics.annotation.Counted;
import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Gauge;
import com.codahale.metrics.annotation.Timed;
import javassist.*;

import java.io.IOException;

/**
 * Author santhosh.ct .
 */
public class ResourceClassEnhancer implements Enhancer {

    private int summaryCounter;
    private int histogramCounter;
    private int exceptionCounter;
    private int gaugeCounter;
    private int counter;

    @Override
    public boolean enhance(CtClass ctClass) throws CannotCompileException, NotFoundException, IOException {
        boolean isClassModified = false;
        for (CtMethod ctMethod: ctClass.getDeclaredMethods()){
            if (ctMethod.hasAnnotation(Timed.class)){
                //add summary
                addSummary(ctClass, ctMethod);
                //add histogram
                addHistogram(ctClass, ctMethod);
                isClassModified = true;
            }
            if (ctMethod.hasAnnotation(ExceptionMetered.class)){
                //add uncaught exception counter
                addExceptionCounter(ctClass, ctMethod);
                isClassModified = true;
            }
            if (ctMethod.hasAnnotation(Gauge.class)){
                addGauge(ctClass, ctMethod);
                isClassModified = true;
            }
            if (ctMethod.hasAnnotation(Counted.class)){
                addCounter(ctClass, ctMethod);
                isClassModified = true;
            }
        }
        return isClassModified;
    }

    /**
     * Add counter
     * @param ctClass
     * @param ctMethod
     * @throws CannotCompileException
     */
    private void addCounter(CtClass ctClass, CtMethod ctMethod) throws CannotCompileException {
        final String newCounter = "__counter"+counter++;
        ctClass.addField(createCounter(ctClass, ctMethod, newCounter, false));
        ctMethod.insertBefore(newCounter+".inc();");
    }

    /**
     * Add gauge
     * @param ctClass
     * @param ctMethod
     * @throws CannotCompileException
     */
    private void addGauge(CtClass ctClass, CtMethod ctMethod) throws CannotCompileException {
        final String newGaugeCounter = "__gaugeCounter"+gaugeCounter++;
        ctClass.addField(createGaugeCounter(ctClass, newGaugeCounter));
        ctMethod.insertBefore(newGaugeCounter + ".labels(\"" + ctMethod.getName() + "\"+)" + ".inc()");
        ctMethod.insertBefore(newGaugeCounter+".labels(\""+ctMethod.getName()+"\"+)"+".dec()");
    }

    /**
     * Add uncaught exception counter
     * @param ctClass
     * @param ctMethod
     * @throws CannotCompileException
     * @throws NotFoundException
     */
    private void addExceptionCounter(CtClass ctClass, CtMethod ctMethod) throws CannotCompileException, NotFoundException {
        final String newExceptionCounter = "__exceptionCounter"+exceptionCounter++;
        ctClass.addField(createCounter(ctClass, ctMethod, newExceptionCounter, true));
        ctMethod.addCatch("{" +
                newExceptionCounter + ".inc();" +
                "throw $e;" +
                "}", ClassPool.getDefault().get("java.lang.Exception"));
    }

    /**
     * Add summary timer to the method
     * @param ctClass
     * @param ctMethod
     * @throws CannotCompileException
     * @throws NotFoundException
     */
    private void addSummary(CtClass ctClass, CtMethod ctMethod) throws CannotCompileException, NotFoundException {
        final String newSummaryField = "__summary"+summaryCounter++;
        ctClass.addField(createSummary(ctClass, ctMethod, newSummaryField));
        ctMethod.addLocalVariable("__requestSummaryTimer", ClassPool.getDefault().get("io.prometheus.client.Summary$Timer"));
        ctMethod.insertBefore("__requestSummaryTimer = " + newSummaryField + ".startTimer();");
        ctMethod.insertAfter("__requestSummaryTimer.observeDuration();");
    }

    /**
     * Add histogram timer to the method
     * @param ctClass
     * @param ctMethod
     * @throws CannotCompileException
     * @throws NotFoundException
     */
    private void addHistogram(CtClass ctClass, CtMethod ctMethod) throws CannotCompileException, NotFoundException {
        final String newHistogramNameField = "__histogram"+histogramCounter++;
        ctClass.addField(createHistogram(ctClass, ctMethod, newHistogramNameField));
        ctMethod.addLocalVariable("__requestHistogramTimer", ClassPool.getDefault().get("io.prometheus.client.Histogram$Timer"));
        ctMethod.insertBefore("__requestHistogramTimer = "+newHistogramNameField+".startTimer();");
        ctMethod.insertAfter("__requestHistogramTimer.observeDuration();");
    }

    /**
     *
     * @param ctClass
     * @param ctMethod
     * @param fieldName
     * @return
     * @throws CannotCompileException
     */
    private CtField createCounter(CtClass ctClass, CtMethod ctMethod, String fieldName, boolean exception) throws CannotCompileException {
        if (!exception)
            return CtField.make("private static final io.prometheus.client.Counter "  + fieldName + "=" + "io.prometheus.core.MetricsHelper.getCounter(\"" + ctClass.getName() + "." + ctMethod.getName() + ".counter" + "\");", ctClass);
        else return CtField.make("private static final io.prometheus.client.Counter "  + fieldName + "=" + "io.prometheus.core.MetricsHelper.getCounter(\"" + ctClass.getName() + "." + ctMethod.getName() + ".counter.exception" + "\");", ctClass);
    }

    /**
     *
     * @param ctClass
     * @param ctMethod
     * @param fieldName
     * @return
     * @throws CannotCompileException
     */
    private CtField createHistogram(CtClass ctClass, CtMethod ctMethod, String fieldName) throws CannotCompileException {
        return CtField.make("private static final io.prometheus.client.Histogram "  + fieldName + "=" + "io.prometheus.core.MetricsHelper.getHistogram(\"" + ctClass.getName() + "." + ctMethod.getName() + ".histogram.timer" + "\");", ctClass);
    }

    /**
     *
     * @param ctClass
     * @param ctMethod
     * @param fieldName
     * @return
     * @throws NotFoundException
     * @throws CannotCompileException
     */
    private CtField createSummary(CtClass ctClass, CtMethod ctMethod, String fieldName) throws NotFoundException, CannotCompileException {
        return CtField.make("private static final io.prometheus.client.Summary "  + fieldName + "=" + "io.prometheus.core.MetricsHelper.getSummary(\"" + ctClass.getName() + "." + ctMethod.getName() + ".summary.timer" + "\");", ctClass);
    }

    /**
     *
     * @param ctClass
     * @param fieldName
     * @return
     * @throws CannotCompileException
     */
    private CtField createGaugeCounter(CtClass ctClass, String fieldName) throws CannotCompileException {
        return CtField.make("private static final io.prometheus.client.Gauge "  + fieldName + "=" + "io.prometheus.core.MetricsHelper.getGauge(\"" + ctClass.getName() + ".gauge" + "\");", ctClass);
    }
}
