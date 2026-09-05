package com.agnes.studio;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ApiService {
    private static final String TAG = "ApiService";
    private static final String PREFS_NAME = "agnes_prefs";

    // 通用配置 key
    private static final String KEY_API_KEY = "api_key";
    private static final String KEY_API_KEYS_LIST = "api_keys_list";
    private static final String KEY_BASE_URL = "base_url";
    private static final String KEY_API_KEY_INDEX = "api_key_index";

    // 各 section 配置 key 前缀: text_, image_, video_
    private static final String KEY_PREFIX_URL = "_url";
    private static final String KEY_PREFIX_CUSTOM_URL = "_custom_url";
    private static final String KEY_PREFIX_KEY = "_key";
    private static final String KEY_PREFIX_MODEL = "_model";
    private static final String KEY_PREFIX_CUSTOM_MODEL = "_custom_model";

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
        {"agnes-image-2.5-flash", "Agnes Image 2.5 (推荐)"},
        {"agnes-image-2.1-flash", "Agnes Image 2.1"},
        {"agnes-image-2.0-flash", "Agnes Image 2.0"},
        {"doubao-seedream-3-0", "豆包 Seedream 3.0"},
        {"minimax-image-01", "MiniMax Image 01"},
        {"qwen-image-plus", "通义千问 Image"},
    };

    public static final String[][] VIDEO_MODELS = {
        {"agnes-video-2.5-flash", "Agnes Video 2.5 (推荐)"},
        {"agnes-video-v2.0", "Agnes Video 2.0"},
        {"minimax-video-01", "MiniMax Video 01"},
        {"doubao-seaweed-t2v", "豆包 Seaweed"},
        {"qwen-video-gen", "通义千问 Video"},
    };

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client;
    private final SharedPreferences prefs;
    private final AtomicInteger keyIndex = new AtomicInteger(0);

    public ApiService(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.keyIndex.set(prefs.getInt(KEY_API_KEY_INDEX, 0));
    }

    // ==================== 单个 Key 兼容 ====================

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

    // ==================== 各 Section 配置 ====================

    public void setSectionUrl(String section, String url) {
        prefs.edit().putString(section + KEY_PREFIX_URL, url).apply();
    }

    public String getSectionUrl(String section) {
        return prefs.getString(section + KEY_PREFIX_URL, URL_CN);
    }

    public void setSectionCustomUrl(String section, String url) {
        prefs.edit().putString(section + KEY_PREFIX_CUSTOM_URL, url).apply();
    }

    public String getSectionCustomUrl(String section) {
        return prefs.getString(section + KEY_PREFIX_CUSTOM_URL, "");
    }

    public void setSectionKey(String section, String key) {
        prefs.edit().putString(section + KEY_PREFIX_KEY, key).apply();
    }

    public String getSectionKey(String section) {
        return prefs.getString(section + KEY_PREFIX_KEY, "");
    }

    public void setSectionModel(String section, String model) {
        prefs.edit().putString(section + KEY_PREFIX_MODEL, model).apply();
    }

    public String getSectionModel(String section) {
        return prefs.getString(section + KEY_PREFIX_MODEL, "");
    }

    public void setSectionCustomModel(String section, String model) {
        prefs.edit().putString(section + KEY_PREFIX_CUSTOM_MODEL, model).apply();
    }

    public String getSectionCustomModel(String section) {
        return prefs.getString(section + KEY_PREFIX_CUSTOM_MODEL, "");
    }

    /**
     * 获取 section 最终使用的 URL
     */
    public String getSectionBaseUrl(String section) {
        String urlName = getSectionUrl(section);
        if ("custom".equals(urlName)) {
            return getSectionCustomUrl(section);
        }
        return urlName;
    }

    /**
     * 获取 section 最终使用的 model
     */
    public String getSectionModelName(String section, String[][] models) {
        String modelId = getSectionModel(section);
        if ("custom".equals(modelId)) {
            return getSectionCustomModel(section);
        }
        return modelId;
    }

    // ==================== 多 Key 轮询 ====================

    public List<String> getApiKeyList() {
        String json = prefs.getString(KEY_API_KEYS_LIST, "[]");
        List<String> list = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                list.add(arr.getString(i));
            }
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse API keys list", e);
        }
        // 兼容：如果列表为空但有单个 key，加入列表
        if (list.isEmpty()) {
            String singleKey = getApiKey();
            if (!singleKey.isEmpty()) {
                list.add(singleKey);
            }
        }
        return list;
    }

    public void setApiKeyList(List<String> keys) {
        JSONArray arr = new JSONArray();
        for (String key : keys) {
            arr.put(key);
        }
        prefs.edit()
                .putString(KEY_API_KEYS_LIST, arr.toString())
                .apply();
    }

    public void addApiKey(String key) {
        List<String> list = getApiKeyList();
        if (!list.contains(key)) {
            list.add(key);
            setApiKeyList(list);
        }
    }

    public void removeApiKey(String key) {
        List<String> list = getApiKeyList();
        list.remove(key);
        setApiKeyList(list);
    }

    public void clearApiKeys() {
        prefs.edit()
                .remove(KEY_API_KEYS_LIST)
                .remove(KEY_API_KEY_INDEX)
                .apply();
        keyIndex.set(0);
    }

    /**
     * 获取下一个 API Key（轮询）
     * 如果只有一个 key，直接返回
     * 如果有多个 key，轮流返回
     */
    public String getNextApiKey() {
        List<String> keys = getApiKeyList();
        if (keys.isEmpty()) {
            return "";
        }
        if (keys.size() == 1) {
            return keys.get(0);
        }
        int idx = keyIndex.getAndIncrement() % keys.size();
        if (idx < 0) idx = 0;
        // 持久化当前索引
        prefs.edit().putInt(KEY_API_KEY_INDEX, keyIndex.get()).apply();
        return keys.get(idx);
    }

    /**
     * 获取当前正在使用的 Key（不轮询，仅查看）
     */
    public String getCurrentApiKey() {
        List<String> keys = getApiKeyList();
        if (keys.isEmpty()) return "";
        int idx = keyIndex.get() % keys.size();
        if (idx < 0) idx = 0;
        return keys.get(idx);
    }

    /**
     * 获取 Key 统计信息
     */
    public String getKeyStats() {
        List<String> keys = getApiKeyList();
        int total = keys.size();
        int current = total > 0 ? (keyIndex.get() % total) + 1 : 0;
        return current + "/" + total;
    }

    // ==================== API 调用（section 独立配置） ====================

    public String chatCompletion(String baseUrl, String apiKey, String model, String prompt, String systemPrompt) throws IOException {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IOException("请先配置 API Key");
        }

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
            body.put("temperature", 0.6);
            body.put("max_tokens", 1024);
        } catch (JSONException e) {
            throw new IOException("JSON error", e);
        }

        Request request = new Request.Builder()
                .url(baseUrl + "/chat/completions")
                .addHeader("Authorization", "Bearer " + apiKey)
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

    public String generateImage(String baseUrl, String apiKey, String model, String prompt) throws IOException {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IOException("请先配置 API Key");
        }

        JSONObject body = new JSONObject();
        try {
            body.put("model", model);
            body.put("prompt", prompt);
            body.put("size", "1024x768");
            JSONObject extraBody = new JSONObject();
            extraBody.put("response_format", "url");
            body.put("extra_body", extraBody);
        } catch (JSONException e) {
            throw new IOException("JSON error", e);
        }

        Request request = new Request.Builder()
                .url(baseUrl + "/images/generations")
                .addHeader("Authorization", "Bearer " + apiKey)
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

    public String generateVideo(String baseUrl, String apiKey, String model, String prompt,
                                  String firstFrameUrl, String lastFrameUrl, String negativePrompt) throws IOException {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IOException("请先配置 API Key");
        }

        JSONObject body = new JSONObject();
        try {
            body.put("model", model);
            body.put("prompt", prompt);
            body.put("seconds", "5");
            body.put("size", "720P");
            body.put("aspect_ratio", "16:9");

            boolean hasFirst = firstFrameUrl != null && !firstFrameUrl.isEmpty();
            boolean hasLast = lastFrameUrl != null && !lastFrameUrl.isEmpty();

            if (hasFirst || hasLast) {
                // keyframe 模式
                body.put("mode", "keyframe");
                if (hasFirst) body.put("first_frame", firstFrameUrl);
                if (hasLast) body.put("last_frame", lastFrameUrl);
            } else {
                body.put("mode", "text");
            }

            if (negativePrompt != null && !negativePrompt.isEmpty()) {
                body.put("negative_prompt", negativePrompt);
            }
        } catch (JSONException e) {
            throw new IOException("JSON error", e);
        }

        Request request = new Request.Builder()
                .url(baseUrl + "/videos")
                .addHeader("Authorization", "Bearer " + apiKey)
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("API错误: " + response.code() + "\n" + response.body().string());
            }
            JSONObject resp = new JSONObject(response.body().string());
            return resp.getString("video_id");
        } catch (JSONException e) {
            throw new IOException("响应解析失败", e);
        }
    }

    public JSONObject getVideoStatus(String videoId, String model) throws IOException {
        String apiKey = getSectionKey("video");
        String baseUrl = getSectionBaseUrl("video");

        String pollingUrl;
        if (baseUrl.contains("api.agnes-ai.cn")) {
            pollingUrl = "https://api.agnes-ai.cn/agnesapi?video_id=" + videoId + "&model_name=" + model;
        } else if (baseUrl.contains("apihub.agnes-ai.com")) {
            pollingUrl = "https://apihub.agnes-ai.com/agnesapi?video_id=" + videoId + "&model_name=" + model;
        } else {
            pollingUrl = baseUrl.replace("/v1", "") + "/agnesapi?video_id=" + videoId + "&model_name=" + model;
        }

        Request request = new Request.Builder()
                .url(pollingUrl)
                .addHeader("Authorization", "Bearer " + apiKey)
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

    /**
     * 将本地图片转为 base64 Data URI，供 keyframe 使用
     */
    public String fileToDataUri(File file) throws IOException {
        java.io.FileInputStream fis = new java.io.FileInputStream(file);
        byte[] bytes = new byte[(int) file.length()];
        fis.read(bytes);
        fis.close();
        String ext = file.getName().substring(file.getName().lastIndexOf('.') + 1).toLowerCase();
        String mime = "png".equals(ext) ? "image/png" : "image/jpeg";
        String base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP);
        return "data:" + mime + ";base64," + base64;
    }
}
