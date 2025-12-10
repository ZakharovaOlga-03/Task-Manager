package com.example.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class CalendarGridAdapter extends RecyclerView.Adapter<CalendarGridAdapter.DayViewHolder> {

    private List<CalendarDay> days;
    private Context context;
    private OnDayClickListener listener;

    public interface OnDayClickListener {
        void on_day_click(int position);
    }

    public CalendarGridAdapter(List<CalendarDay> days, Context context, OnDayClickListener listener) {
        this.days = days;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_grid_day, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        CalendarDay day = days.get(position);
        int dayNumber = day.get_day_number();

        if (dayNumber > 0) {
            holder.tv_day_number.setText(String.valueOf(dayNumber));
            holder.tv_day_number.setVisibility(View.VISIBLE);

            if (day.is_selected()) {
                holder.tv_day_number.setTextColor(context.getResources().getColor(R.color.white_text));
                holder.day_indicator.setVisibility(View.VISIBLE);
            } else if (day.is_today()) {
                holder.tv_day_number.setTextColor(context.getResources().getColor(R.color.accent_color));
                holder.day_indicator.setVisibility(View.GONE);
            } else {
                holder.tv_day_number.setTextColor(context.getResources().getColor(R.color.light_text));
                holder.day_indicator.setVisibility(View.GONE);
            }
        } else {
            holder.tv_day_number.setVisibility(View.INVISIBLE);
            holder.day_indicator.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null && dayNumber > 0) {
                listener.on_day_click(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    static class DayViewHolder extends RecyclerView.ViewHolder {
        TextView tv_day_number;
        View day_indicator;

        public DayViewHolder(@NonNull View itemView) {
            super(itemView);
            tv_day_number = itemView.findViewById(R.id.tv_day_number);
            day_indicator = itemView.findViewById(R.id.day_indicator);
        }
    }
}