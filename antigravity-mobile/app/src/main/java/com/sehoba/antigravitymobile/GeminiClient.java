package com.sehoba.antigravitymobile;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

final class GeminiClient {
    private static final String ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/interactions?alt=sse";
    interface Listener { void onDelta(String text); void onComplete(); void onError(String message); }
    private GeminiClient() {}

    static void streamInteraction(String apiKey, boolean agentMode, String modelOrAgent, String systemInstruction, String input, Listener listener) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(ENDPOINT).openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(180000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("Accept", "text/event-stream");
            connection.setRequestProperty("x-goog-api-key", apiKey);
            JSONObject body = new JSONObject();
            if (agentMode) body.put("agent", modelOrAgent); else body.put("model", modelOrAgent);
            body.put("input", input);
            body.put("stream", true);
            body.put("store", false);
            if (systemInstruction != null && !systemInstruction.trim().isEmpty()) body.put("system_instruction", systemInstruction.trim());
            byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(payload.length);
            try (OutputStream out = connection.getOutputStream()) { out.write(payload); }
            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) {
                String error = readAll(connection.getErrorStream());
                listener.onError("HTTP " + code + (error.isEmpty() ? "" : ": " + extractError(error)));
                return;
            }
            boolean sawDelta = false;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("data:")) continue;
                    String data = line.substring(5).trim();
                    if (data.isEmpty() || "[DONE]".equals(data)) continue;
                    JSONObject event;
                    try { event = new JSONObject(data); } catch (Exception ignored) { continue; }
                    if (event.has("error")) { listener.onError(extractError(event.toString())); return; }
                    if ("step.delta".equals(event.optString("event_type", ""))) {
                        JSONObject delta = event.optJSONObject("delta");
                        if (delta != null && "text".equals(delta.optString("type"))) {
                            String text = delta.optString("text", "");
                            if (!text.isEmpty()) { sawDelta = true; listener.onDelta(text); }
                        }
                    } else if (!sawDelta && event.has("steps")) {
                        String text = extractTextFromSteps(event.optJSONArray("steps"));
                        if (!text.isEmpty()) listener.onDelta(text);
                    }
                }
            }
            listener.onComplete();
        } catch (Exception e) {
            listener.onError(e.getClass().getSimpleName() + (e.getMessage() == null ? "" : ": " + e.getMessage()));
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static String extractTextFromSteps(JSONArray steps) {
        if (steps == null) return "";
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < steps.length(); i++) {
            JSONObject step = steps.optJSONObject(i);
            if (step == null || !"model_output".equals(step.optString("type"))) continue;
            JSONArray content = step.optJSONArray("content");
            if (content == null) continue;
            for (int j = 0; j < content.length(); j++) {
                JSONObject part = content.optJSONObject(j);
                if (part != null && "text".equals(part.optString("type"))) result.append(part.optString("text", ""));
            }
        }
        return result.toString();
    }

    private static String extractError(String body) {
        try {
            JSONObject root = new JSONObject(body);
            JSONObject error = root.optJSONObject("error");
            if (error != null) return error.optString("message", body);
            return root.optString("message", body);
        } catch (Exception ignored) {
            return body.length() > 700 ? body.substring(0, 700) + "…" : body;
        }
    }

    private static String readAll(InputStream input) {
        if (input == null) return "";
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line; while ((line = reader.readLine()) != null) result.append(line).append('\n');
        } catch (Exception ignored) {}
        return result.toString().trim();
    }
}
