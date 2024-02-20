package io.qy.spring.bean;

/**
 * @author qinyue
 * @create 2024-02-20 18:12:00
 */
public interface DisposableBean {
    void destroy() throws Exception;
}
