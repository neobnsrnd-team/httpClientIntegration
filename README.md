# MyData HTTP Client Integration

## Goal

YAML 설정 기반으로 외부 시스템(은행, 카드사, 보험사, 지로 등)과의 HTTP 메시지 송수신을 자동 조립하는 **범용 HTTP 클라이언트 프레임워크**.

- 외부 시스템별로 **서로 다른 JSON 응답 형식**을 YAML 설정만으로 처리
- **응답 필드 매핑**: 외부 시스템의 약어/비표준 필드명을 내부 표준 필드명으로 자동 변환
- 거래코드 기반 호출: `bankClient.request("계좌목록조회", params)`
- URL 조립(Path Variable, Query Param), Request Body 조립, 응답 파싱을 자동 수행
- Spring Boot 3.2 + **RestClient** (Spring 6.1) + Apache HttpClient 5 커넥션 풀링

## Project Structure

```
httpClientIntegration/
├── pom.xml                     # Parent POM (Multi-module, 5 modules)
├── banking-server/             # Mock Banking REST API (port 8081)
├── card-server/                # Mock Card REST API (port 8082)
├── insurance-server/           # Mock Insurance REST API (port 8083)
├── giro-server/                # Mock GIRO REST API (port 8084)
└── mydata-client/              # Generic HTTP Message Client (port 8080)
    └── src/main/java/com/example/mydata/
        ├── client/
        │   ├── core/
        │   │   ├── MessageClient.java          # Core: YAML 기반 메시지 자동 조립 + 응답 매핑
        │   │   ├── GenericHttpClient.java       # RestClient wrapper
        │   │   ├── SystemProperties.java        # 시스템별 설정 (baseUrl, 응답필드 매핑)
        │   │   ├── MessageSpecProperties.java   # 거래별 설정 (method, path, params, responseMapping)
        │   │   └── ExternalSystemException.java # 비즈니스 에러 래핑
        │   ├── config/
        │   │   ├── ExternalSystemsProperties.java  # @ConfigurationProperties 바인딩
        │   │   └── HttpClientConfig.java           # RestClient + 커넥션 풀 설정
        │   ├── bank/
        │   │   ├── BankMessageClient.java       # 은행 전용 클라이언트
        │   │   └── BankClientConfig.java
        │   ├── card/
        │   │   ├── CardMessageClient.java       # 카드 전용 클라이언트
        │   │   └── CardClientConfig.java
        │   ├── insurance/
        │   │   ├── InsuranceMessageClient.java  # 보험 전용 클라이언트
        │   │   └── InsuranceClientConfig.java
        │   └── giro/
        │       ├── GiroMessageClient.java       # 지로 전용 클라이언트
        │       └── GiroClientConfig.java
        ├── service/MydataService.java           # 비즈니스 서비스
        ├── controller/
        │   ├── MydataController.java            # REST Controller
        │   └── GlobalExceptionHandler.java      # 전역 예외 처리
        └── dto/MydataResponse.java              # 표준 응답 래퍼
```

## External System Response Formats

| System | Success Code Field | Success Value | Error Message Field | Data Field | 특징 |
|--------|-------------------|---------------|-------------------- |------------|------|
| Banking | `result_code` | `0000` | `result_msg` | `data` | 표준 필드명 |
| Card | `status` | `SUCCESS` | `message` | `payload` | 표준 필드명 |
| Insurance | `code` | `00` | `msg` | `result` | 표준 필드명 |
| GIRO | `rsp_cd` | `000` | `rsp_msg` | `rsp_data` | **약어 필드명 → 응답 매핑 적용** |

## Response Field Mapping (신규 기능)

GIRO 시스템처럼 외부 API가 약어 필드명을 사용하는 경우, YAML의 `response-mapping` 설정으로 내부 표준 필드명으로 자동 변환합니다.

```
외부 응답: {"bill_no":"B001", "bill_nm":"전기요금", "pay_amt":50000}
       ↓ response-mapping 적용
내부 결과: {"billNumber":"B001", "billName":"전기요금", "paymentAmount":50000}
```

- **List 응답**: 각 항목(item)마다 매핑 적용
- **Map 응답**: 최상위 키에 매핑 적용
- **매핑에 없는 필드**: 원래 이름 유지 (pass-through)
- **매핑 미설정 시**: 기존 동작 유지 (하위호환)

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

**테스트 항목 (44건)**

