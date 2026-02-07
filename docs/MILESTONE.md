# CAA Collector Service - Milestone

> **프로젝트 중장기 로드맵 및 Phase별 진행 상황 관리**

---

## Vision Statement

**"관심종목만 등록하면 AI가 매수/매도 시점을 카카오톡으로 알림"**

사용자는 한국투자증권 계정에 관심종목을 등록하기만 하면, 시스템이 자동으로 데이터를 수집하고 AI가 학습/예측하여 투자 시점을 카카오톡으로 실시간 알림합니다. Collector Service는 이 전체 프로세스의 **컨트롤 타워**로서 데이터 수집부터 워크플로우 오케스트레이션까지 담당합니다.

---

## Phase 1: 데이터 수집 인프라 구축 ✅ 완료

**목표**: KIS API 연동 및 데이터 수집 파이프라인 구축

**기간**: 2026-01-12 (월) ~ 01-25 (일) | **진행률: 100%**

### 주요 성과
- ✅ KIS API 연동 (OAuth2 토큰, Redis 캐싱, Rate Limiter 20/s)
- ✅ Watchlist 도메인 (3-way 동기화, 관심종목 편집 반영)
- ✅ StockPrice 도메인 (4가지 Entity, Strategy Pattern, 일간/백필 스케줄러)
- ✅ 공통 인프라 (BaseEntity, JPA Converter, ShedLock)
- ✅ 테스트 (단위 26개, 통합 5개, 커버리지 80%)

---

## Phase 2: 배포 및 운영 인프라 🚧 진행 중

**목표**: Docker 컨테이너화, CI/CD 파이프라인, 자동 배포 체계 구축

**기간**: 2026-01-28 (화) ~ 02-22 (일) | **예상 시간: 57.3시간** | **진행률: 38%**

### Week 1: 문서화 + 보안 강화 (2026-01-26 ~ 02-07) ✅ 완료

**완료 항목**:
- ✅ 프로젝트 문서화 (README, MILESTONE, TODO, PRD, TECHSPEC) - 3.5시간
- ✅ 관심종목 편집 반영 (3-way 동기화, 백필 플래그 보존) - 8시간
- ✅ 보안 취약점 수정 (H-01 Actuator, H-02 DTO 제거, H-04 Testcontainers) - 1.8시간

**Week 1 총합**: 13.3시간 (완료)

### Week 2-3: 배포 자동화 (2026-02-08 ~ 02-22) 🚧 진행 중

**주요 작업** (44시간):

#### 0. Docker Hub 설정 ✅ 완료 (2026-02-08)
- [x] Docker Hub 계정 생성 및 Access Token 발급
- [x] GitHub Secrets 설정 (DOCKERHUB_USERNAME, DOCKERHUB_TOKEN)

#### 1. 컨테이너화 ✅ 완료 (2026-02-08)
- [x] Dockerfile 최적화 (프로파일 제어: JVM 프로퍼티 → 환경 변수)
- [x] Docker Compose 구성 (MySQL, Redis, Collector)
- [x] 환경 변수 설정 (.env.local 방식, REDIS/TOKEN 보안 변수 추가)
- [x] 보안 설정 (네트워크 격리, 127.0.0.1 바인딩)
- [x] .dockerignore 설정
- [x] 로컬 테스트 및 디버깅 (dev, log-dev, db-dev 프로파일 검증)
- [x] 문서화 (DEPLOYMENT.md, .env.example, README 업데이트)
#### 2. CI/CD 파이프라인 🔜 진행 예정 (9시간)
- [ ] GitHub Actions 워크플로우 작성 (빌드, 테스트, 이미지 푸시)
- [ ] Docker Hub 연동 (docker/login-action@v3)
- [ ] 보안 스캔 통합 (gitleaks, Trivy)
- [ ] 자동 버전 태깅 (semantic versioning)
- [ ] 빌드 성공/실패 알림 설정

#### 3. 자동 배포 🔜 진행 예정 (10시간)
- [ ] Watchtower 설정 및 테스트
- [ ] NAS 환경 배포 스크립트 작성
- [ ] Health Check 엔드포인트 구현
- [ ] 롤백 전략 설계 및 테스트

