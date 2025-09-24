package com.ape.meditationretreattimer.data;

import java.lang.System;

/**
 * The current data model is simple: a Timer has a list of bell times that are stored in
 * ascending order by local time, and each bell time has a name. The segment between
 * consecutive bell times is labeled in the UI by the name of its starting bell time. The
 * last bell time's name is ignored.
 *
 * Conceptually, a "Timer" is for a full session, and a session is a subset of a calendar
 * day. A session may be something like alternating sitting and walking segments with a meal
 * break or two and a closing bell.
 *
 * In the future, it would be nice to decouple segments, bells, names, and sounds. For
 * example, perhaps a 2hr sit block has a lower-volume halfway bell. This should not change
 * the fact that there is conceptually one 2hr segment rather than two 1hr segments.
 */
@kotlin.Metadata(mv = {1, 8, 0}, k = 1, d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\b\u0007\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0012\u0010\u0003\u001a\u00020\u00042\b\u0010\u0005\u001a\u0004\u0018\u00010\u0006H\u0007J\u0012\u0010\u0007\u001a\u00020\u00062\b\u0010\b\u001a\u0004\u0018\u00010\u0004H\u0007\u00a8\u0006\t"}, d2 = {"Lcom/ape/meditationretreattimer/data/Converters;", "", "()V", "timerDataFromString", "Lcom/ape/meditationretreattimer/model/TimerData;", "value", "", "timerDataToString", "timerData", "app_debug"})
public final class Converters {
    
    public Converters() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    @androidx.room.TypeConverter()
    public final com.ape.meditationretreattimer.model.TimerData timerDataFromString(@org.jetbrains.annotations.Nullable()
    java.lang.String value) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    @androidx.room.TypeConverter()
    public final java.lang.String timerDataToString(@org.jetbrains.annotations.Nullable()
    com.ape.meditationretreattimer.model.TimerData timerData) {
        return null;
    }
}