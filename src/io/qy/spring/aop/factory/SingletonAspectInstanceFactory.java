package io.qy.spring.aop.factory;

public class SingletonAspectInstanceFactory implements AspectInstanceFactory {
    private final Object aspectInstance;

    public SingletonAspectInstanceFactory(Object aspectInstance) {
        this.aspectInstance = aspectInstance;
    }

    @Override
    public Object getAspectInstance() {
        return this.aspectInstance;
    }
}
