# OPERATIONS.md

Collector 서비스 운영 및 모니터링 가이드

---

## 📋 목차

1. [컨테이너 메모리 모니터링](#컨테이너-메모리-모니터링)
2. [JVM 메모리 분석](#jvm-메모리-분석)
3. [GC 로그 분석](#gc-로그-분석)
4. [Heap Dump 분석](#heap-dump-분석)
5. [Prometheus 알람 설정](#prometheus-알람-설정)
6. [트러블슈팅](#트러블슈팅)

---

## 컨테이너 메모리 모니터링

### Docker Stats 기본 모니터링

```bash
# 실시간 메모리 사용량 확인
docker stats caa-collector --no-stream

# 메모리 사용률만 추출
docker stats caa-collector --no-stream --format "table {{.Container}}\t{{.MemUsage}}\t{{.MemPerc}}"
```

### 컨테이너 메모리 임계치
- **정상**: < 512MB (80%)
- **주의**: 512-576MB (80-90%)
- **위험**: > 576MB (90%+)

---

## JVM 메모리 분석

### 1. jcmd를 통한 실시간 메모리 추적

#### Native Memory Tracking 확인
```bash
# 컨테이너 내부 접속
docker exec -it caa-collector sh

# NMT 요약 정보 확인
jcmd 1 VM.native_memory summary

# NMT 상세 정보 확인 (메모리 누수 추적 시)
jcmd 1 VM.native_memory detail
```

#### 주요 메트릭 해석
- **Java Heap**: Xmx 설정값 (350MB) 대비 사용률
- **Class**: Metaspace 사용량 (MaxMetaspaceSize=100MB)
- **Thread**: 스레드 스택 메모리 (32 threads × 1MB = 32MB)
- **Code**: JIT 컴파일 코드 캐시 (ReservedCodeCacheSize=50MB)
- **GC**: G1 GC 내부 구조 메모리
- **Compiler**: JIT 컴파일러 메모리
- **Internal**: JVM 내부 메모리
- **Symbol**: 심볼 테이블 메모리

#### GC 상태 확인
```bash
# GC 통계 확인
jcmd 1 GC.heap_info

# GC 힙 덤프 생성 (문제 발생 시)
jcmd 1 GC.heap_dump /app/logs/heap_dumps/manual-$(date +%Y%m%d-%H%M%S).hprof
```

### 2. Actuator를 통한 메트릭 수집

#### Actuator 메트릭 엔드포인트
```bash
# 전체 메트릭 목록
curl -u actuator:$ACTUATOR_PASSWORD http://localhost:9090/internal/management/metrics

# JVM 메모리 메트릭
curl -u actuator:$ACTUATOR_PASSWORD http://localhost:9090/internal/management/metrics/jvm.memory.used

# GC 메트릭
curl -u actuator:$ACTUATOR_PASSWORD http://localhost:9090/internal/management/metrics/jvm.gc.pause
```

#### 주요 메트릭
- `jvm.memory.used`: 힙/비힙 메모리 사용량
- `jvm.memory.max`: 최대 메모리 설정값
- `jvm.gc.pause`: GC 일시정지 시간
- `jvm.gc.memory.allocated`: GC 할당 메모리
- `jvm.threads.live`: 활성 스레드 수

---

## GC 로그 분석

### 로그 위치
- **경로**: `/app/logs/gc/gc.log`
- **로테이션**: 10개 파일 × 10MB (최대 100MB)
- **파일명**: `gc.log.0`, `gc.log.1`, ..., `gc.log.9`

### 로그 확인
```bash
# 최신 GC 로그 확인
docker exec caa-collector tail -f /app/logs/gc/gc.log

# 로그 파일 목록
docker exec caa-collector ls -lh /app/logs/gc/
```

### GC 분석 도구

#### 1. GCViewer (로컬 분석)
```bash
# GC 로그 복사
docker cp caa-collector:/app/logs/gc/gc.log ./gc.log

# GCViewer로 시각화 (https://github.com/chewiebug/GCViewer)
java -jar gcviewer.jar ./gc.log
```

#### 2. GCeasy (온라인 분석)
1. https://gceasy.io/ 접속
2. GC 로그 파일 업로드
3. 자동 분석 결과 확인

### 주요 GC 메트릭
- **GC Pause Time**: < 200ms (목표값)
- **GC Frequency**: 분당 1-2회 (정상), 분당 10회 이상 (문제)
- **Heap After GC**: Full GC 후에도 80% 이상 사용 시 메모리 부족

---

## Heap Dump 분석

### Heap Dump 자동 생성
OOM 발생 시 자동으로 생성됩니다:
- **경로**: `/app/logs/heap_dumps/java_pid1.hprof`
- **크기**: 최대 350MB (Xmx 설정값)

### Heap Dump 복사
```bash
# 컨테이너에서 로컬로 복사
docker cp caa-collector:/app/logs/heap_dumps/java_pid1.hprof ./heap_dump.hprof
```

### Eclipse MAT 분석

#### 1. 설치
- 다운로드: https://eclipse.dev/mat/downloads.php
- 요구사항: Java 11+

#### 2. 분석 절차
1. **파일 열기**: File → Open Heap Dump → `heap_dump.hprof` 선택
2. **Leak Suspects 자동 분석**: "Leak Suspects Report" 선택
3. **주요 분석 뷰**:
   - **Histogram**: 클래스별 인스턴스 개수 및 메모리 사용량
   - **Dominator Tree**: 메모리를 가장 많이 점유한 객체 트리
   - **Top Consumers**: 메모리 소비 상위 객체
   - **Duplicate Classes**: 중복 클래스 로딩 확인

#### 3. 일반적인 메모리 누수 패턴
- **Collection 누적**: List, Map에 객체가 계속 추가되지만 제거되지 않음
- **Cache 미설정**: 캐시에 TTL/Eviction 정책 없음
- **Listener 미제거**: 이벤트 리스너가 해제되지 않음
- **Thread Local 미정리**: ThreadLocal 변수가 정리되지 않음
- **Static 참조**: Static 필드가 대량의 객체 참조

#### 4. Collector 특화 체크리스트
- **Redis Connection Pool**: Lettuce 연결이 제대로 반환되는지
- **RestClient Connection**: KIS API 호출 후 연결이 닫히는지
- **JPA Session**: Hibernate 세션이 제대로 종료되는지
- **Scheduled Task**: ShedLock이 제대로 해제되는지

---

## Prometheus 알람 설정

### Prometheus 메트릭 수집 (Phase 5 예정)

#### prometheus.yml 설정
```yaml
scrape_configs:
  - job_name: 'caa-collector'
    scrape_interval: 30s
    metrics_path: '/internal/management/prometheus'
    basic_auth:
      username: actuator
      password: ${ACTUATOR_PASSWORD}
    static_configs:
      - targets: ['collector:9090']
```

### 알람 규칙 (alertmanager.yml)

```yaml
groups:
  - name: caa-collector-memory
    interval: 1m
    rules:
      # 힙 메모리 사용률 80% 이상 (주의)
      - alert: CollectorHeapUsageHigh
        expr: jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.8
        for: 5m
        labels:
          severity: warning
          service: caa-collector
        annotations:
          summary: "Collector 힙 메모리 사용률 80% 초과"
          description: "현재 사용률: {{ $value | humanizePercentage }}"

      # 힙 메모리 사용률 90% 이상 (위험)
      - alert: CollectorHeapUsageCritical
        expr: jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.9
        for: 2m
        labels:
          severity: critical
          service: caa-collector
        annotations:
          summary: "Collector 힙 메모리 사용률 90% 초과 - OOM 위험"
          description: "현재 사용률: {{ $value | humanizePercentage }}"

      # GC 빈도 과다 (분당 10회 이상)
      - alert: CollectorGCFrequencyHigh
        expr: rate(jvm_gc_pause_seconds_count[1m]) > 10
        for: 5m
        labels:
          severity: warning
          service: caa-collector
        annotations:
          summary: "Collector GC 빈도 과다 (분당 10회 이상)"
          description: "현재 GC 빈도: {{ $value | humanize }} 회/분"

      # GC 일시정지 시간 과다 (평균 200ms 이상)
      - alert: CollectorGCPauseTimeHigh
        expr: rate(jvm_gc_pause_seconds_sum[5m]) / rate(jvm_gc_pause_seconds_count[5m]) > 0.2
        for: 5m
        labels:
          severity: warning
          service: caa-collector
        annotations:
          summary: "Collector GC 일시정지 시간 과다 (평균 200ms 이상)"
          description: "현재 평균 일시정지 시간: {{ $value | humanizeDuration }}"

      # Metaspace 사용률 90% 이상
      - alert: CollectorMetaspaceUsageHigh
        expr: jvm_memory_used_bytes{area="nonheap",id="Metaspace"} / jvm_memory_max_bytes{area="nonheap",id="Metaspace"} > 0.9
        for: 5m
        labels:
          severity: warning
          service: caa-collector
        annotations:
          summary: "Collector Metaspace 사용률 90% 초과"
          description: "현재 사용률: {{ $value | humanizePercentage }}"

      # 컨테이너 메모리 사용률 90% 이상
      - alert: CollectorContainerMemoryHigh
        expr: container_memory_usage_bytes{name="caa-collector"} / container_spec_memory_limit_bytes{name="caa-collector"} > 0.9
        for: 2m
        labels:
          severity: critical
          service: caa-collector
        annotations:
          summary: "Collector 컨테이너 메모리 사용률 90% 초과 - OOM Kill 위험"
          description: "현재 사용률: {{ $value | humanizePercentage }}"
```

---

## 트러블슈팅

### OOM 발생 시 대응 절차

#### 1. 즉시 조치 (5분 이내)
```bash
# 1. 컨테이너 상태 확인
docker ps -a | grep caa-collector

# 2. OOM 발생 확인
docker logs caa-collector --tail 100 | grep -i "OutOfMemoryError"

# 3. Heap Dump 복사 (삭제 전)
docker cp caa-collector:/app/logs/heap_dumps/java_pid1.hprof ./oom-$(date +%Y%m%d-%H%M%S).hprof

# 4. 컨테이너 재시작 (자동 재시작 실패 시)
docker-compose restart collector
```

#### 2. 원인 분석 (1시간 이내)
1. Heap Dump를 Eclipse MAT로 분석
2. GC 로그에서 메모리 증가 패턴 확인
3. Actuator 메트릭에서 메모리 트렌드 확인

#### 3. 임시 조치 (필요 시)
```yaml
# docker-compose.yml 메모리 증설 (긴급)
services:
  collector:
    mem_limit: 768m      # 640m → 768m
    environment:
      - JAVA_XMX=480m    # 350m → 480m
```

#### 4. 근본 원인 해결
- 메모리 누수 수정
- 불필요한 캐시 제거
- 배치 크기 조정
- 쿼리 최적화 (N+1 문제 등)

### 일반적인 문제 및 해결

#### GC 로그가 생성되지 않음
```bash
# 원인: 로그 디렉토리 권한 문제
docker exec caa-collector ls -la /app/logs/gc/

# 해결: 디렉토리 권한 확인 (Dockerfile에서 이미 설정됨)
# RUN mkdir -p /app/logs/gc && chown -R collector:collector /app
```

#### Heap Dump가 생성되지 않음
```bash
# 원인: 디스크 공간 부족
docker exec caa-collector df -h /app/logs/heap_dumps/

# 해결: 볼륨 공간 확보 또는 증설
```

#### Native Memory Tracking 결과가 보이지 않음
```bash
# 원인: JVM 옵션 미적용
docker exec caa-collector sh -c 'echo $JAVA_OPTS' | grep NativeMemoryTracking

# 해결: Dockerfile 빌드 확인
```

---

## 참고 자료

### 공식 문서
- [Java 21 GC Tuning Guide](https://docs.oracle.com/en/java/javase/21/gctuning/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Eclipse MAT User Guide](https://help.eclipse.org/latest/index.jsp?topic=%2Forg.eclipse.mat.ui.help%2Fwelcome.html)

### 루트 문서
- [DEPLOYMENT.md](../../docs/DEPLOYMENT.md): 전체 배포 가이드
- [BLUEPRINT.md](../../docs/BLUEPRINT.md): MSA 아키텍처

### Collector 문서
- [TECHSPEC.md](./TECHSPEC.md): 기술 명세
- [ADR-003](./adr/ADR-003-memory-optimization-strategy.md): 메모리 최적화 전략
- [ADR-004](./adr/ADR-004-docker-hub-deployment-strategy.md): Docker Hub 배포 전략
