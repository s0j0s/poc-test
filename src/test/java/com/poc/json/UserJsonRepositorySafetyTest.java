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
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

/**
 * [Safety] 이상한 값, 못된 사용자, 극단적 입력 가정.
 * "어떻게든 시스템을 망가뜨리려는 사용자"를 시뮬레이션.
 *
 * 목적: 현재 구현의 한계와 취약점을 명시적으로 문서화.
 *       예외가 터지는 케이스는 의도적으로 드러냄.
 */
@Tag("safety")
@SpringBootTest
@TestPropertySource(properties = {
    "json.output.dir=./output-test",
    "json.data.file=./output-test/safety-users.json"
})
@DisplayName("[Safety] UserJsonRepository 극단/오용 시나리오")
class UserJsonRepositorySafetyTest {

    @MockBean ConsoleMenu consoleMenu;
    @Autowired UserJsonRepository repository;

    @BeforeEach
    void cleanup() throws IOException {
        // distinct: 동시성 테스트가 중복 ID를 생성할 수 있으므로
        // NoSuchElementException 무시: 이미 삭제된 ID 재시도 방지
        List<Long> ids = repository.findAll().stream()
                .map(User::getId)
                .filter(id -> id != null)
                .distinct()
                .collect(java.util.stream.Collectors.toList());
        for (Long id : ids) {
            try {
                repository.deleteById(id);
            } catch (NoSuchElementException ignored) {}
        }
    }

    // ── Null 입력 ─────────────────────────────────────────────

    @Test
    @DisplayName("findById(null) → 빈 Optional 반환 (Long.equals(null)=false, NPE 없음)")
    void findById_null_returns_empty_optional() {
        // u.getId().equals(null) → false → 매칭 없음 → Optional.empty()
        assertThat(repository.findById(null)).isEmpty();
    }

