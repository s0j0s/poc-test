package com.poc.json;

import com.poc.json.jackson.JacksonJsonService;
import com.poc.json.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = "json.output.dir=./output-test")
class JacksonJsonServiceTest {

    @Autowired
    JacksonJsonService service;

    User sampleUser() {
        return User.builder()
                .id(1L).name("테스트유저").email("test@test.com").age(25)
                .createdAt(LocalDateTime.of(2024, 1, 15, 10, 30, 0))
                .roles(List.of("USER"))
                .address(User.Address.builder().city("부산").country("KR").zipCode("48058").build())
                .build();
    }

    @Test
    void 직렬화_역직렬화_동등성() throws Exception {
        User original = sampleUser();
        String json = service.toJson(original);
        User parsed = service.parseUser(json);

        assertThat(parsed.getName()).isEqualTo(original.getName());
        assertThat(parsed.getCreatedAt()).isEqualTo(original.getCreatedAt());
        assertThat(parsed.getRoles()).containsExactlyElementsOf(original.getRoles());
    }

    @Test
    void 파일_저장_및_로드() throws Exception {
        User original = sampleUser();
        File saved = service.saveToFile(original, "test_jackson.json");
        assertThat(saved).exists();

        User loaded = service.loadUserFromFile("test_jackson.json");
        assertThat(loaded.getId()).isEqualTo(original.getId());
        assertThat(loaded.getAddress().getCity()).isEqualTo("부산");
    }

    @Test
    void 리스트_파싱() throws Exception {
        List<User> users = List.of(sampleUser(), sampleUser());
        String json = service.toJson(users);
        List<User> parsed = service.parseUserList(json);

        assertThat(parsed).hasSize(2);
    }

    @Test
    void JsonNode_동적_접근() throws Exception {
        String json = "{\"key\":\"value\",\"num\":42}";
        var node = service.parseRawNode(json);

        assertThat(node.get("key").asText()).isEqualTo("value");
        assertThat(node.get("num").asInt()).isEqualTo(42);
    }
}
