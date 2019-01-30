此 project 为[qiurunze123](https://github.com/qiurunze123/miaosha) 秒杀系统的原始版本，没有使用dobbo+zk
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
入口：http://localhost:8080/login/to_login
需要配置：redis  rabbitMQ  mysql
框架技术： SpringBoot thymeleaf  Redis RabbitMQ

