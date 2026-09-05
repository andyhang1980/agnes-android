package com.agnes.studio;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.OkHttpClient;

public class MainActivity extends AppCompatActivity {

    private ApiService api;
    private DramaGenerator dramaGen;

    // UI 组件
    private EditText etInput;
    private EditText etApiKey;
    private EditText etCustomUrl;
    private Spinner spUrl;
    private Spinner spTextModel;
    private Spinner spImageModel;
    private Spinner spVideoModel;
    private TextView tvOutput;
    private TextView tvKeyStats;
    private TextView tvKeyList;
    private ScrollView svOutput;
    private ImageView ivPreview;
    private Button btnGenerate;

    // 预设提示词
    private static final String[][] PROMPT_TEMPLATES = {
        {"都市爱情", "现代都市背景，男女主角从相识到相爱的故事"},
        {"悬疑推理", "侦探调查神秘案件，层层揭开真相"},
        {"古装武侠", "江湖恩怨，侠客行侠仗义的故事"},
        {"搞笑日常", "轻松幽默的日常生活趣事"},
        {"科幻未来", "未来世界，科技与人性的碰撞"},
        {"职场剧情", "职场中的竞争、合作与成长"},
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        api = new ApiService(this);
        dramaGen = new DramaGenerator(api);

        initViews();
        loadConfig();
    }

    private void initViews() {
        etInput = findViewById(R.id.et_input);
        etApiKey = findViewById(R.id.et_api_key);
        etCustomUrl = findViewById(R.id.et_custom_url);
        spUrl = findViewById(R.id.sp_url);
        spTextModel = findViewById(R.id.sp_text_model);
        spImageModel = findViewById(R.id.sp_image_model);
        spVideoModel = findViewById(R.id.sp_video_model);
        tvOutput = findViewById(R.id.tv_output);
        tvKeyStats = findViewById(R.id.tv_key_stats);
        tvKeyList = findViewById(R.id.tv_key_list);
        svOutput = findViewById(R.id.sv_output);
        ivPreview = findViewById(R.id.iv_preview);
        btnGenerate = findViewById(R.id.btn_generate);

        // 添加 Key 按钮
        findViewById(R.id.btn_add_key).setOnClickListener(v -> addApiKey());

        // 删除 Key 按钮
        findViewById(R.id.btn_remove_key).setOnClickListener(v -> removeApiKey());

        // 设置 API 地址下拉
        List<String> urlNames = new ArrayList<>();
        for (String[] item : ApiService.API_URLS) {
            urlNames.add(item[0]);
        }
        ArrayAdapter<String> urlAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, urlNames);
        urlAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spUrl.setAdapter(urlAdapter);

