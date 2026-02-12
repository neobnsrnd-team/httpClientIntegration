# MyData HTTP Client Integration

## Goal

YAML 설정 기반으로 외부 시스템(은행, 카드사 등)과의 HTTP 메시지 송수신을 자동 조립하는 **범용 HTTP 클라이언트 프레임워크**.

- 외부 시스템별로 **서로 다른 JSON 응답 형식**을 YAML 설정만으로 처리
- 거래코드 기반 호출: `bankClient.request("계좌목록조회", params)`
- URL 조립(Path Variable, Query Param), Request Body 조립, 응답 파싱을 자동 수행
- Spring Boot 3.2 + **RestClient** (Spring 6.1) + Apache HttpClient 5 커넥션 풀링

## Project Structure

```
httpClient/
├── pom.xml                     # Parent POM (Multi-module)
├── banking-server/             # Mock Banking REST API (port 8081)
├── card-server/                # Mock Card REST API (port 8082)
└── mydata-client/              # Generic HTTP Message Client (port 8080)
    └── src/main/java/com/example/mydata/
        ├── client/
        │   ├── core/
        │   │   ├── MessageClient.java          # Core: YAML 기반 메시지 자동 조립
        │   │   ├── GenericHttpClient.java       # RestClient wrapper
        │   │   ├── SystemProperties.java        # 시스템별 설정 (baseUrl, 응답필드 매핑)
        │   │   ├── MessageSpecProperties.java   # 거래별 설정 (method, path, params)
        │   │   └── ExternalSystemException.java # 비즈니스 에러 래핑
        │   ├── config/
        │   │   ├── ExternalSystemsProperties.java  # @ConfigurationProperties 바인딩
        │   │   └── HttpClientConfig.java           # RestClient + 커넥션 풀 설정
        │   ├── bank/
        │   │   ├── BankMessageClient.java       # 은행 전용 클라이언트
        │   │   └── BankClientConfig.java
        │   └── card/
        │       ├── CardMessageClient.java       # 카드 전용 클라이언트
        │       └── CardClientConfig.java
        ├── service/MydataService.java           # 비즈니스 서비스
        └── controller/MydataController.java     # REST Controller
```

## External System Response Formats

| System | Success Code Field | Success Value | Error Message Field | Data Field |
|--------|-------------------|---------------|-------------------- |------------|
| Banking | `result_code` | `0000` | `result_msg` | `data` |
| Card | `status` | `SUCCESS` | `message` | `payload` |

## Prerequisites

- Java 17+
- Maven 3.8+
- (Optional) PowerShell 콘솔 UTF-8: `chcp 65001`

---

## Test Guide

### 1. Unit Test (서버 불필요)

MockRestServiceServer를 사용한 단위 테스트. 외부 서버 없이 실행 가능합니다.

```powershell
mvn test -pl mydata-client
```

**테스트 항목 (21건)**

| Test Class | 항목 | 건수 |
|-----------|------|------|
| `BankMessageClientTest` | 계좌목록조회, 이체, 거래내역조회 성공 + 에러(E001/E002/E003, 연결실패, 미등록 거래코드) | 11 |
| `CardMessageClientTest` | 보유카드목록조회, 결제예정금액조회 성공 + 에러(CARD_NOT_FOUND, CARD_CANCELLED, 연결실패, 미등록 거래코드) | 7 |
| `PerformanceTest` | Banking 동시 100건, Card 동시 100건, Mixed 동시 200건 (Mock 기반) | 3 |

### 2. Server Start (Integration 테스트 사전 준비)

각 서버를 **별도 터미널**에서 실행합니다.

```powershell
# Terminal 1 - Banking Server (port 8081)
mvn spring-boot:run -pl banking-server

# Terminal 2 - Card Server (port 8082)
mvn spring-boot:run -pl card-server

# Terminal 3 - MyData Client (port 8080) — optional
mvn spring-boot:run -pl mydata-client
```

### 3. API Manual Test (PowerShell)

서버가 기동된 상태에서 아래 명령으로 각 API를 테스트합니다.

**Banking API (port 8081)**

