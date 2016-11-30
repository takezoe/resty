package com.github.takezoe.resty;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Action {

    String path();

    /**
     * GET, POST, PUT or DELETE
     */
    String method();

    String description() default "";

    boolean deprecated() default false;

}
