# CAA Collector Service - TODO

> **현재 Phase 2의 단기 작업 목록 및 우선순위 관리**

---

## Header

- **Last Updated**: 2026-02-07 (금)
- **Current Focus**: Phase 2 Week 2 진행 중 (30%)
- **Next Sprint**: 2026-02-09 (월) ~ 2026-02-15 (일)
- **Priority**: 배포 자동화 (Docker Hub, CI/CD) → MA-01 재시도 로직

---

## ⚠️ 최신 완료 항목

### 📝 Phase 2 문서 동기화 (2026-02-07)
- [x] **코드 변경사항 문서 반영**
  - ADR README 업데이트 (ADR-0018, 0019 추가)
  - TECHSPEC.md 보안 섹션 추가 (TokenEncryptor, RedisKeyHasher, LogMaskingUtil)
  - TECHSPEC.md Package Structure 업데이트 (common/constant, common/util, watchlist/mapper)
  - TECHSPEC.md Database Schema 업데이트 (WatchlistStock Index 추가)
  - TECHSPEC.md Testing Strategy 업데이트 (Testcontainers, Security 테스트)
  - CLAUDE.md Architecture 섹션 업데이트
  - 8e601e2a 커밋 이후 45개 파일 변경사항 완전 반영

### 🔒 보안 강화 인프라 구축 (2026-02-02 ~ 2026-02-07)
- [x] **TokenEncryptor 구현** (AES-256-GCM)
  - Redis 토큰 암호화/복호화
  - IV 자동 생성 및 무결성 검증
- [x] **RedisKeyHasher 구현** (SHA-256)
  - 계정번호 해싱으로 Redis 키 보호
  - 솔트 기반 무지개 테이블 공격 방어
- [x] **LogMaskingUtil 구현**
  - 사용자 ID, 계정번호, 토큰 마스킹
  - KisAuthService, KisWatchlistService 로그 적용
- [x] **DateFormatConstants 추가**
  - KST_ZONE_ID 타임존 상수화
  - 날짜 포맷 패턴 중앙 관리
- [x] **WatchlistMapper 추가**
  - API DTO → Entity 변환 로직 분리
  - 코드 재사용성 및 가독성 향상
- [x] **ADR-0018, 0019 작성**
  - Dockerfile Shell Injection 완화 전략
  - Watchlist API 재시도 전략 결정 기록

### 🔒 C-01 Critical 보안 이슈 해결 (2026-02-01)

### 🔒 C-01 Critical 보안 이슈 해결 (완료)
- [x] **민감 자격 증명 평문 저장 문제 해결**
  - security-auditor, backend-security-coder 보안 검토 완료
  - 통합 보안 조치 계획 문서 생성: `docs/security/security-action-plan-2026-02-01.md`
  - .env.prod 통합 파일 생성 및 NAS 전송 완료
  - 환경변수 계층별 구분 원칙 채택 (인프라 vs 애플리케이션)
  - Dockerfile HEALTHCHECK 경로 수정 (`/internal/management/health`)
  - KisAccountProperties, KisProperties toString() 마스킹 추가
  - MSA 루트 `.gitignore` 업데이트 (secrets/, data/ 제외)
  - NAS 보호 디렉토리 권한 600 설정 완료
  - 문서 업데이트: DEPLOYMENT.md, TODO.md, MILESTONE.md, README.md

---

## Priority 0 (P0) - Critical
> **Week 1: 2026-01-26 (월) ~ 02-01 (일) - 11시간**

### 📝 프로젝트 문서화 (3.5시간, 완료)
- [x] `README.md` 업데이트 (30분)
  - ✅ 현재 구현 상태 업데이트 (Phase 2 Week 1 완료 반영)
  - ✅ Directory Structure 업데이트 (strategy 패키지 추가)
  - ✅ Key Features 업데이트 (관심종목 동기화 개선)
