package com.example.app;

import android.content.Context;
import android.graphics.Paint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class CalendarTaskAdapter extends RecyclerView.Adapter<CalendarTaskAdapter.TaskViewHolder> {

    private static final String TAG = "CalendarTaskAdapter";

    private List<Task> tasks;
    private Context context;
    private int[] card_backgrounds = {
            R.drawable.card_task_main,
            R.drawable.card_task_main2,
            R.drawable.card_task_main3,
            R.drawable.card_task_main4
    };

    public CalendarTaskAdapter(List<Task> tasks, Context context) {
        this.tasks = tasks;
        this.context = context;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = tasks.get(position);

        // Проверяем, не является ли это сообщением "Нет задач"
        if (task.get_title() != null && task.get_title().equals("Нет задач на этот день")) {
            holder.tv_time.setVisibility(View.GONE);
            holder.tv_status.setVisibility(View.GONE);
            holder.tv_title.setText(task.get_title());
            holder.tv_title.setTextColor(context.getResources().getColor(R.color.text_secondary));
            holder.tv_title.setGravity(android.view.Gravity.CENTER);
            holder.itemView.setBackgroundResource(android.R.color.transparent);
            return;
        }

        holder.tv_time.setVisibility(View.VISIBLE);
        holder.tv_status.setVisibility(View.VISIBLE);
        holder.tv_title.setGravity(android.view.Gravity.START);

        holder.tv_time.setText(task.get_time());
        holder.tv_title.setText(task.get_title());
        holder.tv_status.setText(task.get_status());

        // Устанавливаем цвет плашки в зависимости от статуса
        String status = task.get_status();
        boolean isCompleted = task.is_completed();
        int badgeBackground = R.drawable.badge_dark_bg; // По умолчанию

        if ("выполнено".equals(status) || isCompleted) {
            badgeBackground = R.drawable.badge_green_bg;
            // Зачеркиваем текст для выполненных задач
            holder.tv_title.setPaintFlags(holder.tv_title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            // Делаем карточку полупрозрачной
            holder.itemView.setAlpha(0.7f);
        } else if ("просрочено".equals(status)) {
            badgeBackground = R.drawable.badge_error_bg;
            holder.tv_title.setPaintFlags(holder.tv_title.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.itemView.setAlpha(1.0f);
        } else if ("текущая".equals(status)) {
            badgeBackground = R.drawable.badge_dark_bg;
            holder.tv_title.setPaintFlags(holder.tv_title.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.itemView.setAlpha(1.0f);
        } else if ("в процессе".equals(status)) {
            badgeBackground = R.drawable.badge_dark_bg;
            holder.tv_title.setPaintFlags(holder.tv_title.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.itemView.setAlpha(1.0f);
        } else {
            badgeBackground = R.drawable.badge_dark_bg;
            holder.tv_title.setPaintFlags(holder.tv_title.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.itemView.setAlpha(1.0f);
        }

        holder.tv_status.setBackgroundResource(badgeBackground);
        // Текст всегда белый
        holder.tv_status.setTextColor(context.getResources().getColor(R.color.white_text));

        int background_index = position % 4;
        holder.itemView.setBackgroundResource(card_backgrounds[background_index]);
    }

    @Override
    public int getItemCount() {
        return tasks != null ? tasks.size() : 0;
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tv_time, tv_title, tv_status;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tv_time = itemView.findViewById(R.id.tv_time);
            tv_title = itemView.findViewById(R.id.tv_title);
            tv_status = itemView.findViewById(R.id.tv_status);
        }
    }
}
