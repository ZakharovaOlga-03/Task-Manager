package com.example.app;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import androidx.core.content.ContextCompat;

public class NavigationHelper {

    public static void setup_navigation(Activity activity, int current_page) {
        View bottom_nav_container = activity.findViewById(R.id.bottom_nav_container);
        if (bottom_nav_container == null) {
            // Если контейнер не найден, ищем в activity_main
            bottom_nav_container = activity.findViewById(R.id.bottom_nav);
            if (bottom_nav_container == null) return;
        }

        View nav_home = bottom_nav_container.findViewById(R.id.nav_home);
        View nav_calendar = bottom_nav_container.findViewById(R.id.nav_calendar);
        View nav_add = bottom_nav_container.findViewById(R.id.nav_add);
        View nav_chart = bottom_nav_container.findViewById(R.id.nav_chart);
        View nav_profile = bottom_nav_container.findViewById(R.id.nav_profile);

        // Проверяем существование всех элементов
        if (nav_home == null || nav_calendar == null || nav_add == null ||
                nav_chart == null || nav_profile == null) {
            return;
        }

        ImageView nav_home_icon = bottom_nav_container.findViewById(R.id.nav_home_icon);
        ImageView nav_calendar_icon = bottom_nav_container.findViewById(R.id.nav_calendar_icon);
        ImageView nav_chart_icon = bottom_nav_container.findViewById(R.id.nav_chart_icon);
        ImageView nav_profile_icon = bottom_nav_container.findViewById(R.id.nav_profile_icon);

        View nav_home_indicator = bottom_nav_container.findViewById(R.id.nav_home_indicator);
        View nav_calendar_indicator = bottom_nav_container.findViewById(R.id.nav_calendar_indicator);
        View nav_chart_indicator = bottom_nav_container.findViewById(R.id.nav_chart_indicator);
        View nav_profile_indicator = bottom_nav_container.findViewById(R.id.nav_profile_indicator);

        reset_all_indicators(nav_home_indicator, nav_calendar_indicator, nav_chart_indicator, nav_profile_indicator);
        reset_all_icons(activity, nav_home_icon, nav_calendar_icon, nav_chart_icon, nav_profile_icon);

        // Устанавливаем активную страницу
        switch (current_page) {
            case 0:
                if (nav_home_icon != null && nav_home_indicator != null) {
                    set_active(activity, nav_home_icon, nav_home_indicator);
                }
                break;
            case 1:
                if (nav_calendar_icon != null && nav_calendar_indicator != null) {
                    set_active(activity, nav_calendar_icon, nav_calendar_indicator);
                }
                break;
            case 3:
                if (nav_chart_icon != null && nav_chart_indicator != null) {
                    set_active(activity, nav_chart_icon, nav_chart_indicator);
                }
                break;
            case 4:
                if (nav_profile_icon != null && nav_profile_indicator != null) {
                    set_active(activity, nav_profile_icon, nav_profile_indicator);
                }
                break;
        }

        // Обработчики кликов с проверкой текущей страницы
        if (nav_home != null) {
            nav_home.setOnClickListener(v -> {
                if (current_page != 0) {
                    animate_click(v);
                    Intent intent = new Intent(activity, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    activity.startActivity(intent);
                    activity.overridePendingTransition(0, 0);
                    activity.finish(); // Закрываем текущую активити
                }
            });
        }

        if (nav_calendar != null) {
            nav_calendar.setOnClickListener(v -> {
                if (current_page != 1) {
                    animate_click(v);
                    Intent intent = new Intent(activity, CalendarActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    activity.startActivity(intent);
                    activity.overridePendingTransition(0, 0);
                    activity.finish();
                }
            });
        }

        if (nav_add != null) {
            nav_add.setOnClickListener(v -> {
                animate_click(v);
                Intent intent = new Intent(activity, AddTaskActivity.class);
                activity.startActivityForResult(intent, 100);
                activity.overridePendingTransition(0, 0);
            });
        }

        if (nav_chart != null) {
            nav_chart.setOnClickListener(v -> {
                if (current_page != 3) {
                    animate_click(v);
                    Intent intent = new Intent(activity, StatsActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    activity.startActivity(intent);
                    activity.overridePendingTransition(0, 0);
                    activity.finish();
                }
            });
        }

        if (nav_profile != null) {
            nav_profile.setOnClickListener(v -> {
                if (current_page != 4) {
                    animate_click(v);
                    Intent intent = new Intent(activity, ProfileActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    activity.startActivity(intent);
                    activity.overridePendingTransition(0, 0);
                    activity.finish();
                }
            });
        }
    }

    private static void reset_all_indicators(View... indicators) {
        for (View indicator : indicators) {
            if (indicator != null) {
                indicator.setVisibility(View.GONE);
            }
        }
    }

    private static void reset_all_icons(Activity activity, ImageView... icons) {
        for (ImageView icon : icons) {
            if (icon != null) {
                icon.setColorFilter(ContextCompat.getColor(activity, R.color.light_text));
            }
        }
    }

    private static void set_active(Activity activity, ImageView icon, View indicator) {
        if (icon != null) {
            icon.setColorFilter(ContextCompat.getColor(activity, R.color.accent_color));
        }

        if (indicator != null) {
            indicator.setVisibility(View.VISIBLE);

            Animation scale_up = new ScaleAnimation(
                    0f, 1f, 1f, 1f,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f
            );
            scale_up.setDuration(300);
            indicator.startAnimation(scale_up);
        }
    }

    private static void animate_click(View view) {
        if (view == null) return;

        ScaleAnimation scale_down = new ScaleAnimation(
                1f, 0.9f, 1f, 0.9f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        scale_down.setDuration(100);

        ScaleAnimation scale_up = new ScaleAnimation(
                0.9f, 1f, 0.9f, 1f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        scale_up.setDuration(100);
        scale_up.setStartOffset(100);

        view.startAnimation(scale_down);
        view.startAnimation(scale_up);
    }
}