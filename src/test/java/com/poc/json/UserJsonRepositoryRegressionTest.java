package com.poc.json;

import com.poc.json.console.ConsoleMenu;
import com.poc.json.model.User;
import com.poc.json.repository.UserJsonRepository;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.*;

/**
 * [Regression] 기존 동작을 "보호 코팅"처럼 고정.
 * 이 테스트가 깨지면 = 기존 기능이 손상된 것 → 즉시 비상벨.
 *
 * 규칙: 구현을 바꿀 때 이 테스트가 깨지면, 의도적 변경인지 먼저 확인해야 함.
 */
@Tag("regression")
@SpringBootTest
@TestPropertySource(properties = {
    "json.output.dir=./output-test",
    "json.data.file=./output-test/regression-users.json"
})
@DisplayName("[Regression] UserJsonRepository 핵심 계약")
class UserJsonRepositoryRegressionTest {

    @MockBean ConsoleMenu consoleMenu;
    @Autowired UserJsonRepository repository;

    @BeforeEach
    void cleanup() throws IOException {
        for (User u : List.copyOf(repository.findAll())) {
            repository.deleteById(u.getId());
        }
    }

    // ── ID 자동 생성 계약 ──────────────────────────────────────

    @Test
    @DisplayName("빈 저장소 첫 create → ID = 1")
    void id_starts_at_1_on_empty_store() throws IOException {
        User u = repository.create(user("가", "a@a.com"));
        assertThat(u.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("연속 create → ID 순차 증가 (1, 2, 3)")
    void id_increments_sequentially() throws IOException {
        User u1 = repository.create(user("가", "a@a.com"));
        User u2 = repository.create(user("나", "b@b.com"));
        User u3 = repository.create(user("다", "c@c.com"));
        assertThat(List.of(u1.getId(), u2.getId(), u3.getId()))
                .containsExactly(1L, 2L, 3L);
    }

    @Test
    @DisplayName("create 시 caller가 설정한 ID는 항상 덮어씀 (max+1 강제)")
    void create_always_overwrites_caller_id() throws IOException {
        User u = user("가", "a@a.com");
        u.setId(999L);
        User saved = repository.create(u);
        assertThat(saved.getId()).isEqualTo(1L); // 999가 아닌 1
    }

    @Test
    @DisplayName("중간 삭제 후 create → max(id)+1 사용 (빈 ID 재사용 안 함)")
    void id_uses_max_plus_one_not_gap_fill() throws IOException {
        User u1 = repository.create(user("가", "a@a.com")); // id=1
        repository.create(user("나", "b@b.com"));            // id=2
        repository.deleteById(u1.getId());                   // id=1 삭제
        User u3 = repository.create(user("다", "c@c.com")); // id=3 (1 재사용 X)
        assertThat(u3.getId()).isEqualTo(3L);
    }

    // ── createdAt 자동 설정 계약 ──────────────────────────────

    @Test
    @DisplayName("create 후 createdAt 자동 설정 (null 아님)")
    void create_sets_createdAt_automatically() throws IOException {
        User u = repository.create(user("가", "a@a.com"));
        assertThat(u.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("create 시 caller가 설정한 createdAt도 현재 시각으로 덮어씀")
    void create_always_overwrites_caller_createdAt() throws IOException {
        User u = user("가", "a@a.com");
        u.setCreatedAt(LocalDateTime.of(2000, 1, 1, 0, 0));
        User saved = repository.create(u);
        assertThat(saved.getCreatedAt().getYear()).isNotEqualTo(2000);
    }

    // ── findAll 불변성 계약 ───────────────────────────────────

    @Test
    @DisplayName("findAll 반환 리스트는 직접 수정 불가 (UnmodifiableList)")
    void findAll_returns_unmodifiable_list() throws IOException {
        repository.create(user("가", "a@a.com"));
        List<User> list = repository.findAll();
        assertThatThrownBy(() -> list.add(user("나", "b@b.com")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("create 시 findAll 크기 정확히 증가")
    void findAll_size_grows_on_create() throws IOException {
        assertThat(repository.findAll()).hasSize(0);
        repository.create(user("가", "a@a.com"));
        assertThat(repository.findAll()).hasSize(1);
        repository.create(user("나", "b@b.com"));
        assertThat(repository.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("delete 시 findAll 크기 정확히 감소")
    void findAll_size_shrinks_on_delete() throws IOException {
        User u1 = repository.create(user("가", "a@a.com"));
        repository.create(user("나", "b@b.com"));
        repository.deleteById(u1.getId());
        assertThat(repository.findAll()).hasSize(1);
    }

    // ── findByKeyword 대소문자 무시 계약 ─────────────────────

    @Test
    @DisplayName("findByKeyword — 이름 검색 대소문자 무시")
    void findByKeyword_case_insensitive_name() throws IOException {
        repository.create(user("Alice", "alice@test.com"));
        assertThat(repository.findByKeyword("ALICE")).hasSize(1);
        assertThat(repository.findByKeyword("alice")).hasSize(1);
        assertThat(repository.findByKeyword("AlIcE")).hasSize(1);
    }

    @Test
    @DisplayName("findByKeyword — 이메일 검색 대소문자 무시")
    void findByKeyword_case_insensitive_email() throws IOException {
        repository.create(user("Bob", "Bob@Example.COM"));
        assertThat(repository.findByKeyword("bob@example.com")).hasSize(1);
        assertThat(repository.findByKeyword("BOB@EXAMPLE.COM")).hasSize(1);
    }

    @Test
    @DisplayName("findByKeyword('') — 빈 문자열은 전체 목록 반환 (모든 문자열이 \"\"을 포함)")
    void findByKeyword_empty_string_matches_all() throws IOException {
        repository.create(user("가", "a@a.com"));
        repository.create(user("나", "b@b.com"));
        assertThat(repository.findByKeyword("")).hasSize(2);
    }

    // ── update 격리 계약 ──────────────────────────────────────

    @Test
    @DisplayName("update — 리스트 중간 요소 수정 (loop false 분기: 첫 요소 불일치 후 다음 요소 매칭)")
    void update_non_first_element_covers_loop_false_branch() throws IOException {
        repository.create(user("가", "a@a.com")); // id=1, 인덱스 0
        User u2 = repository.create(user("나", "b@b.com")); // id=2, 인덱스 1
        u2.setName("나수정");
        repository.update(u2);
        // loop: cache[0].id=1 != 2 → false 분기, cache[1].id=2 == 2 → true 분기
        assertThat(repository.findById(2L).get().getName()).isEqualTo("나수정");
        assertThat(repository.findById(1L).get().getName()).isEqualTo("가");
    }

    @Test
    @DisplayName("update — 대상 ID만 변경, 나머지 레코드 불변")
    void update_only_affects_target_id() throws IOException {
        User u1 = repository.create(user("가", "a@a.com"));
        User u2 = repository.create(user("나", "b@b.com"));
        u1.setName("가수정");
        repository.update(u1);

        assertThat(repository.findById(u1.getId()).get().getName()).isEqualTo("가수정");
        assertThat(repository.findById(u2.getId()).get().getName()).isEqualTo("나"); // 불변
    }

    // ── 예외 메시지 계약 ──────────────────────────────────────

    @Test
    @DisplayName("없는 ID update → NoSuchElementException + 메시지에 ID 포함")
    void update_exception_message_contains_id() {
        User ghost = user("귀신", "ghost@test.com");
        ghost.setId(9999L);
        assertThatThrownBy(() -> repository.update(ghost))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("9999");
    }

    @Test
    @DisplayName("없는 ID deleteById → NoSuchElementException + 메시지에 ID 포함")
    void delete_exception_message_contains_id() {
        assertThatThrownBy(() -> repository.deleteById(9999L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("9999");
    }

    // ── 삭제 후 다른 레코드 유지 계약 ────────────────────────

    @Test
    @DisplayName("delete 후 나머지 레코드 findById로 여전히 조회 가능")
    void surviving_records_still_accessible_after_delete() throws IOException {
        User u1 = repository.create(user("가", "a@a.com"));
        User u2 = repository.create(user("나", "b@b.com"));
        repository.deleteById(u1.getId());

        assertThat(repository.findById(u1.getId())).isEmpty();
        assertThat(repository.findById(u2.getId())).isPresent(); // u2 생존
    }

    // ── Helper ────────────────────────────────────────────────

    private User user(String name, String email) {
        return User.builder().name(name).email(email).age(25)
                .roles(List.of("USER")).build();
    }
}
