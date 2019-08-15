package com.geekq.miaosha.access;

import com.geekq.miaosha.domain.MiaoshaUser;

public class UserContext {
	// 往ThreadLocal中填充的变量属于当前线程，该变量对其他线程而言是隔离的
	private static ThreadLocal<MiaoshaUser> userHolder = new ThreadLocal<MiaoshaUser>();
	
	public static void setUser(MiaoshaUser user) {
		userHolder.set(user);
	}
	
	public static MiaoshaUser getUser() {
		return userHolder.get();
	}

	public static void removeUser() {
		userHolder.remove();
	}

}
