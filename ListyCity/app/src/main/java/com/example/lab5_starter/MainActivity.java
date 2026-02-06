package com.example.lab5_starter;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class MainActivity extends AppCompatActivity implements CityDialogFragment.CityDialogListener {

    private Button addCityButton;
    private ListView cityListView;

    private ArrayList<City> cityArrayList;
    private ArrayAdapter<City> cityArrayAdapter;

    // 🔥 Firestore
    private FirebaseFirestore db;
    private CollectionReference citiesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Set views
        addCityButton = findViewById(R.id.buttonAddCity);
        cityListView = findViewById(R.id.listviewCities);

        // create city array + adapter
        cityArrayList = new ArrayList<>();
        cityArrayAdapter = new CityArrayAdapter(this, cityArrayList);
        cityListView.setAdapter(cityArrayAdapter);

        // 🔥 Firestore setup
        db = FirebaseFirestore.getInstance();
        citiesRef = db.collection("cities");

        // 🔥 Listen for changes in Firestore "cities" collection
        citiesRef.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e("Firestore", "Listen failed.", error);
                return;
            }

            cityArrayList.clear();

            if (value != null) {
                for (QueryDocumentSnapshot doc : value) {
                    String name = doc.getString("name");
                    String province = doc.getString("province");
                    cityArrayList.add(new City(name, province));
                }
            }

            cityArrayAdapter.notifyDataSetChanged();
        });

        // set listeners
        addCityButton.setOnClickListener(view -> {
            CityDialogFragment cityDialogFragment = new CityDialogFragment();
            cityDialogFragment.show(getSupportFragmentManager(),"Add City");
        });

        cityListView.setOnItemClickListener((adapterView, view, i, l) -> {
            City city = cityArrayAdapter.getItem(i);
            CityDialogFragment cityDialogFragment = CityDialogFragment.newInstance(city);
            cityDialogFragment.show(getSupportFragmentManager(),"City Details");
        });

        // 🔥 Long-press to delete a city
        cityListView.setOnItemLongClickListener((parent, view, position, id) -> {
            City selectedCity = cityArrayAdapter.getItem(position);
            if (selectedCity == null) return true;

            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Delete city")
                    .setMessage("Delete " + selectedCity.getName() + "?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        // immediate local removal
                        cityArrayList.remove(selectedCity);
                        cityArrayAdapter.notifyDataSetChanged();

                        // remove from Firestore
                        citiesRef.document(selectedCity.getName())
                                .delete()
                                .addOnSuccessListener(aVoid ->
                                        Log.d("Firestore", "City deleted: " + selectedCity.getName()))
                                .addOnFailureListener(e ->
                                        Log.e("Firestore", "Error deleting city", e));
                    })
                    .setNegativeButton("Cancel", null)
                    .show();

            return true;
        });
    }

    @Override
    public void updateCity(City city, String title, String year) {
        String oldName = city.getName();

        city.setName(title);
        city.setProvince(year);
        cityArrayAdapter.notifyDataSetChanged();

        // 🔥 Update Firestore: delete old doc + add new
        citiesRef.document(oldName).delete()
                .addOnSuccessListener(aVoid ->
                        Log.d("Firestore", "Old city deleted: " + oldName))
                .addOnFailureListener(e ->
                        Log.e("Firestore", "Error deleting old city", e));

        citiesRef.document(city.getName()).set(city)
                .addOnSuccessListener(aVoid ->
                        Log.d("Firestore", "City updated: " + city.getName()))
                .addOnFailureListener(e ->
                        Log.e("Firestore", "Error updating city", e));
    }

    @Override
    public void addCity(City city){
        cityArrayList.add(city);
        cityArrayAdapter.notifyDataSetChanged();

        // 🔥 Save to Firestore
        DocumentReference docRef = citiesRef.document(city.getName());
        docRef.set(city)
                .addOnSuccessListener(aVoid ->
                        Log.d("Firestore", "City added: " + city.getName()))
                .addOnFailureListener(e ->
                        Log.e("Firestore", "Error adding city", e));
    }

    // Optional: no longer used, but you can keep or delete it
    public void addDummyData(){
        City m1 = new City("Edmonton", "AB");
        City m2 = new City("Vancouver", "BC");
        cityArrayList.add(m1);
        cityArrayList.add(m2);
        cityArrayAdapter.notifyDataSetChanged();
    }
}
