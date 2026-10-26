package com.timestored.qdoc.parser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * Lint configuration loaded from a 2-column CSV file.
 * Format: RULE_NAME,value  (value is a number for thresholds, "false" to disable)
 * Rules not listed use defaults.
 */
public class QLintConfig {
    private final Map<String, String> overrides;

    // Default thresholds
    public static final int DEFAULT_MAX_LINE_LENGTH = 120;
    public static final int DEFAULT_MAX_FUNCTION_LINES = 40;
    public static final int DEFAULT_MAX_PARAMS = 8;
    public static final int DEFAULT_MAX_GLOBALS = 20;
    public static final int DEFAULT_MAX_CONSTANTS = 110;
    public static final int DEFAULT_MAX_LOCALS = 110;

    public QLintConfig(Map<String, String> overrides) {
        this.overrides = overrides != null ? overrides : Collections.emptyMap();
    }

    public static QLintConfig defaultConfig() {
        return new QLintConfig(Collections.emptyMap());
    }

    public static QLintConfig load(File file) throws IOException {
        Map<String, String> map = new LinkedHashMap<>();
        for (String line : Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] parts = line.split(",", 2);
            if (parts.length >= 1) {
                String rule = parts[0].trim();
                String value = parts.length > 1 ? parts[1].trim() : "true";
                map.put(rule, value);
            }
        }
        return new QLintConfig(map);
    }

    public boolean isEnabled(QLintRule rule) {
        String val = overrides.get(rule.name());
        return !"false".equalsIgnoreCase(val);
    }

    public int getThreshold(QLintRule rule, int defaultValue) {
        String val = overrides.get(rule.name());
        if (val == null) return defaultValue;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
