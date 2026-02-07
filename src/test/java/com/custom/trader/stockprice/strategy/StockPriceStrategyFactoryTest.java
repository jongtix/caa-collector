package com.custom.trader.stockprice.strategy;

import com.custom.trader.common.enums.AssetType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class StockPriceStrategyFactoryTest {

    @Mock
    private DomesticStockStrategy domesticStockStrategy;

    @Mock
    private DomesticIndexStrategy domesticIndexStrategy;

    @Mock
    private OverseasStockStrategy overseasStockStrategy;

    @Mock
    private OverseasIndexStrategy overseasIndexStrategy;

    private StockPriceStrategyFactory factory;

    @BeforeEach
    void setUp() {
        factory = new StockPriceStrategyFactory(
                domesticStockStrategy,
                domesticIndexStrategy,
                overseasStockStrategy,
                overseasIndexStrategy
        );
        factory.init();
    }

    @Nested
    @DisplayName("init 메소드")
    class Init {

        @Test
        @DisplayName("@PostConstruct로 4개 Strategy를 Map에 등록")
        void PostConstruct로_4개_Strategy를_Map에_등록() {
            // given
            var newFactory = new StockPriceStrategyFactory(
                    domesticStockStrategy,
                    domesticIndexStrategy,
                    overseasStockStrategy,
                    overseasIndexStrategy
            );

            // when
            newFactory.init();

            // then
            assertThat(newFactory.getStrategy(AssetType.DOMESTIC_STOCK)).isEqualTo(domesticStockStrategy);
            assertThat(newFactory.getStrategy(AssetType.DOMESTIC_INDEX)).isEqualTo(domesticIndexStrategy);
            assertThat(newFactory.getStrategy(AssetType.OVERSEAS_STOCK)).isEqualTo(overseasStockStrategy);
            assertThat(newFactory.getStrategy(AssetType.OVERSEAS_INDEX)).isEqualTo(overseasIndexStrategy);
        }

        @Test
        @DisplayName("init 호출 후 모든 AssetType에 대한 Strategy 접근 가능")
        void init_호출_후_모든_AssetType에_대한_Strategy_접근_가능() {
            // when & then
            assertThat(factory.getStrategy(AssetType.DOMESTIC_STOCK)).isNotNull();
            assertThat(factory.getStrategy(AssetType.DOMESTIC_INDEX)).isNotNull();
            assertThat(factory.getStrategy(AssetType.OVERSEAS_STOCK)).isNotNull();
            assertThat(factory.getStrategy(AssetType.OVERSEAS_INDEX)).isNotNull();
        }
    }

    @Nested
    @DisplayName("getStrategy 메소드")
    class GetStrategy {

        @Test
        @DisplayName("DOMESTIC_STOCK AssetType으로 DomesticStockStrategy 반환")
        void DOMESTIC_STOCK_AssetType으로_DomesticStockStrategy_반환() {
            // when
            var strategy = factory.getStrategy(AssetType.DOMESTIC_STOCK);

            // then
            assertThat(strategy).isEqualTo(domesticStockStrategy);
        }

        @Test
        @DisplayName("DOMESTIC_INDEX AssetType으로 DomesticIndexStrategy 반환")
        void DOMESTIC_INDEX_AssetType으로_DomesticIndexStrategy_반환() {
            // when
            var strategy = factory.getStrategy(AssetType.DOMESTIC_INDEX);

            // then
            assertThat(strategy).isEqualTo(domesticIndexStrategy);
        }

        @Test
        @DisplayName("OVERSEAS_STOCK AssetType으로 OverseasStockStrategy 반환")
        void OVERSEAS_STOCK_AssetType으로_OverseasStockStrategy_반환() {
            // when
            var strategy = factory.getStrategy(AssetType.OVERSEAS_STOCK);

            // then
            assertThat(strategy).isEqualTo(overseasStockStrategy);
        }

        @Test
        @DisplayName("OVERSEAS_INDEX AssetType으로 OverseasIndexStrategy 반환")
        void OVERSEAS_INDEX_AssetType으로_OverseasIndexStrategy_반환() {
            // when
            var strategy = factory.getStrategy(AssetType.OVERSEAS_INDEX);

            // then
            assertThat(strategy).isEqualTo(overseasIndexStrategy);
        }

        @Test
        @DisplayName("null AssetType 전달시 IllegalArgumentException 발생")
        void null_AssetType_전달시_IllegalArgumentException_발생() {
            // when & then
            assertThatThrownBy(() -> factory.getStrategy(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("AssetType cannot be null");
        }
    }

    @Nested
    @DisplayName("경계값 테스트")
    class EdgeCases {

        @Test
        @DisplayName("동일한 AssetType으로 여러 번 호출해도 같은 인스턴스 반환")
        void 동일한_AssetType으로_여러번_호출해도_같은_인스턴스_반환() {
            // when
            var strategy1 = factory.getStrategy(AssetType.DOMESTIC_STOCK);
            var strategy2 = factory.getStrategy(AssetType.DOMESTIC_STOCK);

            // then
            assertThat(strategy1).isSameAs(strategy2);
        }

        @Test
        @DisplayName("모든 AssetType enum 값에 대해 Strategy 존재 확인")
        void 모든_AssetType_enum_값에_대해_Strategy_존재_확인() {
            // when & then
            for (AssetType assetType : AssetType.values()) {
                assertThat(factory.getStrategy(assetType))
                        .as("AssetType %s should have a strategy", assetType)
                        .isNotNull();
            }
        }
    }

    @Nested
    @DisplayName("Factory 어노테이션 검증")
    class AnnotationVerification {

        @Test
        @DisplayName("@Component 어노테이션 존재 확인")
        void Component_어노테이션_존재() {
            // when
            var annotation = StockPriceStrategyFactory.class
                    .getAnnotation(org.springframework.stereotype.Component.class);

            // then
            assertThat(annotation).isNotNull();
        }

        @Test
        @DisplayName("init 메소드에 @PostConstruct 어노테이션 존재 확인")
        void init_메소드에_PostConstruct_어노테이션_존재() throws NoSuchMethodException {
            // when
            var method = StockPriceStrategyFactory.class.getMethod("init");
            var annotation = method.getAnnotation(jakarta.annotation.PostConstruct.class);

            // then
            assertThat(annotation).isNotNull();
        }
    }
}
