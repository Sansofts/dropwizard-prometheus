package io.prometheus;

import io.prometheus.enhancer.Enhancer;
import javassist.ByteArrayClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.List;

/**
 * Author santhosh.ct .
 */
public class BaseClassTransformer implements ClassFileTransformer {

    protected static final Logger logger = LoggerFactory.getLogger(BaseClassTransformer.class);
    public static final List<String> ignoredPackages = Arrays.asList("javax/", "java/", "sun/", "com/sun/");
    protected Enhancer enhancer;
    protected ClassPool classPool;

    /**
     * Default constructor
     */
    public BaseClassTransformer() {
        this.classPool = new ClassPool();
        this.classPool.appendSystemPath();
        try {
            this.classPool.appendPathList(System.getProperty("java.class.path"));
        } catch (Exception e) {
            logger.error("Error: {}", e);
            throw new RuntimeException("Error: " + e);
        }
    }

    /**
     * @param enhancer
     */
    public BaseClassTransformer(Enhancer enhancer) {
        this();
        this.enhancer = enhancer;
    }

    /**
     * @return classPool
     */
    public ClassPool getClassPool() {
        return classPool;
    }

    /**
     * @param classPool
     */
    public void setClassPool(ClassPool classPool) {
        this.classPool = classPool;
    }

    /**
     * @param className
     * @param ctClass
     * @return
     */
    protected boolean shouldSkip(String className, CtClass ctClass) {
        if (ctClass.isFrozen()) {
            logger.debug("Skip class {}: is frozen", className);
            return true;
        }

        if (ctClass.isPrimitive() || ctClass.isArray() || ctClass.isAnnotation() || ctClass.isEnum() || ctClass.isInterface()) {
            logger.debug("Skip class {}: not a class", className);
            return true;
        }
        return false;
    }

    private boolean skipClass(String className) {
        for (String name : ignoredPackages) {
            if (className.startsWith(name)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public byte[] transform(ClassLoader loader, String fullyQualifiedClassName, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classBytes) throws IllegalClassFormatException {
        if (skipClass(fullyQualifiedClassName)){
            return null;
        }

        String className = fullyQualifiedClassName.replace("/", ".");
        classPool.appendClassPath(new ByteArrayClassPath(className, classBytes));

        try {
            CtClass ctClass = classPool.get(className);
            if (shouldSkip(className, ctClass)){
                return null;
            }
            if (enhancer.enhance(ctClass)) {
                return ctClass.toBytecode();
            }
            return null;
        } catch (Exception e) {
            logger.debug("Unable to transform class {}: Error: {}", className, e);
            return null;
        }
    }
}
