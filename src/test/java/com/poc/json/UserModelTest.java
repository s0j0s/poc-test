package com.poc.json;

import com.poc.json.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * User / Address Lombok @Data equals·hashCode·toString 커버리지.
 * @Data 생성 분기(null 체크, 타입 비교 등) 86개를 최대한 실행.
 */
@DisplayName("User 모델 equals / hashCode / toString")
class UserModelTest {

    // ── User equals ───────────────────────────────────────────

    @Test
    @DisplayName("동일 객체 → equal")
    void user_same_reference_equal() {
        User u = fullUser(1L, "가", "a@a.com");
        assertThat(u).isEqualTo(u);
    }

    @Test
    @DisplayName("모든 필드 동일 → equal")
    void user_all_fields_same_equal() {
        LocalDateTime now = LocalDateTime.of(2024, 1, 1, 0, 0);
        User u1 = User.builder().id(1L).name("가").email("a@a.com").age(25)
                .createdAt(now).roles(List.of("USER"))
                .address(User.Address.builder().city("서울").country("KR").zipCode("04524").build())
                .build();
        User u2 = User.builder().id(1L).name("가").email("a@a.com").age(25)
                .createdAt(now).roles(List.of("USER"))
                .address(User.Address.builder().city("서울").country("KR").zipCode("04524").build())
                .build();
        assertThat(u1).isEqualTo(u2);
        assertThat(u1.hashCode()).isEqualTo(u2.hashCode());
    }

    @Test
    @DisplayName("id 다름 → not equal")
    void user_different_id_not_equal() {
        User u1 = fullUser(1L, "가", "a@a.com");
        User u2 = fullUser(2L, "가", "a@a.com");
        assertThat(u1).isNotEqualTo(u2);
    }

    @Test
    @DisplayName("name 다름 → not equal")
    void user_different_name_not_equal() {
        User u1 = fullUser(1L, "가", "a@a.com");
        User u2 = fullUser(1L, "나", "a@a.com");
        assertThat(u1).isNotEqualTo(u2);
    }

    @Test
    @DisplayName("email 다름 → not equal")
    void user_different_email_not_equal() {
        User u1 = fullUser(1L, "가", "a@a.com");
        User u2 = fullUser(1L, "가", "b@b.com");
        assertThat(u1).isNotEqualTo(u2);
    }

    @Test
    @DisplayName("age 다름 → not equal")
    void user_different_age_not_equal() {
        User u1 = User.builder().id(1L).name("가").email("a@a.com").age(20).build();
        User u2 = User.builder().id(1L).name("가").email("a@a.com").age(30).build();
        assertThat(u1).isNotEqualTo(u2);
    }

    @Test
    @DisplayName("null 비교 → not equal")
    void user_not_equal_to_null() {
        assertThat(fullUser(1L, "가", "a@a.com")).isNotEqualTo(null);
    }

    @Test
    @DisplayName("다른 타입 비교 → not equal")
    void user_not_equal_to_different_type() {
        assertThat(fullUser(1L, "가", "a@a.com")).isNotEqualTo("string");
    }

    @Test
    @DisplayName("id=null 양쪽 동일 → equal (null 분기)")
    void user_both_id_null_equal() {
        User u1 = User.builder().name("가").email("a@a.com").age(20).build();
        User u2 = User.builder().name("가").email("a@a.com").age(20).build();
        assertThat(u1).isEqualTo(u2);
    }

    @Test
    @DisplayName("한쪽 id=null, 다른 쪽 non-null → not equal (null/non-null 분기)")
    void user_one_id_null_not_equal() {
        User u1 = User.builder().name("가").email("a@a.com").age(20).build(); // id=null
        User u2 = fullUser(1L, "가", "a@a.com");                              // id=1
        assertThat(u1).isNotEqualTo(u2);
    }

    @Test
    @DisplayName("name=null 양쪽 동일 → equal")
    void user_both_name_null_equal() {
        User u1 = User.builder().id(1L).email("a@a.com").age(20).build();
        User u2 = User.builder().id(1L).email("a@a.com").age(20).build();
        assertThat(u1).isEqualTo(u2);
    }

