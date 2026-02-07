# ADR-0014: 보안 취약점 스캔 전략 변경 (OWASP Dependency-Check → Dependabot + Trivy)

## 상태
✅ **승인됨** (2026-02-02)

## 컨텍스트

### 기존 보안 스캔 전략

Phase 2 Week 2 시작 전까지 **OWASP Dependency-Check**를 로컬 빌드 환경에서 사용하여 의존성 취약점을 스캔했습니다:

```gradle
// build.gradle
plugins {
    id 'org.owasp.dependencycheck' version '10.0.4'
}

dependencyCheck {
    nvd {
        apiKey = System.getenv('NVD_API_KEY')
    }
    failBuildOnCVSS = 7.0
    outputDirectory = 'build/reports/dependency-check'
}
```

- **실행 방식**: `./gradlew dependencyCheckAnalyze`
- **스캔 대상**: Gradle 의존성 (`build.gradle`)
- **보고서**: HTML 형식 (`build/reports/dependency-check/dependency-check-report.html`)
- **장점**: 로컬에서 즉시 스캔 가능, NVD 데이터베이스 직접 연동

### 문제점 발견

2026-02-02 배포 자동화 작업 준비 중 **OWASP Dependency-Check의 치명적 버그**를 발견했습니다:

#### Issue #7409: CVSSv4 파싱 버그

