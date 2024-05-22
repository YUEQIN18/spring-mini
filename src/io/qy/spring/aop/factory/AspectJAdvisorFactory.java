package io.qy.spring.aop.factory;

import io.qy.spring.aop.advisor.Advisor;

import java.util.List;

public interface AspectJAdvisorFactory {

    /**
     * 是否是切面类 @Aspect
     */
    boolean isAspect(Class<?> clazz);

    /**
     * 解析 @Aspect 切面类中的所有切面
     */
    List<Advisor> getAdvisors(Class<?> clazz);
}
