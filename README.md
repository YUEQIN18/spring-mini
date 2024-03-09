# spring-mini
手写一个mini版的Spring框架

已完成功能：
- 完成了Bean生命周期管理、组件扫描@ComponentScan，依赖注入使用@Autowired根据类名查找依赖
- 进行了循环依赖检测，解决因构造方法注入、属性注入和set方法注入的代理对象的循环依赖问题

# Spring加载流程



## Bean的注册



Spring通过**BeanDefinationReader**将配置元信息加载到内存生成相应的**BeanDefination**之后，就将其注册到**BeanDefinationRegistry**中

- **BeanDefinition**：存储了 bean 对象的所有特征信息，如是否单例、是否懒加载、factoryBeanName 等，和 bean 的关系就是类与对象的关系，一个不同的 bean 对应一个 BeanDefinition

- **BeanDefinationRegistry**：存放 BeanDefination 的容器，是一种键值对的形式，通过特定的 Bean 定义的 id，映射到相应的 BeanDefination

- **BeanDefinitionReader**：读取配置文件，不同的BeanDefinationReader拥有不同的功能，如果我们要读取xml配置元信息，那么可以使用XmlBeanDefinationReader。如果我们要读取properties配置文件，那么可以使用PropertiesBeanDefinitionReader加载。而如果我们要读取注解配置元信息，那么可以使用 AnnotatedBeanDefinitionReader加载



## Bean的加载

### 三级缓存

```java
public class DefaultSingletonBeanRegistry extends SimpleAliasRegistry implements SingletonBeanRegistry {
	...
	private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256); //一级缓存
	private final Map<String, Object> earlySingletonObjects = new HashMap<>(16); // 二级缓存
	private final Map<String, ObjectFactory<?>> singletonFactories = new HashMap<>(16); // 三级缓存
	...
	
	/** Names of beans that are currently in creation. */
	// 这个缓存也十分重要：它表示bean创建过程中都会在里面呆着~
	// 它在Bean开始创建时放值，创建完成时会将其移出
	private final Set<String> singletonsCurrentlyInCreation = Collections.newSetFromMap(new ConcurrentHashMap<>(16));

	/** Names of beans that have already been created at least once. */
	// 当这个Bean被创建完成后，会标记为这个 注意：这里是set集合 不会重复
	// 至少被创建了一次的  都会放进这里
	private final Set<String> alreadyCreated = Collections.newSetFromMap(new ConcurrentHashMap<>(256));
}
```



1. `singletonObjects`：用于存放完全初始化好的 bean，**从该缓存中取出的 bean 可以直接使用**
2. `earlySingletonObjects`：提前曝光的单例对象的cache，存放原始的 bean 对象（尚未填充属性），用于解决循环依赖
3. `singletonFactories`：单例对象工厂的cache，存放 bean 工厂对象，用于解决循环依赖



### 加载流程

