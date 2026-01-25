package com.custom.trader.common.converter;

import com.custom.trader.common.enums.MarketCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class MarketCodeConverter implements AttributeConverter<MarketCode, Integer> {

    @Override
    public Integer convertToDatabaseColumn(MarketCode attribute) {
        return attribute != null ? attribute.getCode() : null;
    }

    @Override
    public MarketCode convertToEntityAttribute(Integer dbData) {
        return dbData != null ? MarketCode.fromCode(dbData) : null;
    }
}
