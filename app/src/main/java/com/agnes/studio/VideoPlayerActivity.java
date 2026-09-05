package com.agnes.studio;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class VideoPlayerActivity extends AppCompatActivity {

    private VideoView videoView;
    private SeekBar seekBar;
    private TextView tvTime;
    private ImageButton btnPlay, btnBack;
    private Handler handler = new Handler();
    private boolean isSeeking = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        setContentView(R.layout.activity_video_player);

        videoView = findViewById(R.id.video_view);
        seekBar = findViewById(R.id.seek_bar);
        tvTime = findViewById(R.id.tv_time);
        btnPlay = findViewById(R.id.btn_play);
        btnBack = findViewById(R.id.btn_back);

        String path = getIntent().getStringExtra("video_path");
        if (path == null) { finish(); return; }

        videoView.setVideoPath(path);
        videoView.setOnPreparedListener(mp -> {
            mp.setLooping(true);
            seekBar.setMax(mp.getDuration());
            tvTime.setText("0:00 / " + formatTime(mp.getDuration()));
            videoView.start();
            btnPlay.setImageResource(android.R.drawable.ic_media_pause);
            updateSeekBar();
        });

        btnPlay.setOnClickListener(v -> {
            if (videoView.isPlaying()) { videoView.pause(); btnPlay.setImageResource(android.R.drawable.ic_media_play); }
            else { videoView.start(); btnPlay.setImageResource(android.R.drawable.ic_media_pause); updateSeekBar(); }
        });

        btnBack.setOnClickListener(v -> finish());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (fromUser) { videoView.seekTo(progress); tvTime.setText(formatTime(progress) + " / " + formatTime(videoView.getDuration())); }
            }
            @Override public void onStartTrackingTouch(SeekBar sb) { isSeeking = true; }
            @Override public void onStopTrackingTouch(SeekBar sb) { isSeeking = false; }
        });
    }

    private void updateSeekBar() {
        handler.postDelayed(new Runnable() {
            @Override public void run() {
                if (videoView.isPlaying() && !isSeeking) {
                    seekBar.setProgress(videoView.getCurrentPosition());
                    tvTime.setText(formatTime(videoView.getCurrentPosition()) + " / " + formatTime(videoView.getDuration()));
                }
                if (videoView.isPlaying()) handler.postDelayed(this, 300);
            }
        }, 300);
    }

    private String formatTime(int ms) {
        int sec = ms / 1000;
        return String.format(Locale.getDefault(), "%d:%02d", sec / 60, sec % 60);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (videoView.isPlaying()) videoView.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
