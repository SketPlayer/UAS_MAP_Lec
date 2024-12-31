package com.example.uts_lec

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class HistoryAdapter(private val parkingDataList: MutableList<ParkingData>) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_parking_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val parkingData = parkingDataList[position]

        // Bind data to views
        holder.textViewLocation.text =
            String.format(Locale.getDefault(), "Location: %.6f, %.6f", parkingData.latitude, parkingData.longitude)

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

        // Delete button functionality
        holder.deleteButton.setOnClickListener {
            deleteItem(parkingData, position, holder)
        }
    }

    override fun getItemCount(): Int = parkingDataList.size

    // ViewHolder class
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageViewParking: ImageView = itemView.findViewById(R.id.imageViewParking)
        val textViewDate: TextView = itemView.findViewById(R.id.textViewDate)
        val textViewTime: TextView = itemView.findViewById(R.id.textViewTime)
        val textViewLocation: TextView = itemView.findViewById(R.id.textViewLocation)
        val deleteButton: ImageView = itemView.findViewById(R.id.deleteButton)
    }

    // Function to delete an item
    private fun deleteItem(parkingData: ParkingData, position: Int, holder: ViewHolder) {
        val db = FirebaseFirestore.getInstance()
        val context = holder.itemView.context
        val storage = com.google.firebase.storage.FirebaseStorage.getInstance()

        val currentUserUID = parkingData.userUID

        // Step 1: Locate the Firestore document
        db.collection("parking_data")
            .whereEqualTo("userUID", currentUserUID)
            .whereEqualTo("latitude", parkingData.latitude)
            .whereEqualTo("longitude", parkingData.longitude)
            .whereEqualTo("timestamp", parkingData.timestamp)
            .get()
            .addOnSuccessListener { documents ->
                if (documents != null && !documents.isEmpty) {
                    for (document in documents) {
                        val parkingDocId = document.id

                        // Step 2: Delete the image from Firebase Storage
                        val imageUrl = parkingData.imageUrl
                        if (imageUrl.isNotEmpty()) {
                            val storageRef = storage.getReferenceFromUrl(imageUrl)
                            storageRef.delete()
                                .addOnSuccessListener {
                                    Log.d("Storage", "Image deleted successfully")

                                    // Step 3: Check and reset activeParking status
                                    resetActiveParkingIfNeeded(currentUserUID, parkingDocId)

                                    // Step 4: Delete the Firestore document
                                    db.collection("parking_data").document(parkingDocId)
                                        .delete()
                                        .addOnSuccessListener {
                                            Toast.makeText(context, "Item deleted", Toast.LENGTH_SHORT).show()

                                            // Remove the item from the list
                                            parkingDataList.removeAt(position)

                                            // Notify adapter of the change
                                            notifyItemRemoved(position)
                                            notifyItemRangeChanged(position, parkingDataList.size)
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("FirestoreError", "Error deleting document", e)
                                            Toast.makeText(context, "Failed to delete Firestore document", Toast.LENGTH_SHORT).show()
                                        }
                                }
                                .addOnFailureListener { e ->
                                    Log.e("StorageError", "Error deleting image", e)
                                    Toast.makeText(context, "Failed to delete image", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            // Step 3: Check and reset activeParking status
                            resetActiveParkingIfNeeded(currentUserUID, parkingDocId)

                            // If imageUrl is empty, just delete the document
                            db.collection("parking_data").document(parkingDocId)
                                .delete()
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Item deleted", Toast.LENGTH_SHORT).show()

                                    // Remove the item from the list
                                    parkingDataList.removeAt(position)

                                    // Notify adapter of the change
                                    notifyItemRemoved(position)
                                    notifyItemRangeChanged(position, parkingDataList.size)
                                }
                                .addOnFailureListener { e ->
                                    Log.e("FirestoreError", "Error deleting document", e)
                                    Toast.makeText(context, "Failed to delete Firestore document", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                } else {
                    Toast.makeText(context, "No matching document found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreError", "Error fetching document", e)
                Toast.makeText(context, "Failed to delete item", Toast.LENGTH_SHORT).show()
            }
    }

    // Function to reset activeParking if this is the active parking session
    private fun resetActiveParkingIfNeeded(userUID: String, parkingDocId: String) {
        val db = FirebaseFirestore.getInstance()

        // Access the user's Firestore document
        db.collection("users").document(userUID)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val currentParkingID = document.getString("currentParkingID")

                    // Compare if the deleted parking session matches the active parking session
                    if (currentParkingID == parkingDocId) {
                        // Reset activeParking status
                        val updates = mapOf(
                            "activeParking" to false,
                            "currentParkingID" to null
                        )
                        db.collection("users").document(userUID)
                            .update(updates)
                            .addOnSuccessListener {
                                Log.d("Firestore", "Active parking reset successfully")
                            }
                            .addOnFailureListener { e ->
                                Log.e("FirestoreError", "Failed to reset active parking", e)
                            }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreError", "Failed to fetch user document", e)
            }
    }

}
