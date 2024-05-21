package io.qy.spring.aop.advisor;

/**
 * Core Spring pointcut abstraction.
 * 切点使用一个 MethodMatcher 对象来判断某个方法是否有资格用于切面
 */
public interface Pointcut {

    /**
     * Return the MethodMatcher for this pointcut.
     * @return the MethodMatcher (never {@code null})
     */
    MethodMatcher getMethodMatcher();

}
