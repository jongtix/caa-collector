---
name: ops-guide
description: "Build, Run, and Terminal Command Strategy"
---

# Operations Guide

## 1. Gradle Commands
- **Quiet Mode:** 토큰 절약을 위해 모든 Gradle 명령에 `-q` 플래그 사용.
    - 예: `./gradlew bootRun -q`, `./gradlew build -q`
- **Verification:** 빌드 실패 시 `-q`를 제외하고 재실행하여 에러 로그 확인.

## 2. Terminal Usage
- **Output Control:** 불필요한 대량 로그 출력 지양.
- **Port Management:** 포트 충돌 시 `lsof -i :8080` 등으로 확인 후 사용자 보고.