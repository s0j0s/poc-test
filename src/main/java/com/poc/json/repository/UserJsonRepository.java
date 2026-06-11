package com.poc.json.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poc.json.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Slf4j
@Repository
public class UserJsonRepository {

    private final ObjectMapper objectMapper;
    private final Path filePath;
    private List<User> cache;

    public UserJsonRepository(ObjectMapper objectMapper,
                               @Value("${json.data.file}") String filePath) {
        this.objectMapper = objectMapper;
        this.filePath = Path.of(filePath);
    }

    @PostConstruct
    public void load() throws IOException {
        if (Files.exists(filePath)) {
            cache = objectMapper.readValue(filePath.toFile(), new TypeReference<>() {});
            log.info("데이터 로드 완료: {}건 ({})", cache.size(), filePath);
        } else {
            cache = new ArrayList<>();
            log.info("새 데이터 파일 생성 예정: {}", filePath);
        }
    }

    private void persist() throws IOException {
        Files.createDirectories(filePath.getParent());
        objectMapper.writeValue(filePath.toFile(), cache);
    }

    // ── Read ─────────────────────────────────────────────────

    public List<User> findAll() {
        return Collections.unmodifiableList(cache);
    }

    public Optional<User> findById(Long id) {
        return cache.stream()
                .filter(u -> u.getId().equals(id))
                .findFirst();
    }

    public List<User> findByKeyword(String keyword) {
        String kw = keyword.toLowerCase();
        return cache.stream()
                .filter(u -> u.getName().toLowerCase().contains(kw)
                        || u.getEmail().toLowerCase().contains(kw))
                .toList();
    }

    // ── Create ───────────────────────────────────────────────

    public User create(User user) throws IOException {
        long nextId = cache.stream().mapToLong(User::getId).max().orElse(0L) + 1;
        user.setId(nextId);
        user.setCreatedAt(LocalDateTime.now());
        cache.add(user);
        persist();
        log.info("생성 완료: ID={}, name={}", user.getId(), user.getName());
        return user;
    }

    // ── Update ───────────────────────────────────────────────

    public User update(User updated) throws IOException {
        for (int i = 0; i < cache.size(); i++) {
            if (cache.get(i).getId().equals(updated.getId())) {
                cache.set(i, updated);
                persist();
                log.info("수정 완료: ID={}", updated.getId());
                return updated;
            }
        }
        throw new NoSuchElementException("ID " + updated.getId() + " 없음");
    }

    // ── Delete ───────────────────────────────────────────────

    public void deleteById(Long id) throws IOException {
        boolean removed = cache.removeIf(u -> u.getId().equals(id));
        if (!removed) {
            throw new NoSuchElementException("ID " + id + " 없음");
        }
        persist();
        log.info("삭제 완료: ID={}", id);
    }
}