| Test Class | 항목 | 건수 |
|-----------|------|------|
| `BankMessageClientTest` | 계좌목록조회, 이체, 거래내역조회 성공 + 에러(E001/E002/E003, 404/400/500, 미등록 거래코드, 경로변수 누락) | 11 |
| `CardMessageClientTest` | 보유카드목록조회, 결제예정금액조회 성공 + 에러(CARD_NOT_FOUND, CARD_CANCELLED, 404/400/500, 미등록 거래코드) | 9 |
| `InsuranceMessageClientTest` | 보험가입내역조회, 보험료납부 성공 + 에러(INS001/INS002/INS003, 404/400/500, 미등록 거래코드) | 9 |
| `GiroMessageClientTest` | 지로청구서목록조회, 지로납부 성공 + 에러(GIRO001/GIRO002/GIRO003, 404/400/500, 미등록 거래코드) + **응답 매핑 검증**(리스트 매핑, 맵 매핑, pass-through) | 12 |
| `PerformanceTest` | Banking 동시 100건, Card 동시 100건, Mixed 동시 200건 (Mock 기반) | 3 |

### 2. Server Start (Integration 테스트 사전 준비)

각 서버를 **별도 터미널**에서 실행합니다.

```powershell
# Terminal 1 - Banking Server (port 8081)
mvn spring-boot:run -pl banking-server

# Terminal 2 - Card Server (port 8082)
mvn spring-boot:run -pl card-server

# Terminal 3 - Insurance Server (port 8083)
mvn spring-boot:run -pl insurance-server

# Terminal 4 - GIRO Server (port 8084)
mvn spring-boot:run -pl giro-server

# Terminal 5 - MyData Client (port 8080)
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
```

**Insurance API (port 8083)**

```powershell
# 보험가입내역조회
Invoke-RestMethod "http://localhost:8083/api/insurance/policies?customerId=C001"

# 보험료납부 (정상)
Invoke-RestMethod http://localhost:8083/api/insurance/premium-payment `
  -Method POST -ContentType "application/json" `
  -Body '{"policyNo":"POL-2024-001","amount":150000}'

# 보험료납부 (에러 - 만료된 보험)
Invoke-RestMethod http://localhost:8083/api/insurance/premium-payment `
  -Method POST -ContentType "application/json" `
  -Body '{"policyNo":"POL-EXPIRED-001","amount":150000}'
```

**GIRO API (port 8084)**

```powershell
# 지로청구서목록조회
Invoke-RestMethod "http://localhost:8084/api/giro/bills?cust_id=C001"

# 지로납부 (정상)
Invoke-RestMethod http://localhost:8084/api/giro/payment `
  -Method POST -ContentType "application/json" `
  -Body '{"bill_no":"BILL-2024-001","pay_amt":50000}'

# 지로납부 (에러 - 청구서 미존재)
Invoke-RestMethod http://localhost:8084/api/giro/payment `
  -Method POST -ContentType "application/json" `
  -Body '{"bill_no":"BILL-9999-999","pay_amt":50000}'

# 지로납부 (에러 - 납부기한 만료)
Invoke-RestMethod http://localhost:8084/api/giro/payment `
  -Method POST -ContentType "application/json" `
  -Body '{"bill_no":"BILL-EXPIRED-001","pay_amt":50000}'
```

**MyData Client - GIRO 통합 호출 (port 8080, 응답 매핑 확인)**

```powershell
# 지로청구서목록조회 (매핑된 필드명: billNumber, billName, paymentAmount 등)
Invoke-RestMethod "http://localhost:8080/api/mydata/giro/bills?custId=C001"

# 지로납부 (매핑된 필드명: paymentNumber, billNumber, paymentAmount 등)
Invoke-RestMethod http://localhost:8080/api/mydata/giro/payment `
  -Method POST -ContentType "application/json" `
  -Body '{"billNo":"BILL-2024-001","amount":50000}'
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

  giro:
    base-url: http://localhost:8084
    success-code-field: rsp_cd
    success-code-value: "000"
    error-message-field: rsp_msg
    data-field: rsp_data
    messages:
      bill-list:
        transaction-code: 지로청구서목록조회
        method: GET
        path: /api/giro/bills
        query-params:
          cust_id: custId
        response-mapping:              # 응답 필드 매핑 (약어 → 표준)
          bill_no: billNumber
          bill_nm: billName
          pay_amt: paymentAmount
          due_dt: dueDate
          pay_st: paymentStatus
          org_nm: organizationName
      payment:
        transaction-code: 지로납부
        method: POST
        path: /api/giro/payment
        body-fields:
          bill_no: billNo
          pay_amt: amount
        response-mapping:
          pay_no: paymentNumber
          bill_no: billNumber
          pay_amt: paymentAmount
          pay_dt: paymentDate
          pay_st: paymentStatus
```

## Tech Stack

| Category | Technology |
|----------|-----------|
| Framework | Spring Boot 3.2.5 |
| HTTP Client | RestClient (Spring 6.1) + Apache HttpClient 5 |
| Connection Pool | 200 max total, 50 per route |
| Build | Maven Multi-module (5 modules) |
| Java | 17 |
| Test | JUnit 5, MockRestServiceServer |
