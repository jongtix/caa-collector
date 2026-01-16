# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

한국투자증권 Open API를 이용한 알고리즘 트레이딩 시스템. 관심 종목을 추적하고 주식 가격 예측 알고리즘(이동평균선, 머신러닝 등)을 통해 상승/하락을 예측하여 카카오톡으로 알림을 전송한다.

## Tech Stack

- Java 21
- Spring Boot 4.0.1
- Gradle

## Build Commands

```bash
# Build
./gradlew build -q

# Run application
./gradlew bootRun -q

# Run all tests
./gradlew test -q

# Run single test class
./gradlew test -q --tests "com.custom.trader.SomeTest"

# Run single test method
./gradlew test -q --tests "com.custom.trader.SomeTest.testMethod"

# Clean build
./gradlew clean build -q
```

## External APIs

- **한국투자증권 Open API**: 관심 종목 조회, 실시간 시세, 과거 주가 데이터
- **카카오톡 메시지 API**: 예측 결과 알림 전송
