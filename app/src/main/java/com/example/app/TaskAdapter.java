package com.example.app;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private static final String TAG = "TaskAdapter";

    private List<Task> tasks;
    private Context context;
    private ApiService apiService;
    private OnTaskStatusChangedListener statusChangedListener;

    private int[] card_backgrounds = {
            R.drawable.card_task_main,
            R.drawable.card_task_main2,
            R.drawable.card_task_main3,
            R.drawable.card_task_main4
    };

    public interface OnTaskStatusChangedListener {
        void onTaskStatusChanged();
    }

    public TaskAdapter(List<Task> tasks, Context context) {
        this.tasks = tasks;
        this.context = context;
        this.apiService = RetrofitClient.getApiService();
    }

    public void setOnTaskStatusChangedListener(OnTaskStatusChangedListener listener) {
        this.statusChangedListener = listener;
    }

    public void updateTasks(List<Task> newTasks) {
        this.tasks = newTasks;
        notifyDataSetChanged();
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

        // Устанавливаем состояние выполнения задачи
        updateTaskCompletionUI(holder, task);

        if (position == tasks.size() - 1) {
            holder.task_line.setVisibility(View.GONE);
        } else {
            holder.task_line.setVisibility(View.VISIBLE);
        }

        // Обработчик клика по иконке статуса
        holder.task_status_icon.setOnClickListener(v -> {
            toggleTaskCompletion(task, position);
        });

        // Также делаем кликабельной всю карточку
        holder.task_card.setOnClickListener(v -> {
            toggleTaskCompletion(task, position);
        });
    }

    private void updateTaskCompletionUI(TaskViewHolder holder, Task task) {
        if (task.is_completed() || "выполнено".equals(task.get_status())) {
            holder.task_status_icon.setImageResource(R.drawable.ic_check_circle);
            holder.task_status_icon.setColorFilter(ContextCompat.getColor(context, R.color.green_complete));
            holder.tv_status.setBackgroundResource(R.drawable.badge_green_bg);
            holder.tv_status.setText("выполнено");
            holder.task_card.setAlpha(0.7f); // Делаем карточку полупрозрачной
        } else {
            holder.task_status_icon.setImageResource(R.drawable.ic_circle_empty);
            holder.task_status_icon.setColorFilter(ContextCompat.getColor(context, R.color.light_text));

            // Устанавливаем цвет статуса в зависимости от текста
            String status = task.get_status();
            if ("просрочено".equals(status)) {
                holder.tv_status.setBackgroundResource(R.drawable.badge_error_bg);
            } else if ("в процессе".equals(status)) {
                holder.tv_status.setBackgroundResource(R.drawable.badge_dark_bg);
            } else {
                holder.tv_status.setBackgroundResource(R.drawable.badge_dark_bg);
            }

            holder.task_card.setAlpha(1.0f); // Полная непрозрачность
        }
    }

    private void toggleTaskCompletion(Task task, int position) {
        // Если задача уже выполнена, не делаем ничего (можно сделать отмену выполнения)
        if (task.is_completed() || "выполнено".equals(task.get_status())) {
            // Если нужно разрешить отмену выполнения, раскомментируйте:
            // task.set_completed(false);
            // task.set_status("в процессе");
            // updateTaskStatusOnServer(task); // Исправлено на правильное имя метода
            // notifyItemChanged(position);
            return;
        }

        // Помечаем задачу как выполненную
        task.set_completed(true);
        task.set_status("выполнено");

        // Обновляем UI
        notifyItemChanged(position);

        // Отправляем на сервер - исправлено на правильное имя метода
        updateTaskStatusOnServer(task); // Убрали лишние параметры

        // Уведомляем пользователя
        Toast.makeText(context, "Задача выполнена!", Toast.LENGTH_SHORT).show();

        // Уведомляем слушателя
        if (statusChangedListener != null) {
            statusChangedListener.onTaskStatusChanged();
        }
    }

    private void updateTaskStatusOnServer(Task task) {
        if (!task.hasId()) {
            Log.e("TaskAdapter", "Task doesn't have ID, cannot update on server");
            Log.e("TaskAdapter", "Task details: " + task.getTaskName() + ", ID: " + task.getId());
            return;
        }

        int taskId = task.getId();

        Log.d("TaskAdapter", "Sending update request for task ID: " + taskId);
        Log.d("TaskAdapter", "Status: completed, IsCompleted: true");

        Call<UpdateTaskResponse> call = apiService.updateTaskStatus(
                taskId,
                "completed",
                true
        );

        call.enqueue(new Callback<UpdateTaskResponse>() {
            @Override
            public void onResponse(Call<UpdateTaskResponse> call, Response<UpdateTaskResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UpdateTaskResponse updateResponse = response.body();
                    if (updateResponse.isSuccess()) {
                        Log.d("TaskAdapter", "Task status updated on server: " + taskId);
                        Log.d("TaskAdapter", "Server message: " + updateResponse.getMessage());
                    } else {
                        Log.e("TaskAdapter", "Failed to update task status: " + updateResponse.getMessage());
                    }
                } else {
                    Log.e("TaskAdapter", "Server error updating task status. Code: " + response.code());
                    Log.e("TaskAdapter", "Error body: " + response.errorBody());
                }
            }

            @Override
            public void onFailure(Call<UpdateTaskResponse> call, Throwable t) {
                Log.e("TaskAdapter", "Network error updating task status: " + t.getMessage());
                t.printStackTrace();
            }
        });
    }

    @Override
    public int getItemCount() {
        return tasks != null ? tasks.size() : 0;
    }

    private int get_icon_resource(String icon_name) {
        if (icon_name == null) {
            return R.drawable.ic_book;
        }

        switch (icon_name) {
            case "book":
                return R.drawable.ic_book;
            case "coffee":
                return R.drawable.ic_coffee;
            case "meeting":
                return R.drawable.ic_meeting;
            case "heart":
                return R.drawable.ic_heart;
            default:
                return R.drawable.ic_book;
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