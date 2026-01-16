---
name: rule-checker
description: "코드 수정 전후 프로젝트 규칙 위반 검사"
---

# Rule Checker Guide

## 1. Audit Items
- **Secrets:** API 키, 비밀번호 등 민감 정보의 환경변수(`env`) 관리 여부.
- **TODOs:** 코드 내 `TODO`, `FIXME` 주석 존재 여부 (완료 전 제거 확인).
- **Errors:** 빈 `catch` 블록 등 에러 무시 금지, 명시적 예외 처리 여부.

## 2. Execution Logic
- **Scan:** 변경된 파일 대상 자동 탐색.
- **Report:** 위반 사항 수집 후 수정 제안 포함 리포트 생성.
- **Timing:** 코드 수정 직후 또는 Commit 전 필수 실행.

## 3. Output Format
- **[File Path]**
- 🔴 **Violation:** 위반 항목 명칭 (Severity)
- 💡 **Suggestion:** 구체적인 수정 가이드