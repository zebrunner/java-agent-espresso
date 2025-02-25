package com.zebrunner.agent.espresso.client;

import java.util.Set;
import java.util.function.Supplier;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

@RequiredArgsConstructor
public class AuthorizationHeaderInterceptor implements Interceptor {

    private final Set<String> exclusions;
    private final Supplier<String> authorizationHeaderSupplier;

    @Override
    @SneakyThrows
    public Response intercept(Chain chain) {
        Request request = chain.request();

        if (this.shouldIntercept(request)) {
            String authorizationHeaderValue = authorizationHeaderSupplier.get();

            if (authorizationHeaderValue != null) {
                request = request.newBuilder()
                                 .header("Authorization", authorizationHeaderValue)
                                 .build();
            }
        }

        return chain.proceed(request);
    }

    private boolean shouldIntercept(Request request) {
        return !exclusions.contains(request.url().url().getPath());
    }

}
