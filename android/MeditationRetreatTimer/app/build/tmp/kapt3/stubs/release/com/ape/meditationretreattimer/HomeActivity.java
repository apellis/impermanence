package com.ape.meditationretreattimer;

import java.lang.System;

@kotlin.Metadata(mv = {1, 7, 1}, k = 1, d1 = {"\u0000^\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010$\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010!\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0003\u0018\u00002\u00020\u00012\u00020\u0002B\u0005\u00a2\u0006\u0002\u0010\u0003J\u0012\u0010\u0012\u001a\u00020\u00132\b\u0010\u0014\u001a\u0004\u0018\u00010\u0015H\u0014J\u0012\u0010\u0016\u001a\u00020\u00172\b\u0010\u0018\u001a\u0004\u0018\u00010\u0019H\u0016J\u0010\u0010\u001a\u001a\u00020\u00132\u0006\u0010\u001b\u001a\u00020\u0011H\u0016J\u0010\u0010\u001c\u001a\u00020\u00132\u0006\u0010\u001b\u001a\u00020\u0011H\u0016J\u0010\u0010\u001d\u001a\u00020\u00172\u0006\u0010\u001e\u001a\u00020\u001fH\u0016J\u0010\u0010 \u001a\u00020\u00132\u0006\u0010\u001b\u001a\u00020\u0011H\u0016J\b\u0010!\u001a\u00020\u0013H\u0003R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082.\u00a2\u0006\u0002\n\u0000R\u001a\u0010\n\u001a\u000e\u0012\u0004\u0012\u00020\f\u0012\u0004\u0012\u00020\f0\u000bX\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\r\u001a\u00020\u000eX\u0082.\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u000f\u001a\b\u0012\u0004\u0012\u00020\u00110\u0010X\u0082.\u00a2\u0006\u0002\n\u0000\u00a8\u0006\""}, d2 = {"Lcom/ape/meditationretreattimer/HomeActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "Lcom/ape/meditationretreattimer/ui/adapter/OnTimerListItemClickListener;", "()V", "binding", "Lcom/ape/meditationretreattimer/databinding/ActivityHomeBinding;", "db", "Lcom/ape/meditationretreattimer/data/AppDatabase;", "settingDao", "Lcom/ape/meditationretreattimer/data/SettingDao;", "settings", "", "", "timerDao", "Lcom/ape/meditationretreattimer/data/TimerDao;", "timers", "", "Lcom/ape/meditationretreattimer/model/Timer;", "onCreate", "", "savedInstanceState", "Landroid/os/Bundle;", "onCreateOptionsMenu", "", "menu", "Landroid/view/Menu;", "onDeleteClick", "timer", "onEditClick", "onOptionsItemSelected", "item", "Landroid/view/MenuItem;", "onStartClick", "refreshTimers", "app_release"})
public final class HomeActivity extends androidx.appcompat.app.AppCompatActivity implements com.ape.meditationretreattimer.ui.adapter.OnTimerListItemClickListener {
    private com.ape.meditationretreattimer.databinding.ActivityHomeBinding binding;
    private com.ape.meditationretreattimer.data.AppDatabase db;
    private com.ape.meditationretreattimer.data.TimerDao timerDao;
    private com.ape.meditationretreattimer.data.SettingDao settingDao;
    private java.util.List<com.ape.meditationretreattimer.model.Timer> timers;
    private java.util.Map<java.lang.String, java.lang.String> settings;
    
    public HomeActivity() {
        super();
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    @java.lang.Override()
    public boolean onCreateOptionsMenu(@org.jetbrains.annotations.Nullable()
    android.view.Menu menu) {
        return false;
    }
    
    @java.lang.Override()
    public boolean onOptionsItemSelected(@org.jetbrains.annotations.NotNull()
    android.view.MenuItem item) {
        return false;
    }
    
    @java.lang.Override()
    public void onStartClick(@org.jetbrains.annotations.NotNull()
    com.ape.meditationretreattimer.model.Timer timer) {
    }
    
    @java.lang.Override()
    public void onEditClick(@org.jetbrains.annotations.NotNull()
    com.ape.meditationretreattimer.model.Timer timer) {
    }
    
    @java.lang.Override()
    public void onDeleteClick(@org.jetbrains.annotations.NotNull()
    com.ape.meditationretreattimer.model.Timer timer) {
    }
    
    @android.annotation.SuppressLint(value = {"NotifyDataSetChanged"})
    private final void refreshTimers() {
    }
}