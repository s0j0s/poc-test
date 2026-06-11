package com.poc.json;

import com.poc.json.console.ConsoleMenu;
import com.poc.json.jackson.JacksonJsonService;
import com.poc.json.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * [Safety] JacksonJsonService 극단/오용 시나리오.
 * 잘못된 JSON, null, 타입 불일치, 인젝션 시도 등을 검증.
 */
@Tag("safety")
@SpringBootTest
@TestPropertySource(properties = {
    "json.output.dir=./output-test",
    "json.data.file=./output-test/jackson-safety-users.json"
})
@DisplayName("[Safety] JacksonJsonService 극단/오용 시나리오")
class JacksonJsonServiceSafetyTest {

    @MockBean ConsoleMenu consoleMenu;
    @Autowired JacksonJsonService service;

    // ── parseUser 비정상 입력 ─────────────────────────────────

    @Test
    @DisplayName("parseUser(null) → 예외 발생 (Jackson IllegalArgumentException)")
    void parseUser_null_throws() {
        assertThatThrownBy(() -> service.parseUser(null))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("parseUser('') → JsonParseException (빈 문자열은 유효하지 않은 JSON)")
    void parseUser_empty_string_throws() {
        assertThatThrownBy(() -> service.parseUser(""))
                .isInstanceOf(IOException.class);
    }

    @Test
    @DisplayName("parseUser('{}') → 모든 필드 null/0인 User 반환 (예외 없음)")
    void parseUser_empty_object_returns_empty_user() throws Exception {
        User u = service.parseUser("{}");
        assertThat(u).isNotNull();
        assertThat(u.getId()).isNull();
        assertThat(u.getName()).isNull();
        assertThat(u.getAge()).isZero();
    }

    @Test
    @DisplayName("parseUser — 알 수 없는 필드 포함 → UnrecognizedPropertyException [Jackson 기본: 엄격 파싱]")
    void parseUser_unknown_fields_throw_by_default() {
        // ObjectMapper 기본값은 FAIL_ON_UNKNOWN_PROPERTIES=true
        // 허용하려면 objectMapper.disable(FAIL_ON_UNKNOWN_PROPERTIES) 설정 필요
        String json = "{\"id\":1,\"name\":\"테스트\",\"hacked\":true,\"extra_field\":\"값\"}";
        assertThatThrownBy(() -> service.parseUser(json))
                .isInstanceOf(com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException.class)
                .hasMessageContaining("hacked");
    }

    @Test
    @DisplayName("parseUser — id 타입 불일치(문자열) → JsonProcessingException")
    void parseUser_type_mismatch_id_as_string_throws() {
        String json = "{\"id\":\"문자열ID\",\"name\":\"테스트\"}";
        assertThatThrownBy(() -> service.parseUser(json))
                .isInstanceOf(IOException.class);
    }

    @Test
    @DisplayName("parseUser — 중첩된 malformed JSON → JsonParseException")
    void parseUser_malformed_json_throws() {
        assertThatThrownBy(() -> service.parseUser("{name: 따옴표없음}"))
                .isInstanceOf(IOException.class);
    }

    @Test
    @DisplayName("parseUser — 배열을 User로 파싱 → JsonProcessingException")
    void parseUser_array_as_user_throws() {
        assertThatThrownBy(() -> service.parseUser("[{\"id\":1}]"))
                .isInstanceOf(IOException.class);
    }

    // ── parseUserList 비정상 입력 ─────────────────────────────

    @Test
    @DisplayName("parseUserList('[]') → 빈 리스트 반환 (예외 없음)")
    void parseUserList_empty_array_returns_empty() throws Exception {
        assertThat(service.parseUserList("[]")).isEmpty();
    }

    @Test
    @DisplayName("parseUserList('{}') → JsonProcessingException (객체를 리스트로 파싱 불가)")
    void parseUserList_object_not_array_throws() {
        assertThatThrownBy(() -> service.parseUserList("{}"))
                .isInstanceOf(IOException.class);
    }

    @Test
    @DisplayName("parseUserList — 일부 항목에 잘못된 타입 → JsonProcessingException")
    void parseUserList_mixed_types_throws() {
        String json = "[{\"id\":1},{\"id\":\"not-a-number\"}]";
        assertThatThrownBy(() -> service.parseUserList(json))
                .isInstanceOf(IOException.class);
    }

    // ── parseRawNode 비정상 입력 ──────────────────────────────

    @Test
    @DisplayName("parseRawNode — malformed JSON → JsonParseException")
    void parseRawNode_malformed_throws() {
        assertThatThrownBy(() -> service.parseRawNode("{invalid json!!!}"))
                .isInstanceOf(IOException.class);
    }

    @Test
    @DisplayName("parseRawNode — 배열 형식도 JsonNode로 파싱 가능")
    void parseRawNode_array_parsed_as_node() throws Exception {
        var node = service.parseRawNode("[1, 2, 3]");
        assertThat(node.isArray()).isTrue();
        assertThat(node.size()).isEqualTo(3);
    }

    // ── toJson 극단값 ─────────────────────────────────────────

    @Test
    @DisplayName("toJson(null) → 'null' 문자열 반환 (예외 없음)")
    void toJson_null_returns_null_string() throws Exception {
        assertThat(service.toJson(null)).isEqualTo("null");
    }

    @Test
    @DisplayName("toJson(User with all null fields) → null 필드 포함 JSON 직렬화")
    void toJson_user_all_null_fields() throws Exception {
        User u = new User(); // Lombok @NoArgsConstructor — 모든 필드 null/0
        String json = service.toJson(u);
        assertThat(json).isNotNull();
        assertThat(json).contains("null"); // null 필드 포함
    }

    // ── 직렬화/역직렬화 무결성 ────────────────────────────────

    @Test
    @DisplayName("이모지/유니코드 이름 → 직렬화/역직렬화 후 원본 보존")
    void toJson_parse_emoji_roundtrip() throws Exception {
        User original = User.builder().id(1L).name("😈악성유저🔥💀").email("evil@test.com").age(99).build();
        User parsed = service.parseUser(service.toJson(original));
        assertThat(parsed.getName()).isEqualTo(original.getName());
    }

    @Test
    @DisplayName("JSON 인젝션 문자열 이름 → Jackson이 이스케이프, 파싱 후 원본 보존")
    void toJson_json_injection_escaped_and_roundtrip() throws Exception {
        String injection = "{\"id\":999,\"name\":\"해커\",\"roles\":[\"ADMIN\"]}";
        User original = User.builder().id(1L).name(injection).email("x@x.com").age(1).build();
        String json = service.toJson(original);
        // 직렬화 결과에 원시 JSON 조각이 탈출 없이 삽입되지 않아야 함
        assertThat(json).doesNotContain("\"roles\":[\"ADMIN\"]"); // 이스케이프됨
        User parsed = service.parseUser(json);
        assertThat(parsed.getName()).isEqualTo(injection); // 역직렬화 후 원본 복원
    }

    @Test
    @DisplayName("LocalDateTime → ISO 문자열로 직렬화 (타임스탬프 숫자 아님)")
    void localdatetime_serialized_as_iso_string_not_timestamp() throws Exception {
        User u = User.builder().id(1L).name("시간테스트").email("t@t.com").age(20)
                .createdAt(LocalDateTime.of(2024, 6, 15, 10, 30, 0)).build();
        String json = service.toJson(u);
        // WRITE_DATES_AS_TIMESTAMPS disabled → ISO 형식
        assertThat(json).contains("2024-06-15T10:30:00");
        assertThat(json).doesNotContain("1718"); // epoch timestamp 아님
    }

    @Test
    @DisplayName("roles 리스트 보존 → 직렬화/역직렬화 후 순서 및 내용 유지")
    void roles_list_order_preserved_in_roundtrip() throws Exception {
        List<String> roles = List.of("ADMIN", "USER", "VIEWER");
        User original = User.builder().id(1L).name("다역할").email("multi@test.com").age(30)
                .roles(roles).build();
        User parsed = service.parseUser(service.toJson(original));
        assertThat(parsed.getRoles()).containsExactly("ADMIN", "USER", "VIEWER");
    }

    // ── saveToFile 디렉토리 생성 분기 ────────────────────────

    @Test
    @DisplayName("saveToFile — 출력 디렉토리 없을 때 자동 생성 (mkdirs 분기 커버)")
    void saveToFile_creates_output_directory_when_absent() throws Exception {
        java.lang.reflect.Field field = JacksonJsonService.class.getDeclaredField("outputDir");
        field.setAccessible(true);
        String original = (String) field.get(service);

        java.nio.file.Path tempBase = java.nio.file.Files.createTempDirectory("jacoco-jackson");
        java.nio.file.Path newSubdir = tempBase.resolve("brand-new-subdir"); // 존재하지 않는 하위 디렉토리
        try {
            field.set(service, newSubdir.toString());
            User u = User.builder().id(1L).name("디렉토리테스트").email("t@t.com").age(20).build();
            java.io.File saved = service.saveToFile(u, "branch-test.json");
            assertThat(newSubdir.toFile()).isDirectory(); // mkdirs 호출로 생성됨
            assertThat(saved).exists();
        } finally {
            field.set(service, original);
            org.springframework.util.FileSystemUtils.deleteRecursively(tempBase.toFile());
        }
    }

    // ── loadUserListFromFile — 미테스트 메서드 커버 ───────────

    @Test
    @DisplayName("saveToFile(List) + loadUserListFromFile → 리스트 왕복 무결성")
    void loadUserListFromFile_roundtrip() throws Exception {
        List<User> users = List.of(
                User.builder().id(1L).name("가").email("a@a.com").age(20).build(),
                User.builder().id(2L).name("나").email("b@b.com").age(25).build(),
                User.builder().id(3L).name("다").email("c@c.com").age(30).build()
        );
        service.saveToFile(users, "safety_list_roundtrip.json");
        List<User> loaded = service.loadUserListFromFile("safety_list_roundtrip.json");

        assertThat(loaded).hasSize(3);
        assertThat(loaded.get(0).getName()).isEqualTo("가");
        assertThat(loaded.get(2).getName()).isEqualTo("다");
    }

    @Test
    @DisplayName("loadUserListFromFile — 파일 없음 → IOException 발생")
    void loadUserListFromFile_missing_file_throws() {
        assertThatThrownBy(() -> service.loadUserListFromFile("존재하지않는파일_99999.json"))
                .isInstanceOf(IOException.class);
    }

    @Test
    @DisplayName("loadUserFromFile — 파일 없음 → IOException 발생")
    void loadUserFromFile_missing_file_throws() {
        assertThatThrownBy(() -> service.loadUserFromFile("존재하지않는파일_99999.json"))
                .isInstanceOf(IOException.class);
    }
}
