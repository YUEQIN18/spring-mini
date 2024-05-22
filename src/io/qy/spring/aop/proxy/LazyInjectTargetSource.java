package io.qy.spring.aop.proxy;

import io.qy.spring.config.MyApplicationContext;

public class LazyInjectTargetSource implements TargetSource {
    private final MyApplicationContext applicationContext;
    private final String beanName;

    public LazyInjectTargetSource(MyApplicationContext applicationContext, String beanName) {
        this.applicationContext = applicationContext;
        this.beanName = beanName;
    }

    @Override
    public Object getTarget() throws Exception {
        return applicationContext.getBean(beanName);
    }
}
