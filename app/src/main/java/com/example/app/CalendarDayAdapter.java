package com.example.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class CalendarDayAdapter extends RecyclerView.Adapter<CalendarDayAdapter.DayViewHolder> {

    private List<CalendarDay> days;
    private Context context;
    private OnDayClickListener listener;

    public interface OnDayClickListener {
        void on_day_click(int position);
    }

    public CalendarDayAdapter(List<CalendarDay> days, Context context, OnDayClickListener listener) {
        this.days = days;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_day, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        CalendarDay day = days.get(position);

        holder.tv_day_name.setText(day.get_day_name());
        holder.tv_day_number.setText(String.valueOf(day.get_day_number()));

        // Выделение выбранного дня
        if (day.is_selected()) {
            holder.day_indicator.setVisibility(View.VISIBLE);
            holder.itemView.setBackgroundResource(R.drawable.calendar_selected_bg);
            holder.tv_day_number.setTextColor(context.getResources().getColor(R.color.white_text));
            holder.tv_day_name.setTextColor(context.getResources().getColor(R.color.white_text));
        } else {
            holder.day_indicator.setVisibility(View.GONE);

            // Сегодняшний день
            if (day.is_today()) {
                holder.itemView.setBackgroundResource(R.drawable.calendar_day_bg);
                holder.tv_day_number.setTextColor(context.getResources().getColor(R.color.white_text));
                holder.tv_day_name.setTextColor(context.getResources().getColor(R.color.white_text));
            } else {
                holder.itemView.setBackgroundResource(R.drawable.calendar_day_bg);
                holder.tv_day_number.setTextColor(context.getResources().getColor(R.color.light_text));
                holder.tv_day_name.setTextColor(context.getResources().getColor(R.color.light_text));
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.on_day_click(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    static class DayViewHolder extends RecyclerView.ViewHolder {
        TextView tv_day_name, tv_day_number;
        View day_indicator;

        public DayViewHolder(@NonNull View itemView) {
            super(itemView);
            tv_day_name = itemView.findViewById(R.id.tv_day_name);
            tv_day_number = itemView.findViewById(R.id.tv_day_number);
            day_indicator = itemView.findViewById(R.id.day_indicator);
        }
    }
}