# ==================== Stage 1: Build ====================
# Gradle 빌드를 수행하는 단계 (빌드 도구 및 의존성 포함)
# Eclipse Temurin 기반으로 Gradle wrapper 사용
# SECURITY: SHA256 다이제스트 고정으로 Build Stage Supply Chain Attack 방어
FROM eclipse-temurin:21-jdk-alpine@sha256:c98f0d2e171c898bf896dc4166815d28a56d428e218190a1f35cdc7d82efd61f AS builder

# 작업 디렉토리 설정
WORKDIR /app

# Gradle wrapper 다운로드를 위한 파일 복사
# gradle-wrapper.properties 포함하여 Gradle 9.2.1 자동 다운로드
COPY gradle gradle
COPY gradlew ./

# Gradle wrapper 다운로드 (Gradle 9.2.1)
# 의존성 다운로드 전에 Gradle 바이너리를 먼저 다운로드
RUN ./gradlew --version

# Gradle 설정 파일 복사 (의존성 다운로드 레이어 캐싱 최적화)
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

# 빌드 결과물 확인 (빌드 성공 여부 체크)
# JAR 파일이 없으면 빌드 실패로 처리
RUN ls /app/build/libs/*.jar || (echo "Build failed: JAR not found" && exit 1)


# ==================== Stage 2: Runtime ====================
# Distroless 이미지 (최소 공격 표면, 보안 강화)
# Runtime Image: gcr.io/distroless/java21-debian12
# - Alpine 85개 취약점 제거 (CRITICAL 5개, HIGH 29개 포함)
# - Shell 없음 (RCE 공격 차단)
# - Package Manager 없음 (런타임 변조 불가)
# - Google 관리, 지속적 보안 유지
# - 참조: ../docs/adr/0007-distroless-image-strategy.md
FROM gcr.io/distroless/java21-debian12@sha256:3525cafa2114ed879d3554acbbd3ba1b388e3cd3e8833ff58713ba133b0e1173

# 메타데이터 라벨
LABEL maintainer="jongtix" \
      description="CAA Collector Service - Data Collection & Workflow Orchestration"

# 환경별 설정 (Distroless 환경)
# - Spring Profile: docker-compose.yml의 SPRING_PROFILES_ACTIVE 환경 변수로 제어
# - JVM 메모리: Dockerfile CMD에 하드코딩 (Xms256m, Xmx350m)
# - 커스터마이징 필요 시: Dockerfile.dev 별도 생성 또는 docker-compose.yml 오버라이드
#
# Distroless 이미지 특성:
# - Non-root 사용자 자동 사용 (UID 65532 'nonroot')
# - adduser/addgroup 명령어 없음 (사전 구성됨)
# - apk 명령어 없음 (패키지 관리자 제거)
# - tini 불필요 (Distroless 자체 init 처리)
# - mkdir/chown 불필요 (애플리케이션이 /app에 쓰기 권한 있음)

# 작업 디렉토리 설정
WORKDIR /app

# Stage 1에서 빌드된 JAR 파일 복사
# Distroless nonroot 사용자가 소유 (UID 65532)
COPY --from=builder /app/build/libs/*.jar app.jar

# SECURITY: Distroless nonroot 사용자 명시적 선언 (UID 65532, GID 65532)
# Distroless 이미지는 기본적으로 nonroot 사용자를 사용하지만,
# 명시적 선언을 통해 보안 감사 시 명확성 제공 및 우발적 root 실행 방지
USER nonroot:nonroot

# 볼륨 마운트 지점 (Distroless에서는 런타임에 자동 생성)
# /app/logs: 애플리케이션 로그 저장
# /app/logs/gc: GC 로그 저장
# /app/logs/heap_dumps: OOM 발생 시 힙 덤프 저장
# /app/config: 외부 설정 파일 마운트 (application-prod.yml 등)
# NOTE: docker-compose.yml에서 볼륨 마운트 시 자동 생성됨

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
#   - Metaspace: 160MB           # Spring Boot 클래스 메타데이터
#   - Direct Memory: 20MB        # NIO 버퍼 (KIS API 호출용)
#   - Reserved Code Cache: 50MB  # JIT 컴파일 코드 캐시
#   - Thread Stack: ~32MB        # 32 threads × 1MB per thread
#   - Total JVM: ~612MB
#
# Recommended Container Settings (docker-compose.yml):
#   mem_limit: 760m        # JVM 612MB + OS 88MB + Buffer 60MB (Phase 2)
#   mem_reservation: 610m  # Guaranteed memory
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
# OOM Handling & Monitoring:
#   -XX:+HeapDumpOnOutOfMemoryError: OOM 발생 시 힙 덤프 자동 생성
#   -XX:HeapDumpPath=/app/logs/heap_dumps/: 힙 덤프 저장 경로
#   -XX:+ExitOnOutOfMemoryError: OOM 발생 시 JVM 즉시 종료 (컨테이너 재시작 트리거)
#   -Xlog:gc*:file=/app/logs/gc/gc.log:time,level,tags:filecount=10,filesize=10M: GC 로그 (10개 파일 × 10MB 로테이션)
#   -XX:NativeMemoryTracking=summary: 네이티브 메모리 추적 활성화
# Other:
#   -Djava.security.egd: 난수 생성 최적화 (컨테이너 환경)
# Note: spring.profiles.active는 docker-compose.yml의 SPRING_PROFILES_ACTIVE 환경 변수로 제어
#       JVM 시스템 프로퍼티(-Dspring.profiles.active)는 환경 변수보다 우선순위가 높아
#       docker-compose.yml 설정을 무시하게 되므로 제거함
#
# Distroless 환경에서는 shell이 없어 ENV JAVA_OPTS 사용 불가
# 모든 JVM 옵션을 CMD에 직접 명시 (보안 강화)

# 애플리케이션 실행
# Distroless 특성:
#   - tini 불필요 (Distroless 자체 init 처리)
#   - shell 없음 (sh -c 사용 불가)
#   - exec 형식 ENTRYPOINT/CMD 사용 (JSON 배열)
#   - SIGTERM/SIGINT 시그널 자동 전달 (Graceful Shutdown 보장)
#
# SECURITY: JVM 옵션이 Dockerfile에 하드코딩되어 외부 변조 불가
# 환경별 커스터마이징 필요 시 Dockerfile.dev 별도 생성 권장
ENTRYPOINT ["java"]
CMD ["-Xms256m", \
     "-Xmx350m", \
     "-XX:MaxMetaspaceSize=160m", \
     "-XX:MaxDirectMemorySize=20m", \
     "-XX:ReservedCodeCacheSize=50m", \
     "-Xss1m", \
     "-XX:+UseG1GC", \
     "-XX:MaxGCPauseMillis=200", \
     "-XX:+UseStringDeduplication", \
     "-XX:+ParallelRefProcEnabled", \
     "-XX:+HeapDumpOnOutOfMemoryError", \
     "-XX:HeapDumpPath=/app/logs/heap_dumps/", \
     "-XX:+ExitOnOutOfMemoryError", \
     "-Xlog:gc*:file=/app/logs/gc/gc.log:time,level,tags:filecount=10,filesize=10M", \
     "-XX:NativeMemoryTracking=summary", \
     "-Djava.security.egd=file:/dev/./urandom", \
     "-jar", \
     "app.jar"]