- [x] `docs/MILESTONE.md` 업데이트 (40분)
  - ✅ Phase 1 진행률 업데이트 (95% → 100%)
  - ✅ Phase 2 Week 1 진행률 업데이트 (70% → 100%)
  - ✅ 관심종목 편집 반영 완료 체크
- [x] `docs/TODO.md` 업데이트 (20분)
  - ✅ P0/P1/P2 우선순위 분류
  - ✅ Week별 작업 목록
  - ✅ 예상 시간
- [x] `docs/PRD.md` 업데이트 (50분)
  - ✅ Executive Summary
  - ✅ User Stories (5가지 시나리오, Scenario 2 완료 반영)
  - ✅ Functional Requirements (FR-1~4, 주문 실행 포함)
  - ✅ Non-Functional Requirements (NFR-1~4)
  - ✅ Constraints, Success Metrics
- [x] `docs/TECHSPEC.md` 업데이트 (60분)
  - ✅ System Architecture 다이어그램
  - ✅ Database Schema (기존 + InvestmentDecision DDL)
  - ✅ API Specifications (KIS, AI Advisor, Notifier)
  - ✅ Scheduler 명세 (Strategy Pattern 적용)
  - ✅ WatchlistService 3-Way Sync 구현 상세
  - ✅ Error Handling & Retry 전략
  - ✅ Configuration 예시


---

## 📋 스케줄 아키텍처 토론 결과 (2026-02-04)

> **현황**: PM + Stock-market-expert 토론 완료
> **결정**: Phase 2 배포 진행 (변경 없음), Phase 3 시작 전 리팩터링 (3시간)
>
> 📚 **상세 내용**: [ADR-0016: 글로벌 주식 시장 스케줄 아키텍처](adr/0016-global-market-schedule-architecture.md)

### 🎯 결론 요약

**Phase 2 (즉시 배포)**:
- 현재 스케줄 유지 (03:00 백필, 18:30 한국 일간)
- 배포 인프라 작업 최우선 (44시간 대기)

**Phase 3 시작 전 (2026-02-22까지, 3시간 리팩터링)**:
- 미국 스케줄 추가: `07:00 KST (TUE-SAT)` (DST 안전 시간)
- 시장별 설정 분리 (`MarketScheduleConfig`)
- 정적 휴장일 캘린더 구현

**최종 스케줄 구조**:
```
03:00 KST: 백필 + 유럽 (향후)
07:00 KST: 미국 (NYSE/NASDAQ)
18:30 KST: 한국/아시아 (KRX, TSE, HKEX)
```

### 📌 주요 기술 결정

| 결정 사항 | 이유 |
|----------|------|
| Phase 2 변경 없음 | 배포 인프라 일정 준수 (진행률 24%) |
| 07:00 KST (미국) | 표준시/서머타임 모두 안전 (±1~2시간 버퍼) |
| 점진적 리팩터링 | 기술 부채 분산, Phase 3 WebSocket 연계 |
| 정적 휴장일 | YAML 관리, Phase 4에서 API 연동 검토 |

자세한 배경, 대안 분석, 구현 계획은 **[ADR-0016](adr/0016-global-market-schedule-architecture.md)** 참조.

---

## Priority 1 (P1) - Important
> **Week 2-3: 2026-01-28 (화) ~ 02-22 (일) - 58시간**

### 🔴 Critical 이슈 (배포 전 필수, 3.5시간)

- [ ] **CR-02**: StockPriceCollectionService 예외 처리 테스트 (1.5시간)
- [ ] **CR-03**: KisAuthService 동시성 테스트 (1.5시간)
- [x] **MA-11**: KisTokenResponse toString() 마스킹 ✅
- [x] **MA-14 + MA-18**: AbstractBackfillStrategy 제네릭화 ✅
- [ ] **MA-19**: "Asia/Seoul" 타임존 상수화 (30분)
- [ ] **MA-09**: Dockerfile CMD 쉘 인젝션 검토 (30분)

### 🚀 MA-01: N+1 API 호출 패턴 (9시간)

