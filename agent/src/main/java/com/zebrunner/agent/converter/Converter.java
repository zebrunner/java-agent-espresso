package com.zebrunner.agent.converter;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.time.OffsetDateTime;

public class Converter {

    public static final Type OFFSET_DATE_TIME_TYPE = new TypeToken<OffsetDateTime>(){}.getType();

    public static GsonBuilder registerOffsetDateTime(GsonBuilder builder) {
        builder.registerTypeAdapter(OFFSET_DATE_TIME_TYPE, new OffsetDateTimeConverter());
        return builder;
    }

}
