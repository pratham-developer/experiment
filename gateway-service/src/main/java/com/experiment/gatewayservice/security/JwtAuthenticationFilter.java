package com.experiment.gatewayservice.security;

import com.experiment.gatewayservice.service.JwtService;
import io.jsonwebtoken.Claims;
import org.jspecify.annotations.NonNull;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_EMAIL_HEADER = "X-User-Email";
    private static final String USER_ROLE_HEADER = "X-User-Role";

    private final JwtService jwtService;
    private final PublicRoutes publicRoutes;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            PublicRoutes publicRoutes
    ) {
        this.jwtService = jwtService;
        this.publicRoutes = publicRoutes;
    }

    @Override
    @NonNull
    public Mono<Void> filter(ServerWebExchange exchange, @NonNull GatewayFilterChain chain) {

        String path = exchange.getRequest().getURI().getPath();

        //for public endpoint return early
        if(publicRoutes.isPublic(path)) {
            return chain.filter(exchange);
        }

        //get authorization header
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        //if invalid header, return 401 (unauthorized)
        if(authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange);
        }

        String token = authHeader.substring(7).trim();

        if(token.isEmpty()) {
            return unauthorized(exchange);
        }

        try{
            //verify jwt token and extract claims
            Claims claims = jwtService.validateAndExtractClaims(token);
            String userId = claims.getSubject();
            String email = claims.get("email", String.class);
            String role = claims.get("role", String.class);

            if (userId == null || email == null || role == null) {
                return unauthorized(exchange);
            }

            //remove incoming headers and put new
            ServerWebExchange mutableExchange = exchange.mutate()
                    .request(request -> request.headers(headers -> {
                        headers.remove(USER_ID_HEADER);
                        headers.remove(USER_EMAIL_HEADER);
                        headers.remove(USER_ROLE_HEADER);

                        //remove auth header
                        headers.remove(HttpHeaders.AUTHORIZATION);

                        //add claims parsed from jwt token
                        headers.add(USER_ID_HEADER, userId);
                        headers.add(USER_EMAIL_HEADER, email);
                        headers.add(USER_ROLE_HEADER, role);
                    })).build();

            return chain.filter(mutableExchange);
        }catch (Exception e){
            return unauthorized(exchange);
        }

    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse()
                .setStatusCode(HttpStatus.UNAUTHORIZED);

        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
