# CAA Collector Service - TODO

> **현재 Phase 2의 단기 작업 목록 및 우선순위 관리**

---

## Header

- **Last Updated**: 2026-02-07 (금)
- **Current Phase**: Phase 2 Week 2 (진행률 24%)
- **Focus**: 배포 자동화 (Docker Hub, Docker, CI/CD)
- **Deadline**: 2026-02-22 (일)
- **Remaining**: 38.5시간 (15일)

---

## 🎯 지금 해야 할 일 (This Week: 2026-02-09 ~ 02-15)

### 배포 인프라 구축 (총 11.5시간)

- [ ] **Docker Hub 설정** (0.5시간, CRITICAL)
  - Docker Hub 계정 생성 및 Access Token 발급
  - GitHub Secrets 설정 (DOCKERHUB_USERNAME, DOCKERHUB_TOKEN)
  - 참조: [MSA/ADR-0011: Docker Hub 공개 배포 전략](../../docs/adr/ADR-0011-docker-hub-public-deployment.md)

- [ ] **컨테이너화** (8시간)
  - Dockerfile Multi-stage Build (JAR 레이어 분리, Non-root 사용자)
  - .dockerignore 설정 (빌드 컨텍스트 최적화)
  - 로컬 테스트 및 디버깅 (docker build, docker run)
  - Docker Compose 최종 검증 (MySQL 8.0, Redis 7.0, Collector)
  - 환경변수 최종 점검 (.env 구조화)

- [ ] **CI/CD 보안 스캔** (2시간)
  - GitHub Dependabot 활성화 (주간 PR 자동 생성)
  - Trivy 스캔 추가 (CVSS 7.0 이상 빌드 실패)
  - ADR-0014 작성 (보안 스캔 전략)

- [ ] **스케줄 리팩터링** (1시간)
  - MA-19: DateFormatConstants.KST_ZONE_ID 전역 적용 확인
  - 참조: [ADR-0016: 글로벌 주식 시장 스케줄](adr/0016-global-market-schedule-architecture.md)

---

## 📅 다음 작업 (Next Week: 2026-02-16 ~ 02-22)

### 배포 자동화 완성 (총 27시간)

- [ ] **CI/CD 파이프라인** (9시간)
  - GitHub Actions 워크플로우 (빌드, 테스트, 이미지 푸시)
  - Semantic Versioning + Git Tag 자동화
  - Discord/Slack 빌드 알림

- [ ] **Watchtower 자동 배포** (10시간)
  - 이미지 갱신 감지 + 자동 재시작
  - Health Check 엔드포인트 확장
  - 롤백 전략 설계 및 테스트

- [ ] **모니터링 및 관리** (10시간)
  - Portainer 대시보드 구성
  - JSON 로그 포맷 + 레벨 표준화
  - MySQL 백업 스크립트 + 복구 테스트

- [ ] **문서화** (7시간)
  - DEPLOYMENT.md 작성 (배포 가이드, 트러블슈팅 FAQ)
  - ADR-0020 작성 (배포 자동화 전략)
  - 로컬 개발 환경 가이드

- [ ] **MA-01: Watchlist API 재시도 로직** (7시간)
  - N+1 호출 패턴 개선 (재시도 전략)
  - 참조: [ADR-0019](adr/0019-watchlist-api-retry-strategy.md)

---

## 💭 나중에 (Later: Phase 3+)

### Phase 3: 실시간 데이터 수집 (2026-02-23 ~ 03-01, 15시간)
- [ ] WebSocket 기반 실시간 시세 (KIS API 승인키 발급)
- [ ] RealtimePrice Entity/Repository 설계
- [ ] 5초 샘플링 + 배치 저장

### Phase 4: AI Advisor 개발 및 연동 (2026-03-02 ~ 03-22, 45시간)
- [ ] AdvisorClient 구현 (학습/예측 API)
- [ ] InvestmentDecision Entity 설계
- [ ] WorkflowOrchestrator 구현 (가격 수집 → AI 판단)

### Phase 5: Notifier 개발 및 연동 - MVP (2026-03-23 ~ 04-05, 30시간)
- [ ] NotifierClient 구현 (알림 발송 API)
- [ ] InvestmentService 상태 변화 감지
- [ ] 카카오톡 알림 템플릿 설계

---

## 📌 참고

### 기술 결정 (ADR)
- [MSA/ADR-0011: Docker Hub 공개 배포 전략](../../docs/adr/ADR-0011-docker-hub-public-deployment.md)
- [ADR-0016: 글로벌 시장 스케줄 아키텍처](adr/0016-global-market-schedule-architecture.md)
- [ADR-0017: Database Migration Strategy](adr/0017-database-migration-strategy.md)
- [ADR-0018: Dockerfile Shell Injection 완화](adr/0018-dockerfile-shell-injection-mitigation.md)
- [ADR-0019: Watchlist API 재시도 전략](adr/0019-watchlist-api-retry-strategy.md)

### 보안 처리 방침
- **Critical/High 등급**: ✅ 완료 (H-01, H-02, H-04)
- **Medium/Low 등급**: Phase 3 시작 전 (2026-02-22~23) 일괄 정리
- 상세: [MILESTONE.md - Phase 2 Week 1](MILESTONE.md#week-1-문서화--관심종목-편집-반영-2026-01-26-월--02-01-일)

### 진행 상황
- Phase 2 진행률: **24%** (13.3h / 57.3h)
- Week 1 완료: 문서화 (3.5h) + 관심종목 편집 (8h) + 보안 (1.8h)
- Week 2-3 남은 작업: 38.5시간 (Docker Hub 전략으로 5.5시간 절약)
- MVP 목표: 2026-04-05 (Phase 5 완료)

---

## Notes

- **완료된 작업**: [MILESTONE.md](MILESTONE.md) 참조
- **상세 기술 명세**: [TECHSPEC.md](TECHSPEC.md) 참조
- **작업 원칙**: 한 번에 하나의 Priority에 집중, 문서화 먼저
