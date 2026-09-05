package com.agnes.studio;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.media.MediaPlayer;
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
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
    private TextView tvImageLabel;
    private TextView tvVideoLabel;
    private TextView tvOutputLabel;
    private ScrollView svOutput;
    private ImageView ivPreview;
    private VideoView vvPreview;
    private Button btnGenerate;
    private View cardModels;

    // 当前生成的图片/视频路径
    private String lastImagePath;
    private String lastVideoPath;
    private String lastScript;
    private String lastShots;

    // Agnes 默认模型
    private static final String AGNES_TEXT_MODEL = "agnes-2.5-flash";
    private static final String AGNES_IMAGE_MODEL = "agnes-image-2.1-flash";
    private static final String AGNES_VIDEO_MODEL = "agnes-video-v2.0";

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
        tvImageLabel = findViewById(R.id.tv_image_label);
        tvVideoLabel = findViewById(R.id.tv_video_label);
        tvOutputLabel = findViewById(R.id.tv_output_label);
        svOutput = findViewById(R.id.sv_output);
        ivPreview = findViewById(R.id.iv_preview);
        vvPreview = findViewById(R.id.vv_preview);
        btnGenerate = findViewById(R.id.btn_generate);
        cardModels = findViewById(R.id.card_models);

        // API URL 下拉
        List<String> urlNames = new ArrayList<>();
        for (String[] item : ApiService.API_URLS) {
            urlNames.add(item[0]);
        }
        spUrl.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, urlNames));

        // 模型下拉
        setupModelSpinner(spTextModel, ApiService.TEXT_MODELS);
        setupModelSpinner(spImageModel, ApiService.IMAGE_MODELS);
        setupModelSpinner(spVideoModel, ApiService.VIDEO_MODELS);

        // Key 管理
        findViewById(R.id.btn_add_key).setOnClickListener(v -> addApiKey());
        findViewById(R.id.btn_remove_key).setOnClickListener(v -> removeApiKey());

        // 预设主题
        findViewById(R.id.btn_preset1).setOnClickListener(v -> showPresetDialog());

        // 工作流按钮
        findViewById(R.id.btn_gen_script).setOnClickListener(v -> generateScript());
        findViewById(R.id.btn_gen_shots).setOnClickListener(v -> generateShots());
        findViewById(R.id.btn_gen_image).setOnClickListener(v -> generateImage());
        findViewById(R.id.btn_gen_video).setOnClickListener(v -> generateVideo());
        btnGenerate.setOnClickListener(v -> generateDrama());

        // URL 选择监听
        spUrl.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String url = ApiService.API_URLS[position][1];
                etCustomUrl.setVisibility(url.isEmpty() ? View.VISIBLE : View.GONE);
                // 非 Agnes 时显示模型配置
                boolean isAgnes = position <= 1;
                cardModels.setVisibility(isAgnes ? View.GONE : View.VISIBLE);
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void setupModelSpinner(Spinner spinner, String[][] models) {
        List<String> names = new ArrayList<>();
        for (String[] m : models) names.add(m[1]);
        spinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, names));
    }

    private void loadConfig() {
        String currentUrl = api.getBaseUrl();
        for (int i = 0; i < ApiService.API_URLS.length; i++) {
            if (ApiService.API_URLS[i][1].equals(currentUrl)) {
                spUrl.setSelection(i);
                break;
            }
        }
        updateKeyListDisplay();
    }

    // ==================== Key 管理 ====================

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
            Toast.makeText(this, "暂无 Key", Toast.LENGTH_SHORT).show();
            return;
        }
        String current = api.getCurrentApiKey();
        api.removeApiKey(current);
        updateKeyListDisplay();
        Toast.makeText(this, "已删除: " + maskKey(current), Toast.LENGTH_SHORT).show();
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
            if (i > 0) sb.append("\n");
            sb.append((i + 1)).append(". ").append(maskKey(keys.get(i)));
        }
        tvKeyList.setText(sb.toString());
    }

    private String maskKey(String key) {
        if (key == null || key.length() < 8) return key;
        return key.substring(0, 4) + "****" + key.substring(key.length() - 4);
    }

    // ==================== 配置获取 ====================

    private String getBaseUrl() {
        int pos = spUrl.getSelectedItemPosition();
        String url = ApiService.API_URLS[pos][1];
        if (url.isEmpty()) {
            url = etCustomUrl.getText().toString().trim();
        }
        api.setBaseUrl(url);
        return url;
    }

    private boolean isAgnesApi() {
        int pos = spUrl.getSelectedItemPosition();
        return pos <= 1; // 国内站或国际站
    }

    private String getTextModel() {
        if (isAgnesApi()) return AGNES_TEXT_MODEL;
        return ApiService.TEXT_MODELS[spTextModel.getSelectedItemPosition()][0];
    }

    private String getImageModel() {
        if (isAgnesApi()) return AGNES_IMAGE_MODEL;
        return ApiService.IMAGE_MODELS[spImageModel.getSelectedItemPosition()][0];
    }

    private String getVideoModel() {
        if (isAgnesApi()) return AGNES_VIDEO_MODEL;
        return ApiService.VIDEO_MODELS[spVideoModel.getSelectedItemPosition()][0];
    }

    private void showPresetDialog() {
        List<String> presets = new ArrayList<>();
        for (String[] p : PROMPT_TEMPLATES) presets.add(p[0] + " - " + p[1]);
        new AlertDialog.Builder(this)
                .setTitle("选择预设主题")
                .setItems(presets.toArray(new String[0]), (d, w) ->
                        etInput.setText(PROMPT_TEMPLATES[w][1]))
                .show();
    }

    // ==================== 结果显示 ====================

    private void appendOutput(String text) {
        tvOutputLabel.setVisibility(View.VISIBLE);
        svOutput.setVisibility(View.VISIBLE);
        tvOutput.append(text + "\n");
        svOutput.post(() -> svOutput.fullScroll(View.FOCUS_DOWN));
        findViewById(R.id.tv_empty).setVisibility(View.GONE);
    }

    private void showImagePreview(byte[] data) {
        tvImageLabel.setVisibility(View.VISIBLE);
        ivPreview.setVisibility(View.VISIBLE);
        Glide.with(this).load(data).into(ivPreview);
        findViewById(R.id.tv_empty).setVisibility(View.GONE);
    }

    private void showVideoPreview(String path) {
        tvVideoLabel.setVisibility(View.VISIBLE);
        vvPreview.setVisibility(View.VISIBLE);
        vvPreview.setVideoURI(Uri.fromFile(new File(path)));
        vvPreview.setOnPreparedListener(mp -> {
            mp.setLooping(true);
            vvPreview.start();
        });
        findViewById(R.id.tv_empty).setVisibility(View.GONE);
    }

    // ==================== 工作流：① 生成剧本 ====================

    private void generateScript() {
        String input = etInput.getText().toString().trim();
        if (input.isEmpty()) {
            Toast.makeText(this, "请输入短剧主题", Toast.LENGTH_SHORT).show();
            return;
        }
        if (api.getApiKeyList().isEmpty()) {
            Toast.makeText(this, "请先配置 API Key", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog pd = new ProgressDialog(this);
        pd.setTitle("① 生成剧本");
        pd.setMessage("正在生成剧本...");
        pd.setCancelable(false);
        pd.show();

        new Thread(() -> {
            try {
                getBaseUrl();
                String result = api.chatCompletion(getTextModel(), input,
                        "你是专业短剧编剧。根据主题生成3-5场的短剧剧本，每场包含画面描述和台词。");
                lastScript = result;
                runOnUiThread(() -> {
                    pd.dismiss();
                    tvOutput.setText("");
                    appendOutput("=== 剧本 ===\n\n" + result);
                    Toast.makeText(this, "剧本生成完成", Toast.LENGTH_SHORT).show();
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    pd.dismiss();
                    Toast.makeText(this, "失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    // ==================== 工作流：② 生成分镜 ====================

    private void generateShots() {
        String script = lastScript;
        if (script == null || script.isEmpty()) {
            Toast.makeText(this, "请先生成剧本", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog pd = new ProgressDialog(this);
        pd.setTitle("② 生成分镜");
        pd.setMessage("正在生成分镜...");
        pd.setCancelable(false);
        pd.show();

        new Thread(() -> {
            try {
                getBaseUrl();
                String result = api.chatCompletion(getTextModel(), script,
                        "你是专业分镜师。将剧本转换为分镜列表，每个分镜包含画面描述。输出JSON格式：[{\"scene\":1,\"shot\":\"画面描述\"}]");
                lastShots = result;
                runOnUiThread(() -> {
                    pd.dismiss();
                    appendOutput("\n=== 分镜 ===\n\n" + result);
                    Toast.makeText(this, "分镜生成完成", Toast.LENGTH_SHORT).show();
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    pd.dismiss();
                    Toast.makeText(this, "失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    // ==================== 工作流：③ 文生图 ====================

    private void generateImage() {
        String input = etInput.getText().toString().trim();
        if (input.isEmpty()) {
            Toast.makeText(this, "请输入图片描述", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog pd = new ProgressDialog(this);
        pd.setTitle("③ 文生图");
        pd.setMessage("正在生成图片...");
        pd.setCancelable(false);
        pd.show();

        new Thread(() -> {
            try {
                getBaseUrl();
                String imageUrl = api.generateImage(getImageModel(), input, null);
                byte[] imageData = api.downloadFile(imageUrl);

                String filename = "agnes_" + System.currentTimeMillis() + ".png";
                File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                File file = new File(dir, filename);
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(imageData);
                fos.close();
                lastImagePath = file.getAbsolutePath();

                runOnUiThread(() -> {
                    pd.dismiss();
                    showImagePreview(imageData);
                    appendOutput("图片已保存: " + file.getAbsolutePath());
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    pd.dismiss();
                    Toast.makeText(this, "失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    // ==================== 工作流：④ 图生视频 ====================

    private void generateVideo() {
        String input = etInput.getText().toString().trim();
        if (input.isEmpty()) {
            Toast.makeText(this, "请输入视频描述", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog pd = new ProgressDialog(this);
        pd.setTitle("④ 图生视频");
        pd.setMessage("正在提交...");
        pd.setCancelable(false);
        pd.show();

        new Thread(() -> {
            try {
                getBaseUrl();
                String taskId = api.generateVideo(getVideoModel(), input, lastImagePath);
                appendOutput("\n视频任务已提交: " + taskId);

                // 轮询等待
                for (int i = 0; i < 120; i++) {
                    Thread.sleep(5000);
                    org.json.JSONObject status = api.getVideoStatus(taskId);
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
                        lastVideoPath = file.getAbsolutePath();

                        runOnUiThread(() -> {
                            pd.dismiss();
                            showVideoPreview(lastVideoPath);
                            appendOutput("视频已保存: " + file.getAbsolutePath());
                        });
                        return;
                    } else if ("failed".equals(state)) {
                        throw new IOException("视频生成失败");
                    }
                    final int sec = (i + 1) * 5;
                    runOnUiThread(() -> pd.setMessage("生成中... " + sec + "秒"));
                }
                throw new IOException("超时");
            } catch (IOException | InterruptedException e) {
                runOnUiThread(() -> {
                    pd.dismiss();
                    Toast.makeText(this, "失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    // ==================== 一键全流程 ====================

    private void generateDrama() {
        String input = etInput.getText().toString().trim();
        if (input.isEmpty()) {
            Toast.makeText(this, "请输入短剧主题", Toast.LENGTH_SHORT).show();
            return;
        }
        if (api.getApiKeyList().isEmpty()) {
            Toast.makeText(this, "请先配置 API Key", Toast.LENGTH_SHORT).show();
            return;
        }

        btnGenerate.setEnabled(false);
        ProgressDialog pd = new ProgressDialog(this);
        pd.setTitle("一键生成短剧");
        pd.setMessage("准备中...");
        pd.setCancelable(false);
        pd.show();

        new Thread(() -> {
            try {
                getBaseUrl();
                String textModel = getTextModel();
                String imageModel = getImageModel();
                String videoModel = getVideoModel();

                // ① 生成剧本
                pd.setMessage("① 生成剧本...");
                String script = api.chatCompletion(textModel, input,
                        "你是专业短剧编剧。根据主题生成3-5场的短剧剧本。");

                // ② 生成分镜
                pd.setMessage("② 生成分镜...");
                String shots = api.chatCompletion(textModel, script,
                        "将剧本转为分镜，JSON格式：[{\"scene\":1,\"shot\":\"画面描述\"}]");

                // ③ 文生图
                pd.setMessage("③ 生成封面图...");
                String imageUrl = api.generateImage(imageModel, input, null);

                // ④ 图生视频
                pd.setMessage("④ 生成视频...");
                String taskId = api.generateVideo(videoModel, input, null);

                // 等待视频
                String videoPath = null;
                for (int i = 0; i < 120; i++) {
                    Thread.sleep(5000);
                    org.json.JSONObject status = api.getVideoStatus(taskId);
                    String state = status.optString("status", "");
                    if ("completed".equals(state) || "success".equals(state)) {
                        String videoUrl = status.optString("video_url", "");
                        byte[] videoData = api.downloadFile(videoUrl);
                        String filename = "drama_" + System.currentTimeMillis() + ".mp4";
                        File dir = getExternalFilesDir(Environment.DIRECTORY_MOVIES);
                        File file = new File(dir, filename);
                        FileOutputStream fos = new FileOutputStream(file);
                        fos.write(videoData);
                        fos.close();
                        videoPath = file.getAbsolutePath();
                        break;
                    } else if ("failed".equals(state)) {
                        throw new IOException("视频生成失败");
                    }
                    pd.setMessage("等待视频... " + ((i + 1) * 5) + "秒");
                }

                // 保存图片
                byte[] imgData = api.downloadFile(imageUrl);
                String imgFile = "drama_" + System.currentTimeMillis() + ".png";
                File imgDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                File imgPath = new File(imgDir, imgFile);
                FileOutputStream fos = new FileOutputStream(imgPath);
                fos.write(imgData);
                fos.close();

                final String finalVideoPath = videoPath;
                final String finalImgPath = imgPath.getAbsolutePath();
                lastScript = script;
                lastShots = shots;

                runOnUiThread(() -> {
                    pd.dismiss();
                    btnGenerate.setEnabled(true);

                    tvOutput.setText("");
                    appendOutput("=== 剧本 ===\n\n" + script);
                    appendOutput("\n=== 分镜 ===\n\n" + shots);
                    appendOutput("\n=== 完成 ===");

                    showImagePreview(imgData);
                    if (finalVideoPath != null) {
                        showVideoPreview(finalVideoPath);
                    }
                });

            } catch (IOException | InterruptedException e) {
                runOnUiThread(() -> {
                    pd.dismiss();
                    btnGenerate.setEnabled(true);
                    Toast.makeText(this, "失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
}
