package io.qy.spring.aware;

import io.qy.spring.config.MyApplicationContext;

public interface ApplicationContextAware {
    void setApplicationContext(MyApplicationContext applicationContext);
}
