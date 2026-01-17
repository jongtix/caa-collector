---
name: git-convention
description: "Feature/Bugfix 브랜치 전략 및 이모지 커밋 규격"
---

# Git & Branch Guide

## 1. Commit Message (Conventional)
- **Format:** `:emoji: description` (e.g., `:sparkles: 로그인 구현`)
- **Emojis:** ✨(feat), 🐛(fix), 🔧(config), 📝(docs), ♻️(refactor), 🎨(style/UI), ✅(test), 🔥(remove), 🚀(deploy), 📦(deps)
- **Rule:** 한국어 사용, 명사형 종결 (~함, ~구현).

## 2. Branch Strategy
- **Naming:** `feature/name` (신규), `bugfix/name` (수정). 소문자/하이픈(-)만 사용.
- **Workflow:** 
  1. `main`에서 최신 pull 후 `feature/` 또는 `bugfix/` 브랜치 생성.
  2. 작업 후 빌드/테스트 검증 필수.
  3. **Linear History:** `main` 병합 전 `git rebase origin/main` 수행하여 선형 히스토리 유지.
- **Atomic Commit:** 한 커밋에는 하나의 논리적 변화만 포함.

## 3. Claude Execution Logic
- **Check:** `add` 전후 `status`, `diff` 확인 필수.
- **Automation:** 성공 시 자동 커밋. `push` 전 사용자 확인 필수. `push` 후 해당 브랜치는 삭제 권장.
- **Rebase:** 충돌 시 해결 후 `rebase --continue` 진행.

## 4. Examples
- `git checkout -b feature/item-registration`
- `git commit -m ":sparkles: 상품 등록 기능 구현"`