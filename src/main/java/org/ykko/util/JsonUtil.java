package org.ykko.util;

import com.fasterxml.jackson.databind.JsonNode;

public class JsonUtil {

    public static String getValue(JsonNode node, String fieldName) {
        if (node == null || !node.has(fieldName)) {
            return null;
        }
        JsonNode fieldNode = node.get(fieldName);
        return fieldNode.isNull() ? null : fieldNode.asText();
    }

}