```powershell
# 계좌목록조회
Invoke-RestMethod http://localhost:8081/api/bank/accounts

# 이체 (정상)
Invoke-RestMethod http://localhost:8081/api/bank/transfer `
  -Method POST -ContentType "application/json" `
  -Body '{"fromAccountNo":"110-234-567890","toAccountNo":"110-987-654321","amount":10000}'

# 이체 (에러 - 계좌 미존재)
Invoke-RestMethod http://localhost:8081/api/bank/transfer `
  -Method POST -ContentType "application/json" `
  -Body '{"fromAccountNo":"999-999-999","toAccountNo":"110-987-654321","amount":10000}'

# 이체 (에러 - 한도 초과)
Invoke-RestMethod http://localhost:8081/api/bank/transfer `
  -Method POST -ContentType "application/json" `
  -Body '{"fromAccountNo":"110-234-567890","toAccountNo":"110-987-654321","amount":50000000}'

# 거래내역조회
Invoke-RestMethod "http://localhost:8081/api/bank/accounts/110-234-567890/transactions?fromDate=20240101&toDate=20241231"
```

**Card API (port 8082)**

```powershell
# 보유카드목록조회
Invoke-RestMethod http://localhost:8082/api/card/cards

# 결제예정금액조회 (정상)
Invoke-RestMethod http://localhost:8082/api/card/cards/1234-5678-9012-3456/scheduled-payments

# 결제예정금액조회 (에러 - 카드 미존재)
Invoke-RestMethod http://localhost:8082/api/card/cards/9999-9999-9999-9999/scheduled-payments

# 결제예정금액조회 (에러 - 해지된 카드)
Invoke-RestMethod http://localhost:8082/api/card/cards/0000-0000-0000-0000/scheduled-payments
```

### 4. Integration Performance Test (실서버 부하 테스트)

Banking Server(8081)와 Card Server(8082)가 **기동된 상태**에서 실행합니다.

```powershell
mvn test -pl mydata-client "-Dtest=IntegrationPerformanceTest" "-Dsurefire.excludedGroups="
```

> PowerShell에서 `-D` 옵션은 반드시 `"` 따옴표로 감싸야 합니다.

**테스트 시나리오 (7건)**

| Order | Test | Requests | Threads |
|-------|------|----------|---------|
| 0 | Warm-up | 1+1 | 1 |
| 1 | Banking - GetAccountList | 100 | 20 |
| 2 | Banking - Transfer | 100 | 20 |
| 3 | Banking - GetTransactions | 100 | 20 |
| 4 | Card - GetCardList | 100 | 20 |
| 5 | Card - GetScheduledPayment | 100 | 20 |
| 6 | Mixed (Banking + Card) | 200 | 30 |
| 7 | HighLoad Banking | 500 | 50 |

**출력 예시**

```
========================================
  Banking - GetAccountList
========================================
  Total Requests : 100
  Success        : 100
  Failure        : 0
  Wall Time      : 245 ms
  TPS            : 408.16 req/s
  Avg Latency    : 38.52 ms
  Min Latency    : 5 ms
  Max Latency    : 120 ms
  P50            : 32 ms
  P90            : 78 ms
  P95            : 95 ms
  P99            : 118 ms
========================================
```

### 5. Full Build

```powershell
mvn clean package
```

---

## Key Configuration (application.yml)

```yaml
external-systems:
  bank:
    base-url: http://localhost:8081
    success-code-field: result_code    # 응답 성공코드 필드명
    success-code-value: "0000"         # 성공 판별 값
    error-message-field: result_msg    # 에러 메시지 필드명
    data-field: data                   # 데이터 추출 필드명
    messages:
      account-list:
        transaction-code: 계좌목록조회   # 거래코드 (호출 시 사용)
        method: GET
        path: /api/bank/accounts
      transfer:
        transaction-code: 이체
        method: POST
        path: /api/bank/transfer
        body-fields:                   # POST body 매핑
          fromAccountNo: fromAccountNo
          toAccountNo: toAccountNo
          amount: amount
      account-transactions:
        transaction-code: 계좌거래내역조회
        method: GET
        path: /api/bank/accounts/{accountNo}/transactions
        path-variables:                # Path Variable
          - accountNo
        query-params:                  # Query Parameter
          fromDate: fromDate
          toDate: toDate
```

## Tech Stack

| Category | Technology |
|----------|-----------|
| Framework | Spring Boot 3.2.5 |
| HTTP Client | RestClient (Spring 6.1) + Apache HttpClient 5 |
| Connection Pool | 200 max total, 50 per route |
| Build | Maven Multi-module |
| Java | 17 |
| Test | JUnit 5, MockRestServiceServer |