        // 设置文本模型下拉
        List<String> textModelNames = new ArrayList<>();
        for (String[] item : ApiService.TEXT_MODELS) {
            textModelNames.add(item[1]);
        }
        ArrayAdapter<String> textAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, textModelNames);
        textAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spTextModel.setAdapter(textAdapter);

        // 设置图片模型下拉
        List<String> imageModelNames = new ArrayList<>();
        for (String[] item : ApiService.IMAGE_MODELS) {
            imageModelNames.add(item[1]);
        }
        ArrayAdapter<String> imageAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, imageModelNames);
        imageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spImageModel.setAdapter(imageAdapter);

        // 设置视频模型下拉
        List<String> videoModelNames = new ArrayList<>();
        for (String[] item : ApiService.VIDEO_MODELS) {
            videoModelNames.add(item[1]);
        }
        ArrayAdapter<String> videoAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, videoModelNames);
        videoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spVideoModel.setAdapter(videoAdapter);

        // 预设提示词按钮
        findViewById(R.id.btn_preset1).setOnClickListener(v -> showPresetDialog());

        // 保存配置按钮
        findViewById(R.id.btn_save_config).setOnClickListener(v -> saveConfig());

        // 生成剧本
        findViewById(R.id.btn_gen_script).setOnClickListener(v -> generateScript());

        // 生成图片
        findViewById(R.id.btn_gen_image).setOnClickListener(v -> generateImage());

        // 生成视频
        findViewById(R.id.btn_gen_video).setOnClickListener(v -> generateVideo());

        // 一键生成短剧
        btnGenerate.setOnClickListener(v -> generateDrama());

        // 查看任务
        findViewById(R.id.btn_tasks).setOnClickListener(v -> {
            Toast.makeText(this, "任务管理功能开发中...", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadConfig() {
        etApiKey.setText(api.getApiKey());

        String currentUrl = api.getBaseUrl();
        for (int i = 0; i < ApiService.API_URLS.length; i++) {
            if (ApiService.API_URLS[i][1].equals(currentUrl)) {
                spUrl.setSelection(i);
                break;
            }
        }

        spUrl.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selectedUrl = ApiService.API_URLS[position][1];
                if (selectedUrl.isEmpty()) {
                    etCustomUrl.setVisibility(View.VISIBLE);
                } else {
                    etCustomUrl.setVisibility(View.GONE);
                    api.setBaseUrl(selectedUrl);
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // 加载多 Key 列表
        updateKeyListDisplay();
    }

    private void saveConfig() {
        String apiKey = etApiKey.getText().toString().trim();
        if (apiKey.isEmpty()) {
            Toast.makeText(this, "请输入 API Key", Toast.LENGTH_SHORT).show();
            return;
        }

        api.setApiKey(apiKey);

        int urlPos = spUrl.getSelectedItemPosition();
        String url = ApiService.API_URLS[urlPos][1];
        if (url.isEmpty()) {
            url = etCustomUrl.getText().toString().trim();
        }
        if (!url.isEmpty()) {
            api.setBaseUrl(url);
        }

        Toast.makeText(this, "配置已保存", Toast.LENGTH_SHORT).show();
    }

    // ==================== 多 Key 管理 ====================

    private void addApiKey() {
        String key = etApiKey.getText().toString().trim();
        if (key.isEmpty()) {
            Toast.makeText(this, "请输入 API Key", Toast.LENGTH_SHORT).show();
            return;
        }
        api.addApiKey(key);
        etApiKey.setText("");
        updateKeyListDisplay();
        Toast.makeText(this, "Key 已添加", Toast.LENGTH_SHORT).show();
    }

    private void removeApiKey() {
        List<String> keys = api.getApiKeyList();
        if (keys.isEmpty()) {
            Toast.makeText(this, "暂无 Key 可删除", Toast.LENGTH_SHORT).show();
            return;
        }
        String currentKey = api.getCurrentApiKey();
        api.removeApiKey(currentKey);
        updateKeyListDisplay();
        Toast.makeText(this, "已删除: " + maskKey(currentKey), Toast.LENGTH_SHORT).show();
    }

    private void updateKeyListDisplay() {
        List<String> keys = api.getApiKeyList();
        tvKeyStats.setText(api.getKeyStats());

        if (keys.isEmpty()) {
            tvKeyList.setText("暂无 Key");
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keys.size(); i++) {
            String masked = maskKey(keys.get(i));
            if (i > 0) sb.append("\n");
            sb.append((i + 1)).append(". ").append(masked);
        }
        tvKeyList.setText(sb.toString());
    }

    private String maskKey(String key) {
        if (key == null || key.length() < 8) return key;
        return key.substring(0, 4) + "****" + key.substring(key.length() - 4);
    }

    private void showPresetDialog() {
        List<String> presets = new ArrayList<>();
        for (String[] p : PROMPT_TEMPLATES) {
            presets.add(p[0] + " - " + p[1]);
        }

        new AlertDialog.Builder(this)
                .setTitle("选择预设主题")
                .setItems(presets.toArray(new String[0]), (dialog, which) -> {
                    etInput.setText(PROMPT_TEMPLATES[which][1]);
                })
                .show();
    }

    private String getTextModel() {
        int pos = spTextModel.getSelectedItemPosition();
        return ApiService.TEXT_MODELS[pos][0];
    }

    private String getImageModel() {
        int pos = spImageModel.getSelectedItemPosition();
        return ApiService.IMAGE_MODELS[pos][0];
    }

    private String getVideoModel() {
        int pos = spVideoModel.getSelectedItemPosition();
        return ApiService.VIDEO_MODELS[pos][0];
    }

    private void appendOutput(String text) {
        tvOutput.append(text + "\n");
        svOutput.post(() -> svOutput.fullScroll(View.FOCUS_DOWN));
    }

    private void setPreviewImage(byte[] imageData) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
        ivPreview.setImageBitmap(bitmap);
        ivPreview.setVisibility(View.VISIBLE);
    }

    // ==================== 生成剧本 ====================

    private void generateScript() {
        String input = etInput.getText().toString().trim();
        if (input.isEmpty()) {
            Toast.makeText(this, "请输入主题或提示词", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog pd = new ProgressDialog(this);
        pd.setTitle("生成剧本");
        pd.setMessage("正在生成...");
        pd.setCancelable(false);
        pd.show();

        new Thread(() -> {
            try {
                String result = api.chatCompletion(getTextModel(), input,
                        "你是专业短剧编剧。根据用户给的主题，生成一个3-5场的短剧剧本。");
                runOnUiThread(() -> {
                    pd.dismiss();
                    tvOutput.setText(result);
                    svOutput.setVisibility(View.VISIBLE);
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    pd.dismiss();
                    Toast.makeText(this, "生成失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    // ==================== 生成图片 ====================

    private void generateImage() {
        String input = etInput.getText().toString().trim();
        if (input.isEmpty()) {
            Toast.makeText(this, "请输入图片描述", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog pd = new ProgressDialog(this);
        pd.setTitle("生成图片");
        pd.setMessage("正在生成...");
        pd.setCancelable(false);
        pd.show();

        new Thread(() -> {
            try {
                String imageUrl = api.generateImage(getImageModel(), input, null);
                byte[] imageData = api.downloadFile(imageUrl);

                // 保存到本地
                String filename = "agnes_" + System.currentTimeMillis() + ".png";
                File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                File file = new File(dir, filename);
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(imageData);
                fos.close();

                runOnUiThread(() -> {
                    pd.dismiss();
                    setPreviewImage(imageData);
                    appendOutput("图片已保存: " + file.getAbsolutePath());
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    pd.dismiss();
                    Toast.makeText(this, "生成失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    // ==================== 生成视频 ====================

    private void generateVideo() {
        String input = etInput.getText().toString().trim();
        if (input.isEmpty()) {
            Toast.makeText(this, "请输入视频描述", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog pd = new ProgressDialog(this);
        pd.setTitle("生成视频");
        pd.setMessage("正在提交任务...");
        pd.setCancelable(false);
        pd.show();

        new Thread(() -> {
            try {
                String taskId = api.generateVideo(getVideoModel(), input, null);
                appendOutput("任务已提交: " + taskId);
                appendOutput("正在等待视频生成...");

                // 轮询状态
                int maxAttempts = 120;
                for (int i = 0; i < maxAttempts; i++) {
                    Thread.sleep(5000);
                    JSONObject status = api.getVideoStatus(taskId);
                    String state = status.optString("status", "");

                    if ("completed".equals(state) || "success".equals(state)) {
                        String videoUrl = status.optString("video_url", "");
                        byte[] videoData = api.downloadFile(videoUrl);

                        String filename = "agnes_" + System.currentTimeMillis() + ".mp4";
                        File dir = getExternalFilesDir(Environment.DIRECTORY_MOVIES);
                        File file = new File(dir, filename);
                        FileOutputStream fos = new FileOutputStream(file);
                        fos.write(videoData);
                        fos.close();

                        runOnUiThread(() -> {
                            pd.dismiss();
                            appendOutput("视频已保存: " + file.getAbsolutePath());
                            Toast.makeText(this, "视频生成完成!", Toast.LENGTH_SHORT).show();
                        });
                        return;
                    } else if ("failed".equals(state)) {
                        throw new IOException("视频生成失败: " + status.optString("error", ""));
                    }

                    final int percent = (i + 1) * 5;
                    runOnUiThread(() -> pd.setMessage("生成中... " + percent + "秒"));
                }
                throw new IOException("视频生成超时");
            } catch (IOException | InterruptedException e) {
                runOnUiThread(() -> {
                    pd.dismiss();
                    Toast.makeText(this, "生成失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    // ==================== 一键生成短剧 ====================

    private void generateDrama() {
        String input = etInput.getText().toString().trim();
        if (input.isEmpty()) {
            Toast.makeText(this, "请输入短剧主题", Toast.LENGTH_SHORT).show();
            return;
        }

        if (api.getApiKey().isEmpty()) {
            Toast.makeText(this, "请先配置 API Key", Toast.LENGTH_SHORT).show();
            return;
        }

        btnGenerate.setEnabled(false);
        ProgressDialog pd = new ProgressDialog(this);
        pd.setTitle("生成短剧");
        pd.setMessage("正在生成...");
        pd.setCancelable(false);
        pd.show();

        dramaGen.generateDrama(input, "现代",
                getTextModel(), getImageModel(), getVideoModel(),
                new DramaGenerator.Callback() {
                    @Override
                    public void onProgress(String step, String message) {
                        runOnUiThread(() -> {
                            pd.setMessage(step + ": " + message);
                            appendOutput("[" + step + "] " + message);
                        });
                    }

                    @Override
                    public void onComplete(DramaGenerator.DramaResult result) {
                        runOnUiThread(() -> {
                            pd.dismiss();
                            btnGenerate.setEnabled(true);
                            tvOutput.setText("=== 短剧剧本 ===\n\n" + result.script);
                            svOutput.setVisibility(View.VISIBLE);
                            appendOutput("\n=== 生成完成 ===");
                            appendOutput("共生成 " + result.videoUrls.length + " 个视频片段");
                            Toast.makeText(MainActivity.this, "短剧生成完成!", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            pd.dismiss();
                            btnGenerate.setEnabled(true);
                            Toast.makeText(MainActivity.this, "生成失败: " + error, Toast.LENGTH_LONG).show();
                        });
                    }
                });
    }
}
