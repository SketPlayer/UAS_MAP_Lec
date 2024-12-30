package com.example.uts_lec;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private final List<ParkingData> parkingDataList;

    public HistoryAdapter(List<ParkingData> parkingDataList) {
        this.parkingDataList = parkingDataList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_parking_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ParkingData parkingData = parkingDataList.get(position);

        // Bind data to views
        holder.textViewLocation.setText(
                String.format(Locale.getDefault(), "Location: %.6f, %.6f", parkingData.getLatitude(), parkingData.getLongitude()));

        // Format timestamp to date and time
        Date date = new Date(parkingData.getTimestamp());
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

        holder.textViewDate.setText(String.format("Date: %s", dateFormat.format(date)));
        holder.textViewTime.setText(String.format("Time: %s", timeFormat.format(date)));

        // Load image using Glide
        Glide.with(holder.imageViewParking.getContext())
                .load(parkingData.getImageUrl())
                .centerCrop()
                .into(holder.imageViewParking);
    }

    @Override
    public int getItemCount() {
        return parkingDataList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageViewParking;
        private final TextView textViewDate;
        private final TextView textViewTime;
        private final TextView textViewLocation;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewParking = itemView.findViewById(R.id.imageViewParking);
            textViewDate = itemView.findViewById(R.id.textViewDate);
            textViewTime = itemView.findViewById(R.id.textViewTime);
            textViewLocation = itemView.findViewById(R.id.textViewLocation);
        }
    }
}
