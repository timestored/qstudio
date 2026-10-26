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

class OpenAIProvider implements AIProvider {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String DEFAULT_BASE_URL = "https://api.openai.com/v1";
    private static final String DEFAULT_MODEL = "gpt-4o-mini";
    public static final String[] MODELS = { "gpt-4o-mini", "gpt-4o", "gpt-4.1", "gpt-4.1-mini", "gpt-4.1-nano", "o3-mini" };

    private String apiKey = "";
    private String model = DEFAULT_MODEL;
    private String baseUrl = DEFAULT_BASE_URL;

    @Override public String getName() { return "OpenAI"; }

    @Override public boolean isConfigured() {
        return apiKey != null && apiKey.trim().length() > 1;
    }

    @Override public String getConfigurationHelp() {
        return "You must configure an OpenAI API key in Settings -> Preferences -> AI";
    }

    @Override public void setApiKey(String apiKey) { this.apiKey = apiKey != null ? apiKey : ""; }
    @Override public void setModel(String model) { this.model = (model != null && !model.isEmpty()) ? model : DEFAULT_MODEL; }
    @Override public void setBaseUrl(String baseUrl) { this.baseUrl = (baseUrl != null && !baseUrl.isEmpty()) ? baseUrl : DEFAULT_BASE_URL; }

    @Override public String[] getModelOptions() { return MODELS; }

    public String getApiKey() { return apiKey; }
    public String getModel() { return model; }
    public String getBaseUrl() { return baseUrl; }

    @Override public String chat(List<ChatMessage> messages) throws IOException {
        if (!isConfigured()) {
            throw new IllegalStateException("OpenAI API key is not set");
        }
        System.setProperty("https.protocols", "TLSv1.2");

        String url = baseUrl + "/chat/completions";
        String jsonBody = buildRequestJson(messages);
        String responseJson = doPost(url, jsonBody);
        return parseResponse(responseJson);
    }

    String buildRequestJson(List<ChatMessage> messages) throws IOException {
        List<Map<String, String>> msgList = new ArrayList<>();
        for (ChatMessage msg : messages) {
            Map<String, String> m = new HashMap<>();
            m.put("role", msg.getRole().name().toLowerCase());
            m.put("content", msg.getContent());
            msgList.add(m);
        }
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", msgList);
        return MAPPER.writeValueAsString(body);
    }

    private String doPost(String url, String jsonBody) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setDoOutput(true);
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
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
            throw new IOException("OpenAI API error (HTTP " + code + "): " + sb.toString().trim());
        }
        return sb.toString();
    }

    static String parseResponse(String responseJson) throws IOException {
        JsonNode root = MAPPER.readTree(responseJson);
        JsonNode choices = root.get("choices");
        if (choices != null && choices.isArray() && choices.size() > 0) {
            JsonNode message = choices.get(0).get("message");
            if (message != null) {
                JsonNode content = message.get("content");
                if (content != null) {
                    return content.asText();
                }
            }
        }
        JsonNode error = root.get("error");
        if (error != null) {
            String msg = error.has("message") ? error.get("message").asText() : error.toString();
            throw new IOException("OpenAI API error: " + msg);
        }
        throw new IOException("Unexpected OpenAI response format: " + responseJson);
    }
}
