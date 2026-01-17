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
│   └── service/         # KisAuthService (토큰 관리), KisWatchlistService
└── watchlist/           # 관심종목 도메인
    ├── entity/          # WatchlistGroup, WatchlistStock (JPA)
    ├── repository/      # Spring Data JPA
    ├── service/         # WatchlistService (동기화 로직)
    └── scheduler/       # ShedLock 기반 스케줄러 (08:00, 18:00 동기화)
```

## Conventions

코드 작성 시 아래 스킬 문서를 참조:

- **코딩 스타일**: `.claude/skills/code-style-guide/SKILL.md`
- **영속성/JPA**: `.claude/skills/persistence-strategy/SKILL.md`
- **에러 처리**: `.claude/skills/error-handling-master/SKILL.md`
- **테스트 작성**: `.claude/skills/test-code-generator/SKILL.md`
- **Git 규칙**: `.claude/skills/git-convention/SKILL.md`
- **코드 리뷰**: `.claude/skills/multi-review/SKILL.md`

## External APIs

- **한국투자증권 Open API**: OAuth2 토큰 → Redis 캐싱, 관심종목/시세 조회
- **카카오톡 메시지 API**: 예측 결과 알림 전송

## Claude 작업 지침

- **CLAUDE.md 자동 업데이트**: 새로운 패키지/도메인 추가, 아키텍처 변경, 새 외부 API 연동 등 주요 작업 완료 시 이 문서도 함께 업데이트할 것
