package io.qy.spring.aware;

/**
 * 将 beanName 传递给 bean
 * 某个bean 实现了这个接口，就能得到它的 beanName
 * 由 Spring 调用
 *
 */
public interface BeanNameAware {
    void setBeanName(String beanName);
}
