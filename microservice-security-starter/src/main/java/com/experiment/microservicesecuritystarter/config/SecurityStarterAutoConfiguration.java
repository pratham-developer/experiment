package com.experiment.microservicesecuritystarter.config;

import com.experiment.microservicesecuritystarter.security.SecurityAuthorizationInterceptor;
import com.experiment.microservicesecuritystarter.security.SecurityContext;
import com.experiment.microservicesecuritystarter.security.SecurityContextFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@AutoConfiguration
public class SecurityStarterAutoConfiguration {

    @Bean
    public SecurityContextFilter securityContextFilter() {
        return new SecurityContextFilter();
    }

    @Bean
    public FilterRegistrationBean<SecurityContextFilter>
    securityContextFilterRegistration(
            SecurityContextFilter securityContextFilter
    ) {

        FilterRegistrationBean<SecurityContextFilter> registration =
                new FilterRegistrationBean<>();

        registration.setFilter(securityContextFilter);
        registration.addUrlPatterns("/*");
        registration.setOrder(1);

        return registration;
    }

    @Bean
    @ConditionalOnMissingBean
    public SecurityContext securityContext(
            HttpServletRequest request
    ) {
        return new SecurityContext(request);
    }

    @Bean
    public SecurityAuthorizationInterceptor
    securityAuthorizationInterceptor(
            SecurityContext securityContext
    ) {
        return new SecurityAuthorizationInterceptor(
                securityContext
        );
    }

    @Bean
    public WebMvcConfigurer securityStarterWebMvcConfigurer(
            SecurityAuthorizationInterceptor interceptor
    ) {

        return new WebMvcConfigurer() {

            @Override
            public void addInterceptors(
                    @NonNull InterceptorRegistry registry
            ) {
                registry.addInterceptor(interceptor);
            }
        };
    }
}