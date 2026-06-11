package com.poc.json;

import com.poc.json.console.ConsoleMenu;
import com.poc.json.model.User;
import com.poc.json.repository.UserJsonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@TestPropertySource(properties = {
    "json.output.dir=./output-test",
    "json.data.file=./output-test/crud-test-users.json"
})
class UserJsonRepositoryTest {

    @MockBean
    ConsoleMenu consoleMenu;

    @Autowired
    UserJsonRepository repository;

    User sampleUser(String name, String email) {
        return User.builder()
                .name(name)
                .email(email)
                .age(25)
                .roles(List.of("USER"))
                .address(User.Address.builder().city("서울").country("KR").zipCode("04524").build())
                .build();
    }

    @BeforeEach
    void cleanup() throws IOException {
        // 각 테스트 전 데이터 초기화
        for (User u : List.copyOf(repository.findAll())) {
            repository.deleteById(u.getId());
        }
    }

    // ── Create ───────────────────────────────────────────────

    @Test
    void create_ID_자동증가() throws IOException {
        User u1 = repository.create(sampleUser("홍길동", "hong@test.com"));
        User u2 = repository.create(sampleUser("이순신", "lee@test.com"));

        assertThat(u1.getId()).isEqualTo(1L);
        assertThat(u2.getId()).isEqualTo(2L);
    }

    @Test
    void create_생성일시_자동설정() throws IOException {
        User created = repository.create(sampleUser("테스트", "test@test.com"));

        assertThat(created.getCreatedAt()).isNotNull();
    }

    @Test
    void create_파일에_저장됨() throws IOException {
        repository.create(sampleUser("홍길동", "hong@test.com"));

        assertThat(repository.findAll()).hasSize(1);
    }

    // ── Read ─────────────────────────────────────────────────

    @Test
    void findAll_전체목록_반환() throws IOException {
        repository.create(sampleUser("홍길동", "hong@test.com"));
        repository.create(sampleUser("이순신", "lee@test.com"));

        assertThat(repository.findAll()).hasSize(2);
    }

    @Test
    void findById_존재하는_ID() throws IOException {
        User created = repository.create(sampleUser("홍길동", "hong@test.com"));

        Optional<User> found = repository.findById(created.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("홍길동");
    }

    @Test
    void findById_없는_ID_빈_Optional() {
        Optional<User> found = repository.findById(999L);

        assertThat(found).isEmpty();
    }

    @Test
    void findByKeyword_이름으로_검색() throws IOException {
        repository.create(sampleUser("홍길동", "hong@test.com"));
        repository.create(sampleUser("이순신", "lee@test.com"));

        List<User> results = repository.findByKeyword("홍");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("홍길동");
    }

    @Test
    void findByKeyword_이메일로_검색() throws IOException {
        repository.create(sampleUser("홍길동", "hong@test.com"));
        repository.create(sampleUser("이순신", "lee@test.com"));

        List<User> results = repository.findByKeyword("lee");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getEmail()).isEqualTo("lee@test.com");
    }

    @Test
    void findByKeyword_없는_키워드_빈_리스트() throws IOException {
        repository.create(sampleUser("홍길동", "hong@test.com"));

        List<User> results = repository.findByKeyword("없는키워드");

        assertThat(results).isEmpty();
    }

    // ── Update ───────────────────────────────────────────────

    @Test
    void update_필드_수정() throws IOException {
        User created = repository.create(sampleUser("홍길동", "hong@test.com"));
        created.setName("홍길동수정");
        created.setAge(99);

        repository.update(created);

        User updated = repository.findById(created.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("홍길동수정");
        assertThat(updated.getAge()).isEqualTo(99);
    }

    @Test
    void update_없는_ID_예외() {
        User ghost = sampleUser("없는사람", "ghost@test.com");
        ghost.setId(999L);

        assertThatThrownBy(() -> repository.update(ghost))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("999");
    }

    // ── Delete ───────────────────────────────────────────────

    @Test
    void delete_정상삭제() throws IOException {
        User created = repository.create(sampleUser("홍길동", "hong@test.com"));

        repository.deleteById(created.getId());

        assertThat(repository.findById(created.getId())).isEmpty();
        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    void delete_없는_ID_예외() {
        assertThatThrownBy(() -> repository.deleteById(999L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("999");
    }

    @Test
    void delete_후_나머지_유지() throws IOException {
        User u1 = repository.create(sampleUser("홍길동", "hong@test.com"));
        User u2 = repository.create(sampleUser("이순신", "lee@test.com"));

        repository.deleteById(u1.getId());

        assertThat(repository.findAll()).hasSize(1);
        assertThat(repository.findById(u2.getId())).isPresent();
    }
}
