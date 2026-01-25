package com.custom.trader.common.converter;

import com.custom.trader.common.enums.AssetType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class AssetTypeConverter implements AttributeConverter<AssetType, Integer> {

    @Override
    public Integer convertToDatabaseColumn(AssetType attribute) {
        return attribute != null ? attribute.getCode() : null;
    }

    @Override
    public AssetType convertToEntityAttribute(Integer dbData) {
        return dbData != null ? AssetType.fromCode(dbData) : null;
    }
}
