package com.github.takezoe.resty;

import java.lang.annotation.*;

@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Param {

    /**
     * Optional (default is a parameter name)
     */
    String name() default "";

    /**
     * query, path, header or body
     */
    String from() default "";

    /**
     * Optional
     */
    String description() default "";

}
