package io.qy.spring.aop.factory;

public interface AspectInstanceFactory {
    /**
     * Create an instance of this factory's aspect.
     * @return the aspect instance (never {@code null})
     */
    Object getAspectInstance();
}
