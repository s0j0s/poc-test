package com.poc.json;

import com.poc.json.console.ConsoleMenu;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@RequiredArgsConstructor
public class JsonPocApplication implements CommandLineRunner {

    private final ConsoleMenu consoleMenu;

    public static void main(String[] args) {
        SpringApplication.run(JsonPocApplication.class, args);
    }

    @Override
    public void run(String... args) {
        consoleMenu.run();
    }
}