**결정 (2026-02-06)**: Option B (재시도 로직) 채택
- 📚 상세: [ADR-0019](adr/0019-watchlist-api-retry-strategy.md)
- 일정: 2026-02-09 ~ 02-15
- 후속: Phase 3에서 병렬화 재검토 (그룹 20개 달성 시)

- [ ] WatchlistService 재시도 로직 구현 (7시간)
- [x] ADR-0019 작성 ✅

### 🔒 Spring Security 도입 (5시간, HIGH 우선순위) ✅ 완료

> **⚠️ C-01 Critical 이슈 해결 완료** (2026-02-01)

#### 1️⃣ 의존성 추가 (완료)
- [x] `build.gradle`에 Spring Security 의존성 추가
  - ✅ `spring-boot-starter-security`
  - ✅ `spring-security-test` (테스트용)

#### 2️⃣ Security Configuration 구현 (완료)
- [x] `SecurityConfig.java` 생성
  - ✅ Actuator 엔드포인트 보호 (`/actuator/**`)
  - ✅ HTTP Basic Authentication
  - ✅ ROLE_ACTUATOR 인증 필요
  - ✅ CSRF 설정 완료

#### 3️⃣ Actuator 보호 (완료)
- [x] `application.yml` 설정
  - ✅ `management.endpoints.web.exposure.include: health,info`
  - ✅ `/actuator/health` public 접근 허용
  - ✅ `/actuator/env`, `/actuator/configprops` 비활성화
  - ✅ 나머지 엔드포인트는 인증 필요

#### 4️⃣ 테스트 작성 및 검증 (완료)
- [x] SecurityConfig 테스트 (2026-02-02)
  - ✅ Actuator 엔드포인트 인증 테스트
  - ✅ Health Check public 접근 테스트
  - ✅ 보안 헤더 검증 (X-Frame-Options: DENY, CSP, HSTS)
  - ✅ 자격 증명 검증 테스트
- [x] 통합 테스트
  - ✅ TestRestTemplate 기반 완전한 통합 테스트
  - ✅ 전체 보안 설정 검증

#### 5️⃣ 문서화 및 리뷰 (완료)
- [x] ADR-0012 작성
  - ✅ Spring Security 도입 배경
  - ✅ Actuator 보호 전략
  - ✅ 인증 방식 선택 이유 (HTTP Basic)
- [x] DEPLOYMENT.md 업데이트
  - ✅ secrets/.env.prod 경로 반영
  - ✅ 환경변수 보안 섹션 업데이트
  - ✅ docker-compose --env-file 명령어 수정

**예상 시간**: 5시간 | **진행률**: 90% (4.5시간 완료)

---

### 🐳 배포 자동화 전체 (44시간)

> **범위**: MSA 공통 인프라 (Collector 우선 적용)
> - Docker Compose로 Collector + MySQL + Redis 통합 배포
> - GitHub Actions는 MSA 루트에 워크플로우 생성
> - 향후 서비스 추가 시 `docker-compose.yml` 확장
>
> **⚠️ 관련 결정**: [ADR-0017: Database Migration Strategy](adr/0017-database-migration-strategy.md)
> - Flyway 도입은 Phase 3 종료 시 (2026-03-01) 재평가
> - 현재는 JPA @Index + 수동 SQL로 운영

#### 0️⃣ CI/CD 보안 스캔 설정 (2시간, CRITICAL)

