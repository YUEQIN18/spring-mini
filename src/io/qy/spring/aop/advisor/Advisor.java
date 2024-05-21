package io.qy.spring.aop.advisor;

import io.qy.spring.core.Ordered;

public interface Advisor extends Ordered {
    /**
     * 此方法应该再封装一个接口：PointcutAdvisor，放在这个接口里，这里直接放在 Advisor 接口 里了
     * @return
     */
    Pointcut getPointcut();

    Advice getAdvice();
}
