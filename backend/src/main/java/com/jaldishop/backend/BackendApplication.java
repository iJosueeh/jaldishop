package com.jaldishop.backend;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendApplication {

    public static void main(String[] args) {

        String envDir = new java.io.File("backend/.env").exists() ? "./backend" : "./";

        Dotenv dotenv = Dotenv.configure()
                .directory(envDir)
                .ignoreIfMissing()
                .ignoreIfMalformed()
                .load();

        dotenv.entries().forEach(entry -> {
            if (System.getenv(entry.getKey()) == null) {
                System.setProperty(
                        entry.getKey(),
                        entry.getValue()
                );
            }
        });

        SpringApplication.run(BackendApplication.class, args);
    }

}
