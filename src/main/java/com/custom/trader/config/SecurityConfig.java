package com.custom.trader.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 설정
 *
 * <p>Actuator 엔드포인트 보호를 담당한다.</p>
 *
 * <h3>접근 정책</h3>
 * <ul>
 *   <li>공개: /internal/management/health, /internal/management/health/liveness, /internal/management/health/readiness</li>
 *   <li>인증 필요: 그 외 모든 Actuator 엔드포인트</li>
 * </ul>
 *
 * <h3>보안 응답 헤더 (M-04)</h3>
 * <ul>
 *   <li><b>X-Frame-Options</b>: {@code DENY} - 클릭재킹(Clickjacking) 공격 방어를 위해 iframe 완전 차단</li>
 *   <li><b>Content-Security-Policy</b>: {@code default-src 'self'} - 자체 출처의 리소스만 허용하여 XSS 공격 방어</li>
 *   <li><b>Strict-Transport-Security</b>: max-age=31536000; includeSubDomains - HTTPS 강제로 중간자 공격(MITM) 방어</li>
 * </ul>
 */
@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private static final String ROLE_ACTUATOR = "ACTUATOR";

    private final Environment environment;

    @Value("${security.actuator.username}")
    private String actuatorUsername;

    @Value("${security.actuator.password}")
    private String actuatorPassword;

    public SecurityConfig(Environment environment) {
        this.environment = environment;
    }

    /**
     * Actuator 자격 증명 검증
     *
     * <p>애플리케이션 시작 시 필수 환경변수가 올바르게 설정되었는지 검증한다.</p>
     * <ul>
     *   <li>ACTUATOR_USERNAME: 비어있지 않아야 함</li>
     *   <li>ACTUATOR_PASSWORD: 비어있지 않아야 함</li>
     * </ul>
     *
     * @throws IllegalStateException 자격 증명이 유효하지 않을 경우
     */
    @PostConstruct
    void validateActuatorCredentials() {
        validateUsernameNotBlank();
        validatePasswordNotBlank();
        validatePasswordLengthForProduction();
        warnPasswordLengthRecommendation();

        log.info("Actuator security credentials validated successfully");
    }

    /**
     * Actuator Username이 설정되어 있는지 검증
     *
     * @throws IllegalStateException Username이 null이거나 빈 문자열인 경우
     */
    private void validateUsernameNotBlank() {
        if (actuatorUsername == null || actuatorUsername.isBlank()) {
            throw new IllegalStateException(
                    "ACTUATOR_USERNAME must be configured. Application will not start without it.");
        }
    }

    /**
     * Actuator Password가 설정되어 있는지 검증
     *
     * @throws IllegalStateException Password가 null이거나 빈 문자열인 경우
     */
    private void validatePasswordNotBlank() {
        if (actuatorPassword == null || actuatorPassword.isBlank()) {
            throw new IllegalStateException(
                    "ACTUATOR_PASSWORD must be configured. Application will not start without it.");
        }
    }

    /**
     * Production 환경에서 Password 최소 길이(16자) 강제
     *
     * @throws IllegalStateException prod 프로필에서 Password가 16자 미만인 경우
     */
    private void validatePasswordLengthForProduction() {
        if (isProdProfile() && actuatorPassword.length() < 16) {
            throw new IllegalStateException(
                    "ACTUATOR_PASSWORD must be at least 16 characters in production. " +
                    "Recommended: openssl rand -base64 24");
        }
    }

    /**
     * 모든 환경에서 Password 권장 길이(12자) 미달 시 경고
     */
    private void warnPasswordLengthRecommendation() {
        if (actuatorPassword.length() < 12) {
            log.warn("ACTUATOR_PASSWORD is shorter than 12 characters. " +
                     "Consider using a stronger password: openssl rand -base64 24");
        }
    }

    /**
     * 현재 활성 프로필이 prod인지 확인
     *
     * @return prod 프로필이 활성화된 경우 true
     */
    private boolean isProdProfile() {
        return environment.matchesProfiles("prod");
    }

    /**
     * 보안 필터 체인 구성
     *
     * @param http HttpSecurity 구성 객체
     * @return 구성된 SecurityFilterChain
     * @throws Exception 보안 설정 실패 시
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // Stateless API이므로 CSRF 보호 불필요
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 헬스체크 엔드포인트 공개 (Kubernetes liveness/readiness probe)
                        // - prod: /internal/management/health (9090 포트, management 서버 분리)
                        // - dev/local: /actuator/health (8080 포트, management 서버 미분리)
                        // 보안: prod 환경에서는 management.server.port 설정으로 8080 포트에 /actuator 경로가 존재하지 않음
                        .requestMatchers(
                                "/internal/management/health",
                                "/internal/management/health/liveness",
                                "/internal/management/health/readiness",
                                "/actuator/health",
                                "/actuator/health/liveness",
                                "/actuator/health/readiness"
                        ).permitAll()
                        // Actuator 엔드포인트 인증 필요
                        .requestMatchers("/internal/management/**", "/actuator/**").hasRole(ROLE_ACTUATOR)
                        // 그 외 모든 요청 인증 필요 (Secure by Default)
                        // - 미등록 경로에 대한 기본 보호 (인증 없이 접근 불가)
                        // - Phase 3 REST API 추가 시 반드시 명시적 접근 규칙을 위에 추가할 것
                        // - denyAll() 대신 authenticated()를 사용하는 이유:
                        //   1) InMemory에 ACTUATOR 사용자만 존재하여 실질적 보안 차이 미미
                        //   2) /error 등 스프링 기본 경로 차단으로 인한 운영 불편 방지
                        //   3) 디버깅 및 운영 편의성 확보
                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults())
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'"))
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000)
                        )
                )
                .build();
    }

    /**
     * 사용자 인증 정보 관리
     *
     * <p>환경변수로 주입된 자격 증명을 사용하여 InMemory 사용자를 생성한다.</p>
     *
     * @param passwordEncoder 비밀번호 인코더
     * @return UserDetailsService 구현체
     */
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        var actuatorUser = User.builder()
                .username(actuatorUsername)
                .password(passwordEncoder.encode(actuatorPassword))
                .roles(ROLE_ACTUATOR)
                .build();

        return new InMemoryUserDetailsManager(actuatorUser);
    }

    /**
     * 비밀번호 인코더
     *
     * @return BCryptPasswordEncoder 인스턴스
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
