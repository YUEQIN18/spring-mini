package io.qy.spring.bean;

public interface SmartInstantiationAwareBeanPostProcessor extends  BeanPostProcessor{

    /**
     * 如果 bean 需要被代理，返回代理对象；不需要被代理直接返回原始对象。
     */
    default Object getEarlyBeanReference(Object bean, String beanName) throws RuntimeException {
        return bean;
    }

}
