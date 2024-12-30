package com.example.uts_lec;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private HistoryAdapter historyAdapter;
    private List<ParkingData> parkingDataList;
    private FirebaseFirestore db;
    private ProgressBar progressBar;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // Initialize UI components
        RecyclerView recyclerViewHistory = findViewById(R.id.recyclerViewHistory);
        progressBar = findViewById(R.id.progressBar);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize RecyclerView
        parkingDataList = new ArrayList<>();
        historyAdapter = new HistoryAdapter(parkingDataList);
        recyclerViewHistory.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewHistory.setAdapter(historyAdapter);

        // Fetch data for the logged-in user
        fetchParkingData();
    }

    private void fetchParkingData() {
        // Get the currently logged-in user's UID
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String userUID = currentUser.getUid();

        // Show progress bar while loading data
        progressBar.setVisibility(View.VISIBLE);

        // Query Firestore for data specific to this user
        db.collection("parking_data")
                .whereEqualTo("userUID", userUID) // Filter by userUID
                .get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE); // Hide progress bar
                    if (task.isSuccessful() && task.getResult() != null) {
                        parkingDataList.clear(); // Clear the list before adding new data
                        for (DocumentSnapshot document : task.getResult()) {
                            ParkingData parkingData = document.toObject(ParkingData.class);
                            parkingDataList.add(parkingData);
                        }
                        // Notify the adapter that data has changed
                        historyAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(this, "Failed to load data", Toast.LENGTH_SHORT).show();
                        Log.e("FirestoreError", "Error fetching data", task.getException());
                    }
                });
    }
}
