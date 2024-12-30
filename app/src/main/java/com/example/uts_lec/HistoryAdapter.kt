package com.example.uts_lec

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.*

class HistoryAdapter(private val parkingDataList: List<ParkingData>) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_parking_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val parkingData = parkingDataList[position]

        // Bind data to views
        holder.textViewLocation.text = String.format(Locale.getDefault(), "Location: %.6f, %.6f", parkingData.latitude, parkingData.longitude)

        // Format timestamp to date and time
        val date = Date(parkingData.timestamp)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

        holder.textViewDate.text = String.format("Date: %s", dateFormat.format(date))
        holder.textViewTime.text = String.format("Time: %s", timeFormat.format(date))

        // Load image using Glide
        Glide.with(holder.imageViewParking.context)
            .load(parkingData.imageUrl)
            .centerCrop()
            .into(holder.imageViewParking)
    }

    override fun getItemCount(): Int = parkingDataList.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageViewParking: ImageView = itemView.findViewById(R.id.imageViewParking)
        val textViewDate: TextView = itemView.findViewById(R.id.textViewDate)
        val textViewTime: TextView = itemView.findViewById(R.id.textViewTime)
        val textViewLocation: TextView = itemView.findViewById(R.id.textViewLocation)
    }
}
