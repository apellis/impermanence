package com.ape.meditationretreattimer.ui.adapter;

import java.lang.System;

@kotlin.Metadata(mv = {1, 8, 0}, k = 1, d1 = {"\u0000<\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0004\b\u0007\u0018\u0000 \u00152\u000e\u0012\u0004\u0012\u00020\u0002\u0012\u0004\u0012\u00020\u00030\u0001:\u0002\u0015\u0016B#\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\f\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\t\u00a2\u0006\u0002\u0010\nJ\b\u0010\u000b\u001a\u00020\fH\u0016J\u0018\u0010\r\u001a\u00020\u000e2\u0006\u0010\u000f\u001a\u00020\u00032\u0006\u0010\u0010\u001a\u00020\fH\u0016J\u0018\u0010\u0011\u001a\u00020\u00032\u0006\u0010\u0012\u001a\u00020\u00132\u0006\u0010\u0014\u001a\u00020\fH\u0016R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0017"}, d2 = {"Lcom/ape/meditationretreattimer/ui/adapter/TimerListItemAdapter;", "Landroidx/recyclerview/widget/ListAdapter;", "Lcom/ape/meditationretreattimer/model/Timer;", "Lcom/ape/meditationretreattimer/ui/adapter/TimerListItemAdapter$ViewHolder;", "context", "Landroid/content/Context;", "timers", "", "itemClickListener", "Lcom/ape/meditationretreattimer/ui/adapter/OnTimerListItemClickListener;", "(Landroid/content/Context;Ljava/util/List;Lcom/ape/meditationretreattimer/ui/adapter/OnTimerListItemClickListener;)V", "getItemCount", "", "onBindViewHolder", "", "viewHolder", "position", "onCreateViewHolder", "parent", "Landroid/view/ViewGroup;", "viewType", "DiffCallback", "ViewHolder", "app_debug"})
public final class TimerListItemAdapter extends androidx.recyclerview.widget.ListAdapter<com.ape.meditationretreattimer.model.Timer, com.ape.meditationretreattimer.ui.adapter.TimerListItemAdapter.ViewHolder> {
    private final android.content.Context context = null;
    private final java.util.List<com.ape.meditationretreattimer.model.Timer> timers = null;
    private final com.ape.meditationretreattimer.ui.adapter.OnTimerListItemClickListener itemClickListener = null;
    @org.jetbrains.annotations.NotNull()
    public static final com.ape.meditationretreattimer.ui.adapter.TimerListItemAdapter.DiffCallback DiffCallback = null;
    
    public TimerListItemAdapter(@org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.NotNull()
    java.util.List<com.ape.meditationretreattimer.model.Timer> timers, @org.jetbrains.annotations.NotNull()
    com.ape.meditationretreattimer.ui.adapter.OnTimerListItemClickListener itemClickListener) {
        super(null);
    }
    
    @org.jetbrains.annotations.NotNull()
    @java.lang.Override()
    public com.ape.meditationretreattimer.ui.adapter.TimerListItemAdapter.ViewHolder onCreateViewHolder(@org.jetbrains.annotations.NotNull()
    android.view.ViewGroup parent, int viewType) {
        return null;
    }
    
    @java.lang.Override()
    public void onBindViewHolder(@org.jetbrains.annotations.NotNull()
    com.ape.meditationretreattimer.ui.adapter.TimerListItemAdapter.ViewHolder viewHolder, int position) {
    }
    
    @java.lang.Override()
    public int getItemCount() {
        return 0;
    }
    
    @kotlin.Metadata(mv = {1, 8, 0}, k = 1, d1 = {"\u0000,\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\b\u0007\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0016\u0010\f\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\u000f2\u0006\u0010\u0010\u001a\u00020\u0011R\u000e\u0010\u0005\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0011\u0010\b\u001a\u00020\t\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\u000b\u00a8\u0006\u0012"}, d2 = {"Lcom/ape/meditationretreattimer/ui/adapter/TimerListItemAdapter$ViewHolder;", "Landroidx/recyclerview/widget/RecyclerView$ViewHolder;", "view", "Landroid/view/View;", "(Landroid/view/View;)V", "deleteButton", "editButton", "startButton", "textView", "Landroid/widget/TextView;", "getTextView", "()Landroid/widget/TextView;", "bind", "", "timer", "Lcom/ape/meditationretreattimer/model/Timer;", "clickListener", "Lcom/ape/meditationretreattimer/ui/adapter/OnTimerListItemClickListener;", "app_debug"})
    public static final class ViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
        @org.jetbrains.annotations.NotNull()
        private final android.widget.TextView textView = null;
        private final android.view.View startButton = null;
        private final android.view.View editButton = null;
        private final android.view.View deleteButton = null;
        
        public ViewHolder(@org.jetbrains.annotations.NotNull()
        android.view.View view) {
            super(null);
        }
        
        @org.jetbrains.annotations.NotNull()
        public final android.widget.TextView getTextView() {
            return null;
        }
        
        public final void bind(@org.jetbrains.annotations.NotNull()
        com.ape.meditationretreattimer.model.Timer timer, @org.jetbrains.annotations.NotNull()
        com.ape.meditationretreattimer.ui.adapter.OnTimerListItemClickListener clickListener) {
        }
    }
    
    @kotlin.Metadata(mv = {1, 8, 0}, k = 1, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0004\b\u0086\u0003\u0018\u00002\b\u0012\u0004\u0012\u00020\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0003J\u0018\u0010\u0004\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u00022\u0006\u0010\u0007\u001a\u00020\u0002H\u0016J\u0018\u0010\b\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u00022\u0006\u0010\u0007\u001a\u00020\u0002H\u0016\u00a8\u0006\t"}, d2 = {"Lcom/ape/meditationretreattimer/ui/adapter/TimerListItemAdapter$DiffCallback;", "Landroidx/recyclerview/widget/DiffUtil$ItemCallback;", "Lcom/ape/meditationretreattimer/model/Timer;", "()V", "areContentsTheSame", "", "oldItem", "newItem", "areItemsTheSame", "app_debug"})
    public static final class DiffCallback extends androidx.recyclerview.widget.DiffUtil.ItemCallback<com.ape.meditationretreattimer.model.Timer> {
        
        private DiffCallback() {
            super();
        }
        
        @java.lang.Override()
        public boolean areItemsTheSame(@org.jetbrains.annotations.NotNull()
        com.ape.meditationretreattimer.model.Timer oldItem, @org.jetbrains.annotations.NotNull()
        com.ape.meditationretreattimer.model.Timer newItem) {
            return false;
        }
        
        @java.lang.Override()
        public boolean areContentsTheSame(@org.jetbrains.annotations.NotNull()
        com.ape.meditationretreattimer.model.Timer oldItem, @org.jetbrains.annotations.NotNull()
        com.ape.meditationretreattimer.model.Timer newItem) {
            return false;
        }
    }
}