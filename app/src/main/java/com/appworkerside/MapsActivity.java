package com.appworkerside;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.appworkerside.utils.Locker;
import com.appworkerside.utils.Posicion;
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

import java.util.Objects;

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

    private Locker ordering;
    private Locker resOrdering;
    private boolean locked;

    private FirebaseAuth mAuth;
    private FirebaseDatabase database;
    private DatabaseReference myRef;
    private StorageReference myPics;

    private FloatingActionButton workSwitch;
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

        workSwitch = findViewById(R.id.workSwitch);
        workSwitch.setEnabled(false);
        workSwitch.setBackgroundTintList(ColorStateList.valueOf(Color.BLACK));
        state = false;
        workSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!state) {
                    if (mMap != null) {
                        updateState(true);
                        mMap.setMapStyle(
                                MapStyleOptions.loadRawResourceStyle(
                                        MapsActivity.this, R.raw.map_off));
                        workSwitch.setBackgroundTintList(ColorStateList.valueOf(Color.RED));
                        Toast.makeText(MapsActivity.this, "Apagado!", Toast.LENGTH_LONG).show();
                    }
                } else {
                    if (mMap != null) {
                        updateState(false);
                        mMap.setMapStyle(
                                MapStyleOptions.loadRawResourceStyle(
                                        MapsActivity.this, R.raw.map_on));
                        workSwitch.setBackgroundTintList(ColorStateList.valueOf(Color.BLACK));
                        Toast.makeText(MapsActivity.this, "Encendido!", Toast.LENGTH_LONG).show();
                    }
                }
                state = !state;
            }
        });

        handler = new Handler();
        delay = 1000; //milliseconds
        handler.postDelayed(new Runnable() {
            public void run() {
                //if(locked) moveToProcess();
                handler.postDelayed(this, delay);
            }

        }, delay);

    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                        MapsActivity.this, R.raw.map_on));
        workSwitch.setEnabled(true);
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


    private void lockSaver() {
        ordering = resOrdering;
    }

    private void checkLock() {
        myRef = database.getReference("locked");
        Query query = myRef;
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                locked = false;
                if (dataSnapshot.exists()) {
                    for (DataSnapshot lock : dataSnapshot.getChildren()) {
                        Log.i("Errores", lock.getValue(Locker.class).getWorker().getNombre());
                        if (Objects.requireNonNull(lock.getValue(Locker.class)).toString().split(";")[1].equals(mAuth.getCurrentUser().getEmail().replace("@", "+").replace(".", "-"))) {
                            locked = true;
                            resOrdering = lock.getValue(Locker.class);
                            myRef = database.getReference("workers");
                            myRef.child(mAuth.getCurrentUser().getEmail().replace("@", "+").replace(".", "-")).child("visible").setValue(false);
                            break;
                        }

                    }
                    lockSaver();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void moveToProcess() {
        Intent intent = new Intent(this, OrderingProcess.class);
        startActivity(intent);
    }

}
