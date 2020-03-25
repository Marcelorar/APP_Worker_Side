package com.appworkerside;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.appworkerside.utils.Posicion;
import com.appworkerside.utils.Profesion;
import com.appworkerside.utils.Worker;
import com.appworkerside.utils.WorkerLocation;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class DataRegistry extends AppCompatActivity implements LocationListener {
    private static final int GALLERY_REQUEST_CODE = 1;
    private static final int PERMISSIONS = 0;
    protected LocationManager locationManager;
    private FirebaseAuth mAuth;
    private FirebaseDatabase database;
    private DatabaseReference myRef;
    private StorageReference myPics;
    private EditText name;
    private EditText lastName;
    private Spinner profesion;
    private ImageButton foto;
    private Button guardar;
    private Uri upFoto;
    private List<Profesion> profesions;
    private List<Profesion> fitProfesions;
    private boolean posibleUbicar;
    private LatLng currentLocation;
    private Location initialLocation;
    private View.OnClickListener imageClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(Intent.ACTION_PICK);
            // Sets the type as image/*. This ensures only components of type image are selected
            intent.setType("image/*");
            //We pass an extra array with the accepted mime types. This will ensure only components with these MIME types as targeted.
            String[] mimeTypes = {"image/jpeg"};
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            // Launching the Intent
            startActivityForResult(intent, GALLERY_REQUEST_CODE);
        }
    };
    private View.OnClickListener saveClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            final StorageReference photoRef = myPics.child("images/" + mAuth.getCurrentUser().getEmail().replace(".", "_") + ".png");
            myRef = database.getReference("workers");
            myRef.child(mAuth.getCurrentUser().getEmail().replace("@", "+").replace(".", "-"))
                    .setValue(new WorkerLocation(new Posicion(currentLocation.latitude, currentLocation.longitude),
                            new Worker(
                                    mAuth.getCurrentUser().getEmail().replace("@", "+").replace(".", "-")
                                    , name.getText().toString()
                                    , lastName.getText().toString()
                                    , mAuth.getCurrentUser().getEmail().replace(".", "_") + ".png"
                                    , profesion.getSelectedItem().toString()
                            ),
                            5.f,
                            true
                    )).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    photoRef.putFile(upFoto)
                            .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                    System.out.println(taskSnapshot.getUploadSessionUri());

                                }
                            });
                }
            });
            moveToMap();
        }

    };

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_registry);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) && ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {
                Toast.makeText(DataRegistry.this, "Permisos Necesarios :(!", Toast.LENGTH_LONG).show();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                        PERMISSIONS);
                posibleUbicar = true;
            }
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        initialLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        name = findViewById(R.id.name);
        lastName = findViewById(R.id.lastName);
        profesion = findViewById(R.id.profesions);
        foto = findViewById(R.id.photo);
        guardar = findViewById(R.id.signIn);

        foto.setOnClickListener(imageClick);
        guardar.setOnClickListener(saveClick);

        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        myPics = FirebaseStorage.getInstance().getReference();

        profesions = new ArrayList<>();
        fitProfesions = new ArrayList<>();
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadProfesions();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            try {
                Uri selectedImage = data.getData();
                upFoto = selectedImage;
                final InputStream imageStream = getContentResolver().openInputStream(selectedImage);
                final Bitmap image = BitmapFactory.decodeStream(imageStream);
                foto.setImageBitmap(image);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Toast.makeText(DataRegistry.this, "Algo salio Mal", Toast.LENGTH_LONG).show();
            }

        } else {

            Toast.makeText(DataRegistry.this, "Debes escoger una foto!", Toast.LENGTH_LONG).show();
        }

    }

    private void moveToMap() {
        Intent intent = new Intent(this, MapsActivity.class);
        startActivity(intent);
    }

    private void saver() {
        profesions = fitProfesions;
    }

    private void loadProfesions() {
        myRef = database.getReference("profesions");
        Query query = myRef;
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<String> aux = new ArrayList<>();
                if (dataSnapshot.exists()) {

                    for (DataSnapshot profesion : dataSnapshot.getChildren()) {
                        Log.i("Test", profesion.getValue(Profesion.class).toString());
                        aux.add(profesion.getValue(Profesion.class).getNombre());
                        fitProfesions.add(profesion.getValue(Profesion.class));
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(DataRegistry.this,
                            android.R.layout.simple_spinner_dropdown_item, aux);

                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    profesion.setAdapter(adapter);
                    saver();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });

    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        // If request is cancelled, the result arrays are empty.
        posibleUbicar = ((requestCode == PERMISSIONS) && (grantResults.length > 0)
                //2 tipos de ubicaciones permitidas
                && (grantResults[0] == PackageManager.PERMISSION_GRANTED) && (grantResults[1] == PackageManager.PERMISSION_GRANTED));
    }

    @Override
    public void onLocationChanged(Location location) {
        currentLocation = new LatLng(location.getLatitude(),
                location.getLongitude());
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d("Latitude", "disable");
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d("Latitude", "enable");
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d("Latitude", "status");
    }

}
