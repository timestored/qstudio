/*
 * qStudio - Free SQL Analysis Tool
 * Copyright C 2013-2024 TimeStored
 *
 * Licensed under the Apache License, Version 2.0 the "License";
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.timestored.misc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;

class ClaudeProvider implements AIProvider {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String DEFAULT_MODEL = "claude-sonnet-4-20250514";
    private static final String API_VERSION = "2023-06-01";
    private static final int MAX_TOKENS = 4096;
    public static final String[] MODELS = { "claude-sonnet-4-20250514", "claude-haiku-4-20250414", "claude-3-5-haiku-20241022" };

    @Getter private String apiKey = "";
    @Getter private String model = DEFAULT_MODEL;

    @Override public String getName() { return "Claude"; }

    @Override public boolean isConfigured() {
        return apiKey != null && apiKey.trim().length() > 1;
    }

    @Override public String getConfigurationHelp() {
        return "You must configure a Claude API key in Settings -> Preferences -> AI";
    }

    @Override public void setApiKey(String apiKey) { this.apiKey = apiKey != null ? apiKey : ""; }
    @Override public void setModel(String model) { this.model = (model != null && !model.isEmpty()) ? model : DEFAULT_MODEL; }
    @Override public void setBaseUrl(String baseUrl) { /* Claude does not support custom base URL */ }

    @Override public String[] getModelOptions() { return MODELS; }

    @Override public String chat(List<ChatMessage> messages) throws IOException {
        if (!isConfigured()) {
            throw new IllegalStateException("Claude API key is not set");
        }
        System.setProperty("https.protocols", "TLSv1.2");

        String jsonBody = buildRequestJson(messages);
        String responseJson = doPost(jsonBody);
        return parseResponse(responseJson);
    }

    String buildRequestJson(List<ChatMessage> messages) throws IOException {
        String systemPrompt = null;
        List<Map<String, String>> msgList = new ArrayList<>();

        for (ChatMessage msg : messages) {
            if (msg.getRole() == ChatMessage.Role.SYSTEM) {
                if (systemPrompt == null) {
                    systemPrompt = msg.getContent();
                } else {
                    systemPrompt += "\n" + msg.getContent();
                }
            } else {
                Map<String, String> m = new HashMap<>();
                m.put("role", msg.getRole().name().toLowerCase());
                m.put("content", msg.getContent());
                msgList.add(m);
            }
        }

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("max_tokens", MAX_TOKENS);
        body.put("messages", msgList);
        if (systemPrompt != null) {
            body.put("system", systemPrompt);
        }
        return MAPPER.writeValueAsString(body);
    }

    private String doPost(String jsonBody) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(API_URL).openConnection();
        conn.setDoOutput(true);
        conn.setRequestProperty("x-api-key", apiKey);
        conn.setRequestProperty("anthropic-version", API_VERSION);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(60000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes("utf-8"));
        }

        int code = conn.getResponseCode();
        BufferedReader reader;
        if (code == HttpURLConnection.HTTP_OK) {
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        } else {
            reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
        }

        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();

        if (code != HttpURLConnection.HTTP_OK) {
            throw new IOException("Claude API error (HTTP " + code + "): " + sb.toString().trim());
        }
        return sb.toString();
    }

    static String parseResponse(String responseJson) throws IOException {
        JsonNode root = MAPPER.readTree(responseJson);

        JsonNode errorNode = root.get("error");
        if (errorNode != null) {
            String msg = errorNode.has("message") ? errorNode.get("message").asText() : errorNode.toString();
            throw new IOException("Claude API error: " + msg);
        }

        JsonNode content = root.get("content");
        if (content != null && content.isArray()) {
            StringBuilder result = new StringBuilder();
            for (JsonNode block : content) {
                if ("text".equals(block.path("type").asText())) {
                    if (result.length() > 0) result.append("\n");
                    result.append(block.get("text").asText());
                }
            }
            if (result.length() > 0) {
                return result.toString();
            }
        }
        throw new IOException("Unexpected Claude response format: " + responseJson);
    }
}
