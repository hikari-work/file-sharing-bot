package com.yann.forcesub.manager;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Callback {
    String trigger();
    CallbackType type() default CallbackType.EXACT;
}
