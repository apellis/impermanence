package com.ape.meditationretreattimer;

import java.lang.System;

@kotlin.Metadata(mv = {1, 8, 0}, k = 1, d1 = {"\u0000P\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010%\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010$\n\u0002\u0010\b\n\u0000\b\u0007\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\f\u001a\u00020\rH\u0002J\u0012\u0010\u000e\u001a\u00020\r2\b\u0010\u000f\u001a\u0004\u0018\u00010\u0010H\u0014J\u0018\u0010\u0011\u001a\u00020\r2\u0006\u0010\u0012\u001a\u00020\u000b2\u0006\u0010\u0013\u001a\u00020\u0014H\u0002J\b\u0010\u0015\u001a\u00020\rH\u0002J,\u0010\u0016\u001a\u00020\r2\u0006\u0010\u0012\u001a\u00020\u000b2\u0006\u0010\u0017\u001a\u00020\u00182\u0012\u0010\u0019\u001a\u000e\u0012\u0004\u0012\u00020\u001b\u0012\u0004\u0012\u00020\u000b0\u001aH\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082.\u00a2\u0006\u0002\n\u0000R\u001a\u0010\t\u001a\u000e\u0012\u0004\u0012\u00020\u000b\u0012\u0004\u0012\u00020\u000b0\nX\u0082.\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001c"}, d2 = {"Lcom/ape/meditationretreattimer/SettingsActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "()V", "binding", "Lcom/ape/meditationretreattimer/databinding/ActivitySettingsBinding;", "db", "Lcom/ape/meditationretreattimer/data/AppDatabase;", "settingDao", "Lcom/ape/meditationretreattimer/data/SettingDao;", "settings", "", "", "loadSettings", "", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "registerCheckboxSettingClickListener", "settingName", "checkBox", "Landroid/widget/CheckBox;", "registerListeners", "registerRadioButtonSettingClickListener", "radioGroup", "Landroid/widget/RadioGroup;", "settingMap", "", "", "app_debug"})
public final class SettingsActivity extends androidx.appcompat.app.AppCompatActivity {
    private com.ape.meditationretreattimer.databinding.ActivitySettingsBinding binding;
    private com.ape.meditationretreattimer.data.AppDatabase db;
    private com.ape.meditationretreattimer.data.SettingDao settingDao;
    private java.util.Map<java.lang.String, java.lang.String> settings;
    
    public SettingsActivity() {
        super();
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    private final void loadSettings() {
    }
    
    private final void registerListeners() {
    }
    
    private final void registerCheckboxSettingClickListener(java.lang.String settingName, android.widget.CheckBox checkBox) {
    }
    
    private final void registerRadioButtonSettingClickListener(java.lang.String settingName, android.widget.RadioGroup radioGroup, java.util.Map<java.lang.Integer, java.lang.String> settingMap) {
    }
}