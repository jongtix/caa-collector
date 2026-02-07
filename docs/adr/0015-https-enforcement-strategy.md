# ADR-0015: HTTPS 강제 전략 - Phase 2 연기 결정

**날짜**: 2026-02-02
**상태**: ✅ Accepted
**작성자**: pm, backend-security-coder

---

## 컨텍스트

### 현재 상황
- **보안 구성**: Spring Security + HTTP Basic Auth 도입 완료 (ADR-0012)
- **인증 메커니즘**: Actuator 엔드포인트 인증 필요 (`/actuator/health` 제외)
- **전송 계층**: HTTPS 강제 설정 없음 (HTTP 평문 전송)
- **보안 헤더**: HSTS (Strict-Transport-Security) 헤더 설정되어 있으나 HTTP 환경에서 무의미

### 보안 위험
- **자격 증명 노출**: HTTP Basic Auth는 Base64로만 인코딩되어 전송 (암호화 없음)
- **MITM 공격**: 평문 HTTP 통신으로 중간자 공격(Man-in-the-Middle) 가능성
- **세션 하이재킹**: 세션 쿠키가 평문으로 전송될 경우 탈취 위험

### 환경 특성
- **네트워크 환경**: 내부 네트워크 (NAS, 외부 인터넷 직접 노출 없음)
- **접근 제어**: 방화벽 및 네트워크 격리로 외부 접근 차단
- **Phase 2 일정**: Week 2-3에 Docker 배포 인프라 작업 예정 (2026-02-03 ~ 02-22)

### 리스크 평가

**리스크 점수**: 0.6 (낮음)

| 항목 | 발생 확률 | 영향도 | 점수 | 근거 |
|------|----------|--------|------|------|
| 내부 네트워크 MITM | 낮음 (0.2) | 중간 (3) | 0.6 | 물리적 접근 제어, 신뢰된 네트워크 |
| 자격 증명 탈취 | 낮음 (0.2) | 높음 (5) | 1.0 | 환경변수 관리, 600 권한 설정 완료 |

**완화 조치** (현재 적용 중):
- ✅ HTTP Basic Auth (최소 인증 계층)
- ✅ HSTS 헤더 설정 (HTTPS 전환 시 즉시 활성화)
- ✅ 환경변수 암호화 저장 (secrets/.env.prod, 권한 600)
- ✅ 내부 네트워크 사용 (외부 직접 노출 없음)

---

## 결정

**Nginx 리버스 프록시 + TLS 종료 방식을 Phase 2 Week 2-3에 통합 구현**

