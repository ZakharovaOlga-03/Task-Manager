package com.example.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<Task> tasks;
    private Context context;
    private int[] card_backgrounds = {
            R.drawable.card_task_main,
            R.drawable.card_task_main2,
            R.drawable.card_task_main3,
            R.drawable.card_task_main4
    };

    public TaskAdapter(List<Task> tasks, Context context) {
        this.tasks = tasks;
        this.context = context;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = tasks.get(position);

        holder.tv_time.setText(task.get_time());
        holder.tv_title.setText(task.get_title());
        holder.tv_duration.setText(task.get_duration());
        holder.tv_status.setText(task.get_status());

        int icon_resource = get_icon_resource(task.get_icon_name());
        holder.iv_icon.setImageResource(icon_resource);

        int background_index = position % 4;
        holder.task_card.setBackgroundResource(card_backgrounds[background_index]);

        if (task.is_completed()) {
            holder.task_status_icon.setImageResource(R.drawable.ic_check_circle);
            holder.task_status_icon.setColorFilter(ContextCompat.getColor(context, R.color.green_complete));
            holder.tv_status.setBackgroundResource(R.drawable.badge_green_bg);
            holder.tv_status.setText("выполнено");
        } else {
            holder.task_status_icon.setImageResource(R.drawable.ic_circle_empty);
            holder.task_status_icon.setColorFilter(ContextCompat.getColor(context, R.color.light_text));
            holder.tv_status.setBackgroundResource(R.drawable.badge_dark_bg);
        }

        if (position == tasks.size() - 1) {
            holder.task_line.setVisibility(View.GONE);
        } else {
            holder.task_line.setVisibility(View.VISIBLE);
        }

        holder.task_status_icon.setOnClickListener(v -> {
            task.set_completed(!task.is_completed());
            notifyItemChanged(position);
        });
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    private int get_icon_resource(String icon_name) {
        switch (icon_name) {
            case "book": return R.drawable.ic_book;
            case "coffee": return R.drawable.ic_coffee;
            case "meeting": return R.drawable.ic_meeting;
            case "heart": return R.drawable.ic_heart;
            default: return R.drawable.ic_book;
        }
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tv_time, tv_title, tv_duration, tv_status;
        ImageView iv_icon, task_status_icon;
        View task_line;
        LinearLayout task_card;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tv_time = itemView.findViewById(R.id.tv_time);
            tv_title = itemView.findViewById(R.id.tv_title);
            tv_duration = itemView.findViewById(R.id.tv_duration);
            tv_status = itemView.findViewById(R.id.tv_status);
            iv_icon = itemView.findViewById(R.id.iv_icon);
            task_status_icon = itemView.findViewById(R.id.task_status_icon);
            task_line = itemView.findViewById(R.id.task_line);
            task_card = itemView.findViewById(R.id.task_card);
        }
    }
}
