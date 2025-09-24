package com.ape.meditationretreattimer.data;

import java.lang.System;

@androidx.room.Dao()
@kotlin.Metadata(mv = {1, 7, 1}, k = 1, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010$\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0004\bg\u0018\u00002\u00020\u0001J\u0010\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0003H\'J\u0014\u0010\u0005\u001a\u000e\u0012\u0004\u0012\u00020\u0003\u0012\u0004\u0012\u00020\u00030\u0006H\u0016J\u000e\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\t0\bH\'J\u0018\u0010\n\u001a\u00020\u000b2\u0006\u0010\u0004\u001a\u00020\u00032\u0006\u0010\f\u001a\u00020\u0003H\u0016J\u0010\u0010\r\u001a\u00020\u000b2\u0006\u0010\u000e\u001a\u00020\tH\'\u00a8\u0006\u000f"}, d2 = {"Lcom/ape/meditationretreattimer/data/SettingDao;", "", "get", "", "key", "getAll", "", "getAllQuery", "", "Lcom/ape/meditationretreattimer/model/Setting;", "set", "", "value", "upsert", "setting", "app_release"})
public abstract interface SettingDao {
    
    @org.jetbrains.annotations.NotNull()
    public abstract java.util.Map<java.lang.String, java.lang.String> getAll();
    
    @org.jetbrains.annotations.NotNull()
    @androidx.room.Query(value = "SELECT key, value FROM Setting ORDER BY key ASC")
    public abstract java.util.List<com.ape.meditationretreattimer.model.Setting> getAllQuery();
    
    @org.jetbrains.annotations.NotNull()
    @androidx.room.Query(value = "SELECT value FROM Setting WHERE key = :key")
    public abstract java.lang.String get(@org.jetbrains.annotations.NotNull()
    java.lang.String key);
    
    public abstract void set(@org.jetbrains.annotations.NotNull()
    java.lang.String key, @org.jetbrains.annotations.NotNull()
    java.lang.String value);
    
    @androidx.room.Upsert()
    public abstract void upsert(@org.jetbrains.annotations.NotNull()
    com.ape.meditationretreattimer.model.Setting setting);
    
    @kotlin.Metadata(mv = {1, 7, 1}, k = 3)
    public final class DefaultImpls {
        
        @org.jetbrains.annotations.NotNull()
        public static java.util.Map<java.lang.String, java.lang.String> getAll(@org.jetbrains.annotations.NotNull()
        com.ape.meditationretreattimer.data.SettingDao $this) {
            return null;
        }
        
        public static void set(@org.jetbrains.annotations.NotNull()
        com.ape.meditationretreattimer.data.SettingDao $this, @org.jetbrains.annotations.NotNull()
        java.lang.String key, @org.jetbrains.annotations.NotNull()
        java.lang.String value) {
        }
    }
}