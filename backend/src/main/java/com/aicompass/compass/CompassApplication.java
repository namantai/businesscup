package com.aicompass.compass;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CompassApplication {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(CompassApplication.class);
        boolean command = java.util.Arrays.stream(args).anyMatch(a -> a.startsWith("--catalog-command="));
        if (command) application.setWebApplicationType(org.springframework.boot.WebApplicationType.NONE);
        var context = application.run(args);
        if (command) context.close();
    }
}
