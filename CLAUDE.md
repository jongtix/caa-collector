# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

한국투자증권 Open API를 이용한 알고리즘 트레이딩 시스템. 관심 종목을 추적하고 주식 가격 예측 알고리즘(이동평균선, 머신러닝 등)을 통해 상승/하락을 예측하여 카카오톡으로 알림을 전송한다.

## Tech Stack

- Java 21, Spring Boot 3.5.9, Gradle
- Spring Data JPA + Redis (토큰 캐싱, ShedLock)
- H2 (개발), MySQL (운영)

## Build Commands

```bash
./gradlew build -q              # Build
./gradlew bootRun -q            # Run (환경변수 .env에서 로드)
./gradlew test -q               # Run all tests
./gradlew test -q --tests "com.custom.trader.SomeTest"           # Single class
./gradlew test -q --tests "com.custom.trader.SomeTest.testMethod" # Single method
./gradlew clean build -q        # Clean build
```

## Architecture

```
com.custom.trader
├── config/              # 전역 설정 (RestClient, ShedLock)
├── common/entity/       # BaseEntity (생성/수정 시간 자동 관리)
├── kis/                 # 한국투자증권 API 연동
│   ├── config/          # KisProperties, KisAccountProperties (record)
│   ├── dto/             # API 요청/응답 record
│   ├── exception/       # KisApiException
│   └── service/         # KisAuthService, KisWatchlistService, KisStockPriceService
├── stockprice/          # 주식 가격 수집 도메인
│   ├── domestic/        # 국내 주식/지수 일간 가격 (entity, repository)
│   ├── overseas/        # 해외 주식/지수 일간 가격 (entity, repository)
│   ├── strategy/        # AssetType별 처리 전략 (Strategy Pattern)
│   │   ├── StockPriceStrategy.java           # Strategy 인터페이스
│   │   ├── DomesticStockStrategy.java        # 국내 주식 처리 전략
│   │   ├── DomesticIndexStrategy.java        # 국내 지수 처리 전략
│   │   ├── OverseasStockStrategy.java        # 해외 주식 처리 전략
│   │   ├── OverseasIndexStrategy.java        # 해외 지수 처리 전략
│   │   └── StockPriceStrategyFactory.java    # AssetType별 Strategy 제공
│   ├── service/         # StockPriceCollectionService (일간 수집, 백필)
│   │                    # StockBackfillService (백필 전용)
│   │                    # StockPricePersistenceService (저장 로직)
│   └── scheduler/       # StockPriceScheduler (03:00 백필, 18:30 일간 수집)
└── watchlist/           # 관심종목 도메인
    ├── entity/          # WatchlistGroup, WatchlistStock (JPA)
    ├── repository/      # Spring Data JPA
    ├── service/         # WatchlistService (3-way 동기화 로직)
    └── scheduler/       # ShedLock 기반 스케줄러 (08:00, 18:00 동기화)
```

## Conventions

각 작업 시 적절한 에이전트와 스킬을 활용해 작업을 진행:

- **프로젝트 총괄**: `pm` 에이전트
- **백엔드 개발**: `backend-developer` 에이전트
- **보안 전략**: `backend-security-coder` 에이전트
- **성능 최적화**: `simple-performance-engineer` 에이전트
- **깃 작업 및 커밋 메시지**: `git-master` 에이전트
- **테스트 코드 작성**: `test-architect` 에이전트
- **코딩 스타일**: `~/.claude/skills/code-style-guide/SKILL.md`
- **영속성/JPA**:~/ `.claude/skills/persistence-strategy/SKILL.md`
- **에러 처리**: `~/.claude/skills/error-handling-master/SKILL.md`
- **테스트 작성**: `~/.claude/skills/test-code-generator/SKILL.md`
- **Git 규칙**: `~/.claude/skills/git-convention/SKILL.md`
- **그 외 정의되지 않은 작업**: `pm` 에이전트와 상의 후 진행

## 대략적인 작업 흐름
1. 새로운 기능 개발 또는 버그 수정 시 `pm` 에이전트와 상의하여 작업 범위 및 요구사항 정의
2. `git-master` 에이전트를 사용해 적절한 브랜치 생성
3. `backend-developer` 에이전트를 활용해 코드 작성 및 리팩토링
4. `backend-security-coder` 및 `simple-performance-engineer` 에이전트를 통해 보안 및 성능 검토, 수정 사항 반영
5. `code-reviewer` 에이전트를 사용해 코드 리뷰 수행 및 피드백 반영
6. `test-architect` 에이전트를 통해 테스트 코드 작성 및 검증
7. `pm` 에이전트와 최종 검토 및 문서 작성 후 배포 준비
8. `git-master` 에이전트를 사용해 커밋 메시지 작성 및 푸시

## Documentation

### MSA 전체 문서 (루트 레벨)

Collector는 MSA의 일부이므로, 전체 아키텍처 및 서비스 간 통신은 루트 문서를 참조:

- **`../README.md`**: MSA 프로젝트 첫 진입점 (Quick Start, 서비스 개요)
- **`../BLUEPRINT.md`**: MSA 아키텍처 설계 (서비스 역할, 워크플로우, API 스펙)
- **`../MILESTONE.md`**: MSA 전체 일정 및 서비스 간 의존성 관리
- **`../CLAUDE.md`**: MSA 전체 작업 지침 (서비스 간 조율 원칙, 공통 규칙)

### Collector 서비스 문서 (docs/ 폴더)

Collector 내부 상세는 `docs/` 폴더에 관리됨 (Phase 2 Week 1 100% 완료):

- **README.md**: Collector 개요 및 시작 가이드
- **MILESTONE.md**: Collector 일정 (Week별 상세, Phase 1 100%, Phase 2 20%)
- **TODO.md**: Collector 단기 작업 목록 (2026-01-27 최신화)
- **PRD.md**: Collector 제품 요구사항 명세 (책임 범위 명확화)
- **TECHSPEC.md**: Collector 기술 명세서 (Strategy Pattern, 3-way sync)
- **adr/**: Collector 아키텍처 결정 기록 (10개 ADR)
  - ADR-0010: Template Method Pattern 평가
  - ADR-0009: Strategy Pattern 도입 (주식 가격 수집)
  - ADR-0008: Rate Limiter 중앙화
  - 결정 배경, 대안, 결과 포함

## External APIs

- **한국투자증권 Open API**: OAuth2 토큰 → Redis 캐싱, 관심종목/시세 조회
- **카카오톡 메시지 API**: 예측 결과 알림 전송

## Claude 작업 지침

- **CLAUDE.md 자동 업데이트**: 새로운 패키지/도메인 추가, 아키텍처 변경, 새 외부 API 연동 등 주요 작업 완료 시 이 문서도 함께 업데이트할 것
- **새로운 브랜치 생성**: 새로운 기능 개발 또는 대규모 리팩토링 시 별도 브랜치에서 작업 후 PR 생성
- **한국투자증권 API 문서 참조**: API 연동 작업 시 한국투자 코딩도우미 MCP 문서 참고