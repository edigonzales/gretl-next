package ch.so.agi.gretl.doclet.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface GretlDslMethod {
    boolean required() default false;

    String defaultValue() default "";

    String description() default "";
}
