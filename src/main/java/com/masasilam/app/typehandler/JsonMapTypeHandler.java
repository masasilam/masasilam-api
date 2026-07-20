package com.masasilam.app.typehandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;

@MappedTypes(Map.class)
@MappedJdbcTypes(JdbcType.OTHER)
public class JsonMapTypeHandler extends BaseTypeHandler<Map<String, Object>> {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Map<String, Object> parameter, JdbcType jdbcType) throws SQLException {
        try {
            String json = objectMapper.writeValueAsString(parameter);
            ps.setString(i, json);
        } catch (JsonProcessingException e) {
            throw new SQLException("Error converting Map to JSON", e);
        }
    }

    @Override
    public Map<String, Object> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        Object jsonObject = rs.getObject(columnName);
        return parseJson(jsonObject);
    }

    @Override
    public Map<String, Object> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        Object jsonObject = rs.getObject(columnIndex);
        return parseJson(jsonObject);
    }

    @Override
    public Map<String, Object> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        Object jsonObject = cs.getObject(columnIndex);
        return parseJson(jsonObject);
    }

    private Map<String, Object> parseJson(Object jsonObject) throws SQLException {
        if (jsonObject == null) {
            return Collections.emptyMap();
        }

        try {
            if (jsonObject instanceof Map<?, ?> rawMap) {
                return objectMapper.convertValue(rawMap, new TypeReference<>() {
                });
            }

            if (jsonObject instanceof String json) {
                if (json.isEmpty()) {
                    return Collections.emptyMap();
                }
                return objectMapper.readValue(json, new TypeReference<>() {
                });
            }

            return Collections.emptyMap();
        } catch (IllegalArgumentException | JsonProcessingException e) {
            throw new SQLException("Error parsing JSON: " + jsonObject, e);
        }
    }
}