    @Test
    @DisplayName("deleteById(null) → NoSuchElementException (removeIf 매칭 없음)")
    void deleteById_null_throws_no_such_element() {
        // u.getId().equals(null) → false → removed=false → 예외 발생
        assertThatThrownBy(() -> repository.deleteById(null))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("findByKeyword(null) → NullPointerException (null.toLowerCase() 호출)")
    void findByKeyword_null_throws_npe() {
        assertThatThrownBy(() -> repository.findByKeyword(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("create(null) → NullPointerException (null.setId() 호출)")
    void create_null_throws_npe() {
        assertThatThrownBy(() -> repository.create(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("update(null) → NullPointerException")
    void update_null_throws_npe() {
        assertThatThrownBy(() -> repository.update(null))
                .isInstanceOf(NullPointerException.class);
    }

    // ── 빈 문자열 / 공백 ──────────────────────────────────────

    @Test
    @DisplayName("이름 빈 문자열(\"\")로 create → 유효성 검사 없어 저장됨 [취약점 문서화]")
    void create_empty_name_no_validation() throws IOException {
        User u = User.builder().name("").email("empty@test.com").age(0).build();
        User saved = repository.create(u);
        // 현재 구현에 유효성 검사 없음 — 빈 이름이 저장되는 취약점
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEmpty();
    }

    @Test
    @DisplayName("이메일 null인 User, 이름 미일치 키워드 검색 → NullPointerException [취약점]")
    void null_email_causes_npe_when_keyword_misses_name() throws IOException {
        // email=null인 User 저장
        User u = User.builder().name("이메일없음").email(null).age(20).build();
        repository.create(u);
        // "xyz"는 이름 "이메일없음"에 미포함 → 단락평가 실패 → email.toLowerCase() 호출 → NPE
        // ※ 이름과 일치하는 키워드("이메일없음")로 검색하면 단락평가로 email 검사 건너뜀 → NPE 미발생
        assertThatThrownBy(() -> repository.findByKeyword("xyz"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("findByKeyword 공백(' ') → 이름/이메일에 공백 포함된 항목만 매칭")
    void findByKeyword_single_space_matches_spaced_names_only() throws IOException {
        repository.create(User.builder().name("Hong Gil Dong").email("hong@test.com").age(30).build());
        repository.create(User.builder().name("홍길동").email("hgd@test.com").age(25).build());
        // "Hong Gil Dong"에는 공백 포함 → 매칭
        // "홍길동"에는 공백 없음 → 미매칭
        assertThat(repository.findByKeyword(" ")).hasSize(1);
        assertThat(repository.findByKeyword(" ").get(0).getName()).isEqualTo("Hong Gil Dong");
    }

    // ── 극단적 값 ─────────────────────────────────────────────

    @Test
    @DisplayName("이름 10,000자 create → 예외 없이 저장 (길이 제한 없음)")
    void create_extremely_long_name() throws IOException {
        String longName = "가".repeat(10_000);
        User saved = repository.create(User.builder().name(longName).email("long@test.com").age(25).build());
        assertThat(repository.findById(saved.getId()).get().getName()).hasSize(10_000);
    }

    @Test
    @DisplayName("나이 음수(-1) → 예외 없이 저장 [유효성 검사 없음]")
    void create_negative_age_no_validation() throws IOException {
        User saved = repository.create(User.builder().name("음수나이").email("neg@test.com").age(-1).build());
        assertThat(saved.getAge()).isEqualTo(-1);
    }

    @Test
    @DisplayName("나이 Integer.MAX_VALUE(2,147,483,647) → 예외 없이 저장")
    void create_max_int_age() throws IOException {
        User saved = repository.create(
                User.builder().name("최대나이").email("max@test.com").age(Integer.MAX_VALUE).build());
        assertThat(saved.getAge()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    @DisplayName("findById(Long.MIN_VALUE) → 빈 Optional (예외 없음)")
    void findById_long_min_value_returns_empty() {
        assertThat(repository.findById(Long.MIN_VALUE)).isEmpty();
    }

    @Test
    @DisplayName("findById(Long.MAX_VALUE) → 빈 Optional (예외 없음)")
    void findById_long_max_value_returns_empty() {
        assertThat(repository.findById(Long.MAX_VALUE)).isEmpty();
    }

    @Test
    @DisplayName("findById(-1L) → 빈 Optional (예외 없음)")
    void findById_negative_id_returns_empty() {
        assertThat(repository.findById(-1L)).isEmpty();
    }

    // ── 특수문자 / 인젝션 시도 ────────────────────────────────

    @Test
    @DisplayName("이름에 JSON 인젝션 시도 → Jackson이 이스케이프 처리, 무결성 유지")
    void create_json_injection_in_name_is_escaped() throws IOException {
        String malicious = "{\"id\":999,\"name\":\"해커\",\"roles\":[\"ADMIN\"]}";
        User saved = repository.create(User.builder().name(malicious).email("hack@test.com").age(20).build());
        // 저장 후 조회 시 원본 문자열 그대로 보존되어야 함 (파싱되지 않음)
        assertThat(repository.findById(saved.getId()).get().getName()).isEqualTo(malicious);
    }

    @Test
    @DisplayName("이름에 XSS 스크립트 → 저장/조회 무결성 (이 레이어에서 sanitize 없음)")
    void create_xss_in_name_stored_as_is() throws IOException {
        String xss = "<script>alert('xss')</script>";
        User saved = repository.create(User.builder().name(xss).email("xss@test.com").age(20).build());
        // JSON 저장소 레이어에서는 XSS를 막지 않음 — 표시 레이어에서 처리 필요
        assertThat(repository.findById(saved.getId()).get().getName()).isEqualTo(xss);
    }

    @Test
    @DisplayName("이름에 이모지/유니코드 → 저장/조회 무결성")
    void create_emoji_and_unicode_intact() throws IOException {
        String emoji = "😈🔥💀 악성유저 🚨⚠️";
        User saved = repository.create(User.builder().name(emoji).email("evil@test.com").age(20).build());
        assertThat(repository.findById(saved.getId()).get().getName()).isEqualTo(emoji);
    }

    @Test
    @DisplayName("이메일에 특수문자 포함 → 저장/조회 무결성")
    void create_special_email_format_intact() throws IOException {
        String weirdEmail = "user+tag@sub.domain.co.kr";
        User saved = repository.create(User.builder().name("특수이메일").email(weirdEmail).age(20).build());
        assertThat(repository.findById(saved.getId()).get().getEmail()).isEqualTo(weirdEmail);
    }

    // ── 비정상 상태 ───────────────────────────────────────────

    @Test
    @DisplayName("같은 이메일 중복 create → 두 건 모두 저장 [유일성 제약 없음, 취약점]")
    void create_duplicate_email_both_saved() throws IOException {
        repository.create(User.builder().name("사용자A").email("dup@test.com").age(20).build());
        repository.create(User.builder().name("사용자B").email("dup@test.com").age(25).build());
        // 중복 이메일 허용 → 2건 저장됨
        assertThat(repository.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("roles = null → 저장/조회 예외 없음")
    void create_null_roles_no_exception() throws IOException {
        User saved = repository.create(
                User.builder().name("역할없음").email("norole@test.com").age(20).roles(null).build());
        assertThat(repository.findById(saved.getId())).isPresent();
        assertThat(repository.findById(saved.getId()).get().getRoles()).isNull();
    }

    @Test
    @DisplayName("roles = 빈 리스트 → 저장/조회 무결성")
    void create_empty_roles_roundtrip() throws IOException {
        User saved = repository.create(
                User.builder().name("빈역할").email("emptyrole@test.com").age(20)
                        .roles(Collections.emptyList()).build());
        assertThat(repository.findById(saved.getId()).get().getRoles()).isEmpty();
    }

    @Test
    @DisplayName("address = null → 저장/조회 예외 없음")
    void create_null_address_no_exception() throws IOException {
        User saved = repository.create(
                User.builder().name("주소없음").email("noaddr@test.com").age(20).address(null).build());
        assertThat(repository.findById(saved.getId()).get().getAddress()).isNull();
    }

    // ── 대량 데이터 ───────────────────────────────────────────

    @Test
    @DisplayName("1,000건 연속 create → 모두 저장, 마지막 ID = 1000")
    void bulk_create_1000_users_no_loss() throws IOException {
        for (int i = 1; i <= 1000; i++) {
            repository.create(User.builder()
                    .name("사용자" + i).email("user" + i + "@test.com").age(i % 100 + 1).build());
        }
        List<User> all = repository.findAll();
        assertThat(all).hasSize(1000);
        assertThat(all.get(999).getId()).isEqualTo(1000L);
    }

    @Test
    @DisplayName("1,000건 중 키워드 검색 → 정확한 부분집합 반환")
    void findByKeyword_on_large_dataset() throws IOException {
        for (int i = 1; i <= 100; i++) {
            repository.create(User.builder()
                    .name("일반유저" + i).email("user" + i + "@test.com").age(20).build());
        }
        for (int i = 1; i <= 10; i++) {
            repository.create(User.builder()
                    .name("관리자" + i).email("admin" + i + "@test.com").age(30).build());
        }
        assertThat(repository.findByKeyword("관리자")).hasSize(10);
        assertThat(repository.findByKeyword("admin")).hasSize(10);
        assertThat(repository.findByKeyword("일반")).hasSize(100);
    }

    // ── 동시성 경고 시나리오 ──────────────────────────────────

    @Test
    @DisplayName("동시 create 10개 → 데이터 손실 가능 [동시성 취약점 문서화]")
    void concurrent_create_may_lose_data_no_synchronization() throws InterruptedException {
        int THREAD_COUNT = 10;
        ExecutorService exec = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int idx = i;
            exec.submit(() -> {
                try {
                    repository.create(User.builder()
                            .name("동시사용자" + idx)
                            .email("concurrent" + idx + "@test.com")
                            .age(20).build());
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        exec.shutdown();

        int saved = repository.findAll().size();
        if (saved < THREAD_COUNT) {
            // 동시성 문제로 데이터 유실 발생 — 현재 구현에 synchronized 없음
            System.out.println("[CONCURRENCY WARNING] " + saved + "/" + THREAD_COUNT + "건만 저장됨 — 동시성 문제 확인 필요");
        }
        // 최소 1건은 반드시 저장
        assertThat(saved).isGreaterThan(0);
    }
}
