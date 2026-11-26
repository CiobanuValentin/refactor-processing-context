package com.bnp.pf.transformation.visproxy.config.general;

import java.time.LocalDateTime;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

// TODO: importul pentru LocalDateTimeTypeAdapter este presupus - pastreaza-l pe cel actual.
import com.bnp.pf.transformation.commons.api.generalproxyobs.builder.gson.LocalDateTimeTypeAdapter;

// Neschimbat.
@Configuration
public class GsonConfig {

    @Bean
    public Gson createGson() {
        return new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeTypeAdapter())
                .create();
    }
}
