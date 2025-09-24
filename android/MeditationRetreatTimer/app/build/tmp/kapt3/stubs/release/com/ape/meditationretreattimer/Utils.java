package com.ape.meditationretreattimer;

import java.lang.System;

@kotlin.Metadata(mv = {1, 7, 1}, k = 1, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\u0018\u0000 \u00032\u00020\u0001:\u0001\u0003B\u0005\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0004"}, d2 = {"Lcom/ape/meditationretreattimer/Utils;", "", "()V", "Companion", "app_release"})
public final class Utils {
    @org.jetbrains.annotations.NotNull()
    public static final com.ape.meditationretreattimer.Utils.Companion Companion = null;
    public static final long TIME_RESOLUTION_MILLIS = 100L;
    
    public Utils() {
        super();
    }
    
    @kotlin.Metadata(mv = {1, 7, 1}, k = 1, d1 = {"\u0000<\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\t\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0016\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\nJ \u0010\u000b\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\u000e2\u0006\u0010\u000f\u001a\u00020\u00102\b\b\u0001\u0010\u0011\u001a\u00020\u0012R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0013"}, d2 = {"Lcom/ape/meditationretreattimer/Utils$Companion;", "", "()V", "TIME_RESOLUTION_MILLIS", "", "formatLocalTime", "", "lt", "Ljava/time/LocalTime;", "use24Hour", "", "playSound", "", "context", "Landroid/content/Context;", "mediaPlayer", "Landroid/media/MediaPlayer;", "rawResId", "", "app_release"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String formatLocalTime(@org.jetbrains.annotations.NotNull()
        java.time.LocalTime lt, boolean use24Hour) {
            return null;
        }
        
        public final void playSound(@org.jetbrains.annotations.NotNull()
        android.content.Context context, @org.jetbrains.annotations.NotNull()
        android.media.MediaPlayer mediaPlayer, @androidx.annotation.RawRes()
        int rawResId) {
        }
    }
}