package com.experiment.microservicesecuritystarter.security;

import com.experiment.microservicesecuritystarter.annotation.RequiresAuthentication;
import com.experiment.microservicesecuritystarter.annotation.RequiresRole;
import com.experiment.microservicesecuritystarter.exception.SecurityForbiddenException;
import com.experiment.microservicesecuritystarter.exception.SecurityUnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.annotation.Annotation;

public class SecurityAuthorizationInterceptor
        implements HandlerInterceptor {

    private final SecurityContext securityContext;

    public SecurityAuthorizationInterceptor(
            SecurityContext securityContext
    ) {
        this.securityContext = securityContext;
    }

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler
    ) {

        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequiresAuthentication requiresAuthentication =
                findAnnotation(
                        handlerMethod,
                        RequiresAuthentication.class
                );

        RequiresRole requiresRole =
                findAnnotation(
                        handlerMethod,
                        RequiresRole.class
                );

        /*
         * @RequiresAuthentication requires authentication.
         *
         * @RequiresRole also requires authentication.
         */
        if (requiresAuthentication != null || requiresRole != null) {

            if (!securityContext.isAuthenticated()) {
                throw new SecurityUnauthorizedException(
                        "Authentication required"
                );
            }
        }

        /*
         * @RequiresRole requires the authenticated user
         * to have the specified role.
         */
        if (requiresRole != null) {

            SecurityRole requiredRole = requiresRole.value();
            SecurityRole actualRole =
                    securityContext.require().role();

            if (actualRole != requiredRole) {
                throw new SecurityForbiddenException(
                        "Insufficient permissions"
                );
            }
        }

        return true;
    }

    private <A extends Annotation> A findAnnotation(
            HandlerMethod handlerMethod,
            Class<A> annotationType
    ) {

        A annotation =
                handlerMethod.getMethodAnnotation(annotationType);

        if (annotation != null) {
            return annotation;
        }

        return handlerMethod
                .getBeanType()
                .getAnnotation(annotationType);
    }
}