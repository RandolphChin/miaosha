package com.geekq.miaosha.access;

import java.lang.annotation.Retention;
		import java.lang.annotation.Target;

		import static java.lang.annotation.ElementType.METHOD;
		import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Retention 注解的生命周期，可选值：SOURCE CLASS RUNTIME
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface AccessLimit {
	int seconds();
	int maxCount();
	boolean needLogin() default true;
}
