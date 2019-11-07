package com.geekq.miaosha.config;

import com.geekq.miaosha.access.UserContext;
import com.geekq.miaosha.domain.MiaoshaUser;
import com.geekq.miaosha.service.MiaoShaUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * HandlerMethodArgumentResolver是一个参数解析器，可以通过写一个类实现HandlerMethodArgumentResolver接口来实现对Controller层中方法请求参数的赋值修改
 */
@Service
public class UserArgumentResolver implements HandlerMethodArgumentResolver {

    @Autowired
    private MiaoShaUserService userService;
    @Override
    public boolean supportsParameter(MethodParameter methodParameter) {
      Class<?> clazz =    methodParameter.getParameterType() ;
       boolean blo = MiaoshaUser.class.isAssignableFrom(clazz);
      return clazz == MiaoshaUser.class ;
    }

    @Override
    public Object resolveArgument(MethodParameter methodParameter, ModelAndViewContainer modelAndViewContainer, NativeWebRequest webRequest, WebDataBinderFactory webDataBinderFactory) throws Exception {
        /**
         *  threadlocal 存储线程副本 保证线程不冲突
         */
        MiaoshaUser obj = UserContext.getUser();
        return UserContext.getUser();
    }

}
