package com.appworkerside;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.PersistableBundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

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

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DataRegistry extends AppCompatActivity implements LocationListener {
    static final int REQUEST_IMAGE_CAPTURE = 1;
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
    private String currentPhotoPath;
    private List<Profesion> profesions;
    private List<Profesion> fitProfesions;
    private boolean posibleUbicar;
    private LatLng currentLocation;
    private Location initialLocation;
    private View.OnClickListener imageClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            // Ensure that there's a camera activity to handle the intent
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                // Create the File where the photo should go
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                } catch (IOException ex) {
                    Toast.makeText(DataRegistry.this, "Algo salio mal al guardar tu foto!", Toast.LENGTH_LONG).show();
                }
                // Continue only if the File was successfully created
                if (photoFile != null) {
                    Uri photoURI = FileProvider.getUriForFile(DataRegistry.this,
                            "com.appworkerside.android.fileprovider",
                            photoFile);
                    upFoto = photoURI;
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                }
            }
        }
    };
    private View.OnClickListener saveClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            final StorageReference photoRef = myPics.child("images/" + mAuth.getCurrentUser().getEmail().replace(".", "_") + ".jpg");
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
                            5.f
                            , true
                            , false
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

        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        myPics = FirebaseStorage.getInstance().getReference();

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) && ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) && ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.CAMERA) && ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Toast.makeText(DataRegistry.this, "Permisos Necesarios :(!", Toast.LENGTH_LONG).show();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
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

        profesions = new ArrayList<>();
        fitProfesions = new ArrayList<>();

        loadProfesions();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onPostCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onPostCreate(savedInstanceState, persistentState);
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            try {
                Bitmap imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), upFoto);
                foto.setImageBitmap(imageBitmap);
            } catch (IOException e) {
                Toast.makeText(DataRegistry.this, "Error en Camara :(", Toast.LENGTH_LONG).show();
                e.printStackTrace();
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
        if (!((requestCode == PERMISSIONS) && (grantResults.length > 0)
                && (grantResults[0] == PackageManager.PERMISSION_GRANTED) && (grantResults[1] == PackageManager.PERMISSION_GRANTED)
                && (grantResults[2] == PackageManager.PERMISSION_GRANTED) && (grantResults[3] == PackageManager.PERMISSION_GRANTED)))
            finish();
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