#### 4. 모니터링 및 관리 🔜 진행 예정 (10시간)
- [ ] Portainer 연동 및 대시보드 구성
- [ ] 로그 수집 전략 구현 (JSON 포맷)
- [ ] 리소스 제한 설정 (NAS 환경 최적화)
- [ ] 백업 및 복구 프로세스

#### 5. 문서화 🔜 진행 예정 (7시간)
- [ ] DEPLOYMENT.md 고도화 (CI/CD, 모니터링 섹션 추가)
- [ ] ADR-0020 작성 (배포 자동화 전략)
- [ ] 로컬 개발 환경 가이드

> **📌 참고**: Docker/CI/CD는 MSA 공통 인프라이지만 Collector 우선 적용 후 향후 서비스 추가 시 재사용

**Phase 2 총 예상 시간**: 57.3시간 (Week 1: 13.3h 완료, Week 2-3: 44h 남음)

---

## Phase 3: 실시간 데이터 수집 ❌ 계획

**목표**: WebSocket 기반 실시간 주가 데이터 수집

**기간**: 2026-02-23 (월) ~ 03-01 (일) | **예상 시간: 15시간** | **진행률: 0%**

### 주요 작업
- [ ] KIS WebSocket API 승인키 발급 및 연결 관리
- [ ] RealtimePrice Entity/Repository 설계
- [ ] SubscriptionManager 구현 (최대 20개 종목 구독)
- [ ] RealtimePriceSampler (5초 샘플링 + 배치 저장)
- [ ] TradingHoursScheduler (장 시작/종료 관리)

**참조**: [ADR-0011: WebSocket vs REST API](docs/adr/0011-websocket-realtime-price-strategy.md)

---

## Phase 4: AI Advisor 개발 및 연동 ❌ 계획

**목표**: AI 기반 투자 판단 엔진 구축 및 Collector 연동

**기간**: 2026-03-02 (월) ~ 03-22 (일) | **예상 시간: 45시간** | **진행률: 0%**

### 주요 작업
- [ ] AI Advisor 서비스 기본 구조 (FastAPI, Poetry) - 18시간
- [ ] Collector ↔ AI Advisor 통신 (AdvisorClient, InvestmentDecision Entity) - 15시간
- [ ] WorkflowOrchestrator 구현 (가격 수집 → AI 학습/예측) - 12시간

---

## Phase 5: Notifier 개발 및 연동 ❌ 계획 **(🎉 MVP)**

**목표**: 카카오톡 알림 기능 구축 및 전체 MSA 워크플로우 완성

**기간**: 2026-03-23 (월) ~ 04-05 (일) | **예상 시간: 30시간** | **진행률: 0%**

### 주요 작업
- [ ] Notifier 서비스 기본 구조 (Spring Boot, 카카오 API 연동) - 15시간
- [ ] Collector ↔ Notifier 통신 (NotifierClient, 상태 변화 감지) - 15시간

**마일스톤**: **🎉 MVP 완성** - 데이터 수집 → AI 판단 → 알림 발송 전체 워크플로우 작동

---

## Phase 6: 주문 실행 ❌ 계획

**목표**: 자동 주문 실행 기능 (안전장치 포함)

**기간**: 2026-04-06 (월) ~ 04-19 (일) | **예상 시간: 28시간**

### 주요 작업
- 주문 API 연동 (국내/해외 주식 매수/매도)
- 안전장치 (일일 한도, 손절매, 테스트 모드)

---

## Phase 7: AI 고도화 및 안정성 ❌ 계획

**목표**: AI 정확도 향상 및 시스템 안정성 강화

**기간**: 2026-04-20 (월) ~ 05-17 (일) | **예상 시간: 50시간**

### 주요 작업
- AI 모델 고도화 (LSTM/Transformer, 백테스팅)
- Circuit Breaker & Retry (Resilience4j)
- Distributed Tracing (Spring Cloud Sleuth)
- Health Check 개선

---

## Phase 8: 비동기 통신 전환 ❌ 미래

**목표**: REST → Kafka/RabbitMQ 전환, 이벤트 기반 아키텍처

**기간**: 2026-05-18 (월) 이후 | **예상 시간: TBD**

---

## Progress Tracking

