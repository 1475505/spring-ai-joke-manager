package com.duduk.jokemanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class JokeManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(JokeManagerApplication.class, args);
    }

    @RestController
    public static class HelloController {
        @GetMapping("/")
        public String hello() {
            return "Hello! Joke Manager Backend is running!";
        }

        @GetMapping("/api/health")
        public String health() {
            return "OK";
        }
    }
}
