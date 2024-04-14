### 顺序
规则：order的值越小>越先被反向代理处理>越靠近目标对象>调用方法是越晚生效

代码规范：多切面对应的注解同时使用时，必须按从低到高的优先级顺序(级数字从大到小)来写

设置Order：
- Ordered接口
- @Order注解
- xml标签属性<aop:aspect order=0>
- 硬编码：AbstractAdvisingBeanPostProcessor.beforeExistingAdvisors=true

本项目对顺序的约定规范（优先级低到高=数字从大到小）:（Ordered.LOWEST_PRECEDENCE=Integer.MAX_VALUE）
1. 线程[MAX_VALUE, MAX_VALUE-10]
    - @AfterCommit事务提交后执行，可用来解决异步后主线程事务数据不能立刻可见的问题；
    - @EnableAsync指定LOWEST_PRECEDENCE-1；
2. 锁[0], KLock框架KlockAspectHandler写死0
3. 事务[-10], EZone基础组件当前设置为0，需改，todo
4. [MIN_VALUE] 框架默认
5. 允许业务自定义或使用框架自定义Order:
    - [9, 1]
    - [-1, -9]
    - [MIN_VALUE+10, MIN_VALUE+1]
        
理想状态: 修改spring逻辑，按注解声明顺序依次生效

Hack: 方法嵌套，偷懒做法，代码会显得冗余麻烦，但逻辑简单可靠

    