- **GitHub Issue**: [jeremylong/DependencyCheck#7409](https://github.com/jeremylong/DependencyCheck/issues/7409)
- **증상**: NVD API에서 CVSSv4 데이터를 파싱하지 못해 스캔 실패
- **에러 메시지**:
  ```
  Failed to parse CVSS v4.0 vector string: CVSS:4.0/AV:N/AC:L/...
  java.lang.IllegalArgumentException: Invalid CVSS v4.0 metric
  ```
- **영향**: 최신 취약점 데이터(CVSSv4)가 포함된 프로젝트에서 스캔 불가
- **해결 전망**: 2026-02-02 현재 수정 예정 일자 불명 (Open 상태)

#### 대안 검토 필요성

- **로컬 빌드 중단**: 개발자 경험 저하 (빌드 실패 시 작업 중단)
- **플러그인 제거 시 보안 공백**: 의존성 취약점 감지 불가
- **CI/CD 환경으로 전환**: 업계 표준 도구 사용, 빌드와 보안 스캔 분리

### 업계 표준 보안 스캔 전략

현대적인 소프트웨어 개발에서는 **CI/CD 파이프라인에서 보안 스캔**을 수행하는 것이 일반적입니다:

| 도구 | 제공자 | 주요 기능 | 특징 |
|------|--------|----------|------|
| **Dependabot** | GitHub | Gradle/npm 의존성 취약점 스캔, PR 자동 생성 | GitHub 네이티브, 무료 |
| **Trivy** | Aqua Security | Docker 이미지, 의존성, IaC 스캔 | 오픈소스, 빠른 스캔 속도 |
| **Snyk** | Snyk | 의존성, 코드 취약점 스캔 | 유료 (무료 티어 제한적) |
| **Clair** | Quay | Docker 이미지 스캔 | 컨테이너 특화 |

**선택 기준**:
- ✅ GitHub Actions 통합 용이성
- ✅ 무료 또는 오픈소스
- ✅ Gradle + Docker 이미지 스캔 지원
- ✅ 빠른 스캔 속도 (빌드 시간 최소화)

## 결정

**OWASP Dependency-Check를 제거하고, CI/CD 파이프라인에서 Dependabot + Trivy를 사용**합니다.

### 핵심 설계

#### 1. OWASP Dependency-Check 제거

```gradle
// build.gradle
plugins {
    // 제거: id 'org.owasp.dependencycheck' version '10.0.4'
}

// dependencyCheck 설정 블록 전체 제거
```

- **이유**: CVSSv4 파싱 버그로 정상 작동 불가, 로컬 빌드 중단 방지
- **영향**: 로컬 환경에서 보안 스캔 미수행 (CI/CD에서 대체)

#### 2. GitHub Dependabot 활성화

```yaml
# .github/dependabot.yml
version: 2
updates:
  - package-ecosystem: "gradle"
    directory: "/"
    schedule:
      interval: "weekly"
      day: "monday"
      time: "09:00"
      timezone: "Asia/Seoul"
    open-pull-requests-limit: 5
    labels:
      - "dependencies"
      - "security"
    reviewers:
      - "jongtix"
    assignees:
      - "jongtix"
```

**주요 설정**:
- **스캔 주기**: 매주 월요일 오전 9시 (KST)
- **PR 자동 생성**: 취약점 발견 시 의존성 업그레이드 PR 생성
- **최대 PR 수**: 5개 (과도한 PR 방지)
- **라벨링**: `dependencies`, `security` 라벨 자동 추가

**장점**:
- GitHub 네이티브 통합 (별도 설정 불필요)
- 취약점 발견 시 즉시 알림 (PR + 이메일)
- 자동 업그레이드 제안 (Semantic Versioning 준수)

#### 3. GitHub Actions에 Trivy 추가

```yaml
# .github/workflows/build.yml
name: Build and Security Scan

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Build with Gradle
        run: ./gradlew build -q

      - name: Run Trivy vulnerability scanner (Gradle)
        uses: aquasecurity/trivy-action@master
        with:
          scan-type: 'fs'
          scan-ref: '.'
          scanners: 'vuln'
          severity: 'HIGH,CRITICAL'
          exit-code: '1'  # CVSS 7.0 이상 시 빌드 실패
          format: 'sarif'
          output: 'trivy-results.sarif'

      - name: Upload Trivy results to GitHub Security
        uses: github/codeql-action/upload-sarif@v3
        if: always()
        with:
          sarif_file: 'trivy-results.sarif'

      - name: Build Docker image
        run: docker build -t caa-collector:${{ github.sha }} .

      - name: Run Trivy vulnerability scanner (Docker)
        uses: aquasecurity/trivy-action@master
        with:
          image-ref: 'caa-collector:${{ github.sha }}'
          format: 'sarif'
          output: 'trivy-docker-results.sarif'
          severity: 'HIGH,CRITICAL'
          exit-code: '1'

      - name: Upload Docker scan results
        uses: github/codeql-action/upload-sarif@v3
        if: always()
        with:
          sarif_file: 'trivy-docker-results.sarif'
```

**스캔 대상**:
1. **Gradle 의존성** (`scan-type: fs`)
   - `build.gradle` 기반 취약점 스캔
   - CVSS 7.0 이상 시 빌드 실패
2. **Docker 이미지** (`image-ref: caa-collector:${{ github.sha }}`)
   - 컨테이너 레이어별 취약점 스캔
   - 베이스 이미지(eclipse-temurin) 포함

**GitHub Security 통합**:
- SARIF 포맷으로 결과 업로드
- **Security > Code scanning alerts**에서 취약점 조회
- Dependabot과 통합되어 통합 대시보드 제공

#### 4. 로컬 환경에서 수동 스캔 (선택사항)

```bash
# Trivy 설치 (macOS)
brew install trivy

# 프로젝트 전체 스캔
trivy fs .

# Gradle 의존성만 스캔
trivy fs --scanners vuln build.gradle

# CVSS 7.0 이상만 표시
trivy fs --severity HIGH,CRITICAL .

# Docker 이미지 스캔
docker build -t caa-collector:local .
trivy image caa-collector:local
```

- **목적**: CI/CD 전 로컬에서 사전 점검 (선택사항)
- **강제성**: 없음 (개발자 판단에 따라 사용)

## 대안 (고려했으나 채택하지 않음)

### 대안 1: OWASP Dependency-Check 유지 (버그 수정 대기)

```gradle
// build.gradle
plugins {
    id 'org.owasp.dependencycheck' version '10.0.4'
}
```

- **장점**: 기존 설정 유지, 로컬 즉시 스캔
- **단점**:
  - CVSSv4 버그 수정 시점 불명 (2026-02-02 현재 Open)
  - 로컬 빌드 중단 위험
  - NVD API 속도 제한 (무료: 5 req/s, 유료: 50 req/s)
- **결정**: ❌ 거부 (빌드 안정성 우선)

### 대안 2: Snyk 유료 도구 사용

```yaml
# .github/workflows/build.yml
- name: Run Snyk to check for vulnerabilities
  uses: snyk/actions/gradle@master
  env:
    SNYK_TOKEN: ${{ secrets.SNYK_TOKEN }}
```

- **장점**: 정확한 스캔, 우선순위 제안, 코드 레벨 취약점 스캔
- **단점**:
  - 유료 (무료 티어: 월 200회 스캔 제한)
  - 1인 개발 환경에서 비용 부담
  - Trivy + Dependabot 조합으로 충분
- **결정**: ❌ 거부 (비용 대비 효과 낮음)

### 대안 3: 보안 스캔 미수행

```bash
# 보안 스캔 없이 배포
./gradlew build
docker build -t caa-collector .
```

- **장점**: 빌드 시간 단축, 설정 단순
- **단점**:
  - 취약한 의존성 사용 위험
  - CVE 발표 후 대응 지연
  - 규제 준수 불가 (금융 서비스 등)
- **결정**: ❌ 거부 (보안 리스크 수용 불가)

## 결과

### 긍정적 영향

#### 빌드 안정성
- **로컬 빌드 중단 해소**: OWASP Dependency-Check 버그 회피
- **CI/CD 분리**: 빌드와 보안 스캔 독립적 실행
- **빠른 빌드**: 로컬에서 `./gradlew build`만 실행 (5~10분 단축)

#### 보안 강화
- **GitHub 통합 대시보드**: Security 탭에서 모든 취약점 통합 조회
- **자동 알림**: Dependabot PR + 이메일 알림
- **지속적 모니터링**: 매주 자동 스캔 (관리 부담 없음)

#### 비용 절감
- **무료 도구**: Dependabot(GitHub 네이티브) + Trivy(오픈소스)
- **NVD API 키 불필요**: Trivy는 자체 데이터베이스 사용

#### 업계 표준 준수
- **모던 DevOps 프랙티스**: CI/CD 기반 보안 스캔
- **Shift Left**: PR 단계에서 취약점 조기 발견
- **자동화**: 수동 스캔 의존성 제거

### 부정적 영향

#### 로컬 스캔 제한
- **현황**: 로컬에서 즉시 스캔 불가 (Trivy 수동 설치 필요)
- **완화**: `brew install trivy`로 간단 설치, CI/CD에서 자동 스캔
- **영향도**: 낮음 (1인 개발 환경, CI/CD 의존 가능)

#### 초기 설정 비용
- **GitHub Actions 워크플로우 작성**: 약 1시간
- **Dependabot 설정**: 약 0.5시간
- **총합**: 1.5시간 (일회성)

### 중립적 영향

#### 스캔 커버리지 동일
- **Dependabot**: Gradle 의존성 스캔 (OWASP와 동일)
- **Trivy**: Gradle + Docker 이미지 스캔 (더 넓은 범위)
- **결론**: 기존 대비 동등하거나 우수

## 구현 세부사항

### 1. Dependabot 설정

```yaml
# .github/dependabot.yml
version: 2
updates:
  - package-ecosystem: "gradle"
    directory: "/"
    schedule:
      interval: "weekly"
      day: "monday"
      time: "09:00"
      timezone: "Asia/Seoul"
    open-pull-requests-limit: 5
    labels:
      - "dependencies"
      - "security"
    reviewers:
      - "jongtix"
    assignees:
      - "jongtix"
```

**동작 방식**:
1. 매주 월요일 오전 9시(KST) 스캔 실행
2. 취약한 의존성 발견 시 PR 자동 생성
3. PR 제목: `Bump [dependency] from [old-version] to [new-version]`
4. PR 본문: CVE 번호, CVSS 점수, 변경 내역 링크

### 2. Trivy 스캔 단계

#### Gradle 의존성 스캔

```yaml
- name: Run Trivy vulnerability scanner (Gradle)
  uses: aquasecurity/trivy-action@master
  with:
    scan-type: 'fs'
    scan-ref: '.'
    scanners: 'vuln'
    severity: 'HIGH,CRITICAL'
    exit-code: '1'
    format: 'sarif'
    output: 'trivy-results.sarif'
```

- **스캔 대상**: `build.gradle`, `gradle.lockfile`
- **실패 조건**: CVSS 7.0 이상 (HIGH, CRITICAL)
- **결과 업로드**: GitHub Security 탭

#### Docker 이미지 스캔

```yaml
- name: Run Trivy vulnerability scanner (Docker)
  uses: aquasecurity/trivy-action@master
  with:
    image-ref: 'caa-collector:${{ github.sha }}'
    format: 'sarif'
    output: 'trivy-docker-results.sarif'
    severity: 'HIGH,CRITICAL'
    exit-code: '1'
```

- **스캔 대상**: Docker 이미지 레이어, OS 패키지, 애플리케이션 의존성
- **베이스 이미지**: `eclipse-temurin:21-jre-alpine`
- **실패 조건**: CVSS 7.0 이상

### 3. GitHub Security 통합

```yaml
- name: Upload Trivy results to GitHub Security
  uses: github/codeql-action/upload-sarif@v3
  if: always()
  with:
    sarif_file: 'trivy-results.sarif'
```

**통합 대시보드 제공**:
- **위치**: `Security > Code scanning alerts`
- **기능**:
  - 취약점 목록 조회 (Dependabot + Trivy 통합)
  - CVE 번호 클릭 시 상세 정보
  - 수정 PR 링크 (Dependabot)
  - 타임라인 추적 (발견 → 수정 → 해결)

### 4. 로컬 스캔 가이드

```bash
# Trivy 설치 (macOS)
brew install trivy

# 프로젝트 전체 스캔
trivy fs .

# Gradle 의존성만 스캔
trivy fs --scanners vuln build.gradle

# CVSS 7.0 이상만 표시
trivy fs --severity HIGH,CRITICAL .

# 결과를 JSON으로 저장
trivy fs --format json --output trivy-report.json .
```

**사용 시나리오**:
- PR 생성 전 사전 점검
- 의존성 추가 시 즉시 확인
- CI/CD 실패 원인 로컬에서 재현

## 마이그레이션 가이드

### 1. 기존 설정 제거

```bash
# build.gradle에서 제거
# plugins {
#     id 'org.owasp.dependencycheck' version '10.0.4'
# }

# dependencyCheck 설정 블록 전체 제거
```

### 2. 신규 설정 추가

```bash
# 1. Dependabot 설정 파일 생성
mkdir -p .github
cat > .github/dependabot.yml << EOF
version: 2
updates:
  - package-ecosystem: "gradle"
    directory: "/"
    schedule:
      interval: "weekly"
      day: "monday"
      time: "09:00"
      timezone: "Asia/Seoul"
    open-pull-requests-limit: 5
EOF

# 2. GitHub Actions 워크플로우 수정
# .github/workflows/build.yml에 Trivy 단계 추가 (위 예시 참조)

# 3. 로컬에서 Trivy 설치 (선택사항)
brew install trivy
```

### 3. README 업데이트

```markdown
# 보안 스캔

## CI/CD 환경에서 자동 스캔

- **Dependabot**: 매주 월요일 의존성 스캔
- **Trivy**: PR/푸시 시 Docker + Gradle 스캔

## 로컬 환경에서 수동 스캔 (선택사항)

```bash
brew install trivy
trivy fs --severity HIGH,CRITICAL .
```
```

## 향후 계획

### Phase 3: Docker 이미지 최적화 (2026-02-23 ~ 03-01)

- **Multi-stage 빌드**: 의존성 레이어 분리로 캐싱 최적화
- **Distroless 이미지 검토**: 더 작은 공격 표면 (alpine → distroless)
- **Trivy 스캔 강화**: OS 패키지 최소화

### Phase 6 이후: SBOM 생성 (2026-04-06 ~)

```yaml
# SBOM (Software Bill of Materials) 생성
- name: Generate SBOM
  run: trivy sbom --format cyclonedx .
```

- **목적**: 규제 준수 (금융권 등), 공급망 보안
- **형식**: CycloneDX, SPDX
- **저장**: 아티팩트로 업로드, 감사 추적

### 장기 계획: SCA 도구 통합 (2026-06 ~)

- **Snyk 무료 티어**: 코드 레벨 취약점 스캔 (Phase 7 이후 검토)
- **FOSSA**: 라이선스 컴플라이언스 스캔 (오픈소스 배포 시)

## 참고 자료

### 공식 문서
- [GitHub Dependabot 문서](https://docs.github.com/en/code-security/dependabot)
- [Trivy 공식 가이드](https://aquasecurity.github.io/trivy/)
- [GitHub Actions Security 강화 가이드](https://docs.github.com/en/actions/security-guides)

### 관련 이슈
- [OWASP Dependency-Check Issue #7409](https://github.com/jeremylong/DependencyCheck/issues/7409)

### 관련 ADR
- [ADR-0012: Spring Security 통합](./0012-spring-security-integration.md)

---

## 검토 기록

- **2026-02-02**: pm 에이전트 문서 업데이트 및 ADR 작성
- **2026-02-02**: OWASP Dependency-Check 제거 결정 및 대안 선정
