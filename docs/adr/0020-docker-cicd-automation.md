# ADR-0020: Docker CI/CD 자동화 전략

**날짜**: 2026-02-13
**작성자**: jongtix + Claude (pm)
**상태**: Accepted

---

## Context

### 문제 상황

CAA Collector 서비스의 Docker 이미지 빌드 및 배포 자동화가 필요합니다.

**현재 상황**:
- Docker Hub 공개 배포 전략 채택 (ADR-0005)
- Reusable Workflow 기반 CI/CD 구조 완성 (ADR-0009)
- Docker 이미지 수동 빌드 및 푸시

**발생하는 문제**:
1. **수동 버전 관리**: 개발자가 git tag 수동 생성, 실수 가능
2. **일관성 부재**: 커밋 메시지와 버전 증가 규칙 불명확
3. **CHANGELOG 부재**: 릴리스 히스토리 자동 생성 없음
4. **배포 시간 낭비**: 이미지 빌드/푸시 수동 실행 (릴리스당 5-10분)

### 요구사항

1. **Semantic Versioning 자동 적용**: 커밋 메시지 기반 버전 자동 증가
2. **Git Tag 자동 생성**: 릴리스 시점에 자동 태그 생성
3. **CHANGELOG 자동화**: 릴리스 노트 자동 생성
4. **PR 기반 검토**: 버전 변경 사항 승인 후 배포 (안정성)
5. **Java/Gradle 지원**: Spring Boot 프로젝트 네이티브 지원

---

## Decision

**선택된 도구**: release-please (Google)

### 결정 근거

#### 1. release-please 구조

**워크플로우**:
```
커밋 메시지 (Conventional Commits)
  ↓
release-please: PR 자동 생성
  - 버전 증가 제안 (MAJOR/MINOR/PATCH)
  - CHANGELOG.md 업데이트
  - build.gradle 버전 업데이트
  ↓
개발자: PR 검토 및 승인
  ↓
PR 머지 (main 브랜치)
  ↓
release-please: 릴리스 자동 생성
  - Git Tag 생성 (v1.2.0)
  - GitHub Release 생성
  ↓
GitHub Actions: Docker 이미지 빌드 및 푸시
  - jongtix/caa-collector:v1.2.0
  - jongtix/caa-collector:latest
```

**핵심 특징**:
- **PR 기반 워크플로우**: 릴리스 전 검토 가능 (안정성)
- **Conventional Commits 기반**: `feat:` → MINOR, `fix:` → PATCH
- **Java Gradle 공식 지원**: build.gradle 버전 자동 업데이트
- **Google 공식 유지보수**: GitHub Actions 네이티브
- **1인 개발 최적화**: 자동화 + 수동 제어 균형

#### 2. Conventional Commits 규칙

**기본 형식**:
```
type(scope): description

[optional body]

[optional footer]
```

**Type별 버전 증가**:
| Type | 의미 | 버전 증가 | 예시 |
|------|------|----------|------|
| `feat` | 새 기능 | MINOR | v1.2.0 → v1.3.0 |
| `fix` | 버그 수정 | PATCH | v1.2.0 → v1.2.1 |
| `chore` | 설정, 빌드 변경 | 없음 | (버전 유지) |
| `docs` | 문서 변경 | 없음 | (버전 유지) |
| `BREAKING CHANGE` | 하위 호환 깨짐 | MAJOR | v1.2.0 → v2.0.0 |

**Hybrid 방식 지원**:
```bash
# Conventional Commits 표준
feat(watchlist): 관심종목 검색 기능 추가

# Hybrid (기존 이모지 유지 가능)
✨ feat(watchlist): 관심종목 검색 기능 추가
```

**release-please 파싱 규칙**:
- Type 위치: 라인 시작 또는 이모지 뒤
- `feat:`, `✨ feat:` 모두 인식

#### 3. Java Gradle 지원

**build.gradle 자동 업데이트**:
```gradle
// 기존
version = '1.2.0'

// release-please PR 생성 시
version = '1.3.0'
```

**release-please-config.json**:
```json
{
  "packages": {
    ".": {
      "release-type": "java",
      "extra-files": ["build.gradle"]
    }
  }
}
```

**지원 범위**:
- `version = 'x.y.z'` 패턴 자동 인식
- Gradle Wrapper 버전 업데이트 (선택 사항)
- CHANGELOG.md 자동 생성

