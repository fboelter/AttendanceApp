package com.cs407.attendanceapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cs407.attendanceapp2.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MissedDayAdapter extends RecyclerView.Adapter<MissedDayAdapter.ViewHolder> {

    private List<Date> missedDays;
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat dayFormat;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Date date);
    }

    // Constructor
    public MissedDayAdapter(List<Date> missedDays, OnItemClickListener listener) {
        this.missedDays = missedDays;
        this.listener = listener;
        dateFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
        dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.missed_day_item, parent, false);
        return new ViewHolder(itemView, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Date missedDay = missedDays.get(position);
        holder.bind(missedDay);
    }

    @Override
    public int getItemCount() {
        return missedDays.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView dateTextView;
        public TextView dayTextView;
        public TextView statusTextView;
        private Date currentMissedDay;

        public ViewHolder(View view, OnItemClickListener listener) {
            super(view);
            dateTextView = view.findViewById(R.id.dateTextView);
            dayTextView = view.findViewById(R.id.dayTextView);
            statusTextView = view.findViewById(R.id.statusTextView);

            view.setOnClickListener(v -> {
                if (listener != null && currentMissedDay != null) {
                    listener.onItemClick(currentMissedDay);
                }
            });
        }

        public void bind(Date missedDay) {
            this.currentMissedDay = missedDay;
            dateTextView.setText(dateFormat.format(missedDay));
            dayTextView.setText(dayFormat.format(missedDay));
            statusTextView.setText(itemView.getContext().getString(R.string.absent));
        }
    }
}
