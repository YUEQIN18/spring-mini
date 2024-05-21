package io.qy.spring.aop.advisor;

/**
 * 就是一个 MethodInvocation，
 * Spring 的实现 MethodInvocationProceedingJoinPoint 中就是内置了一个 MethodInvocation
 */
public interface ProceedingJoinPoint {
    /**
     * 不带参数调用proceed()将导致调用者的原始参数在调用时提供给底层方法
     */
    Object proceed() throws Throwable;

    /**
     * 重载方法，它接受参数数组 (Object[] args)。调用时，数组中的值将用作底层方法的参数。
     * 可以修改实参
     */
    Object proceed(Object[] args) throws Throwable;

    /**
     * 获取执行链中目标方法的实参
     */
    Object[] getArgs();

    /**
     * 获取执行链中目标方法的方法名
     */
    String getMethodName();
}