#### 4. Docker Hub 배포 통합

**릴리스 이벤트 트리거**:
```yaml
# .github/workflows/docker-publish.yml
on:
  push:
    tags:
      - 'v*.*.*'  # release-please가 Git Tag 생성 시 자동 실행

jobs:
  docker-build-push:
    name: Build & Push Docker Image
    uses: jongtix/caa/.github/workflows/reusable-docker-build-push.yml@main
    with:
      service_name: 'caa-collector'
      dockerfile_path: 'caa-collector/Dockerfile'
      docker_context: 'caa-collector'
      image_name: 'jongtix/caa-collector'
      version: ${{ github.ref_name }}
    secrets:
      DOCKERHUB_USERNAME: ${{ secrets.DOCKERHUB_USERNAME }}
      DOCKERHUB_TOKEN: ${{ secrets.DOCKERHUB_TOKEN }}
```

**배포 흐름**:
1. release-please: Git Tag `v1.3.0` 생성
2. GitHub Actions: Docker 이미지 빌드
3. Docker Hub Push: `jongtix/caa-collector:v1.3.0`, `:latest`
4. Watchtower: 이미지 갱신 감지 → 자동 재시작

---

## Alternatives Considered

### Alternative 1: semantic-release

**평가**:
- ✅ **완전 자동화**: PR 없이 커밋 시 즉시 릴리스
- ✅ **플러그인 풍부**: npm, Docker, Slack 등 다양한 플러그인
- ❌ **검토 과정 없음**: PR 기반 워크플로우 부재 (위험)
- ❌ **Java 지원 약함**: 플러그인 추가 필요 (@semantic-release/exec)
- ❌ **복잡한 설정**: 플러그인 조합 설정 필요
- **결론**: 1인 개발 환경에 과도, 안정성 낮음

### Alternative 2: standard-version

**평가**:
- ✅ **간단한 구조**: CLI 도구, 설정 간단
- ❌ **수동 실행**: 개발자가 직접 `npm run release` 실행
- ❌ **GitHub Actions 통합 약함**: 자동화 추가 작업 필요
- ❌ **Java 지원 부족**: npm 기반, Gradle 통합 어려움
- **결론**: 자동화 부족, Java 환경 부적합

### Alternative 3: 수동 Git Tag + GitHub Actions

**평가**:
- ✅ **완전 제어**: 개발자가 모든 단계 제어
- ❌ **휴먼 에러**: 버전 증가 실수, 태그 오타 가능
- ❌ **CHANGELOG 수동 관리**: 릴리스 노트 수동 작성
- ❌ **일관성 보장 어려움**: 커밋 메시지 규칙 강제 불가
- **결론**: 자동화 부재, 유지보수 부담

---

## Consequences

### Positive

- ✅ **배포 시간 단축**: 릴리스당 5-10분 → 2분 (자동화)
- ✅ **휴먼 에러 제거**: 버전 증가, 태그 생성 자동화
- ✅ **CHANGELOG 자동화**: 릴리스 히스토리 자동 생성
- ✅ **안정성 향상**: PR 검토 후 릴리스 (수동 제어 유지)
- ✅ **Semantic Versioning 강제**: 커밋 메시지 규칙 자동 적용
- ✅ **Gradle 네이티브 지원**: build.gradle 버전 자동 업데이트
- ✅ **Hybrid 커밋 지원**: 기존 이모지 규칙 유지 가능

### Negative

- ⚠️ **커밋 메시지 규칙 학습**: Conventional Commits 형식 준수 필요
  - **완화**: DEVELOPMENT.md 가이드 작성, 예시 제공
- ⚠️ **PR 추가 단계**: 릴리스 전 PR 검토 필수 (시간 소요)
  - **완화**: 자동 생성 PR, 승인만 하면 됨 (30초)
- ⚠️ **release-please 의존성**: Google 도구 의존

### Neutral

- 🔵 **기존 커밋 메시지 호환**: `:emoji: description` → Hybrid 방식 지원
- 🔵 **GitHub Actions 사용량 불변**: 워크플로우 실행 횟수 변화 없음

---

## Implementation

### 구현 개요

