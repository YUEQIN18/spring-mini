package io.qy.spring.aop.annotation;

import io.qy.spring.aop.advisor.Advice;
import io.qy.spring.aop.advisor.MethodInterceptor;
import io.qy.spring.aop.advisor.MethodInvocation;
import io.qy.spring.aop.factory.AspectInstanceFactory;

import java.lang.reflect.Method;

public class AfterAdvice extends CommonAdvice implements Advice, MethodInterceptor {

    public AfterAdvice(Method aspectJAdviceMethod, AspectInstanceFactory aspectInstanceFactory) {
        super(aspectJAdviceMethod, aspectInstanceFactory);
    }

    /*
    	@Override
	@Nullable
	public Object invoke(MethodInvocation mi) throws Throwable {
		try {
			return mi.proceed();
		}
		finally {
			invokeAdviceMethod(getJoinPointMatch(), null, null);
		}
	}
     */

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        try {
            return invocation.proceed();
        } finally {
            after();
        }
    }

    public void after() throws Throwable {
        invokeAdviceMethod(null,null);
    }

    @Override
    public int getOrder() {
        return 1;
    }
}