| Phase | 기간 | 진행률 | 상태 |
|-------|------|--------|------|
| Phase 1: 데이터 수집 인프라 | 2026-01-12 ~ 01-25 (2주) | 100% | ✅ 완료 |
| Phase 2: 배포 및 운영 인프라 | 2026-01-28 ~ 02-22 (3.5주) | 38% | 🚧 진행 중 |
| Phase 3: 실시간 데이터 수집 | 2026-02-23 ~ 03-01 (1주) | 0% | ⏳ 대기 |
| Phase 4: AI Advisor 개발 및 연동 | 2026-03-02 ~ 03-22 (3주) | 0% | ⏳ 대기 |
| Phase 5: Notifier 개발 및 연동 **(MVP)** | 2026-03-23 ~ 04-05 (2주) | 0% | ⏳ 대기 |
| Phase 6: 주문 실행 | 2026-04-06 ~ 04-19 (2주) | 0% | ⏳ 대기 |
| Phase 7: AI 고도화 및 안정성 | 2026-04-20 ~ 05-17 (4주) | 0% | ⏳ 대기 |
| Phase 8: 비동기 통신 전환 | 2026-05-18 이후 | 0% | ⏳ 미래 |

---

## Key Milestones

| 날짜 | 요일 | 마일스톤 | 상태 |
|------|------|----------|------|
| 2026-01-25 | 일 | Phase 1 완료 (데이터 수집 인프라) | ✅ |
| 2026-02-01 | 일 | 문서화 완료 | ✅ |
| 2026-02-07 | 금 | 보안 High 등급 취약점 수정 완료 | ✅ |
| 2026-02-08 | 일 | Docker 로컬 테스트 완료 (컨테이너화 + Docker Hub 설정) | ✅ |
| 2026-02-22 | 일 | Phase 2 완료 (배포 자동화) | 🔜 |
| 2026-03-01 | 일 | Phase 3 완료 (실시간 데이터 수집) | 🔜 |
| 2026-03-22 | 일 | Phase 4 완료 (AI Advisor 연동) | 🔜 |
| **2026-04-05** | **일** | **Phase 5 완료 (MVP - 전체 워크플로우 작동)** | **🔜** |
| 2026-04-19 | 일 | Phase 6 완료 (주문 실행) | 🔜 |
| 2026-05-17 | 일 | Phase 7 완료 (AI 고도화 & 안정성) | 🔜 |

---

## Notes

### 1인 개발 최적화
- Phase 2는 3주 목표였으나 일요일 종료를 위해 3.5주(25일)로 조정
- Phase 단위로 집중하여 한 번에 하나의 작업만 수행
- 점진적 통합 전략: AI Advisor 연동 → Notifier 연동 (순차적)

### MVP 정의
**Phase 5 완료 시점 = MVP**
- 사용자가 관심종목 등록
- 시스템이 자동으로 데이터 수집 (일간 + 실시간)
- AI가 투자 판단 (매수/매도/보류)
- **카카오톡으로 알림 발송** ← 핵심 가치 전달 완료

Phase 6 이후는 추가 자동화(주문 실행) 및 개선(AI 고도화)입니다.

### 총 예상 시간 (Phase 1-7)
- Phase 1: 약 14일 (완료)
- Phase 2: 57.3시간 (보안 작업 1.8시간 추가)
- Phase 3: 15시간
- Phase 4: 45시간
- Phase 5: 30시간
- Phase 6: 28시간
- Phase 7: 50시간
- **총합: 225.3시간** (약 141일, 주당 15-20시간 기준)

---

## Related Documentation

### MSA 전체 문서
- [README.md](../../README.md) - MSA 프로젝트 개요
- [BLUEPRINT.md](../../BLUEPRINT.md) - MSA 아키텍처 설계
- [MILESTONE.md](../../MILESTONE.md) - MSA 전체 일정

### Collector 문서
- [README.md](../README.md) - Collector 개요 및 시작 가이드
- [TODO.md](TODO.md) - 단기 작업 목록 (현재 주차 집중)
- [PRD.md](PRD.md) - 제품 요구사항 정의
- [TECHSPEC.md](TECHSPEC.md) - 기술 명세서
- [ADR](adr/) - 아키텍처 결정 기록