### 결정 사항
1. **현재 (2026-02-02)**: HTTPS 강제 구현 없이 문서화만 수행
2. **Phase 2 Week 2-3 (2026-02-03 ~ 02-22)**: Docker 배포 인프라 작업 시 Nginx + TLS 한 번에 구성
3. **인증서 전략**: 자체 서명 인증서 사용 (내부 네트워크 전용, Let's Encrypt 불필요)
4. **네트워크 격리**: Docker Compose `internal: true` 설정으로 외부 직접 접근 차단
5. **Spring Boot 설정**: `forward-headers-strategy: native`만 추가 (최소 변경)

### 구현 아키텍처

```
[클라이언트]
    |
    | HTTPS (443)
    v
[Nginx Reverse Proxy]
    | - TLS Termination
    | - HTTP → HTTPS Redirect
    | - X-Forwarded-* 헤더 추가
    |
    | HTTP (8080, Docker 내부)
    v
[Spring Boot Application]
    | - forward-headers-strategy: native
    | - HTTP Basic Auth
    | - Actuator 엔드포인트 보호
```

---

## 근거

### 1. 비용 대비 효과 (ROI)
**"두 번 일하기" 방지**
- **지금 구현**: Spring Boot에 requiresChannel() 추가 → 8-12시간 소요
- **Phase 2 마이그레이션**: Nginx로 TLS 이관 → 추가 4-6시간 소요
- **총 비용**: 12-18시간

**한 번에 올바르게 구현**
- **Phase 2 구현**: Nginx + TLS + Docker 통합 → 8시간 소요 (기존 작업에 포함)
- **총 비용**: 8시간 (40% 시간 절약)

### 2. 일정 영향
- **Phase 2 전체 일정**: 62.5시간 (2026-01-28 ~ 02-22)
- **컨테이너화 작업**: 8시간 (Nginx 설정 포함)
- **추가 시간 없음**: 기존 Docker 배포 인프라 작업에 통합 가능

### 3. 운영 편의성
- **인증서 관리 집중화**: Nginx에서 TLS 인증서 단일 관리
- **갱신 프로세스 단순화**: 자체 서명 인증서 (필요 시 갱신 스크립트 작성)
- **디버깅 용이성**: TLS 문제 발생 시 Nginx 로그만 확인

### 4. 기술적 우수성
- **관심사 분리**: Spring Boot는 비즈니스 로직, Nginx는 TLS/라우팅
- **확장성**: 향후 MSA 서비스 추가 시 Nginx 라우팅 규칙만 추가
- **성능**: Nginx는 정적 파일 서빙, TLS 처리에 최적화
- **보안**: TLS 1.3, 강력한 암호화 스위트 설정 가능

---

## 대안

### 대안 1: 지금 Spring Boot requiresChannel() 추가
**장점**:
- ✅ 즉시 HTTPS 강제
- ✅ Spring Security 통합 완벽 활용

**단점**:
- ❌ 8-12시간 소요 (Phase 2 일정 압박)
- ❌ 인증서 관리 부담 (Let's Encrypt 설정, 갱신 자동화)
- ❌ Phase 2에서 Nginx로 마이그레이션 필요 (이중 작업)
- ❌ 디버깅 복잡도 증가 (Spring Boot + TLS)

**채택하지 않은 이유**: "두 번 일하기" 방지, Phase 2 일정 준수

### 대안 2: Docker 네트워크 격리만 적용
**장점**:
- ✅ 1-2시간 소요 (즉시 적용 가능)
- ✅ 외부 직접 접근 차단

**단점**:
- ❌ 내부 네트워크에서 여전히 평문 전송
- ❌ MITM 공격 가능성 (낮지만 존재)
- ❌ 보안 감사 시 지적 가능성

**채택하지 않은 이유**: Phase 2에서 Nginx + TLS로 완전히 해결 예정, 부분적 해결은 기술 부채

### 대안 3: Let's Encrypt 자동화
**장점**:
- ✅ 공인 인증서 (브라우저 경고 없음)
- ✅ 자동 갱신 (Certbot)

**단점**:
- ❌ 외부 도메인 필요 (NAS 내부 네트워크에 불필요)
- ❌ 80포트 노출 필요 (HTTP-01 Challenge)
- ❌ 설정 복잡도 증가

**채택하지 않은 이유**: 내부 네트워크 환경에서 불필요, 자체 서명 인증서로 충분

---

## 구현 계획

### Phase 1: 현재 (2026-02-02)
**완화 조치 유지**:
- ✅ HTTP Basic Auth (Spring Security)
- ✅ HSTS 헤더 설정 (HTTPS 전환 시 활성화 대기)
- ✅ 환경변수 암호화 저장 (secrets/.env.prod, 권한 600)
- ✅ 내부 네트워크 사용 (외부 직접 노출 없음)

**문서화**:
- ✅ ADR-0015 작성 (이 문서)
- ✅ docs/TODO.md 업데이트 (Phase 2 Week 2-3 작업 추가)
- ✅ docs/adr/README.md 업데이트 (ADR 목록 추가)

### Phase 2: Week 2-3 (2026-02-03 ~ 02-22)

#### 1️⃣ Docker 네트워크 격리 (1시간)
```yaml
# docker-compose.yml
services:
  collector:
    networks:
      - internal
networks:
  internal:
    driver: bridge
    internal: true  # 외부 직접 접근 차단
```

#### 2️⃣ Nginx 리버스 프록시 설정 (3시간)
```nginx
# nginx.conf
server {
    listen 443 ssl http2;
    server_name localhost;

    ssl_certificate /etc/nginx/ssl/server.crt;
    ssl_certificate_key /etc/nginx/ssl/server.key;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;

    location / {
        proxy_pass http://collector:8080;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Host $host;
        proxy_set_header X-Forwarded-Port $server_port;
    }
}

server {
    listen 80;
    server_name localhost;
    return 301 https://$host$request_uri;  # HTTP → HTTPS 리디렉트
}
```

#### 3️⃣ 자체 서명 TLS 인증서 생성 (0.5시간)
```bash
# generate-cert.sh
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout server.key \
  -out server.crt \
  -subj "/C=KR/ST=Seoul/L=Seoul/O=CAA/OU=IT/CN=localhost"
```

#### 4️⃣ Spring Boot 설정 (0.5시간)
```yaml
# application-prod.yml
server:
  forward-headers-strategy: native  # X-Forwarded-* 헤더 신뢰
```

#### 5️⃣ Docker Compose 통합 (1시간)
```yaml
# docker-compose.yml
services:
  nginx:
    image: nginx:alpine
    ports:
      - "443:443"
      - "80:80"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
      - ./ssl:/etc/nginx/ssl:ro
    networks:
      - internal

  collector:
    build: .
    expose:
      - "8080"
    networks:
      - internal
```

#### 6️⃣ 테스트 및 검증 (2시간)
- [ ] HTTPS 접근 테스트 (`https://localhost/actuator/health`)
- [ ] HTTP → HTTPS 리디렉트 테스트 (`http://localhost/actuator/health`)
- [ ] X-Forwarded-* 헤더 전파 검증
- [ ] HSTS 헤더 확인 (`Strict-Transport-Security: max-age=31536000`)
- [ ] 인증 필요 엔드포인트 테스트 (`/actuator/env`)

**총 예상 시간**: 8시간 (기존 컨테이너화 작업에 통합)

---

## 결과

### 긍정적 영향
1. ✅ **보안 강화**: 모든 Actuator 접근이 HTTPS로 보호됨
2. ✅ **코드 변경 최소화**: Spring Boot는 `forward-headers-strategy: native`만 추가
3. ✅ **운영 편의성**: Nginx에서 TLS 인증서 집중 관리
4. ✅ **확장성**: MSA 서비스 추가 시 Nginx 라우팅 규칙만 추가
5. ✅ **성능**: Nginx TLS 최적화 (정적 파일 서빙, HTTP/2 지원)
6. ✅ **디버깅 용이성**: TLS 문제는 Nginx 레이어에서 격리

### 부정적 영향
1. ⚠️ **일시적 위험**: Phase 2 Week 2-3 완료까지 평문 HTTP 전송 (내부 네트워크 완화)
2. ⚠️ **인프라 복잡도**: Nginx 컨테이너 추가 (관리 대상 증가)
3. ⚠️ **인증서 경고**: 자체 서명 인증서로 인한 브라우저 경고 (내부 사용자만 영향)

### 완화 조치
- **일시적 위험**: 내부 네트워크 사용, HTTP Basic Auth 유지
- **인프라 복잡도**: Docker Compose로 통합 관리, 설정 파일 단순화
- **인증서 경고**: 내부 사용자 교육 (신뢰된 네트워크, 경고 무시 안내)

---

## 참고 자료

- [ADR-0012: Spring Security 통합](0012-spring-security-integration.md)
- [docs/MILESTONE.md](../MILESTONE.md) - Phase 2 Week 2-3 일정
- [docs/TODO.md](../TODO.md) - Phase 2 배포 자동화 작업 목록
- [RFC 7617: HTTP Basic Authentication](https://datatracker.ietf.org/doc/html/rfc7617)
- [Nginx Reverse Proxy Best Practices](https://docs.nginx.com/nginx/admin-guide/web-server/reverse-proxy/)
- [Docker Compose Network Isolation](https://docs.docker.com/compose/networking/)

---

## 버전 히스토리

- **v1.0** (2026-02-02): 초안 작성 (pm, backend-security-coder)
