package com.poc.json.jackson;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.poc.json.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Jackson ObjectMapper를 사용한 JSON 파싱 및 파일 저장 서비스.
 * Spring Boot 기본 내장 라이브러리.
 */
@Slf4j
@Service
public class JacksonJsonService {

    private final ObjectMapper objectMapper;

    @Value("${json.output.dir}")
    private String outputDir;

    public JacksonJsonService() {
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(SerializationFeature.INDENT_OUTPUT); // pretty-print
    }

    /** JSON 문자열 → 객체 파싱 */
    public User parseUser(String json) throws IOException {
        return objectMapper.readValue(json, User.class);
    }

    /** JSON 문자열 → List<User> 파싱 */
    public List<User> parseUserList(String json) throws IOException {
        return objectMapper.readValue(json, new TypeReference<List<User>>() {});
    }

    /** JSON 문자열 → JsonNode (스키마 불명 시 동적 접근) */
    public JsonNode parseRawNode(String json) throws IOException {
        return objectMapper.readTree(json);
    }

    /** 객체 → JSON 문자열 직렬화 */
    public String toJson(Object obj) throws IOException {
        return objectMapper.writeValueAsString(obj);
    }

    /** 객체 → JSON 파일 저장 */
    public File saveToFile(Object obj, String fileName) throws IOException {
        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(dir, fileName);
        objectMapper.writeValue(file, obj);
        log.info("[Jackson] 파일 저장 완료: {}", file.getAbsolutePath());
        return file;
    }

    /** JSON 파일 → 객체 로드 */
    public User loadUserFromFile(String fileName) throws IOException {
        File file = new File(outputDir, fileName);
        return objectMapper.readValue(file, User.class);
    }

    /** JSON 파일 → List<User> 로드 */
    public List<User> loadUserListFromFile(String fileName) throws IOException {
        File file = new File(outputDir, fileName);
        return objectMapper.readValue(file, new TypeReference<List<User>>() {});
    }
}
