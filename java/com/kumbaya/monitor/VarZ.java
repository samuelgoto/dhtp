package com.kumbaya.monitor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface VarZ {
	enum Type {
		COUNTER,
		QPS
	}
	public String value() default "/default";
	public Type[] type() default {Type.QPS};
}