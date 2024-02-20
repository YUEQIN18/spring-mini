package io.qy.spring.bean;

/**
 * @author qinyue
 * @create 2024-02-20 18:22:00
 */
@FunctionalInterface
public interface ObjectFactory<T> {
    T getObject() throws RuntimeException;
}
