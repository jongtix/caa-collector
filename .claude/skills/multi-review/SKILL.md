---
name: multi-review
description: "Git PUSH 전 다각도 전문가 코드 리뷰"
---

# Multi-Persona Review

## 1. Review Perspectives
- **Security:** 취약점(Injection, Auth 등) 탐지.
- **Performance:** DB(N+1), Memory, 병목 지점 분석.
- **Maintainability:** 복잡도, 결합도, 스타일 준수 여부.

## 2. Execution Logic
- **Process:** 병렬 리뷰 수행 -> 결과 종합 -> 우선순위별 리포트.
- **Trigger:** Git PUSH 직전 또는 대규모 리팩토링 후 사용 권장.

## 3. Output Format
- **🔴 Critical:** 즉시 수정 필요한 심각한 결함.
- **🟠 Major:** 권장되는 중요 수정 사항.
- **🟡 Minor:** 사소한 스타일 또는 구조 개선.
- **💡 Suggestions:** 더 나은 방식에 대한 아이디어.