package io.qy.spring.aop.proxy;

public class ObjenesisCglibAopProxy implements AopProxy {
    private ProxyFactory proxyFactory;

    public ObjenesisCglibAopProxy(ProxyFactory proxyFactory) {
        this.proxyFactory = proxyFactory;
    }

    @Override
    public Object getProxy() {
        return null;
    }
}
