package com.thingtrust.sportslottery;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SportsLotteyApplication {

    public static void main(final String[] args) {
        new SpringApplicationBuilder(SportsLotteyApplication.class).run(args);
    }

}
