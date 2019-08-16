package com.geekq.miaosha.redis;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import redis.clients.jedis.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class RedisService {
/*
	@Autowired
	JedisPool jedisPool;
	*/
	@Autowired
	private StringRedisTemplate stringRedisTemplate;

	/**
	 * 设置失效时间
	 * @param key
	 * @param value
	 * @return
	 */
	public Integer setnx(String key ,String value){
       boolean result =stringRedisTemplate.opsForValue().setIfAbsent(key,value);
       return  result?1:0;
	}
	/**
	 * 设置key的有效期，单位是秒
	 * @param key
	 * @param exTime
	 * @return
	 */
	public Integer expire(String key,int exTime){

         boolean result = stringRedisTemplate.expire(key,exTime, TimeUnit.SECONDS);

		return result?1:0;
	}

	/**
	 * 获取当个对象
	 * */
	public <T> T get(KeyPrefix prefix, String key,  Class<T> clazz) {

		String realKey  = prefix.getPrefix() + key;
		String  str =  stringRedisTemplate.opsForValue().get(realKey);
		T t =  stringToBean(str, clazz);
		return t;
	}

    public  String get(String key){
        String result = stringRedisTemplate.opsForValue().get(key);
        return result;
    }


    public  String getset(String key,String value){
        String result =  stringRedisTemplate.opsForValue().getAndSet(key,value);
        return result;
    }
	/**
	 * 设置对象
	 * */
	public <T> boolean set(KeyPrefix prefix, String key,  T value) {
			 String str = beanToString(value);
			 if(str == null || str.length() <= 0) {
				 return false;
			 }
			//生成真正的key
			 String realKey  = prefix.getPrefix() + key;
			 int seconds =  prefix.expireSeconds();
			 if(seconds <= 0) {
                 stringRedisTemplate.opsForValue().set(realKey, str);;
			 }else {
                 stringRedisTemplate.opsForValue().set(realKey,str,seconds,TimeUnit.SECONDS);
			 }
			 return true;
	}
	
	/**
	 * 判断key是否存在
	 * */
	public <T> boolean exists(KeyPrefix prefix, String key) {
        //生成真正的key
         String realKey  = prefix.getPrefix() + key;
         return stringRedisTemplate.hasKey(realKey);
	}
	
	/**
	 * 删除
	 * */
	public boolean delete(KeyPrefix prefix, String key) {
			//生成真正的key
			String realKey  = prefix.getPrefix() + key;
             return stringRedisTemplate.delete(realKey);
	}
	
	/**
	 * 增加值
	 * */
	public <T> Long incr(KeyPrefix prefix, String key) {
			//生成真正的key
			 String realKey  = prefix.getPrefix() + key;
             Long num = stringRedisTemplate.boundValueOps(realKey).increment(1);
			return  num;
	}
	
	/**
	 * 减少值
	 * */
	public <T> Long decr(KeyPrefix prefix, String key) {
			//生成真正的key
			 String realKey  = prefix.getPrefix() + key;
             Long num = stringRedisTemplate.boundValueOps(realKey).increment(-1);
			return  num;
	}

    public  Integer del(String key){
         boolean boo = stringRedisTemplate.delete(key);
        return boo?1:0;
    }


	public static <T> String beanToString(T value) {
		if(value == null) {
			return null;
		}
		Class<?> clazz = value.getClass();
		if(clazz == int.class || clazz == Integer.class) {
			 return ""+value;
		}else if(clazz == String.class) {
			 return (String)value;
		}else if(clazz == long.class || clazz == Long.class) {
			return ""+value;
		}else {
			return JSON.toJSONString(value);
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> T stringToBean(String str, Class<T> clazz) {
		if(str == null || str.length() <= 0 || clazz == null) {
			 return null;
		}
		if(clazz == int.class || clazz == Integer.class) {
			 return (T)Integer.valueOf(str);
		}else if(clazz == String.class) {
			 return (T)str;
		}else if(clazz == long.class || clazz == Long.class) {
			return  (T)Long.valueOf(str);
		}else {
			return JSON.toJavaObject(JSON.parseObject(str), clazz);
		}
	}

	private void returnToPool(Jedis jedis) {
		 if(jedis != null) {
			 jedis.close();
		 }
	}

}
