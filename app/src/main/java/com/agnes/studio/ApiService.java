package com.agnes.studio;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ApiService {
    private static final String TAG = "ApiService";
    private static final String PREFS_NAME = "agnes_prefs";
    private static final String KEY_API_KEY = "api_key";
    private static final String KEY_BASE_URL = "base_url";

    // 内置 Agnes AI 地址
    public static final String URL_CN = "https://api.agnes-ai.cn/v1";
    public static final String URL_INTL = "https://apihub.agnes-ai.com/v1";
    public static final String URL_DEEPSEEK = "https://api.deepseek.com/v1";
    public static final String URL_QWEN = "https://dashscope.aliyuncs.com/compatible-mode/v1";
    public static final String URL_DOUBAO = "https://ark.cn-beijing.volces.com/api/v3";
    public static final String URL_MINIMAX = "https://api.minimaxi.com/v1";
    public static final String URL_OPENAI = "https://api.openai.com/v1";

    public static final String[][] API_URLS = {
        {"Agnes AI 国内站", URL_CN},
        {"Agnes AI 国际站", URL_INTL},
        {"DeepSeek", URL_DEEPSEEK},
        {"通义千问", URL_QWEN},
        {"豆包", URL_DOUBAO},
        {"MiniMax", URL_MINIMAX},
        {"OpenAI", URL_OPENAI},
        {"自定义", ""},
    };

    // 内置模型
    public static final String[][] TEXT_MODELS = {
        {"agnes-2.5-flash", "Agnes 2.5 Flash (免费)"},
        {"agnes-2.5-pro-alpha", "Agnes 2.5 Pro"},
        {"agnes-2.0-flash", "Agnes 2.0 Flash"},
        {"deepseek-v4-flash", "DeepSeek V4 Flash"},
        {"deepseek-chat", "DeepSeek Chat"},
        {"deepseek-reasoner", "DeepSeek Reasoner"},
        {"qwen-turbo", "通义千问 Turbo"},
        {"qwen-plus", "通义千问 Plus"},
        {"doubao-pro-32k", "豆包 Pro 32K"},
        {"doubao-lite-32k", "豆包 Lite 32K"},
    };

    public static final String[][] IMAGE_MODELS = {
        {"agnes-image-2.1-flash", "Agnes Image 2.1 (推荐)"},
        {"agnes-image-2.0-flash", "Agnes Image 2.0"},
        {"doubao-seedream-3-0", "豆包 Seedream 3.0"},
        {"minimax-image-01", "MiniMax Image 01"},
        {"qwen-image-plus", "通义千问 Image"},
    };

    public static final String[][] VIDEO_MODELS = {
        {"agnes-video-v2.0", "Agnes Video 2.0 (推荐)"},
        {"minimax-video-01", "MiniMax Video 01"},
        {"doubao-seaweed-t2v", "豆包 Seaweed"},
        {"qwen-video-gen", "通义千问 Video"},
    };

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client;
    private final SharedPreferences prefs;

    public ApiService(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public String getApiKey() {
        return prefs.getString(KEY_API_KEY, "");
    }

    public void setApiKey(String apiKey) {
        prefs.edit().putString(KEY_API_KEY, apiKey).apply();
    }

    public String getBaseUrl() {
        return prefs.getString(KEY_BASE_URL, URL_CN);
    }

    public void setBaseUrl(String baseUrl) {
        prefs.edit().putString(KEY_BASE_URL, baseUrl).apply();
    }

    // ==================== 文本生成 ====================

    public String chatCompletion(String model, String prompt, String systemPrompt) throws IOException {
        JSONObject body = new JSONObject();
        try {
            body.put("model", model);
            JSONArray messages = new JSONArray();
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                JSONObject sysMsg = new JSONObject();
                sysMsg.put("role", "system");
                sysMsg.put("content", systemPrompt);
                messages.put(sysMsg);
            }
            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content", prompt);
            messages.put(userMsg);
            body.put("messages", messages);
            body.put("temperature", 0.7);
            body.put("max_tokens", 4096);
        } catch (JSONException e) {
            throw new IOException("JSON error", e);
        }

        Request request = new Request.Builder()
                .url(getBaseUrl() + "/chat/completions")
                .addHeader("Authorization", "Bearer " + getApiKey())
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("API错误: " + response.code() + "\n" + response.body().string());
            }
            JSONObject resp = new JSONObject(response.body().string());
            return resp.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");
        } catch (JSONException e) {
            throw new IOException("响应解析失败", e);
        }
    }

    // ==================== 图片生成 ====================

    public String generateImage(String model, String prompt, String imageUrl) throws IOException {
        JSONObject body = new JSONObject();
        try {
            body.put("model", model);
            body.put("prompt", prompt);
            if (imageUrl != null && !imageUrl.isEmpty()) {
                body.put("image_url", imageUrl);
            }
            body.put("n", 1);
            body.put("size", "1024x1024");
        } catch (JSONException e) {
            throw new IOException("JSON error", e);
        }

        Request request = new Request.Builder()
                .url(getBaseUrl() + "/images/generations")
                .addHeader("Authorization", "Bearer " + getApiKey())
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("API错误: " + response.code() + "\n" + response.body().string());
            }
            JSONObject resp = new JSONObject(response.body().string());
            return resp.getJSONArray("data")
                    .getJSONObject(0)
                    .getString("url");
        } catch (JSONException e) {
            throw new IOException("响应解析失败", e);
        }
    }

    // ==================== 视频生成 ====================

    public String generateVideo(String model, String prompt, String imageUrl) throws IOException {
        JSONObject body = new JSONObject();
        try {
            body.put("model", model);
            body.put("prompt", prompt);
            if (imageUrl != null && !imageUrl.isEmpty()) {
                body.put("image_url", imageUrl);
            }
            body.put("num_frames", 121);
            body.put("frame_rate", 24);
        } catch (JSONException e) {
            throw new IOException("JSON error", e);
        }

        Request request = new Request.Builder()
                .url(getBaseUrl() + "/video/generations")
                .addHeader("Authorization", "Bearer " + getApiKey())
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("API错误: " + response.code() + "\n" + response.body().string());
            }
            JSONObject resp = new JSONObject(response.body().string());
            return resp.getString("task_id");
        } catch (JSONException e) {
            throw new IOException("响应解析失败", e);
        }
    }

    // ==================== 视频状态查询 ====================

    public JSONObject getVideoStatus(String taskId) throws IOException {
        Request request = new Request.Builder()
                .url(getBaseUrl() + "/video/generations/" + taskId)
                .addHeader("Authorization", "Bearer " + getApiKey())
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("API错误: " + response.code());
            }
            return new JSONObject(response.body().string());
        } catch (JSONException e) {
            throw new IOException("响应解析失败", e);
        }
    }

    // ==================== 下载文件 ====================

    public byte[] downloadFile(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("下载失败: " + response.code());
            }
            return response.body().bytes();
        }
    }
}
