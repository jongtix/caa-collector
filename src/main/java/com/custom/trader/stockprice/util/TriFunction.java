package com.custom.trader.stockprice.util;

/**
 * 3개의 파라미터를 받아 결과를 반환하는 함수형 인터페이스.
 *
 * @param <T> 첫 번째 파라미터 타입
 * @param <U> 두 번째 파라미터 타입
 * @param <V> 세 번째 파라미터 타입
 * @param <R> 결과 타입
 */
@FunctionalInterface
public interface TriFunction<T, U, V, R> {

    /**
     * 주어진 3개의 인자를 받아 결과를 반환합니다.
     *
     * @param t 첫 번째 인자
     * @param u 두 번째 인자
     * @param v 세 번째 인자
     * @return 함수 실행 결과
     */
    R apply(T t, U u, V v);
}
