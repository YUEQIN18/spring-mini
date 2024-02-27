package io.qy.spring.bean;

/**
 * @author qinyue
 * @create 2024-02-27 09:57:00
 */
public interface BeanPostProcessor {

    default Object postProcessBeforeInitialization(Object bean, String beanName) {
        return bean;
    }

    default Object postProcessAfterInitialization(Object bean, String beanName) {
        return bean;
    }

}