1. getBean() -> doGetBean()
2. getSingleton() 尝试从一级缓存、二级缓存、三级缓存获取
3. createBean() -> doCreateBean()
4. createBeanInstance() 实例化，其实也就是调用对象的**构造方法**实例化对象，并放入**三级缓存**
5. polulateBean() 填充属性，这一步主要是对bean的依赖属性进行注入(`@Autowired`
6. initializeBean() 初始化，回到一些形如`initMethod`、`InitializingBean`等方法
7. 返回bean



### getBean方法

```java
public abstract class AbstractBeanFactory {

    @Override
    public Object getBean(String name) throws BeansException {
        return doGetBean(name, null, null, false);
    }

    protected <T> T doGetBean(final String name, @Nullable final Class<T> requiredType,
            @Nullable final Object[] args, boolean typeCheckOnly) throws BeansException {
        // 别名转换
        final String beanName = transformedBeanName(name);
        Object bean;

        // 检查缓存中是否存在 该 Bean 的单例
        Object sharedInstance = getSingleton(beanName);
        if (sharedInstance != null && args == null) {
						...
            // 返回实例
            bean = getObjectForBeanInstance(sharedInstance, name, beanName, null);
        }
        else {
            // 存在循环依赖则报错
            if (isPrototypeCurrentlyInCreation(beanName)) {
                throw new BeanCurrentlyInCreationException(beanName);
            }
            // 判断工厂中是否含有当前 Bean 的定义
            BeanFactory parentBeanFactory = getParentBeanFactory();
            if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
                // 如果没有，查询父工厂
                String nameToLookup = originalBeanName(name);
                if (parentBeanFactory instanceof AbstractBeanFactory) {
                    return ((AbstractBeanFactory) parentBeanFactory).doGetBean(
                            nameToLookup, requiredType, args, typeCheckOnly);
                }
                else if (args != null) {
                    // 执行带有 args 参数的 getBean 方法
                    return (T) parentBeanFactory.getBean(nameToLookup, args);
                }
                else {
                    // 如果没有参数，执行标准的 getBean 方法 
                    return parentBeanFactory.getBean(nameToLookup, requiredType);
                }
            }
            if (!typeCheckOnly) { // 如果不是做类型检查，则需要标记此 Bean 正在创建之中
                markBeanAsCreated(beanName);
            }
            try {
                // 将存储XML配置文件的GernericBeanDefinition转换成RootBeanDefinition，如果BeanName是子Bean的话会合并父类的相关属性
                final RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
                checkMergedBeanDefinition(mbd, beanName, args);
                // 获取依赖的 Bean
                String[] dependsOn = mbd.getDependsOn();
                if (dependsOn != null) {
                    for (String dep : dependsOn) {
                        if (isDependent(beanName, dep)) {
                            throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                                    "Circular depends-on relationship between '" + beanName + "' and '" + dep + "'");
                        }
                        registerDependentBean(dep, beanName);
                        getBean(dep);
                    }
                }
                // 开始创建 Bean 实例了，如果是单例的，那么会创建一个单例的匿名工厂，
                if (mbd.isSingleton()) {
                    sharedInstance = getSingleton(beanName, () -> {
                        try {
                            return createBean(beanName, mbd, args);// 调用构造函数
                        }
                        catch (BeansException ex) {
                            destroySingleton(beanName);
                            throw ex;
                        }
                    });
                    bean = getObjectForBeanInstance(sharedInstance, name, beanName, mbd);
                }
								...
            }
            catch (BeansException ex) {
                cleanupAfterBeanCreationFailure(beanName);
                throw ex;
            }
        }

        // 类型检查，如果不能进行类型转换，则抛出异常
				...
        return (T) bean;
    }

}
```





### getSingleton方法

```java
public Object getSingleton(String beanName) {
    return getSingleton(beanName, true);//为true代表查看三级缓存。
}
protected Object getSingleton(String beanName, boolean allowEarlyReference) {
    // 在一级缓存中获取 beanName 对应的单实例对象。
    Object singletonObject = this.singletonObjects.get(beanName);
    // 单实例确实尚未创建；单实例正在创建，发生了循环依赖
    if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
        synchronized (this.singletonObjects) {
            // 从二级缓存获取
            singletonObject = this.earlySingletonObjects.get(beanName);
            // 二级缓存不存在，并且允许获取早期实例对象，去三级缓存查看
            if (singletonObject == null && allowEarlyReference) {
                ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
                if (singletonFactory != null) {
                    // 从三级缓存获取工厂对象，并得到 bean 的提前引用
                    singletonObject = singletonFactory.getObject();
                    // 【缓存升级】，放入二级缓存，提前引用池
                    this.earlySingletonObjects.put(beanName, singletonObject);
                    // 从三级缓存移除该对象
                    this.singletonFactories.remove(beanName);
                }
            }
        }
    }
    return singletonObject;
}
```

如果在一级缓存，二级缓存，三级缓存都没获取到，则会创建bean



### createBean()

```java
public class AbstractAutowireCapableBeanFactory {

    @Override
    protected Object createBean(String beanName, RootBeanDefinition mbd, @Nullable Object[] args) throws BeanCreationException {
				...
        RootBeanDefinition mbdToUse = mbd;
        // 首先判断需要创建的bean是否可以被实例化，这个类是否可以通过类装载器来载入。
        Class<?> resolvedClass = resolveBeanClass(mbd, beanName);
        if (resolvedClass != null && !mbd.hasBeanClass() && mbd.getBeanClassName() != null) {
            mbdToUse = new RootBeanDefinition(mbd);
            mbdToUse.setBeanClass(resolvedClass);
        }
        // 准备方法覆盖
        try {
            mbdToUse.prepareMethodOverrides();
        }
        ...
        try {
            // 如果 Bean 实现了PostProcessor接口，那么这里直接返回的是一个代理
            Object bean = resolveBeforeInstantiation(beanName, mbdToUse);
            if (bean != null) {
                return bean; // 直接返回代理
            }
        }
        ...
        try {
            // 重点来了
            Object beanInstance = doCreateBean(beanName, mbdToUse, args);
						...
            return beanInstance;
        }
        ...
    }
}
```



### doCreateBean方法

```java
protected Object doCreateBean(final String beanName, final RootBeanDefinition mbd, final @Nullable Object[] args) throws BeanCreationException {

    // 创建Bean对象，并且将对象包裹在BeanWrapper中
    BeanWrapper instanceWrapper = null;
    if (mbd.isSingleton()) {
        instanceWrapper = this.factoryBeanInstanceCache.remove(beanName);
    }
    // 实例化（一般会调用无参构造方法）
    instanceWrapper = createBeanInstance(beanName, mbd, args);
    // 获取实例化好的 Bean，此处还未进行赋值
    final Object bean = instanceWrapper.getWrappedInstance();
    // 获得实例化好的 class
    Class<?> beanType = instanceWrapper.getWrappedClass();
    if (beanType != NullBean.class) {
        mbd.resolvedTargetType = beanType;
    }

    // 记录创建 Bean 的 ObjectFactory，初始化前调用 post-processors，可以让我们在 bean 实例化之前做一些定制操作
    synchronized (mbd.postProcessingLock) {
        if (!mbd.postProcessed) {
            try {
                applyMergedBeanDefinitionPostProcessors(mbd, beanType, beanName);
            }
            catch (Throwable ex) {
                throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                        "Post-processing of merged bean definition failed", ex);
            }
            mbd.postProcessed = true;
        }
    }

    // 检测循环依赖，需要允许提前暴露对象引用&&当前bean正在被创建中
    boolean earlySingletonExposure = (mbd.isSingleton() && this.allowCircularReferences &&
            isSingletonCurrentlyInCreation(beanName));
    if (earlySingletonExposure) {
        ...
        // getEarlyBeanReference()里可以实现AOP的逻辑，参考自动代理创建器AbstractAutoProxyCreator
        // 然后把创建好的ObjectFactory（可能创建出代理对象）加入三级缓存
        addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, mbd, bean));
    }
    Object exposedObject = bean;
    try {
        // 填充字段，解决@Autowired依赖
        populateBean(beanName, mbd, instanceWrapper);
      	// 初始化，并执行相关PostProcessor
        exposedObject = initializeBean(beanName, exposedObject, mbd);
    }
    ...
		
    if (earlySingletonExposure) {
      	//注意，注意：第二参数为false  表示不会再去三级缓存里查了
        Object earlySingletonReference = getSingleton(beanName, false);
        if (earlySingletonReference != null) {
          	// bean没变
            if (exposedObject == bean) {
                exposedObject = earlySingletonReference;
            }
          	// bean变了说明被代理过
            else if (!this.allowRawInjectionDespiteWrapping && hasDependentBean(beanName)) {
                String[] dependentBeans = getDependentBeans(beanName);
                Set<String> actualDependentBeans = new LinkedHashSet<>(dependentBeans.length);
                for (String dependentBean : dependentBeans) {
                    if (!removeSingletonIfCreatedForTypeCheckOnly(dependentBean)) {
                        actualDependentBeans.add(dependentBean);
                    }
                }
                if (!actualDependentBeans.isEmpty()) {
                    // 报错...
                }
            }
        }
    }

    // 用于销毁方法
    try {
        registerDisposableBeanIfNecessary(beanName, bean, mbd);
    }
    ...
    return exposedObject;
}
```



### createBeanInstance方法

```java
protected BeanWrapper createBeanInstance(String beanName, RootBeanDefinition mbd, Object[] args) {
    // 解析class
    Class<?> beanClass = resolveBeanClass(mbd, beanName);

    if (beanClass != null && !Modifier.isPublic(beanClass.getModifiers()) && !mbd.isNonPublicAccessAllowed()) {
        throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                "Bean class isn't public, and non-public access not allowed: " + beanClass.getName());
    }

    // 若工厂方法不为空则使用工厂方法初始化
    if (mbd.getFactoryMethodName() != null)  {
        return instantiateUsingFactoryMethod(beanName, mbd, args);
    }
    ...
    // 如果已经解析过则使用解析好的构造方法不需要再次锁定
    if (resolved) {
        if (autowireNecessary) {
            // 构造方法自动注入
            return autowireConstructor(beanName, mbd, null, null);
        }
        else {
            // 使用默认构造方法
            return instantiateBean(beanName, mbd);
        }
    }
    // 根据参数解析构造方法
    Constructor<?>[] ctors = determineConstructorsFromBeanPostProcessors(beanClass, beanName);
    if (ctors != null ||
            mbd.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_CONSTRUCTOR ||
            mbd.hasConstructorArgumentValues() || !ObjectUtils.isEmpty(args))  {
        return autowireConstructor(beanName, mbd, ctors, args);
    }

    // 默认使用无参构造方法
    return instantiateBean(beanName, mbd);
}
```

可以看出如果在 *`RootBeanDefinition`* 中存在 **`factoryMethodName`**属性，或者说配置文件中配置了 *`factory-method`*，那么 `Spring` 会尝试使用 **`instantiateUsingFactoryMethod(beanName, mbd, args)`** 方法根据 *`RootBeanDefinition`* 中的配置生成bean实例。然后再解析构造方法并进行实例化，`Spring` 会根据参数及类型判断使用哪个构造方法进行实例化。判断调用哪个构造方法的过程会采用缓存机制，如果已经解析过则不需要重复解析而是从 *`RootBeanDefinition`* 中的属性 `resolvedConstructorOrFactoryMethod` 缓存的值去取，否则需再次解析。



### populateBean方法

```java
protected void populateBean(String beanName, RootBeanDefinition mbd, BeanWrapper bw) {
    PropertyValues pvs = mbd.getPropertyValues();

    if (bw == null) {
        if (!pvs.isEmpty()) {
            throw new BeanCreationException(
                    mbd.getResourceDescription(), beanName, "Cannot apply property values to null instance");
        }
        else {
            // Skip property population phase for null instance.
            return;
        }
    }

    // InstantiationAwareBeanPostProcessor 处理器的 postProcessAfterInstantiation 方法 是否继续填充属性；
    boolean continueWithPropertyPopulation = true;

    if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
        for (BeanPostProcessor bp : getBeanPostProcessors()) {
            if (bp instanceof InstantiationAwareBeanPostProcessor) {
                InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor) bp;
                if (!ibp.postProcessAfterInstantiation(bw.getWrappedInstance(), beanName)) {
                    continueWithPropertyPopulation = false;
                    break;
                }
            }
        }
    }

    if (!continueWithPropertyPopulation) {
        return;
    }
		// 解析@Autowried, 根据注入类型提取依赖的 bean, 并存入 PropertyValues 中
    if (mbd.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_BY_NAME ||
            mbd.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_BY_TYPE) {
        MutablePropertyValues newPvs = new MutablePropertyValues(pvs);
        // Add property values based on autowire by name if applicable.
        if (mbd.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_BY_NAME) {
            autowireByName(beanName, mbd, bw, newPvs);
        }
        // Add property values based on autowire by type if applicable.
        if (mbd.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_BY_TYPE) {
            autowireByType(beanName, mbd, bw, newPvs);
        }

        pvs = newPvs;
    }

    boolean hasInstAwareBpps = hasInstantiationAwareBeanPostProcessors();
    boolean needsDepCheck = (mbd.getDependencyCheck() != RootBeanDefinition.DEPENDENCY_CHECK_NONE);
		// InstantiationAwareBeanPostProcessor 处理器的 postProcessPropertyValues 方法对属性在填充前再次处理（主要还是验证属性）
    if (hasInstAwareBpps || needsDepCheck) {
        PropertyDescriptor[] filteredPds = filterPropertyDescriptorsForDependencyCheck(bw, mbd.allowCaching);
        if (hasInstAwareBpps) {
            for (BeanPostProcessor bp : getBeanPostProcessors()) {
                if (bp instanceof InstantiationAwareBeanPostProcessor) {
                    InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor) bp;
                    pvs = ibp.postProcessPropertyValues(pvs, filteredPds, bw.getWrappedInstance(), beanName);
                    if (pvs == null) {
                        return;
                    }
                }
            }
        }
        if (needsDepCheck) {
            checkDependencies(beanName, mbd, filteredPds, pvs);
        }
    }
		// 将所有 PropertyValues 中的属性填充到 BeanWrapper 中
    applyPropertyValues(beanName, mbd, bw, pvs);
}
```

在 `populateBean` 方法的中的主要处理流程：

- *`InstantiationAwareBeanPostProcessor`* 处理器的 *`postProcessAfterInstantiation`* 方法控制程序是否继续填充属性；
- 根据注入类型提取依赖的 `bean`，并存入 `PropertyValues` 中；
- *`InstantiationAwareBeanPostProcessor`* 处理器的 *`postProcessPropertyValues`* 方法对属性在填充前再次处理（主要还是验证属性）；
- 将所有 *`PropertyValues`* 中的属性填充到 **`BeanWrapper`** 中；



### initializeBean方法

```java
protected Object initializeBean(final String beanName, final Object bean, RootBeanDefinition mbd) {
    if (System.getSecurityManager() != null) {
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                invokeAwareMethods(beanName, bean);
                return null;
            }
        }, getAccessControlContext());
    }
    else {
        // 特殊bean处理
        invokeAwareMethods(beanName, bean);
    }

    Object wrappedBean = bean;
    // 如果实现了接口，执行BeanPostProcessorsBeforeInitialization方法
    if (mbd == null || !mbd.isSynthetic()) {
        wrappedBean = applyBeanPostProcessorsBeforeInitialization(wrappedBean, beanName);
    }

    try {
        // bean配置时有一个init-method属性，此时执行
        invokeInitMethods(beanName, wrappedBean, mbd);
    }
    catch (Throwable ex) {
        throw new BeanCreationException(
                (mbd != null ? mbd.getResourceDescription() : null),
                beanName, "Invocation of init method failed", ex);
    }
		// 如果实现了接口，执行BeanPostProcessorsAfterInitialization
    if (mbd == null || !mbd.isSynthetic()) {
        wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
    }
    return wrappedBean;
}
```





## 循环依赖问题

这里有A、B两个bean，他们互相依赖

整个流程做文字步骤总结如下：

1. 使用`context.getBean(A.class)`，获取容器内的单例A，显然初次获取A是不存在的，因此走**A的创建之路**
2. `实例化`A（注意此处仅仅是实例化），并将它放进`三级缓存`（此时A已经实例化完成，已经可以被引用了）
3. `初始化`A：`@Autowired`依赖注入B（此时需要去容器内获取B）
4. 为了完成依赖注入B，会通过`getBean(B)`去容器内找B。但此时B在容器内不存在，就走向**B的创建之路**
5. `实例化`B，并将其放入缓存。（此时B也能够被引用了）
6. `初始化`B，`@Autowired`依赖注入A（此时需要去容器内获取A）
7. `此处重要`：初始化B时会调用`getBean(A)`去容器内找到A，上面我们已经说过了此时候因为A已经实例化完成了并且放进了缓存里，所以这个时候去看缓存里是已经存在A的引用了的，所以`getBean(A)`能够正常返回
8. **B初始化成功**（此时已经注入A成功了，已成功持有A的引用了），return（注意此处return相当于是返回最上面的`getBean(B)`这句代码，回到了初始化A的流程中~）。
9. 因为B实例已经成功返回了，因此最终**A也初始化成功**
10. 到此，B持有的已经是初始化完成的A，A持有的也是初始化完成的B



为什么只能解决setter注入的循环依赖问题，不能解决构造器循环依赖问题？

问题就在于如果A是使用构造器注入，那么A还没有实例化，就要去实例化B，B也没办法拿到A的引用，形成死循环。



## AOP对循环依赖的影响

我们都知道**Spring AOP、事务**等都是通过代理对象来实现的，而**事务**的代理对象是由自动代理创建器来自动完成的。也就是说Spring最终给我们放进容器里面的是一个代理对象，**而非原始对象**。

本文结合`循环依赖`，回头再看AOP代理对象的创建过程，和最终放进容器内的动作

```java
@Service
public class HelloServiceImpl implements HelloService {
    @Autowired
    private HelloService helloService;
    
    @Transactional
    @Override
    public Object hello(Integer id) {
        return "service hello";
    }
}
```

此`Service`类使用到了事务，所以最终会生成一个JDK动态代理对象`Proxy`。刚好它又存在`自己引用自己`的循环依赖。看看这个Bean的创建概要描述如下：

```java
protected Object doCreateBean( ... ){
	...
	
	// 这段告诉我们：如果允许循环依赖的话，此处会添加一个ObjectFactory到三级缓存里
	// getEarlyBeanReference是后置处理器SmartInstantiationAwareBeanPostProcessor的一个方法
	// 保证自己被循环依赖的时候，即使被别的Bean @Autowire进去的也是代理对象~~~~  
  // AOP自动代理创建器此方法里会创建的代理对象~~~
	// Eagerly cache singletons to be able to resolve circular references
	// even when triggered by lifecycle interfaces like BeanFactoryAware.
	boolean earlySingletonExposure = (mbd.isSingleton() && this.allowCircularReferences && isSingletonCurrentlyInCreation(beanName));
	if (earlySingletonExposure) { 
    // 需要提前暴露（支持循环依赖），就注册一个ObjectFactory到三级缓存
		addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, mbd, bean));
	}

	// 此处注意：如果此处自己被循环依赖了  那它会走上面的getEarlyBeanReference，从而创建一个代理对象从三级缓存转移到二级缓存里
	// 注意此时候对象还在二级缓存里，并没有在一级缓存。并且此时可以知道exposedObject仍旧是原始对象~~~
	populateBean(beanName, mbd, instanceWrapper);
	exposedObject = initializeBean(beanName, exposedObject, mbd);
	
	// 经过这两大步后，exposedObject还是原始对象（注意此处以事务的AOP为例子的，
	// 因为事务的AOP自动代理创建器在getEarlyBeanReference创建代理后，initializeBean就不会再重复创建了，二选一的）
	
	...
	
	// 循环依赖校验（非常重要）~~~~
	if (earlySingletonExposure) {
		// 前面说了因为自己被循环依赖了，所以此时候代理对象还在二级缓存里（自己被循环依赖了的情况）
		// so，此处getSingleton，就会把里面的对象拿出来，我们知道此时候它已经是个Proxy代理对象
		// 最后赋值给exposedObject  然后return出去，进而最终被addSingleton()添加进一级缓存里面去  
		// 这样就保证了我们容器里**最终实际上是代理对象**，而非原始对象
		Object earlySingletonReference = getSingleton(beanName, false);
		if (earlySingletonReference != null) {
			if (exposedObject == bean) { 
        // 这个判断不可少（因为如果initializeBean改变了exposedObject ，就不能这么玩了，否则就是两个对象了）
				exposedObject = earlySingletonReference;
			}
		}
		...
	}
	
}
```



最后，以`AbstractAutoProxyCreator`为例看看自动代理创建器是怎么配合实现创建代理

`AbstractAutoProxyCreator`是抽象类，它有三大实现子类`InfrastructureAdvisorAutoProxyCreator`、`AspectJAwareAdvisorAutoProxyCreator`、`AnnotationAwareAspectJAutoProxyCreator



```java
// 它实现代理创建的方法有如下两个
// 实现了SmartInstantiationAwareBeanPostProcessor 所以有方法getEarlyBeanReference来只能的解决循环引用问题：提前把代理对象暴露出去
public abstract class AbstractAutoProxyCreator extends ProxyProcessorSupport implements SmartInstantiationAwareBeanPostProcessor, BeanFactoryAware {
	...
	// 下面两个方法是自动代理创建器创建代理对象的唯二的两个节点

	// 提前暴露代理对象的引用  它肯定在postProcessAfterInitialization之前执行
	// 创建好后放进缓存earlyProxyReferences里  注意此处value是原始Bean
	@Override
	public Object getEarlyBeanReference(Object bean, String beanName) {
		Object cacheKey = getCacheKey(bean.getClass(), beanName);
		this.earlyProxyReferences.put(cacheKey, bean);
		return wrapIfNecessary(bean, beanName, cacheKey);
	}

	// 因为它会在getEarlyBeanReference之后执行，所以此处的重要逻辑是下面的判断
	@Override
	public Object postProcessAfterInitialization(@Nullable Object bean, String beanName) {
		if (bean != null) {
			Object cacheKey = getCacheKey(bean.getClass(), beanName);
			// remove方法返回被移除的value，上面说了它记录的是原始bean
			// 若被循环引用了，那就是执行了上面的`getEarlyBeanReference`方法，所以此时remove返回值肯定是==bean的（注意此时方法入参的bean还是原始对象）
			// 若没有被循环引用，getEarlyBeanReference()不执行 所以remove方法返回null，所以就进入if执行此处的创建代理对象方法~~~
			if (this.earlyProxyReferences.remove(cacheKey) != bean) {
				return wrapIfNecessary(bean, beanName, cacheKey);
			}
		}
		return bean;
	}
	...
}
```

自动代理创建器它保证了代理对象只会被**创建一次，**而且支持循环依赖的自动注入的依旧是代理对象。



## 为什么采用三级缓存

从上述Spring源码可知，其在第三级缓存中放入的是匿名类ObjectFactory，每次需要获取对象实例就会调用其getObject方法。我们举个例子：

假如现在没有earlySingletonObjects这一层缓存（也就是第二级缓存），也就是两级缓存结构，现在有2个对象，其依赖关系如下A->B、B->A，从这个依赖关系可以得出，A所在的ObjectFactory会被调用两次getObject()，如果两次都返回不同的proxy_A（毕竟后置处理器的代码是使用者自己写的，可能代码是new Proxy(A)），那么就可能导致，B、C对象依赖的proxy_A不是一个对象，那么这种设计是致命的。

假如没有singletonFactories这一层缓存（也就是第三级缓存），在发生循环依赖时，B从缓存中取出的是原始对象A，而A经过初始化后处理创建出了代理对象A，显然这两个不是一个对象。