    @Test
    @DisplayName("한쪽 name=null, 다른 쪽 non-null → not equal")
    void user_one_name_null_not_equal() {
        User u1 = User.builder().id(1L).name(null).email("a@a.com").age(20).build();
        User u2 = User.builder().id(1L).name("가").email("a@a.com").age(20).build();
        assertThat(u1).isNotEqualTo(u2);
    }

    @Test
    @DisplayName("roles=null 양쪽 동일 → equal")
    void user_both_roles_null_equal() {
        User u1 = User.builder().id(1L).name("가").email("a@a.com").age(20).roles(null).build();
        User u2 = User.builder().id(1L).name("가").email("a@a.com").age(20).roles(null).build();
        assertThat(u1).isEqualTo(u2);
    }

    @Test
    @DisplayName("한쪽 roles=null, 다른 쪽 non-null → not equal")
    void user_one_roles_null_not_equal() {
        User u1 = User.builder().id(1L).name("가").email("a@a.com").age(20).roles(null).build();
        User u2 = User.builder().id(1L).name("가").email("a@a.com").age(20).roles(List.of("USER")).build();
        assertThat(u1).isNotEqualTo(u2);
    }

    @Test
    @DisplayName("address=null 양쪽 동일 → equal")
    void user_both_address_null_equal() {
        User u1 = User.builder().id(1L).name("가").email("a@a.com").age(20).address(null).build();
        User u2 = User.builder().id(1L).name("가").email("a@a.com").age(20).address(null).build();
        assertThat(u1).isEqualTo(u2);
    }

    @Test
    @DisplayName("한쪽 address=null, 다른 쪽 non-null → not equal")
    void user_one_address_null_not_equal() {
        User u1 = User.builder().id(1L).name("가").email("a@a.com").age(20).address(null).build();
        User u2 = User.builder().id(1L).name("가").email("a@a.com").age(20)
                .address(User.Address.builder().city("서울").build()).build();
        assertThat(u1).isNotEqualTo(u2);
    }

    @Test
    @DisplayName("createdAt 다름 → not equal")
    void user_different_createdAt_not_equal() {
        User u1 = User.builder().id(1L).name("가").email("a@a.com").age(20)
                .createdAt(LocalDateTime.of(2024, 1, 1, 0, 0)).build();
        User u2 = User.builder().id(1L).name("가").email("a@a.com").age(20)
                .createdAt(LocalDateTime.of(2025, 1, 1, 0, 0)).build();
        assertThat(u1).isNotEqualTo(u2);
    }

    // ── User hashCode ─────────────────────────────────────────

    @Test
    @DisplayName("hashCode 일관성 — 같은 객체 반복 호출 동일")
    void user_hashcode_consistent() {
        User u = fullUser(1L, "가", "a@a.com");
        assertThat(u.hashCode()).isEqualTo(u.hashCode());
    }

    @Test
    @DisplayName("hashCode — 모든 필드 null 객체 예외 없이 계산")
    void user_all_null_hashcode_no_exception() {
        User u = new User();
        assertThat(u.hashCode()).isNotNull(); // 예외 없이 계산됨
    }

    // ── User toString ─────────────────────────────────────────

    @Test
    @DisplayName("toString — 주요 필드 포함")
    void user_toString_contains_key_fields() {
        User u = fullUser(1L, "홍길동", "hong@test.com");
        String str = u.toString();
        assertThat(str).contains("id=1").contains("name=홍길동").contains("email=hong@test.com");
    }

    @Test
    @DisplayName("toString — null 필드 포함 예외 없음")
    void user_toString_with_null_fields_no_exception() {
        User u = new User();
        assertThat(u.toString()).isNotNull();
    }

    // ── Address equals ────────────────────────────────────────

    @Nested
    @DisplayName("Address")
    class AddressTest {

        @Test
        @DisplayName("동일 객체 → equal")
        void address_same_reference_equal() {
            User.Address a = address("서울", "KR", "04524");
            assertThat(a).isEqualTo(a);
        }

