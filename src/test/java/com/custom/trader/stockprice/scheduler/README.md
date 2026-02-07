# ShedLock 통합 테스트

## 개요

`ShedLockIntegrationTest.java`는 ShedLock 분산 잠금이 여러 인스턴스 환경에서 정상적으로 동작하는지 검증하는 통합 테스트입니다.

## 사전 요구사항

이 테스트는 **Testcontainers**를 사용하여 실제 Redis 컨테이너를 실행하므로, **Docker가 실행 중이어야 합니다**.

### Docker 실행 확인

```bash
docker info
```

출력이 정상적으로 나타나면 Docker가 실행 중인 것입니다.

만약 `Cannot connect to the Docker daemon` 오류가 발생하면 Docker Desktop을 실행해주세요.

- **macOS**: Docker Desktop 앱 실행
- **Linux**: `sudo systemctl start docker`
- **Windows**: Docker Desktop 앱 실행

## 테스트 실행

```bash
# 전체 테스트 실행
./gradlew test --tests "com.custom.trader.stockprice.scheduler.ShedLockIntegrationTest"

# 특정 테스트 케이스만 실행
./gradlew test --tests "com.custom.trader.stockprice.scheduler.ShedLockIntegrationTest.singleInstanceShouldAcquireLockAndExecute"
```

## 테스트 시나리오

### TC-1: 단일 인스턴스 잠금 획득 및 작업 실행
- 스케줄러가 정상적으로 Lock을 획득
- `StockPriceCollectionService.collectDailyPrices()` 호출 확인
- Redis에 Lock 레코드 존재 확인

### TC-2: 동시 실행 2개 인스턴스 - 1개만 실행
- 두 스레드가 동시에 같은 이름의 작업 시도
- 1개 인스턴스만 Lock을 획득하고 작업 실행
- 다른 1개는 Lock을 획득하지 못하고 건너뜀
- `StockPriceCollectionService` 호출 횟수 = 1회 확인

### TC-3: 잠금 타임아웃 후 재획득
- Lock이 만료된 후 다른 인스턴스가 Lock 재획득 가능
- Awaitility로 비동기 재획득 검증

### TC-4: Redis 연결 실패 시 안전한 처리
- 작업 진행 중 Redis 장애 발생 시뮬레이션
- 예외가 안전하게 처리되는지 확인
- 로그에 에러 메시지 남는지 검증

## 테스트 구조

```
ShedLockIntegrationTest
├── @Container RedisContainer    # Testcontainers로 Redis 실행
├── @DynamicPropertySource        # Redis 연결 정보 동적 주입
├── @SpyBean CollectionService    # Mockito Spy로 호출 검증
└── LockProvider                  # ShedLock 제공 인터페이스
```

## 트러블슈팅

### 문제 1: Docker 연결 오류
**증상**: `Could not find a valid Docker environment`

**해결**:
1. Docker Desktop이 실행 중인지 확인
2. Docker 데몬 상태 확인: `docker info`
3. Docker Desktop을 재시작

### 문제 2: 포트 충돌
**증상**: `Bind for 0.0.0.0:6379 failed: port is already allocated`

**해결**:
1. 기존 Redis 컨테이너 중지: `docker stop $(docker ps -aq --filter ancestor=redis:7-alpine)`
2. 또는 로컬 Redis 서비스 중지: `brew services stop redis` (macOS)

### 문제 3: 테스트 시간 초과
**증상**: `awaitility timeout`

**해결**:
- Docker 리소스 할당 증가 (Docker Desktop > Settings > Resources)
- 테스트 타임아웃 시간 증가 (필요 시)

## 관련 파일

- **테스트 대상**: `StockPriceScheduler.java`
- **ShedLock 설정**: `ShedLockConfig.java`
- **비즈니스 로직**: `StockPriceCollectionService.java`
- **Redis 설정**: `application-test.yml`

## 참고

- ShedLock 공식 문서: https://github.com/lukas-krecan/ShedLock
- Testcontainers 공식 문서: https://www.testcontainers.org/
- Awaitility 공식 문서: http://www.awaitility.org/
