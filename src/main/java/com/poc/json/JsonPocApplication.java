package com.poc.json;

import com.poc.json.jackson.JacksonJsonService;
import com.poc.json.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@SpringBootApplication
@RequiredArgsConstructor
public class JsonPocApplication implements CommandLineRunner {

    private final JacksonJsonService jacksonService;

    public static void main(String[] args) {
        SpringApplication.run(JsonPocApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        User user = User.builder()
                .id(1L)
                .name("홍길동")
                .email("hong@example.com")
                .age(30)
                .createdAt(LocalDateTime.now())
                .roles(List.of("ADMIN", "USER"))
                .address(User.Address.builder()
                        .city("서울")
                        .country("KR")
                        .zipCode("04524")
                        .build())
                .build();

        String json = jacksonService.toJson(user);
        log.info("직렬화:\n{}", json);

        User parsed = jacksonService.parseUser(json);
        log.info("역직렬화: name={}, roles={}", parsed.getName(), parsed.getRoles());

        jacksonService.saveToFile(user, "user.json");
        jacksonService.saveToFile(List.of(user, user), "users.json");

        log.info("완료 → ./output/ 확인");
    }
}