> **⚠️ 우선순위**: 배포 자동화 완료 전 필수 조건
>
> **배경**: OWASP dependency-check를 build.gradle에서 제거함 (NVD API CVSSv4 파싱 버그 Issue #7409로 인해 로컬 빌드에서 정상 작동 불가). CI/CD 파이프라인에서 업계 표준인 Dependabot + Trivy로 대체.

- [ ] GitHub Dependabot 활성화 (0.5시간)
  - `.github/dependabot.yml` 설정 파일 생성
  - Gradle 의존성 스캔 설정
  - 주간 PR 자동 생성 설정
- [ ] GitHub Actions에 Trivy 스캔 추가 (1시간)
  - 빌드 워크플로우에 Trivy 단계 추가
  - Docker 이미지 취약점 스캔
  - 의존성 취약점 스캔 (Gradle)
  - CVSS 7.0 이상 시 빌드 실패
- [ ] ADR-0014 작성 (0.5시간)
  - 보안 스캔 전략 변경 결정 기록
  - OWASP Dependency-Check → Dependabot + Trivy

**예상 시간**: 2시간

#### 0️⃣ NAS Private Registry 구축 (6시간)

> **⚠️ 우선순위**: 컨테이너화 작업 전 필수 (이미지 보호)
>
> **배경**: GitHub Repository는 Public (포트폴리오용), Docker 이미지는 NAS Private Registry에 저장 (환경변수, 설정 보호)
>
> 📚 **상세 가이드**: [DEPLOYMENT.md - NAS Private Registry](../../docs/DEPLOYMENT.md#nas-private-registry-구축)

- [ ] Docker Registry 컨테이너 설치 (2시간)
- [ ] Registry 인증 설정 (Basic Auth) (1.5시간)
- [ ] TLS 인증서 설정 (자체 서명) (1.5시간)
- [ ] 이미지 Push/Pull 테스트 (1시간)

**예상 시간**: 6시간

#### 1️⃣ 컨테이너화 (8시간)
- [ ] Dockerfile 작성 (Multi-stage build)
  - Spring Boot 최적화 (JAR 레이어 분리)
  - 레이어 캐싱 전략 (의존성 → 애플리케이션)
  - JRE 경량화 (eclipse-temurin:21-jre-alpine)
- [x] Docker Compose 구성
  - ✅ MySQL 8.0 컨테이너
  - ✅ Redis 7.0 컨테이너
  - ✅ Collector 서비스 컨테이너
  - ✅ 네트워크 구성 (bridge)
  - ✅ 볼륨 마운트 (데이터 영속성)
- [x] 환경 변수 설정
  - ✅ `.env.example` 파일 생성 (템플릿)
  - ✅ KIS API 인증 정보 (APP_KEY, APP_SECRET)
  - ✅ DB 연결 정보 (URL, USERNAME, PASSWORD)
  - ✅ Redis 연결 정보
  - ✅ 볼륨 경로 플레이스홀더 (VOLUME_SSD_BASE, VOLUME_HDD_BASE)
- [ ] **Nginx 리버스 프록시 + TLS 설정** (ADR-0015)
  - Docker 네트워크 격리 (`internal: true`)
  - 자체 서명 TLS 인증서 생성 (`generate-cert.sh`)
  - Nginx 설정 파일 작성 (`nginx.conf`)
  - HTTP → HTTPS 리디렉트 설정
  - `forward-headers-strategy: native` 설정 (`application-prod.yml`)
- [ ] 로컬 테스트 및 디버깅
  - `docker-compose up` 전체 스택 실행
  - 컨테이너 간 통신 검증
  - 스케줄러 동작 확인
  - HTTPS 접근 테스트 (`https://localhost/actuator/health`)
  - HTTP → HTTPS 리디렉트 검증

#### 2️⃣ CI/CD 파이프라인 (9시간)
- [ ] GitHub Actions 워크플로우 작성
  - 빌드 단계: `./gradlew build -q`
  - 테스트 단계: `./gradlew test -q`
  - 이미지 빌드 단계: `docker build`
  - 이미지 푸시 단계: `docker push`
- [ ] Docker Registry 설정
  - GitHub Container Registry (ghcr.io) 사용
  - `GITHUB_TOKEN` 인증
  - 이미지 태그 전략 (latest, version)
- [ ] 자동 버전 태깅
  - Semantic Versioning (v1.0.0)
  - Git Tag 연동 (`git tag -a v1.0.0 -m "..."`)
  - 태그 푸시 트리거
- [ ] 빌드 성공/실패 알림
  - Discord/Slack 웹훅 연동
  - 빌드 상태 메시지 전송
- [ ] 통합 테스트 및 디버깅
  - 워크플로우 실행 검증
  - 에러 핸들링 확인

#### 3️⃣ 자동 배포 (10시간)
- [ ] Watchtower 설정 및 테스트
  - Watchtower 컨테이너 추가
  - 이미지 갱신 감지 (Poll interval: 300s)
  - 자동 컨테이너 재시작
- [ ] NAS 환경 배포 스크립트 작성
  - 초기 환경 설정 스크립트
  - 시크릿 관리 (.env 안전 관리)
  - 네트워크 구성 스크립트
- [ ] Health Check 엔드포인트 구현
  - `/actuator/health` 확장
  - `/actuator/readiness` 구현
  - KIS API 연결 상태 체크
  - Redis/MySQL 연결 상태 체크
- [ ] 롤백 전략 설계 및 테스트
  - 이전 이미지 버전 복구 프로세스
  - 배포 실패 시 자동 롤백
  - 롤백 시나리오 테스트

#### 4️⃣ 모니터링 및 관리 (10시간)
- [ ] Portainer 연동 및 대시보드 구성
  - 컨테이너 상태 시각화
  - 로그 뷰어 설정
  - 컨테이너 재시작 관리
- [ ] 로그 수집 전략 구현
  - JSON 포맷 로그 출력 (Logback 설정)
  - 로그 레벨 표준화 (INFO, WARN, ERROR)
  - 컨테이너 로그 수집 (`docker logs`)
- [ ] 리소스 제한 설정
  - NAS 환경 (8GB RAM) 최적화
  - CPU 제한 (cpus: 2.0)
  - 메모리 제한 (mem_limit: 2G)
- [ ] 백업 및 복구 프로세스
  - MySQL 백업 스크립트 (mysqldump)
  - 볼륨 백업 전략 (docker volume)
  - 복구 테스트 (restore 검증)

#### 5️⃣ 문서화 (7시간)
- [ ] DEPLOYMENT.md 작성
  - 배포 가이드 (단계별 설명)
  - 환경 변수 설명 (필수/선택)
  - 트러블슈팅 FAQ
- [ ] ADR-0012-deployment-automation.md
  - 배포 전략 결정 배경
  - Docker vs K8s 비교
  - Watchtower 선택 이유
- [ ] 로컬 개발 환경 가이드
  - Docker Compose로 전체 스택 실행
  - 개발 모드 vs 운영 모드 차이
  - 컨테이너 디버깅 팁

#### 6️⃣ Phase 2 마무리 체크리스트
- [ ] Flyway 도입 재평가 (Phase 3 종료 시, 2026-03-01)
  - ADR-0017 참조
  - WebSocket 도입 후 스키마 변경 빈도 확인
  - 월 2회 이상 변경 시 Flyway 도입 고려

---

## Priority 2 (P2) - Nice to Have
> **Week 3-4: 2026-02-09 (월) ~ 02-22 (일) - 42시간**

### 📊 InvestmentDecision 엔티티 설계 및 구현 (4시간)
- [ ] Entity 설계 (2시간)
  - 컬럼 정의:
    - `id` (BIGINT, PK)
    - `stock_code` (VARCHAR, 종목 코드)
    - `trade_date` (DATE, 거래일)
    - `decision_type` (VARCHAR, BUY/SELL/HOLD)
    - `confidence_score` (DECIMAL, 신뢰도)
    - `predicted_price` (DECIMAL, 예측 가격)
    - `ai_model_version` (VARCHAR, AI 모델 버전)
    - `created_at`, `updated_at`
  - Enum 클래스: `InvestmentState` (BUY, SELL, HOLD, UNKNOWN)
  - DDL 작성
- [ ] Repository 구현 (1시간)
  - 최신 상태 조회 메서드
  - 날짜 범위 조회 메서드
- [ ] 테스트 작성 (1시간)

### 🤖 AI Advisor Client 구현 (6시간)
- [ ] Interface 정의 (0.5시간)
  - `AdvisorClient` Interface
  - `TrainRequest`, `TrainResponse` record
  - `PredictRequest`, `PredictResponse` record
- [ ] RestClient 구현 (2.5시간)
  - POST `/v1/train` 호출
  - POST `/v1/predict` 호출
  - Timeout 설정 (30초)
  - 재시도 로직 (지수 백오프: 1s, 2s, 4s)
- [ ] 설정 및 RateLimiter (1시간)
  - `AdvisorProperties` (base-url, timeout)
  - RateLimiter 설정 (10/s)
- [ ] 에러 핸들링 (1시간)
  - Timeout → UNKNOWN 상태 반환
  - 5xx 에러 재시도
  - Circuit Breaker (Phase 3로 이관)
- [ ] 테스트 작성 (1시간)
  - WireMock 기반 통합 테스트
  - Timeout 시나리오 테스트

### 💬 Notifier Client 구현 (6시간)
- [ ] Interface 정의 (0.5시간)
  - `NotifierClient` Interface
  - `NotifyRequest`, `NotifyResponse` record
- [ ] RestClient 구현 (2.5시간)
  - POST `/v1/notify` 호출
  - Timeout 설정 (10초)
  - 재시도 로직 (지수 백오프: 1s, 2s, 4s)
- [ ] 설정 및 RateLimiter (1시간)
  - `NotifierProperties` (base-url, timeout)
  - RateLimiter 설정 (5/s)
- [ ] 에러 핸들링 (1시간)
  - 3회 재시도 후 실패 로그
  - 실패 시 재시도 큐 추가 (Phase 3로 이관)
- [ ] 테스트 작성 (1시간)
  - WireMock 기반 통합 테스트
  - 재시도 시나리오 테스트

### 🔍 InvestmentService 구현 (8시간)
- [ ] 이전 상태 조회 로직 (2시간)
  - `InvestmentDecision` Repository 쿼리
  - 최신 상태 조회 (종목별)
- [ ] 상태 비교 로직 (2시간)
  - 이전 상태 vs 현재 상태 비교
  - 변화 감지 알고리즘
- [ ] 알림 조건 판단 (2시간)
  - HOLD → BUY: 매수 알림
  - HOLD → SELL: 매도 알림
  - BUY → SELL: 포지션 변경 알림
  - 변화 없음: 알림 스킵
- [ ] 테스트 작성 (2시간)
  - 상태 변화 시나리오 (10가지)
  - 알림 조건 검증

### 🎯 WorkflowOrchestrator 구현 (12시간)
- [ ] 일간 워크플로우 구현 (8시간)
  1. 가격 수집 완료 확인 (1시간)
     - StockPriceScheduler 완료 여부 확인
  2. AI Advisor 학습 요청 (1.5시간)
     - 전체 종목 데이터 전달
     - 학습 완료 대기
  3. AI Advisor 예측 요청 (2시간)
     - 종목별 예측 요청
     - 배치 처리 (10개씩)
  4. 상태 변화 감지 (1.5시간)
     - InvestmentService 호출
     - 변화 종목 필터링
  5. Notifier 알림 요청 (2시간)
     - 변화 종목별 알림 발송
     - 에러 핸들링
- [ ] WorkflowScheduler 구현 (2시간)
  - cron 설정: `0 35 18 * * ?` (18:35 실행)
  - ShedLock 적용
  - 각 단계별 로깅
- [ ] 에러 핸들링 (1시간)
  - 각 단계별 실패 시 중단 여부 결정
  - 실패 로그 및 알림
- [ ] 테스트 작성 (1시간)
  - 통합 시나리오 테스트
  - 에러 시나리오 테스트

### 📋 InvestmentDecision 상태 저장 (6시간)
- [ ] AI Advisor 응답 파싱 (1.5시간)
  - PredictResponse → InvestmentDecision 변환
  - Enum 매핑 (BUY/SELL/HOLD)
- [ ] 상태 저장 로직 (2시간)
  - 배치 저장 (여러 종목)
  - 중복 체크 (upsert)
- [ ] 히스토리 관리 (1.5시간)
  - 이전 상태 아카이빙
  - 30일 이상 데이터 정리
- [ ] 테스트 작성 (1시간)

---

## Backlog
> **Phase 3: 2026-02-23 (월) ~ 03-29 (일)**

### 📦 주문 실행 기능
- [ ] Notifier 주문 요청 수신 API
  - POST `/collector/v1/order` 엔드포인트
  - Request: `stock_code`, `order_type`, `quantity`, `price`
- [ ] KIS API 주문 실행
  - POST `/uapi/domestic-stock/v1/trading/order-cash` (국내 매수/매도)
  - POST `/uapi/overseas-stock/v1/trading/order` (해외 매수/매도)
- [ ] OrderExecution Entity (주문 이력 저장)
- [ ] OrderService 구현
  - 주문 검증 (잔고, 수량)
  - 주문 실행 및 결과 저장

### 🛡️ Circuit Breaker
- [ ] Resilience4j 통합
- [ ] AI Advisor Circuit Breaker 설정
  - Failure Rate: 50%
  - Wait Duration: 60s
- [ ] Notifier Circuit Breaker 설정
- [ ] Fallback 로직

### 📍 Distributed Tracing
- [ ] Spring Cloud Sleuth 통합
- [ ] Trace-ID 전파 (Collector → AI/Notifier)
- [ ] JSON 로그 포맷 개선

### ❤️ Health Check 개선
- [ ] `/health` 엔드포인트 확장
  - KIS API 연결 상태
  - Redis, MySQL 연결 상태
  - AI Advisor, Notifier 연결 상태
- [ ] Actuator 설정

### 🔁 알림 재시도 큐
- [ ] Redis 기반 실패 알림 큐
- [ ] 재시도 스케줄러 (5분 간격)
- [ ] 최대 재시도 3회

---

## Completed (This Week: 2026-01-26 월 ~ 02-01 일)

### ✅ Spring Security 테스트 상세 문서화 (2026-02-01)
- [x] SecurityConfigTest.java 주석 보완
  - ✅ 클래스 레벨 JavaDoc 대폭 확장 (150줄 → 250줄)
  - ✅ MockMvc vs TestRestTemplate 기술적 차이 상세 설명
  - ✅ management.server.port: -1 제거 배경 및 근본 원인 설명
  - ✅ RANDOM_PORT 사용 이유 (포트 충돌 방지, CI/CD 호환성)
  - ✅ createBasicAuthHeader() 메서드 상세 주석 (withBasicAuth() 사용 불가 이유)
  - ✅ RFC 7617 표준 준수 설명 및 Base64 인코딩 과정 문서화
  - ✅ 실무 관점의 기술 결정 배경 설명 (후임 개발자 이해 용이)

## Completed (This Week: 2026-01-26 월 ~ 02-01 일)

### ✅ 프로젝트 문서화 (2026-01-27)
- [x] `README.md` 업데이트 (30분)
  - 현재 구현 상태 업데이트 (Phase 2 Week 1 완료 반영)
  - Directory Structure 업데이트 (strategy 패키지 추가)
  - Key Features 업데이트 (관심종목 동기화 개선)
- [x] `docs/MILESTONE.md` 업데이트 (40분)
  - Phase 1 진행률 업데이트 (95% → 100%)
  - Phase 2 Week 1 진행률 업데이트 (90% → 100%)
  - Progress Tracking 테이블 업데이트
- [x] `docs/TODO.md` 업데이트 (20분)
  - P0/P1/P2 우선순위 분류
  - Week별 작업 목록
  - Completed 섹션 업데이트
- [x] `docs/PRD.md` 업데이트 (50분)
  - Scenario 2 (관심종목 편집) 완료 반영
  - Phase 1 Success Metrics 달성 현황 업데이트
- [x] `docs/TECHSPEC.md` 업데이트 (60분)
  - WatchlistService 3-Way Sync 구현 상세 추가
  - Strategy Pattern 아키텍처 반영
  - Testing Strategy 업데이트 (31개 테스트 통과)
- [x] `CLAUDE.md` 업데이트 (10분)
  - Documentation 섹션 최신화
  - Architecture 섹션 strategy 패키지 반영

### ✅ 관심종목 동기화 개선 (2026-01-26)
- [x] 3-way 동기화 로직 구현 (API 기준 삭제 전략)
  - API에 없으면 DB에서 삭제 (그룹/종목)
  - API에 있으면 DB에 추가/업데이트
  - backfillCompleted 플래그 보존
- [x] 방어적 프로그래밍 적용
  - null stockCode 필터링
  - 중복 stockCode 제거 (Set 사용)
- [x] Repository 메서드 추가
  - `WatchlistGroupRepository.deleteByGroupId()`
  - `WatchlistStockRepository.deleteByStockCode()`
- [x] 테스트 작성 (총 31개)
  - 단위 테스트 26개 (모두 통과)
  - 통합 테스트 5개 (모두 통과)
- [x] 코드 리뷰 승인 완료
- [x] 4개 커밋 완료

### ✅ StockPrice 도메인 리팩터링 (2026-01-26)
- [x] Strategy Pattern 도입 (6개 파일 생성)
  - `StockPriceStrategy.java` 인터페이스
  - `DomesticStockStrategy.java` / `DomesticIndexStrategy.java`
  - `OverseasStockStrategy.java` / `OverseasIndexStrategy.java`
  - `StockPriceStrategyFactory.java`
- [x] 함수형 인터페이스 추가 (2개)
  - `TriFunction.java` / `QuadFunction.java`
- [x] Date Parsing 중앙화
  - `StockPriceConstants.parseDate()` 도입
- [x] Generic 메서드 도입
  - Backfill/Persistence 메서드 통합
- [x] StockPriceFetchService 제거
  - 불필요한 위임 레이어 제거
- [x] ADR 0009 작성
  - Strategy Pattern 도입 결정 기록
- [x] 문서 업데이트
  - `TECHSPEC.md` (strategy 패키지 추가)
  - `TODO.md` (리팩터링 작업 완료 기록)
  - `CLAUDE.md` (Architecture 섹션 업데이트)

## Completed (Previous Week: 2026-01-19 월 ~ 01-25 일)

### ✅ Phase 1 완료 항목
- [x] KIS API OAuth2 토큰 발급/갱신
- [x] Redis 토큰 캐싱 (TTL 관리)
- [x] 계정별 토큰 락 (분산 환경 동시성 제어)
- [x] Rate Limiter 구현 (KIS 20/s)
- [x] WatchlistGroup/WatchlistStock Entity 설계
- [x] Watchlist 동기화 로직 (KIS API → MySQL)
- [x] 4가지 StockDailyPrice Entity 설계
  - DomesticStockDailyPrice
  - DomesticIndexDailyPrice
  - OverseasStockDailyPrice
  - OverseasIndexDailyPrice
- [x] 일간 가격 수집 (18:30)
- [x] 과거 데이터 백필 (03:00)
- [x] StockPriceScheduler 구현 (ShedLock)
- [x] 단위/통합 테스트 작성 (커버리지 80%)
- [x] BaseEntity, JPA Converter, RestClient 설정

---

## Notes

- **우선순위 기준**:
  - P0: 현재 주차 필수 작업
  - P1: 다음 주차 중요 작업
  - P2: 다다음 주차 이후 작업
- **예상 시간**:
  - Week 1 (P0): 11시간
  - Week 2 (P1): 10시간
  - Week 3-4 (P2): 42시간
- **작업 원칙**:
  - 한 번에 하나의 Priority에 집중
  - 각 작업 완료 후 즉시 테스트 작성
  - 문서화 먼저, 코딩은 나중
