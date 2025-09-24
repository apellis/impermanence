package com.ape.meditationretreattimer.ui.adapter;

import java.lang.System;

@kotlin.Metadata(mv = {1, 8, 0}, k = 1, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\bf\u0018\u00002\u00020\u0001J\u0010\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H&J\u0010\u0010\u0006\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H&J\u0010\u0010\u0007\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H&\u00a8\u0006\b"}, d2 = {"Lcom/ape/meditationretreattimer/ui/adapter/OnTimerListItemClickListener;", "", "onDeleteClick", "", "timer", "Lcom/ape/meditationretreattimer/model/Timer;", "onEditClick", "onStartClick", "app_debug"})
public abstract interface OnTimerListItemClickListener {
    
    public abstract void onStartClick(@org.jetbrains.annotations.NotNull()
    com.ape.meditationretreattimer.model.Timer timer);
    
    public abstract void onEditClick(@org.jetbrains.annotations.NotNull()
    com.ape.meditationretreattimer.model.Timer timer);
    
    public abstract void onDeleteClick(@org.jetbrains.annotations.NotNull()
    com.ape.meditationretreattimer.model.Timer timer);
}