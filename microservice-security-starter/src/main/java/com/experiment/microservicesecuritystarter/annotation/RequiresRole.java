package com.experiment.microservicesecuritystarter.annotation;

import com.experiment.microservicesecuritystarter.security.SecurityRole;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresRole {

    SecurityRole value();
}