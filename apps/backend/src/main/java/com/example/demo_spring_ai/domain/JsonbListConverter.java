package com.example.demo_spring_ai.domain;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Converter
public class JsonbListConverter implements AttributeConverter<List<String>, String> {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static final TypeReference<List<String>> LIST_TYPE = new TypeReference<>() {};

	@Override
	public String convertToDatabaseColumn(List<String> attribute) {
		try {
			return OBJECT_MAPPER.writeValueAsString(attribute == null ? List.of() : attribute);
		}
		catch (Exception ex) {
			throw new IllegalArgumentException("Unable to serialize JSONB list", ex);
		}
	}

	@Override
	public List<String> convertToEntityAttribute(String dbData) {
		try {
			return dbData == null || dbData.isBlank() ? new ArrayList<>() : OBJECT_MAPPER.readValue(dbData, LIST_TYPE);
		}
		catch (Exception ex) {
			throw new IllegalArgumentException("Unable to deserialize JSONB list", ex);
		}
	}
}
