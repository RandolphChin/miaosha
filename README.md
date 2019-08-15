此 project 为[qiurunze123](https://github.com/qiurunze123/miaosha) 秒杀系统的原始版本springboot 1的基础上升级为 springboot2
##### 秒杀时步骤
1. 检查核验验证码
redis中获取是否有有时效期限的校验码。 没有：直接返回false   ； 有：删除redis中的校验码，返回 true
2. 设置 redis MiaoshaPath
redis中设置具有时效期限的 MiaoshaPath，key格式为 nickName_goodId，value为md5加密的uuid，返回 value 值
3. 校验秒杀到的商品
redis 中查询秒杀到的商品，如果存在则为重复秒杀
4. redis 预减库存
5. 商品和个人信息发送到 MQ

##### 基本信息
入口：http://localhost:8080/login/do_login
需要配置：redis  rabbitMQ  mysql
框架技术： SpringBoot thymeleaf  Redis RabbitMQ
#### GoodsController.java
```
import org.thymeleaf.spring4.context.SpringWebContext;
import org.thymeleaf.spring4.view.ThymeleafViewResolver;

升级更改为

import org.thymeleaf.spring5.view.ThymeleafViewResolver;
import org.thymeleaf.context.WebContext;

```
>为了优化访问速度，应对高并发，想把页面信息全部获取出来存到redis缓存中，
这样每次访问就不用客户端进行渲染了
```
SpringWebContext ctx = new SpringWebContext(request,response,
                request.getServletContext(),request.getLocale(), model.asMap(), applicationContext );
String  html = viewResolver.getTemplateEngine().process("goods_detail", ctx);    
更改为
                
IWebContext ctx = new WebContext(request,response,
                request.getServletContext(),request.getLocale(), model.asMap() );     
// 返回字符串形式的 html，在请求 Controller中加上注解 @ResponseBody                
String  html = viewResolver.getTemplateEngine().process("goods_detail", ctx); 
```
BaseController.java 内容更改同上

#### WebConfig.java
```
public class WebConfig  extends WebMvcConfigurer(过时)

更改为

public class WebConfig  extends WebMvcConfigurationSupport
```
#### 实现限流(一段时间内限制访问秒杀方法次数)
实现思路：
1. 定义限流注解，定义时间段 second 和 最大访问次数 maxCount
2. 在目标方法上使用限流注解
3. 定义拦截器，第一次访问方法A时，在 redis 中记录key,value为访问次数count=1，并设定过期时间为 second
4. 后面每次访问方法A,从 redis中根据 key取 count值. 判断 count 值 与定义的限流方法注解设定值 maxCount 对比，
如果小于maxCount，则 count加1，如果大于maxCount 则直接返回false

1 定义限流注解
```
@Retention(RUNTIME)
@Target(METHOD)
public @interface AccessLimit {
	int seconds();
	int maxCount();
	boolean needLogin() default true;
}
```
2 秒杀方法上使用限流注解
```
 @AccessLimit(seconds = 5, maxCount = 5, needLogin = true)
    @RequestMapping(value = "/result", method = RequestMethod.GET)
    @ResponseBody
    public ResultGeekQ<Long> miaoshaResult(Model model, MiaoshaUser user,
                                           @RequestParam("goodsId") long goodsId) {
        //  do something                                           
 }
```
3 定义拦截器
见 AccessInterceptor.java
#### 注册登录
1 进入注册页面
```
1.1 先请求后台输出流输出BufferedImage对象生成验证码，并把该验证码放入 redis ，设定过期时间
1.2 填写注册信息，异步提交至后台
1.3 后台先从 redis中取出校验码与前台的作对比是否相同
1.4 密码加密后insert用户信息，新建一个秒杀信息设置用户id后发送MQ
1.5 生成 cookie, cookie设置有效期和 redis 一致, response中添加 cookie
1.6 redis 设置有期限的 key 为 cookie, value 为用户对象
1.7 ajax回调函数中跳转至其他页面
```
2 登录页面
```
2.1 异步提交登录信息
2.2 根据用户名从数据库查询出用户,如果为空，统一异常报用户不存在
2.3 参数密码进行加密后与用户中的密码对比是否相同
2.4 生成 cookie， cookie设置有效期和 redis 一致, response中添加 cookie
2.5 redis 设置有期限的 key 为 cookie, value 为用户对象
2.6 ajax回调函数中跳转至其他页面
```
#### 拦截器 AccessInterceptor.java
1 从 request 中获取 cookie，以 cookie为 key 从 redis 中获取用户对象
2 对象 set 到 ThreadLocal中
3 判断是否方法中添加注解 @AccessLimit
4 如果添加限流注解，则判断是否一段时间内超过一定访问次数
#### redis中预存商品库存数量
MiaoshaController.java 实现接口 InitializingBean， 实例化后中 查询出商品数量 for 循环存入 redis,并存入库存标识
```
redisService.set(GoodsKey.getMiaoshaGoodsStock, "" + goods.getId(), goods.getStockCount());
localOverMap.put(goods.getId(), false);
```
#### 商品列表展示
后台查询出商品列表放入 Model 中，生成 html 字符串放入 redis 设置有效期时间 60秒
