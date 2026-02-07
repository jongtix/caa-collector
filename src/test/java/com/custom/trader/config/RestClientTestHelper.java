package com.custom.trader.config;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.lang.reflect.Field;
import java.net.http.HttpClient;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RestClient 내부 구조 테스트를 위한 Reflection 헬퍼 유틸리티.
 *
 * <h3>주의사항</h3>
 * <p><b>이 클래스는 Spring Framework의 내부 구현에 의존합니다.</b></p>
 * <ul>
 *   <li>Spring Framework 버전 변경 시 내부 클래스명/필드명이 변경될 수 있음</li>
 *   <li>변경 발생 시 이 클래스의 상수값과 메서드만 수정하면 됨</li>
 *   <li>프로덕션 코드에서는 절대 사용하지 말것 (테스트 전용)</li>
 * </ul>
 *
 * <h3>현재 지원 버전</h3>
 * <ul>
 *   <li>Spring Framework: 6.2.1 (Spring Boot 3.5.9)</li>
 *   <li>테스트 일자: 2026-02-02</li>
 * </ul>
 *
 * <h3>버전 변경 시 수정 가이드</h3>
 * <p>Spring 버전 업그레이드 후 테스트 실패 시:</p>
 * <ol>
 *   <li>INTERNAL_CLASS_NAME: RestClient 구현 클래스명 확인</li>
 *   <li>REQUEST_FACTORY_FIELD_NAME: RequestFactory 필드명 확인</li>
 *   <li>HTTP_CLIENT_FIELD_NAME: HttpClient 필드명 확인</li>
 *   <li>READ_TIMEOUT_FIELD_NAME: ReadTimeout 필드명 확인</li>
 * </ol>
 *
 * @see RestClientConfigTest
 * @since 2026-02-02
 */
public final class RestClientTestHelper {

    /**
     * RestClient 내부 구현 클래스명 (Spring Framework 6.2.1 기준).
     *
     * <p><b>변경 가능성:</b> Spring 버전 업그레이드 시 클래스명이 변경될 수 있음</p>
     */
    private static final String INTERNAL_CLASS_NAME = "org.springframework.web.client.DefaultRestClient";

    /**
     * RequestFactory 필드명 (Spring Framework 6.2.1 기준).
     *
     * <p><b>변경 가능성:</b> Spring 내부 구조 변경 시 필드명이 변경될 수 있음</p>
     */
    private static final String REQUEST_FACTORY_FIELD_NAME = "clientRequestFactory";

    /**
     * HttpClient 필드명 (JdkClientHttpRequestFactory 기준).
     */
    private static final String HTTP_CLIENT_FIELD_NAME = "httpClient";

    /**
     * ReadTimeout 필드명 (JdkClientHttpRequestFactory 기준).
     */
    private static final String READ_TIMEOUT_FIELD_NAME = "readTimeout";

    private RestClientTestHelper() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * RestClient에서 JdkClientHttpRequestFactory 추출.
     *
     * <p>RestClient는 내부적으로 requestFactory를 private 필드로 가지고 있으므로
     * Reflection을 사용하여 접근한다.</p>
     *
     * <p><b>내부 구조 (Spring Framework 6.2.1):</b></p>
     * <pre>
     * RestClient (interface)
     *   └─ DefaultRestClient (implementation)
     *        └─ clientRequestFactory (field)
     *             └─ JdkClientHttpRequestFactory (실제 구현체)
     * </pre>
     *
     * @param restClient RestClient 인스턴스
     * @return JdkClientHttpRequestFactory 인스턴스
     * @throws Exception Reflection 오류 또는 타입 불일치 시
     */
    public static JdkClientHttpRequestFactory extractRequestFactory(RestClient restClient) throws Exception {
        var defaultRestClientClass = Class.forName(INTERNAL_CLASS_NAME);
        var requestFactoryField = findFieldInHierarchy(defaultRestClientClass, REQUEST_FACTORY_FIELD_NAME);
        requestFactoryField.setAccessible(true);

        var requestFactory = requestFactoryField.get(restClient);
        assertThat(requestFactory).isInstanceOf(JdkClientHttpRequestFactory.class);

        return (JdkClientHttpRequestFactory) requestFactory;
    }

    /**
     * JdkClientHttpRequestFactory에서 HttpClient 추출.
     *
     * @param factory JdkClientHttpRequestFactory 인스턴스
     * @return HttpClient 인스턴스
     * @throws Exception Reflection 오류 시
     */
    public static HttpClient extractHttpClient(JdkClientHttpRequestFactory factory) throws Exception {
        var httpClientField = JdkClientHttpRequestFactory.class.getDeclaredField(HTTP_CLIENT_FIELD_NAME);
        httpClientField.setAccessible(true);

        var httpClient = httpClientField.get(factory);
        assertThat(httpClient).isInstanceOf(HttpClient.class);

        return (HttpClient) httpClient;
    }

    /**
     * JdkClientHttpRequestFactory에서 readTimeout 추출.
     *
     * @param factory JdkClientHttpRequestFactory 인스턴스
     * @return ReadTimeout Duration
     * @throws Exception Reflection 오류 시
     */
    public static Duration extractReadTimeout(JdkClientHttpRequestFactory factory) throws Exception {
        var readTimeoutField = JdkClientHttpRequestFactory.class.getDeclaredField(READ_TIMEOUT_FIELD_NAME);
        readTimeoutField.setAccessible(true);

        var readTimeout = readTimeoutField.get(factory);
        assertThat(readTimeout).isInstanceOf(Duration.class);

        return (Duration) readTimeout;
    }

    /**
     * 클래스 계층 구조에서 특정 필드 찾기.
     *
     * <p>상위 클래스까지 탐색하여 필드를 찾는다.</p>
     *
     * @param clazz 시작 클래스
     * @param fieldName 찾을 필드명
     * @return Field 인스턴스
     * @throws NoSuchFieldException 필드를 찾지 못한 경우
     */
    private static Field findFieldInHierarchy(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Class<?> currentClass = clazz;

        while (currentClass != null) {
            try {
                return currentClass.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                currentClass = currentClass.getSuperclass();
            }
        }

        throw new NoSuchFieldException("Field " + fieldName + " not found in class hierarchy");
    }
}