        @Test
        @DisplayName("모든 필드 동일 → equal")
        void address_all_fields_same_equal() {
            User.Address a1 = address("서울", "KR", "04524");
            User.Address a2 = address("서울", "KR", "04524");
            assertThat(a1).isEqualTo(a2);
            assertThat(a1.hashCode()).isEqualTo(a2.hashCode());
        }

        @Test
        @DisplayName("city 다름 → not equal")
        void address_different_city_not_equal() {
            assertThat(address("서울", "KR", "04524")).isNotEqualTo(address("부산", "KR", "04524"));
        }

        @Test
        @DisplayName("country 다름 → not equal")
        void address_different_country_not_equal() {
            assertThat(address("서울", "KR", "04524")).isNotEqualTo(address("서울", "US", "04524"));
        }

        @Test
        @DisplayName("zipCode 다름 → not equal")
        void address_different_zipcode_not_equal() {
            assertThat(address("서울", "KR", "04524")).isNotEqualTo(address("서울", "KR", "99999"));
        }

        @Test
        @DisplayName("null 비교 → not equal")
        void address_not_equal_to_null() {
            assertThat(address("서울", "KR", "04524")).isNotEqualTo(null);
        }

        @Test
        @DisplayName("다른 타입 → not equal")
        void address_not_equal_to_different_type() {
            assertThat(address("서울", "KR", "04524")).isNotEqualTo("string");
        }

        @Test
        @DisplayName("city=null 양쪽 동일 → equal")
        void address_both_city_null_equal() {
            User.Address a1 = User.Address.builder().country("KR").zipCode("04524").build();
            User.Address a2 = User.Address.builder().country("KR").zipCode("04524").build();
            assertThat(a1).isEqualTo(a2);
        }

        @Test
        @DisplayName("한쪽 city=null, 다른 쪽 non-null → not equal")
        void address_one_city_null_not_equal() {
            User.Address a1 = User.Address.builder().city(null).country("KR").zipCode("04524").build();
            User.Address a2 = User.Address.builder().city("서울").country("KR").zipCode("04524").build();
            assertThat(a1).isNotEqualTo(a2);
        }

        @Test
        @DisplayName("country=null 양쪽 동일 → equal")
        void address_both_country_null_equal() {
            User.Address a1 = User.Address.builder().city("서울").zipCode("04524").build();
            User.Address a2 = User.Address.builder().city("서울").zipCode("04524").build();
            assertThat(a1).isEqualTo(a2);
        }

        @Test
        @DisplayName("zipCode=null 양쪽 동일 → equal")
        void address_both_zipcode_null_equal() {
            User.Address a1 = User.Address.builder().city("서울").country("KR").build();
            User.Address a2 = User.Address.builder().city("서울").country("KR").build();
            assertThat(a1).isEqualTo(a2);
        }

        @Test
        @DisplayName("모든 필드 null → equal")
        void address_all_null_equal() {
            assertThat(new User.Address()).isEqualTo(new User.Address());
        }

        @Test
        @DisplayName("hashCode — 모든 필드 null 예외 없음")
        void address_all_null_hashcode_no_exception() {
            assertThat(new User.Address().hashCode()).isNotNull();
        }

        @Test
        @DisplayName("toString — 주요 필드 포함")
        void address_toString_contains_fields() {
            String str = address("서울", "KR", "04524").toString();
            assertThat(str).contains("city=서울").contains("country=KR").contains("zipCode=04524");
        }
    }

    // ── Helper ────────────────────────────────────────────────

    private User fullUser(Long id, String name, String email) {
        return User.builder().id(id).name(name).email(email).age(25)
                .createdAt(LocalDateTime.of(2024, 1, 1, 0, 0))
                .roles(List.of("USER"))
                .address(address("서울", "KR", "04524"))
                .build();
    }

    private User.Address address(String city, String country, String zipCode) {
        return User.Address.builder().city(city).country(country).zipCode(zipCode).build();
    }
}
