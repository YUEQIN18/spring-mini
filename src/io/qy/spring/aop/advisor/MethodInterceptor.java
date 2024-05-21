package io.qy.spring.aop.advisor;

/**
 * 环绕通知
 */
public interface MethodInterceptor extends Interceptor {

    Object invoke(MethodInvocation invocation) throws Throwable;
}
