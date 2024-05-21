package io.qy.spring.aop.advisor;

public class DefaultPointcutAdvisor implements Advisor {

    private Pointcut pointcut;
    private Advice advice;

    public DefaultPointcutAdvisor(Pointcut pointcut, Advice advice) {
        this.pointcut = pointcut;
        this.advice = advice;
    }

    @Override
    public Pointcut getPointcut() {
        return pointcut;
    }

    @Override
    public Advice getAdvice() {
        return advice;
    }

    @Override
    public int getOrder() {
        return this.advice.getOrder();
    }

}
