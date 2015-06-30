package io.prometheus.enhancer;

import io.prometheus.BaseClassTransformer;

/**
 * Author santhosh.ct .
 */
public class ResourceClassTransformer extends BaseClassTransformer {

    /**
     * Default constructor
     */
    public ResourceClassTransformer() {
        super(new ResourceClassEnhancer());
    }
}
