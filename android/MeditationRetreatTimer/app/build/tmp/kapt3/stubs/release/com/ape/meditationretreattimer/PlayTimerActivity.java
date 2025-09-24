package com.ape.meditationretreattimer;

import java.lang.System;

@kotlin.Metadata(mv = {1, 7, 1}, k = 1, d1 = {"\u0000X\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010!\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010$\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0012\u0010\u0017\u001a\u00020\u00182\b\u0010\u0019\u001a\u0004\u0018\u00010\u001aH\u0014J\b\u0010\u001b\u001a\u00020\u0018H\u0014R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u000b\u001a\b\u0012\u0004\u0012\u00020\r0\fX\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000e\u001a\u00020\u000fX\u0082.\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u0010\u001a\u000e\u0012\u0004\u0012\u00020\u0012\u0012\u0004\u0012\u00020\u00120\u0011X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0013\u001a\u00020\u0014X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0015\u001a\u00020\u0016X\u0082.\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001c"}, d2 = {"Lcom/ape/meditationretreattimer/PlayTimerActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "()V", "binding", "Lcom/ape/meditationretreattimer/databinding/ActivityPlayTimerBinding;", "db", "Lcom/ape/meditationretreattimer/data/AppDatabase;", "handler", "Landroid/os/Handler;", "mediaPlayer", "Landroid/media/MediaPlayer;", "segments", "", "Lcom/ape/meditationretreattimer/model/Segment;", "settingDao", "Lcom/ape/meditationretreattimer/data/SettingDao;", "settings", "", "", "timer", "Lcom/ape/meditationretreattimer/model/Timer;", "timerDao", "Lcom/ape/meditationretreattimer/data/TimerDao;", "onCreate", "", "savedInstanceState", "Landroid/os/Bundle;", "onDestroy", "app_release"})
public final class PlayTimerActivity extends androidx.appcompat.app.AppCompatActivity {
    private com.ape.meditationretreattimer.databinding.ActivityPlayTimerBinding binding;
    private com.ape.meditationretreattimer.model.Timer timer;
    private java.util.List<com.ape.meditationretreattimer.model.Segment> segments;
    private com.ape.meditationretreattimer.data.AppDatabase db;
    private com.ape.meditationretreattimer.data.TimerDao timerDao;
    private com.ape.meditationretreattimer.data.SettingDao settingDao;
    private java.util.Map<java.lang.String, java.lang.String> settings;
    private android.os.Handler handler;
    private final android.media.MediaPlayer mediaPlayer = null;
    
    public PlayTimerActivity() {
        super();
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    @java.lang.Override()
    protected void onDestroy() {
    }
}