package io.qy.spring.aop.annotation;

import io.qy.spring.aop.advisor.Advice;
import io.qy.spring.aop.advisor.MethodInterceptor;
import io.qy.spring.aop.advisor.MethodInvocation;
import io.qy.spring.aop.factory.AspectInstanceFactory;

import java.lang.reflect.Method;

public class BeforeAdvice extends CommonAdvice implements Advice, MethodInterceptor {


    public BeforeAdvice(Method aspectJAdviceMethod, AspectInstanceFactory aspectInstanceFactory) {
        super(aspectJAdviceMethod, aspectInstanceFactory);
    }

    /*
    	public Object invoke(MethodInvocation mi) throws Throwable {
		this.advice.before(mi.getMethod(), mi.getArguments(), mi.getThis());
		return mi.proceed();
	}
     */

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        before();
        return invocation.proceed();
    }

    public void before () throws Throwable {
        invokeAdviceMethod(null,null);
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
