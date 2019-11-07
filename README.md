此 project 为[qiurunze123](https://github.com/qiurunze123/miaosha) 秒杀系统的原始版本springboot 1的基础上升级为 springboot2
看点：
1. rabbitMQ异步创建订单
2. HandlerMethodArgumentResolver参数解析器
3. 利用redis控流
4. 秒杀流程逻辑
##### 基本信息
入口：http://localhost:8080/login/to_login
需要配置：redis  rabbitMQ  mysql
框架技术： SpringBoot thymeleaf  Redis RabbitMQ
使用RabbitMQ 生产者redis库存预减1，redis查看订单是否存在，消费者实现库存数量减少1，成功后创建秒杀订单并写入redis
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
2.2 自定义注解@MobileCheck校验form表单参数中的手机用户名的有效性
2.2 根据用户名从数据库查询出用户,如果为空，统一异常报用户不存在
2.3 参数密码进行加密后与用户中的密码对比是否相同
2.4 生成 cookie， cookie设置有效期和 redis 一致, response中添加 cookie
2.5 redis 设置有期限的 key 为 cookie, value 为用户对象
2.6 ajax回调函数中跳转至其他页面
```
#### 拦截器 AccessInterceptor.java
应用于分布式系统中以cookie为key，从 redis获取登录用户信息
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
后台数据库查询出商品列表放入 Model 中，生成 html 字符串放入 redis 设置有效期时间 60秒
前台查询时会先从 redis 中获取，没有获取到则会手动渲染到指定视图
```
示例： BaseController.java 中的部分
IWebContext ctx = new WebContext(request,response,request.getServletContext(),request.getLocale(), model.asMap());
String  html = thymeleafViewResolver.getTemplateEngine().process(tplName, ctx);
OutputStream 输出 html        
```
#### 商品详情
Thymeleaf 中的 th:href 也可以跳转至指定的 html 页面，通过 ajax 加载数据
>th:href="'../static/goods_detail.htm?goodsId='+${goods.id}"

1. 由商品列表，点击"详情"按钮跳转至详情 html 页面
2. 异步请求数据库加载商品详情
3. 秒杀剩余时间大于0时，使秒杀按钮灰色无法点击，开始一个JavaScript的倒计时，验证码显示隐藏
4. 秒杀剩余时间等于0时，使秒杀按钮可以点击，清除倒计时标识，请求后台获取页面验证码，验证码显示
5. 秒杀剩余时间小于0时，使秒杀按钮灰色无法点击，验证码隐藏不显示

#### 商品秒杀
Spring 初始化时:
````
1 把每种商品的库存数量存入 redis 中,key为商品ID,value为商品库存;
2 内存标记localOverMap变量(HashMap<Long, Boolean> localOverMap) key为把商品ID,value为 false,存入redis作为库存标记变量
````
1. 填写验证码，点击"商品秒杀"按钮，异步get请求参数商品ID和验证码,请求方法A
2. 后台先检验用户是否登录，未登录报错
3. 从 redis 中获取验证码，对比前台参数，如果不同则抛出异常报错，如果相同则 redis 删除该验证码
4. 随机生成一个path，放入 redis中，设置过期时间，返回该 path 给前台
5. 前台根据返回的随机生成的path,异步post请求参数商品ID,请求另一个方法B
6. 后台先检验用户是否登录，未登录报错
7. 从 redis 中获取之前方法A生成的path，对比前台参数，如果不同则抛出异常报错
8. 根据用户信息和商品ID从 redis中查询是否已经存在秒杀订单MiaoshaOrder,表示已经秒杀过了,如果存在则,返回抛出异步(防止重复秒杀)
9. 如果 redis 中不存在该用户的秒杀订单,则先检查该商品是否有库存(内存标记 localOverMap.get(goodsId);)
10. 如果内存标记 localOverMap 根据该商品ID 获取的值为 true,则表示没有库存则抛出异常
11. 如果有库存,则 redis 预减库存数量 1,得到 redis中减去1之后的商品的库存数量
12. 检查库存数量,如果小于0,则更改商品的内存标记为 true ,并抛出异常报错
13. 构造订单对象,发送给MQ
14. MQ接收者接收订单对象，从中取出用户和商品ID，从数据库查询出商品库存数量 ,如果小于等于0则返回
15. 继续根据用户信息和商品ID从 redis中查询是否已经存在秒杀订单MiaoshaOrder,表示已经秒杀过了,如果存在则返回,不抛出异常
16. 根据商品ID从数据库更新商品数量，减少1，返回更新是否成功标识
17. 如果返回成功，则 insert 创建订单信息，秒杀订单MiaoshaOrder信息放入 redis中
18. 如果返回失败，则 redis 中设置商品秒杀库存为 0 标识key goodOver

#### 查看秒杀请求参数商品ID
1. 根据商品ID和用户信息从 redis 中查询出秒杀订单MiaoshaOrder
2. 如果订单不为空则秒杀成功
3. 如果订单为空,redis 中判断商品秒杀库存为 0 标识key goodOver是否存在
4. 如果存在则秒杀失败，返回-1；否则返回 0，让客户端重新发起查看秒杀请求
#### 参数解析器 HandlerMethodArgumentResolver
HandlerMethodArgumentResolver是一个参数解析器，可以通过写一个类实现HandlerMethodArgumentResolver接口来实现对
Controller层中方法请求参数的赋值修改
见 UserArgumentResolver.java 的用法