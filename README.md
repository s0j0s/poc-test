# Spring Boot JSON POC

Spring Boot + Jackson 기반 JSON 파일 CRUD 콘솔 애플리케이션 POC.

## 프로젝트 구조

```
src/main/java/com/poc/json/
├── JsonPocApplication.java       # 진입점 (CommandLineRunner)
├── config/
│   └── JacksonConfig.java        # ObjectMapper Bean 설정
├── model/
│   └── User.java                 # 도메인 모델 (Lombok @Data)
├── repository/
│   └── UserJsonRepository.java   # JSON 파일 기반 CRUD (in-memory cache + persist)
├── jackson/
│   └── JacksonJsonService.java   # JSON 직렬화/역직렬화 유틸
└── console/
    └── ConsoleMenu.java          # 콘솔 메뉴 UI
```

## 기술 스택

| 항목 | 내용 |
|---|---|
| Java | 17 |
| Spring Boot | 3.3.0 |
| Jackson | spring-boot-starter 내장 |
| Lombok | 보일러플레이트 제거 |
| 테스트 | JUnit 5 + AssertJ + JaCoCo |
| 빌드 | Gradle 8.7 |

## 실행

```bash
./gradlew bootRun
```

실행 시 콘솔 메뉴가 시작됩니다.

```
=============================
   JSON CRUD 콘솔 애플리케이션
=============================
1. 전체 조회
2. ID 조회
3. 키워드 검색
4. 생성
5. 수정
6. 삭제
0. 종료
```

## 설정

`src/main/resources/application.properties`

```properties
json.output.dir=./output       # Jackson 파일 저장 디렉토리
json.data.file=./data/users.json  # CRUD 데이터 파일 경로
```

## 테스트

```bash
./gradlew test                 # 테스트 실행
./gradlew test jacocoTestReport  # 커버리지 리포트 생성
```

리포트 위치: `build/reports/jacoco/test/html/index.html`

### 테스트 구성

| 파일 | 유형 | 설명 |
|---|---|---|
| `UserJsonRepositoryRegressionTest` | Regression | ID 생성·createdAt·findAll 불변성·예외 메시지 등 핵심 계약 고정 |
| `UserJsonRepositorySafetyTest` | Safety | null·극단값·XSS·JSON 인젝션·대량 데이터·동시성 취약점 문서화 |
| `JacksonJsonServiceTest` | 기능 | 직렬화·역직렬화·파일 저장/로드 |
| `JacksonJsonServiceSafetyTest` | Safety | 비정상 JSON·타입 불일치·미지 필드 동작 검증 |
| `UserModelTest` | 단위 | Lombok `@Data` equals·hashCode·toString 분기 커버 |

### 커버리지 현황

| 패키지 | 명령어 | 브랜치 |
|---|---|---|
| `repository` | 100% | 100% |
| `jackson` | 100% | 100% |
| `model` | 93% | 87% |
| `config` | 100% | - |
| `console` | 0% | 0% (인터랙티브 UI, 단위 테스트 제외) |
| **전체** | **50%** | **52%** |

## 데이터 모델

```json
{
  "id": 1,
  "name": "홍길동",
  "email": "hong@example.com",
  "age": 30,
  "createdAt": "2024-01-15T10:30:00",
  "roles": ["USER"],
  "address": {
    "city": "서울",
    "country": "KR",
    "zipCode": "04524"
  }
}
```

## 주요 특징 및 제약

- **ID 생성**: `max(id) + 1` 방식. 삭제 후 빈 ID 재사용 없음.
- **동시성**: `UserJsonRepository`는 thread-safe하지 않음. 동시 write 시 데이터 유실 가능.
- **유효성 검사**: 없음. 빈 이름·음수 나이·중복 이메일 모두 허용.
- **이메일 null**: `findByKeyword` 호출 시 NPE 발생 가능 (이름 미일치 키워드 검색 시).
