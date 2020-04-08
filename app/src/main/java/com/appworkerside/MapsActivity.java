package com.appworkerside;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Point;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.appworkerside.utils.Posicion;
import com.appworkerside.utils.WorkerLocation;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener {

    private GoogleMap mMap;
    protected LocationManager locationManager;
    private static final int PERMISSIONS = 0;
    private boolean posibleUbicar;
    private LatLng currentLocation;
    private Location initialLocation;
    private Marker mCurrLocationMarker;

    private Handler handler;
    private int delay; //milliseconds
    private Runnable runn;
    private FirebaseAuth mAuth;
    private FirebaseDatabase database;
    private DatabaseReference myRef;
    private StorageReference myPics;
    private WorkerLocation currentWorker = new WorkerLocation();
    private WorkerLocation resCurrentWorker;

    private TextView workerName;
    private TextView workerProfesion;
    private Switch workSwitch;

    private boolean state;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        workerName = findViewById(R.id.workerName);
        workerProfesion = findViewById(R.id.workerProfesion);
        workSwitch = findViewById(R.id.workSwitch);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) && ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {
                Toast.makeText(MapsActivity.this, "Permisos Necesarios :(!", Toast.LENGTH_LONG).show();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},
                        PERMISSIONS);
                posibleUbicar = true;
            }
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        initialLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        //FLoating button de centrado

        FloatingActionButton fab = findViewById(R.id.centrador);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(initialLocation.getLatitude(), initialLocation.getLongitude()), 19.f));
            }
        });


        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        myPics = FirebaseStorage.getInstance().getReference();


        handler = new Handler();
        delay = 500; //milliseconds
        runn = new Runnable() {
            public void run() {
                getCurrentWorker();
                state = currentWorker.isVisible();

                if (currentWorker.getWorkUser().getNombre() != null && workerName.getText().toString().isEmpty()) {
                    workerName.setText(currentWorker.getWorkUser().getNombre() + " " + currentWorker.getWorkUser().getApellido());
                    workerProfesion.setText(currentWorker.getWorkUser().getEspecializacion());
                    workSwitch.setChecked(currentWorker.isVisible());
                } else if (state != workSwitch.isChecked()) {
                    updateState(workSwitch.isChecked());
                }
                if (mMap != null) {
                    if (workSwitch.isChecked()) {
                        try {
                            boolean success = mMap.setMapStyle(
                                    MapStyleOptions.loadRawResourceStyle(
                                            MapsActivity.this, R.raw.map_on));

                            if (!success) {
                                Log.e("Mapa:", "Style parsing failed.");
                            }
                        } catch (Resources.NotFoundException e) {
                            Log.e("Mapa", "Can't find style. Error: ", e);
                        }
                    } else {
                        try {
                            boolean success = mMap.setMapStyle(
                                    MapStyleOptions.loadRawResourceStyle(
                                            MapsActivity.this, R.raw.map_off));

                            if (!success) {
                                Log.e("Mapa:", "Style parsing failed.");
                            }
                        } catch (Resources.NotFoundException e) {
                            Log.e("Mapa", "Can't find style. Error: ", e);
                        }
                    }
                }
                if (currentWorker.getContratado() != null && !currentWorker.getContratado().isEmpty()) {
                    moveToProcess();
                }
                handler.postDelayed(this, delay);
            }

        };
    }

    @Override
    protected void onStart() {
        super.onStart();
        workSwitch.setChecked(true);
        handler.postDelayed(runn, delay);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                        MapsActivity.this, R.raw.map_on));
        mMap.setMinZoomPreference(10.0f);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(initialLocation.getLatitude(), initialLocation.getLongitude()), 19.f));

        //Place current location marker
        LatLng latLng = new LatLng(initialLocation.getLatitude(), initialLocation.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title(mAuth.getCurrentUser().getEmail());
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
        mCurrLocationMarker = mMap.addMarker(markerOptions);
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
        updatePosition(location);
        if (mMap != null) animateMarker(mCurrLocationMarker, currentLocation, false);
    }
    @Override
    public void onProviderDisabled(String provider) {
        Log.d("Latitude","disable");
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d("Latitude","enable");
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d("Latitude","status");
    }


    public void animateMarker(final Marker marker, final LatLng toPosition, final boolean hideMarker) {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        Projection proj = mMap.getProjection();
        Point startPoint = proj.toScreenLocation(marker.getPosition());
        final LatLng startLatLng = proj.fromScreenLocation(startPoint);
        final long duration = 500;

        final Interpolator interpolator = new LinearInterpolator();

        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed
                        / duration);
                double lng = t * toPosition.longitude + (1 - t)
                        * startLatLng.longitude;
                double lat = t * toPosition.latitude + (1 - t)
                        * startLatLng.latitude;
                marker.setPosition(new LatLng(lat, lng));

                if (t < 1.0) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16);
                } else {
                    if (hideMarker) {
                        marker.setVisible(false);
                    } else {
                        marker.setVisible(true);
                    }
                }
            }
        });
    }

    private void updatePosition(Location location) {
        myRef = database.getReference("workers");
        myRef.child(mAuth.getCurrentUser().getEmail().replace("@", "+").replace(".", "-"))
                .child("posicion").setValue(new Posicion(location.getLatitude(), location.getLongitude())).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.i("Real:", "Posicion actualizada");
            }
        });
    }

    private void updateState(final boolean state) {
        myRef = database.getReference("workers");
        myRef.child(mAuth.getCurrentUser().getEmail().replace("@", "+").replace(".", "-"))
                .child("visible").setValue(state).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.i("Real:", "Estado actualizado");
            }
        });
    }

    public void saverWorker() {
        currentWorker = resCurrentWorker;
    }


    private void getCurrentWorker() {

        myRef = database.getReference("workers");
        Query query = myRef.orderByChild("workUser/username").equalTo(mAuth.getCurrentUser().getEmail().replace("@", "+").replace(".", "-"));
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    resCurrentWorker = dataSnapshot.getChildren().iterator().next().getValue(WorkerLocation.class);
                    Log.i("Current", resCurrentWorker.getWorkUser().getNombre());
                }
                saverWorker();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        handler.removeCallbacks(runn);
    }

    private void moveToProcess() {
        Intent intent = new Intent(this, OrderingProcess.class);
        intent.putExtra("orderCode", currentWorker.getContratado());
        intent.putExtra("currentUser", currentWorker);
        startActivity(intent);
    }

}
