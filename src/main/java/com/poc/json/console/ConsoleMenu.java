package com.poc.json.console;

import com.poc.json.model.User;
import com.poc.json.repository.UserJsonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ConsoleMenu {

    private final UserJsonRepository repository;
    private final Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);
    private final PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);

    public void run() {
        out.println("\n=============================");
        out.println("   JSON CRUD 콘솔 애플리케이션");
        out.println("=============================");

        while (true) {
            printMenu();
            String choice = scanner.nextLine().trim();
            out.println();

            switch (choice) {
                case "1" -> handleReadAll();
                case "2" -> handleReadById();
                case "3" -> handleSearch();
                case "4" -> handleCreate();
                case "5" -> handleUpdate();
                case "6" -> handleDelete();
                case "0" -> {
                    out.println("종료합니다.");
                    return;
                }
                default -> out.println("[오류] 올바른 번호를 입력하세요.");
            }
        }
    }

    // ── 메뉴 ─────────────────────────────────────────────────

    private void printMenu() {
        out.println("\n-----------------------------");
        out.println(" 1. 전체 목록");
        out.println(" 2. ID로 검색");
        out.println(" 3. 이름/이메일 검색");
        out.println(" 4. 새 사용자 추가");
        out.println(" 5. 사용자 수정");
        out.println(" 6. 사용자 삭제");
        out.println(" 0. 종료");
        out.println("-----------------------------");
        out.print("선택 > ");
    }

    // ── Read ─────────────────────────────────────────────────

    private void handleReadAll() {
        List<User> users = repository.findAll();
        if (users.isEmpty()) {
            out.println("등록된 사용자가 없습니다.");
            return;
        }
        out.printf("총 %d명%n%n", users.size());
        users.forEach(this::printUserSummary);
    }

    private void handleReadById() {
        Long id = readLong("검색할 ID > ");
        if (id == null) return;

        repository.findById(id).ifPresentOrElse(
                this::printUserDetail,
                () -> out.println("[오류] ID " + id + " 사용자 없음")
        );
    }

    private void handleSearch() {
        out.print("검색어 (이름/이메일) > ");
        String keyword = scanner.nextLine().trim();
        if (keyword.isBlank()) return;

        List<User> results = repository.findByKeyword(keyword);
        if (results.isEmpty()) {
            out.println("검색 결과 없음: " + keyword);
            return;
        }
        out.printf("검색 결과: %d건%n%n", results.size());
        results.forEach(this::printUserSummary);
    }

    // ── Create ───────────────────────────────────────────────

    private void handleCreate() {
        out.println("[새 사용자 추가]");

        String name = readRequired("이름 > ");
        if (name == null) return;
        String email = readRequired("이메일 > ");
        if (email == null) return;
        Integer age = readInt("나이 > ");
        if (age == null) return;

        out.print("역할 (쉼표 구분, 예: ADMIN,USER) > ");
        String rolesInput = scanner.nextLine().trim();
        List<String> roles = Arrays.stream(rolesInput.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());

        out.println("[주소]");
        String city = readRequired("도시 > ");
        if (city == null) return;
        String country = readRequired("국가 코드 (예: KR) > ");
        if (country == null) return;
        String zipCode = readRequired("우편번호 > ");
        if (zipCode == null) return;

        User user = User.builder()
                .name(name)
                .email(email)
                .age(age)
                .roles(roles)
                .address(User.Address.builder()
                        .city(city)
                        .country(country)
                        .zipCode(zipCode)
                        .build())
                .build();

        try {
            User created = repository.create(user);
            out.println("\n✔ 추가 완료!");
            printUserDetail(created);
        } catch (IOException e) {
            out.println("[오류] 저장 실패: " + e.getMessage());
        }
    }

    // ── Update ───────────────────────────────────────────────

    private void handleUpdate() {
        Long id = readLong("수정할 사용자 ID > ");
        if (id == null) return;

        Optional<User> opt = repository.findById(id);
        if (opt.isEmpty()) {
            out.println("[오류] ID " + id + " 사용자 없음");
            return;
        }

        User user = opt.get();
        out.println("\n[현재 정보]");
        printUserDetail(user);
        out.println("\n수정할 항목 선택 (미입력 시 기존값 유지):");

        out.printf("이름 [%s] > ", user.getName());
        String name = scanner.nextLine().trim();
        if (!name.isBlank()) user.setName(name);

        out.printf("이메일 [%s] > ", user.getEmail());
        String email = scanner.nextLine().trim();
        if (!email.isBlank()) user.setEmail(email);

        out.printf("나이 [%d] > ", user.getAge());
        String ageStr = scanner.nextLine().trim();
        if (!ageStr.isBlank()) {
            try { user.setAge(Integer.parseInt(ageStr)); }
            catch (NumberFormatException e) { out.println("[경고] 나이 형식 오류, 기존값 유지"); }
        }

        out.printf("역할 [%s] (쉼표 구분) > ", String.join(",", user.getRoles()));
        String rolesInput = scanner.nextLine().trim();
        if (!rolesInput.isBlank()) {
            user.setRoles(Arrays.stream(rolesInput.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.toList()));
        }

        User.Address addr = user.getAddress() != null ? user.getAddress()
                : User.Address.builder().build();

        out.printf("도시 [%s] > ", addr.getCity());
        String city = scanner.nextLine().trim();
        if (!city.isBlank()) addr.setCity(city);

        out.printf("국가 [%s] > ", addr.getCountry());
        String country = scanner.nextLine().trim();
        if (!country.isBlank()) addr.setCountry(country);

        out.printf("우편번호 [%s] > ", addr.getZipCode());
        String zipCode = scanner.nextLine().trim();
        if (!zipCode.isBlank()) addr.setZipCode(zipCode);

        user.setAddress(addr);

        try {
            repository.update(user);
            out.println("\n✔ 수정 완료!");
            printUserDetail(user);
        } catch (IOException | NoSuchElementException e) {
            out.println("[오류] 수정 실패: " + e.getMessage());
        }
    }

    // ── Delete ───────────────────────────────────────────────

    private void handleDelete() {
        Long id = readLong("삭제할 사용자 ID > ");
        if (id == null) return;

        Optional<User> opt = repository.findById(id);
        if (opt.isEmpty()) {
            out.println("[오류] ID " + id + " 사용자 없음");
            return;
        }

        out.println("\n[삭제 대상]");
        printUserDetail(opt.get());
        out.print("\n정말 삭제하시겠습니까? (y/N) > ");
        String confirm = scanner.nextLine().trim();

        if (!confirm.equalsIgnoreCase("y")) {
            out.println("삭제 취소.");
            return;
        }

        try {
            repository.deleteById(id);
            out.println("✔ 삭제 완료: ID=" + id);
        } catch (IOException | NoSuchElementException e) {
            out.println("[오류] 삭제 실패: " + e.getMessage());
        }
    }

    // ── 출력 유틸 ─────────────────────────────────────────────

    private void printUserSummary(User u) {
        out.printf("  [%d] %s (%s) | 나이: %d | 역할: %s%n",
                u.getId(), u.getName(), u.getEmail(), u.getAge(),
                String.join(", ", u.getRoles() != null ? u.getRoles() : List.of()));
    }

    private void printUserDetail(User u) {
        out.println("  ┌─────────────────────────────");
        out.printf("  │ ID       : %d%n", u.getId());
        out.printf("  │ 이름     : %s%n", u.getName());
        out.printf("  │ 이메일   : %s%n", u.getEmail());
        out.printf("  │ 나이     : %d%n", u.getAge());
        out.printf("  │ 역할     : %s%n", u.getRoles() != null ? String.join(", ", u.getRoles()) : "");
        if (u.getAddress() != null) {
            out.printf("  │ 주소     : %s, %s (%s)%n",
                    u.getAddress().getCity(), u.getAddress().getCountry(), u.getAddress().getZipCode());
        }
        if (u.getCreatedAt() != null) {
            out.printf("  │ 생성일시 : %s%n", u.getCreatedAt());
        }
        out.println("  └─────────────────────────────");
    }

    // ── 입력 유틸 ─────────────────────────────────────────────

    private String readRequired(String prompt) {
        out.print(prompt);
        String value = scanner.nextLine().trim();
        if (value.isBlank()) {
            out.println("[오류] 필수 입력값입니다.");
            return null;
        }
        return value;
    }

    private Long readLong(String prompt) {
        out.print(prompt);
        try {
            return Long.parseLong(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            out.println("[오류] 숫자를 입력하세요.");
            return null;
        }
    }

    private Integer readInt(String prompt) {
        out.print(prompt);
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            out.println("[오류] 숫자를 입력하세요.");
            return null;
        }
    }
}
