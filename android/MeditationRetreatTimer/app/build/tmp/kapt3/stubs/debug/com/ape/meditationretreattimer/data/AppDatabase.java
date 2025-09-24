package com.ape.meditationretreattimer.data;

import java.lang.System;

@androidx.room.TypeConverters(value = {com.ape.meditationretreattimer.data.Converters.class})
@androidx.room.Database(entities = {com.ape.meditationretreattimer.model.Timer.class, com.ape.meditationretreattimer.model.Setting.class}, version = 5, exportSchema = false)
@kotlin.Metadata(mv = {1, 8, 0}, k = 1, d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\b\'\u0018\u0000 \u00072\u00020\u0001:\u0001\u0007B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u0003\u001a\u00020\u0004H&J\b\u0010\u0005\u001a\u00020\u0006H&\u00a8\u0006\b"}, d2 = {"Lcom/ape/meditationretreattimer/data/AppDatabase;", "Landroidx/room/RoomDatabase;", "()V", "settingDao", "Lcom/ape/meditationretreattimer/data/SettingDao;", "timerDao", "Lcom/ape/meditationretreattimer/data/TimerDao;", "Companion", "app_debug"})
public abstract class AppDatabase extends androidx.room.RoomDatabase {
    @org.jetbrains.annotations.NotNull()
    public static final com.ape.meditationretreattimer.data.AppDatabase.Companion Companion = null;
    @kotlin.jvm.Volatile()
    private static volatile com.ape.meditationretreattimer.data.AppDatabase INSTANCE;
    
    public AppDatabase() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public abstract com.ape.meditationretreattimer.data.TimerDao timerDao();
    
    @org.jetbrains.annotations.NotNull()
    public abstract com.ape.meditationretreattimer.data.SettingDao settingDao();
    
    @kotlin.Metadata(mv = {1, 8, 0}, k = 1, d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u000e\u0010\u0005\u001a\u00020\u00042\u0006\u0010\u0006\u001a\u00020\u0007R\u0010\u0010\u0003\u001a\u0004\u0018\u00010\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006\b"}, d2 = {"Lcom/ape/meditationretreattimer/data/AppDatabase$Companion;", "", "()V", "INSTANCE", "Lcom/ape/meditationretreattimer/data/AppDatabase;", "getDatabase", "context", "Landroid/content/Context;", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.ape.meditationretreattimer.data.AppDatabase getDatabase(@org.jetbrains.annotations.NotNull()
        android.content.Context context) {
            return null;
        }
    }
}