본 결정의 상세 구현 방법은 **DEVELOPMENT.md**에서 확인하세요:
- [Conventional Commits 가이드](../DEVELOPMENT.md#conventional-commits-가이드) - 커밋 메시지 작성 규칙 및 예시
- [release-please 워크플로우](../DEVELOPMENT.md#release-please-워크플로우) - 릴리스 프로세스 전체 흐름

### 핵심 설정 파일

#### 1. release-please-config.json
```json
{
  "packages": {
    ".": {
      "release-type": "java",
      "package-name": "caa-collector",
      "extra-files": ["build.gradle"]
    }
  }
}
```

#### 2. GitHub Actions 워크플로우

**release-please.yml**: main 브랜치 푸시 시 릴리스 PR 자동 생성
**docker-publish.yml**: Git Tag 생성 시 Docker 이미지 빌드 및 푸시

워크플로우 파일 전체 내용 및 릴리스 프로세스는 [DEVELOPMENT.md](../DEVELOPMENT.md#release-please-워크플로우) 참조

---

## Related Documents

- **ADR-0005**: [Docker Hub 공개 배포 전략](./0005-docker-hub-public-deployment.md)
  - Docker Hub 배포 결정, 보안 조치
- **ADR-0009**: [Reusable Workflow 기반 CI/CD 전략](../../docs/adr/0009-reusable-workflow-cicd-strategy.md)
  - MSA 멀티레포 CI/CD 구조
- **DEVELOPMENT.md**: [개발자 가이드](../DEVELOPMENT.md)
  - Conventional Commits 상세 가이드
- **MILESTONE.md**: [Collector 일정](../MILESTONE.md)
  - Phase 2 Week 2-3: 배포 자동화 작업 범위

---

## References

- **release-please 공식 문서**: https://github.com/googleapis/release-please
- **Conventional Commits 표준**: https://www.conventionalcommits.org/
- **Semantic Versioning**: https://semver.org/
- **Docker Hub 공식 Action**: https://github.com/docker/build-push-action

---

## Notes

### MSA 공통 인프라

- 이 전략은 향후 추가될 AI Advisor, Notifier 서비스에도 동일하게 적용됩니다.
- Python 서비스 추가 시 `release-type: python` 설정 변경 예정.

### 1인 개발 최적화

- **자동화 + 수동 제어 균형**: PR 검토 후 릴리스 (안정성)
- **최소 학습 비용**: Conventional Commits 1회 학습
- **장기 유지보수 용이**: CHANGELOG 자동 생성

### 주의사항

- **커밋 메시지 규칙 준수 필수**: `feat:`, `fix:` 등 Type 명시
- **BREAKING CHANGE**: Footer에 명시 (본문에만 작성 시 인식 안 됨)
- **release-please PR 머지 필수**: 머지해야 릴리스 생성됨

### 향후 전환 가능성 (Future Migration)

**Phase 7 이후 semantic-release 전환 고려 가능**

현재 release-please는 1인 개발 환경에 최적화되어 있으나, 향후 다음 요구사항 발생 시 semantic-release로 전환을 고려할 수 있습니다:

**전환 고려 시나리오**:
- **완전 자동화 필요**: PR 검토 없이 즉시 릴리스 필요 (Phase 7: AI 고도화)
- **플러그인 확장**: Discord/Slack 알림, npm 퍼블리시 등 (SDK 배포)
- **복잡한 워크플로우**: 멀티패키지 릴리스, 조건부 배포

**마이그레이션 용이성**:
- ✅ **Conventional Commits 공통 기반**: 커밋 히스토리 그대로 호환
- ✅ **작업 시간**: 1~2시간 (설정 파일 변경만)
- ✅ **데이터 손실 없음**: Git Tag, CHANGELOG 유지
- ✅ **롤백 가능**: 워크플로우 파일만 변경하므로 쉽게 되돌릴 수 있음

**전환 절차** (요약):
1. `.releaserc.json` 생성 (semantic-release 설정)
2. `.github/workflows/semantic-release.yml` 추가
3. `.github/workflows/release-please.yml` 제거
4. Dry-run 테스트 후 적용

**참고**: 상세 비교 및 전환 가이드는 DEVELOPMENT.md 참조

---

**Decision Made**: 2026-02-13
**Implemented**: 2026-02-14
**Last Updated**: 2026-02-14
