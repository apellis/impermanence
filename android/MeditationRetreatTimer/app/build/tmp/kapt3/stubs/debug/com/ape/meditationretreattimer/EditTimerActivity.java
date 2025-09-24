package com.ape.meditationretreattimer;

import java.lang.System;

@kotlin.Metadata(mv = {1, 8, 0}, k = 1, d1 = {"\u0000P\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010!\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010$\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\b\u0007\u0018\u00002\u00020\u00012\u00020\u0002B\u0005\u00a2\u0006\u0002\u0010\u0003J\u0012\u0010\u0014\u001a\u00020\u00152\b\u0010\u0016\u001a\u0004\u0018\u00010\u0017H\u0014J\u0010\u0010\u0018\u001a\u00020\u00152\u0006\u0010\u0019\u001a\u00020\u0006H\u0016J\u0010\u0010\u001a\u001a\u00020\u00152\u0006\u0010\u0019\u001a\u00020\u0006H\u0016J\b\u0010\u001b\u001a\u00020\u0015H\u0003R\u0014\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\nX\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\fX\u0082.\u00a2\u0006\u0002\n\u0000R\u001a\u0010\r\u001a\u000e\u0012\u0004\u0012\u00020\u000f\u0012\u0004\u0012\u00020\u000f0\u000eX\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0010\u001a\u00020\u0011X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0012\u001a\u00020\u0013X\u0082.\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001c"}, d2 = {"Lcom/ape/meditationretreattimer/EditTimerActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "Lcom/ape/meditationretreattimer/ui/adapter/OnEditBellTimeItemClickListener;", "()V", "bellTimes", "", "Lcom/ape/meditationretreattimer/model/BellTime;", "binding", "Lcom/ape/meditationretreattimer/databinding/ActivityEditTimerBinding;", "db", "Lcom/ape/meditationretreattimer/data/AppDatabase;", "settingDao", "Lcom/ape/meditationretreattimer/data/SettingDao;", "settings", "", "", "timer", "Lcom/ape/meditationretreattimer/model/Timer;", "timerDao", "Lcom/ape/meditationretreattimer/data/TimerDao;", "onCreate", "", "savedInstanceState", "Landroid/os/Bundle;", "onDeleteClick", "bellTime", "onEditClick", "refreshBellTimes", "app_debug"})
public final class EditTimerActivity extends androidx.appcompat.app.AppCompatActivity implements com.ape.meditationretreattimer.ui.adapter.OnEditBellTimeItemClickListener {
    private com.ape.meditationretreattimer.databinding.ActivityEditTimerBinding binding;
    private com.ape.meditationretreattimer.model.Timer timer;
    private java.util.List<com.ape.meditationretreattimer.model.BellTime> bellTimes;
    private com.ape.meditationretreattimer.data.AppDatabase db;
    private com.ape.meditationretreattimer.data.TimerDao timerDao;
    private com.ape.meditationretreattimer.data.SettingDao settingDao;
    private java.util.Map<java.lang.String, java.lang.String> settings;
    
    public EditTimerActivity() {
        super();
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    @java.lang.Override()
    public void onEditClick(@org.jetbrains.annotations.NotNull()
    com.ape.meditationretreattimer.model.BellTime bellTime) {
    }
    
    @java.lang.Override()
    public void onDeleteClick(@org.jetbrains.annotations.NotNull()
    com.ape.meditationretreattimer.model.BellTime bellTime) {
    }
    
    @android.annotation.SuppressLint(value = {"NotifyDataSetChanged"})
    private final void refreshBellTimes() {
    }
}