# ==================== Stage 1: Build ====================
# Gradle 빌드를 수행하는 단계 (빌드 도구 및 의존성 포함)
FROM gradle:8.11.1-jdk21-alpine AS builder

# 작업 디렉토리 설정
WORKDIR /app

# Gradle 래퍼 및 설정 파일 복사 (의존성 다운로드 레이어 캐싱 최적화)
COPY gradle gradle
COPY gradlew .
COPY build.gradle settings.gradle ./

# 의존성 다운로드 (빌드 캐시 활용)
# 소스 코드 변경 시 의존성 다운로드를 다시 하지 않도록 분리
# NOTE: 의존성 다운로드 실패 시 빌드 중단 (|| true 제거)
#       네트워크 일시 장애 시 Docker 빌드 재시도 권장
RUN ./gradlew dependencies --no-daemon

# 소스 코드 복사
COPY src src

# 빌드 실행 (테스트 제외, 프로덕션 최적화)
# -x test: 테스트 스킵 (CI에서 이미 실행됨)
# --no-daemon: Docker 환경에서 Gradle Daemon 비활성화
# -Dorg.gradle.jvmargs: 빌드 JVM 메모리 제한 (NAS 환경 고려)
RUN ./gradlew build -x test --no-daemon \
    -Dorg.gradle.jvmargs="-Xmx512m -XX:MaxMetaspaceSize=256m"

# 빌드 결과물 확인 (디버깅용, 빌드 성공 여부 체크)
RUN ls -lh /app/build/libs/


# ==================== Stage 2: Runtime ====================
# 경량 런타임 이미지 (빌드 도구 제외, 실행 환경만 포함)
# Runtime Image: eclipse-temurin:21.0.5_11-jre-alpine
# SHA256: decee204b9a1eb333c364ba4d859a6b1380eb13f0980d2acfd65c09fee53a48a
# Updated: 2026-02-01
# Fixed with SHA256 digest to ensure 100% reproducible builds
FROM eclipse-temurin@sha256:decee204b9a1eb333c364ba4d859a6b1380eb13f0980d2acfd65c09fee53a48a

# 메타데이터 라벨
LABEL maintainer="jongtix" \
      description="CAA Collector Service - Data Collection & Workflow Orchestration" \
      version="1.0.0"

# Build Arguments (환경별 커스터마이징 가능)
# 기본값: prod 환경 기준
ARG SPRING_PROFILE=prod
ARG JAVA_XMS=256m
ARG JAVA_XMX=350m

# Non-root 사용자 생성 (보안 강화)
# collector 그룹 및 사용자 생성 (UID/GID 1000)
RUN addgroup -g 1000 collector && \
    adduser -u 1000 -G collector -s /bin/sh -D collector

# 필수 유틸리티 설치
# tini: 경량 init 시스템 (8KB, Zombie process 방지 및 Graceful Shutdown 보장)
# wget: Alpine Linux 기본 포함 (HEALTHCHECK용 사용)
RUN apk add --no-cache tini

# 작업 디렉토리 설정
WORKDIR /app

# Stage 1에서 빌드된 JAR 파일 복사
# --chown으로 소유권을 collector 사용자로 변경
COPY --from=builder --chown=collector:collector /app/build/libs/*.jar app.jar

# 볼륨 마운트 지점 생성 (로그, 설정 파일 등)
# /app/logs: 애플리케이션 로그 저장
# /app/config: 외부 설정 파일 마운트 (application-prod.yml 등)
RUN mkdir -p /app/logs /app/config && \
    chown -R collector:collector /app

# Non-root 사용자로 전환
USER collector

# 애플리케이션 포트 노출
# 8080: Spring Boot 기본 포트 (API 요청)
# 9090: Management Port (Actuator 엔드포인트 - health, metrics, info)
EXPOSE 8080 9090

# HEALTHCHECK는 docker-compose.yml에서 환경별로 관리
# - local/dev: 8080 포트 (Management Port 분리 안 함)
# - prod: 9090 포트 (Management Port 분리)
# Dockerfile에서는 제거하여 환경별 유연성 확보
# HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
#     CMD wget -qO- http://localhost:9090/internal/management/health || exit 1

# ==================== Container Memory Requirement ====================
# JVM Memory Breakdown:
#   - Heap (Xmx): 350MB          # 애플리케이션 객체 메모리
#   - Metaspace: 100MB           # Spring Boot 클래스 메타데이터
#   - Direct Memory: 20MB        # NIO 버퍼 (KIS API 호출용)
#   - Reserved Code Cache: 50MB  # JIT 컴파일 코드 캐시
#   - Thread Stack: ~32MB        # 32 threads × 1MB per thread
#   - Total JVM: ~552MB
#
# Recommended Container Settings (docker-compose.yml):
#   mem_limit: 640m        # JVM 552MB + OS overhead 88MB (Phase 2)
#   mem_reservation: 512m  # Guaranteed memory
#   oom_score_adj: -200    # OOM Killer priority (protect from early termination)
#
# NAS Environment: Safe within 8GB RAM (supports multiple services)
# ======================================================================

# JVM 옵션 최적화 (NAS 환경 8GB RAM 고려)
# Heap Memory:
#   -Xms256m: 초기 힙 크기 256MB (빠른 시작)
#   -Xmx350m: 최대 힙 크기 350MB (OOM 방지)
# Off-Heap Memory:
#   -XX:MaxMetaspaceSize=100m: Metaspace 제한 (Spring Boot 클래스 로딩)
#   -XX:MaxDirectMemorySize=20m: NIO 버퍼 메모리 제한 (KIS API 호출용)
#   -XX:ReservedCodeCacheSize=50m: JIT 컴파일 코드 캐시
#   -Xss1m: 스레드 스택 크기 (기본값 명시적 설정)
# GC Tuning:
#   -XX:+UseG1GC: G1 GC 사용 (작은 힙에 적합)
#   -XX:MaxGCPauseMillis=200: GC 일시정지 목표 200ms
#   -XX:+UseStringDeduplication: 문자열 중복 제거 (메모리 절감)
#   -XX:+ParallelRefProcEnabled: 병렬 참조 처리 (GC 성능 향상)
# Other:
#   -Djava.security.egd: 난수 생성 최적화 (컨테이너 환경)
#   -Dspring.profiles.active: 프로파일 설정 (환경 변수로 재정의 가능)
ENV JAVA_OPTS="-Xms${JAVA_XMS} \
               -Xmx${JAVA_XMX} \
               -XX:MaxMetaspaceSize=100m \
               -XX:MaxDirectMemorySize=20m \
               -XX:ReservedCodeCacheSize=50m \
               -Xss1m \
               -XX:+UseG1GC \
               -XX:MaxGCPauseMillis=200 \
               -XX:+UseStringDeduplication \
               -XX:+ParallelRefProcEnabled \
               -Djava.security.egd=file:/dev/./urandom \
               -Dspring.profiles.active=${SPRING_PROFILE}"

# 애플리케이션 실행 (tini로 PID 1 관리)
# tini: 경량 init 시스템 (8KB)
#   - Zombie process 정리 자동화
#   - SIGTERM/SIGINT 시그널 완벽 전달
#   - Graceful Shutdown 보장
# $JAVA_OPTS: 환경 변수로 JVM 옵션 주입 가능
ENTRYPOINT ["tini", "--"]
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
