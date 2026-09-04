package com.agnes.studio;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

public class VideoActivity extends AppCompatActivity {

    private VideoView videoView;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        videoView = findViewById(R.id.video_view);
        tvStatus = findViewById(R.id.tv_status);
        Button btnBack = findViewById(R.id.btn_back);

        btnBack.setOnClickListener(v -> finish());

        // 获取传入的视频路径
        String videoPath = getIntent().getStringExtra("video_path");
        if (videoPath != null) {
            playVideo(videoPath);
        }
    }

    private void playVideo(String path) {
        File file = new File(path);
        if (!file.exists()) {
            tvStatus.setText("视频文件不存在");
            return;
        }

        MediaController controller = new MediaController(this);
        controller.setAnchorView(videoView);
        videoView.setMediaController(controller);
        videoView.setVideoURI(Uri.fromFile(file));

        videoView.setOnPreparedListener(mp -> {
            mp.setLooping(true);
            tvStatus.setText("正在播放...");
            videoView.start();
        });

        videoView.setOnErrorListener((mp, what, extra) -> {
            tvStatus.setText("播放失败: " + what);
            return true;
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (videoView.isPlaying()) {
            videoView.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (videoView.isPlaying()) {
            videoView.stopPlayback();
        }
    }
}
