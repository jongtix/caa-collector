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
│   ├── domestic/        # 국내 주식 일간 가격 (entity, repository)
│   ├── overseas/        # 해외 주식 일간 가격 (entity, repository)
│   ├── strategy/        # AssetType별 처리 전략 (Strategy Pattern)
│   ├── service/         # StockPriceCollectorService (일간 수집, 백필)
│   └── scheduler/       # StockPriceScheduler (03:00 백필, 18:30 일간 수집)
└── watchlist/           # 관심종목 도메인
    ├── entity/          # WatchlistGroup, WatchlistStock (JPA)
    ├── repository/      # Spring Data JPA
    ├── service/         # WatchlistService (동기화 로직)
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

## Documentation

프로젝트 문서는 `docs/` 폴더에 관리됨:

- **README.md**: 프로젝트 개요 및 시작 가이드
- **PRD.md**: 제품 요구사항 명세 (Product Requirements Document)
- **TECHSPEC.md**: 기술 명세서 (Technical Specification)
- **MILESTONE.md**: 개발 일정 및 마일스톤
- **TODO.md**: 작업 항목 및 우선순위
- **adr/**: 아키텍처 결정 기록 (Architecture Decision Records)
  - 주요 기술적 결정사항을 문서화
  - 결정 배경, 대안, 결과를 포함
  - 현재 5개 ADR 관리 중 (배치 쿼리, 토큰 락, Entity 분리, 타임아웃, 인덱싱)

## External APIs

- **한국투자증권 Open API**: OAuth2 토큰 → Redis 캐싱, 관심종목/시세 조회
- **카카오톡 메시지 API**: 예측 결과 알림 전송

## Claude 작업 지침

- **CLAUDE.md 자동 업데이트**: 새로운 패키지/도메인 추가, 아키텍처 변경, 새 외부 API 연동 등 주요 작업 완료 시 이 문서도 함께 업데이트할 것
- **새로운 브랜치 생성**: 새로운 기능 개발 또는 대규모 리팩토링 시 별도 브랜치에서 작업 후 PR 생성
- **한국투자증권 API 문서 참조**: API 연동 작업 시 한국투자 코딩도우미 MCP 문서 참고