package io.qy.spring.aop.advisor;

import io.qy.spring.core.Ordered;

/**
 * 通知
 * Spring 中此接口并没有实现 Ordered，而是使用别的方法进行排序
 */
public interface Advice extends Ordered {

}
