package io.qy.spring.bean;

/**
 * @author qinyue
 * @create 2024-02-20 18:13:00
 */
public class DisposableBeanAdapter implements DisposableBean{

    private final Object bean;
    private final String beanName;
    private final BeanDefinition beanDefinition;

    public DisposableBeanAdapter(Object bean, String beanName, BeanDefinition beanDefinition) {
        this.bean = bean;
        this.beanName = beanName;
        this.beanDefinition = beanDefinition;
    }

    public static boolean hasDestroyMethod(Object bean, BeanDefinition beanDefinition) {
        if (bean instanceof DisposableBean || bean instanceof AutoCloseable) {
            return true;
        }
        return false;
    }

    @Override
    public void destroy() {
        try {
            if (bean instanceof DisposableBean) {
                ((DisposableBean) bean).destroy();
            } else if (bean instanceof AutoCloseable) {
                ((AutoCloseable) bean).close();
            }
        } catch (Exception e) {
            System.out.println("Invocation of destroy method failed on bean with name '" + this.beanName + "'");
        }
    }
}
