package io.github.resty;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Controller {
    String name();
    String description() default "";
}
