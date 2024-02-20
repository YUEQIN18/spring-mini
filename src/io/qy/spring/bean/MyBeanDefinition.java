package io.qy.spring.bean;

/**
 * @author qinyue
 * @create 2024-02-20 17:54:00
 * Bean 的基本信息
 */
public class MyBeanDefinition {

    private Class<?> type;
    private String scope;

    public boolean isSingleton() {
        return "singleton".equals(scope);
    }

    public Class<?> getType() {
        return type;
    }

    public void setType(Class<?> type) {
        this.type = type;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    @Override
    public String toString() {
        return "BeanDefinition{" +
                "type=" + type +
                ", scope='" + scope + '\'' +
                '}';
    }
}
