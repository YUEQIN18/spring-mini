package io.qy.spring.aop.factory;

import java.lang.reflect.InvocationTargetException;

public class PrototypeAspectInstanceFactory implements AspectInstanceFactory {
    private final Class<?> clazz;

    public PrototypeAspectInstanceFactory(Class<?> clazz) {
        this.clazz = clazz;
    }

    @Override
    public Object getAspectInstance() {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}
