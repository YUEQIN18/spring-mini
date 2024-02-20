# spring-mini
手写一个mini版的Spring框架

已完成功能：
- 完成了Bean生命周期管理、组件扫描@ComponentScan，依赖注入使用@Autowired根据类名查找依赖
- 进行了循环依赖检测，解决因构造方法注入、属性注入和set方法注入的代理对象的循环依赖问题