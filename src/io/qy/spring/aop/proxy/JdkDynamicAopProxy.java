package io.qy.spring.aop.proxy;

import io.qy.spring.aop.advisor.DefaultMethodInvocation;
import io.qy.spring.aop.advisor.Interceptor;
import io.qy.spring.utils.AopUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

public class JdkDynamicAopProxy implements AopProxy, InvocationHandler {

    private ProxyFactory proxyFactory;
    private final Class<?>[] proxiedInterfaces;

    public JdkDynamicAopProxy(ProxyFactory proxyFactory) {
        this.proxyFactory = proxyFactory;
        this.proxiedInterfaces = completeProxiedInterfaces(this.proxyFactory);
    }

    @Override
    public Object getProxy() {
        return Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                this.proxiedInterfaces, this);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object oldProxy = null;
        boolean setProxyContext = false;

        TargetSource targetSource = this.proxyFactory.getTargetSource();
        Object target = null;

        try {
            Object retVal;

            // 暴露当前正在运行的代理对象给 AopContext
            if (this.proxyFactory.exposeProxy()) {
                oldProxy = AopContext.setCurrentProxy(proxy);
                setProxyContext = true;
            }

            target = targetSource.getTarget();
            Class<?> targetClass = target.getClass();

            // 得到此 method 的拦截器链，就是一堆环绕通知
            // 需要根据 invoke 的 method 来做进一步确定，过滤出应用在这个 method 上的 Advice
            List<Interceptor> chain = this.proxyFactory.getInterceptorsAndDynamicInterceptionAdvice(method, targetClass);

            if (chain.isEmpty()) {
                retVal = AopUtils.invokeJoinpointUsingReflection(target, method, args);
            } else {
                DefaultMethodInvocation methodInvocation = new DefaultMethodInvocation(target, method, args, chain);
                retVal = methodInvocation.proceed();
            }

            // 处理特殊的返回值 this
            Class<?> returnType = method.getReturnType();
            if (retVal != null && retVal == target &&
                    returnType != Object.class && returnType.isInstance(proxy)) {
                retVal = proxy;
            }
            return retVal;
        } finally {
            if (setProxyContext) {
                // Restore old proxy.
                AopContext.setCurrentProxy(oldProxy);
            }
        }
    }

    /**
     * TODO 补充代理对象的接口，如 SpringProxy、Advised、DecoratingProxy
     *
     * @param proxyFactory
     * @return
     */
    private Class<?>[] completeProxiedInterfaces(ProxyFactory proxyFactory) {
        Class<?>[] proxiedInterfaces = proxyFactory.getProxiedInterfaces();
        return proxiedInterfaces;
    }

}
