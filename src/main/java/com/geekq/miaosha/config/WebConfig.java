package com.geekq.miaosha.config;

import com.geekq.miaosha.access.AccessInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;


import java.util.List;

@SpringBootConfiguration
public class WebConfig  extends WebMvcConfigurationSupport {

    @Autowired
    UserArgumentResolver resolver;

    @Autowired
    AccessInterceptor interceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        super.addInterceptors(registry);
        registry.addInterceptor(interceptor);
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        super.addArgumentResolvers(argumentResolvers);
        argumentResolvers.add(resolver);
    }

 // 可能无法加载 static 中的静态资源
    @Override
     public void addResourceHandlers(ResourceHandlerRegistry registry) {
          registry.addResourceHandler("/static/**").addResourceLocations("classpath:/static/");
     }


}
