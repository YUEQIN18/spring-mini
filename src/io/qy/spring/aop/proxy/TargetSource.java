package io.qy.spring.aop.proxy;

public interface TargetSource {
    Object getTarget() throws Exception;
}
