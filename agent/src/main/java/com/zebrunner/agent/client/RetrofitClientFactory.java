package com.zebrunner.agent.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.zebrunner.agent.core.converter.OffsetDateTimeConverter;
import com.zebrunner.agent.core.config.ConfigurationHolder;

import java.lang.reflect.Type;
import java.time.OffsetDateTime;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public final class RetrofitClientFactory {

    public static final Type OFFSET_DATE_TIME_TYPE = new TypeToken<OffsetDateTime>(){}.getType();

    public static <T> T createClient(Class<T> clientClass) {
        String apiHost = ConfigurationHolder.getHost();
        Retrofit.Builder builder = new Retrofit.Builder().baseUrl(apiHost);
        OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient.Builder();

        addConverterFactories(builder);
        return builder.client(okHttpClientBuilder.build())
                      .build()
                      .create(clientClass);
    }

    private static void addConverterFactories(Retrofit.Builder builder) {
        GsonBuilder gsonBuilder = new GsonBuilder();
        Gson gson = gsonBuilder.registerTypeAdapter(OFFSET_DATE_TIME_TYPE, new OffsetDateTimeConverter())
                               .create();
        builder.addConverterFactory(ScalarsConverterFactory.create())
               .addConverterFactory(GsonConverterFactory.create(gson));
    }

}
