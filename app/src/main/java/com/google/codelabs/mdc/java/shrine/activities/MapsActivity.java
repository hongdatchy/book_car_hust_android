package com.google.codelabs.mdc.java.shrine.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.codelabs.mdc.java.shrine.R;
import com.google.codelabs.mdc.java.shrine.entities.Message;
import com.google.codelabs.mdc.java.shrine.fragments.DistanceFragment;
import com.google.gson.Gson;
import com.google.maps.DirectionsApi;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.model.DirectionsLeg;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.DirectionsStep;
import com.google.maps.model.EncodedPolyline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static com.google.android.gms.maps.GoogleMap.MAP_TYPE_HYBRID;
import static com.google.android.gms.maps.GoogleMap.MAP_TYPE_NORMAL;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private SupportMapFragment mapFragment;
    Location currentLocation;
    LocationRequest locationRequest;
    FusedLocationProviderClient fusedLocationProviderClient;
    private final static int REQUEST_CODE = 101;
    private final static int GET_DISTANCE = 100;
    public static final int RequestCheck =102;
    Button getDirection;
    ImageButton currentLocationBtn;
    EditText originEditText, destinationEditText;
    DistanceFragment fragmentDistance;
    double lat1 = 0, long1 = 0, lat2 = 0, long2 = 0;
    String type;
    FragmentManager fm;
    String origin, destination;
    String message;
    boolean isDriver;
    CheckBox checkBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
//        get gg map
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.fragment);
        mapFragment.getMapAsync(this);

        getDirection = findViewById(R.id.button);
        originEditText = findViewById(R.id.origin_edit_text);
        destinationEditText = findViewById(R.id.destination_edit_text);
        fm = getSupportFragmentManager();
        fragmentDistance = (DistanceFragment) fm.findFragmentById(R.id.container);
        currentLocationBtn = findViewById(R.id.current_location_btn);
        checkBox = findViewById(R.id.my_checkbox);

        getDirection.setOnClickListener(v -> {
            if(lat1 != 0 && long1 != 0 && lat2 !=0 && long2 != 0){
                drawRoute();
            }
        });

        Places.initialize(getApplicationContext(), getResources().getString(R.string.google_maps_key));
        originEditText.setFocusable(false);
        originEditText.setOnClickListener(v -> {
            type = "Origin";
            List<Place.Field> fields = Arrays.asList(Place.Field.ADDRESS, Place.Field.LAT_LNG);
            Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                    .build(MapsActivity.this);
            startActivityForResult(intent, GET_DISTANCE);
        });
        destinationEditText.setFocusable(false);
        destinationEditText.setOnClickListener(v -> {
            type = "Destination";
            List<Place.Field> fields = Arrays.asList(Place.Field.ADDRESS, Place.Field.LAT_LNG);
            Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                    .build(MapsActivity.this);
            startActivityForResult(intent, GET_DISTANCE);
        });

        currentLocationBtn.setOnClickListener(v -> {
            if (ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
            }
            else {
                ActivityCompat.requestPermissions(MapsActivity.this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_CODE);
            }
        });

        checkBox.setOnClickListener(v -> {
            if (checkBox.isChecked()) {
                mMap.setMapType(MAP_TYPE_HYBRID);
            } else {
                mMap.setMapType(MAP_TYPE_NORMAL);
            }
        });

        // Get the Intent that started this activity and extract the string
        message = getIntent().getStringExtra("driver apply contract");
        if(message != null){
            isDriver = true;
        }else {
            isDriver = false;
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        if(isDriver){
            LatLng desContract = new LatLng(lat2,long2);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(desContract, 10));
            driverApplyContract(message);
        }else{
            LatLng HaNoi = new LatLng(21.069450,105.810852);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(HaNoi, 10));
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode == GET_DISTANCE && resultCode == RESULT_OK){

//            delete distance fragment
            if(fragmentDistance != null){
                fm.beginTransaction()
                        .remove(fragmentDistance)
                        .commit();
            }

            Place place = Autocomplete.getPlaceFromIntent(data);
            if(type.equals("Origin")){
                originEditText.setText(place.getAddress());
                String sOrigin = String.valueOf(place.getLatLng());
                sOrigin = sOrigin.replaceAll("lat/lng: ", "");
                sOrigin = sOrigin.replace("(", "");
                sOrigin = sOrigin.replace(")", "");
                String[] split = sOrigin.split(",");
                lat1 = Double.parseDouble(split[0]);
                long1 = Double.parseDouble(split[1]);
            }else{
                destinationEditText.setText(place.getAddress());
                String sDestination = String.valueOf(place.getLatLng());
                sDestination = sDestination.replaceAll("lat/lng: ", "");
                sDestination = sDestination.replace("(", "");
                sDestination = sDestination.replace(")", "");
                String[] split = sDestination.split(",");
                lat2 = Double.parseDouble(split[0]);
                long2 = Double.parseDouble(split[1]);
            }
            mMap.clear();
            animateCameraAndMaker(new LatLng(lat1, long1), "Origin");
            animateCameraAndMaker(new LatLng(lat2, long2),"Destination");
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @SuppressLint("MissingPermission")
    private void getCurrentLocation() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MapsActivity.this);
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
            fusedLocationProviderClient.getLastLocation().addOnCompleteListener(task -> {
                Location location = task.getResult();
                if(location != null){
                    if(!isDriver){
                        currentLocation = location;
                        mapFragment.getMapAsync(googleMap -> {
                            mMap.clear();
                            lat1 = location.getLatitude();
                            long1 = location.getLongitude();
                            animateCameraAndMaker(new LatLng(lat1, long1), "Origin");
                            animateCameraAndMaker(new LatLng(lat2, long2), "Destination");
                            originEditText.setText("Your current Location");
                        });
                    }
                }else{
                    LocationRequest locationRequest = new LocationRequest().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                            .setInterval(10000)
                            .setFastestInterval(1000)
                            .setNumUpdates(1);
                    LocationCallback locationCallback = new LocationCallback(){
                        @Override
                        public void onLocationResult(LocationResult locationResult) {
                            Location location1 =  locationResult.getLastLocation();
                            if(!isDriver){
                                mMap.clear();
                                lat1 = location1.getLatitude();
                                long1 = location1.getLongitude();
                                animateCameraAndMaker(new LatLng(lat1, long1), "Origin");
                                animateCameraAndMaker(new LatLng(lat2, long2), "Destination");
                                originEditText.setText("Your current Location");
                            }
                        }
                    };
                    fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
                }

            });
        }else{
            locationRequest = LocationRequest.create();
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            locationRequest.setInterval(5000);
            locationRequest.setFastestInterval(2000);
            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
            builder.setAlwaysShow(true);
            Task<LocationSettingsResponse> result = LocationServices.getSettingsClient(getApplicationContext())
                    .checkLocationSettings(builder.build());
            result.addOnCompleteListener(task -> {
                try {
                    task.getResult(ApiException.class);
                } catch (ApiException e) {
                    switch (e.getStatusCode()){
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            try {
                                ResolvableApiException resolvableApiException = (ResolvableApiException) e;
                                resolvableApiException.startResolutionForResult(MapsActivity.this, RequestCheck);
                            } catch (IntentSender.SendIntentException sendIntentException) {
                                sendIntentException.printStackTrace();
                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            break;
                    }
                }
            });
        }
    }

    public void animateCameraAndMaker(LatLng latLng, String type){
        Geocoder geocoder;
        String address;
        MarkerOptions markerOptions ;
        geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> mapsActivities = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if(mapsActivities.size() != 0){
                mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
                address = mapsActivities.get(0).getAddressLine(0);

                markerOptions = new MarkerOptions();
                markerOptions.position(latLng);
                markerOptions.title(type + ": " + address);
                if(type.equals("Origin")){
                    origin = address;
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
                }else {
                    destination = address;
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                }
                mMap.addMarker(markerOptions).showInfoWindow();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void drawRoute(){
        //Define list to get all latLng for the route
        List<LatLng> path = new ArrayList();
        //Execute Directions API request
        GeoApiContext context = new GeoApiContext.Builder()
                .apiKey(getResources().getString(R.string.google_maps_key))
                .build();
        DirectionsApiRequest req = DirectionsApi.getDirections(context, lat1 + "," + long1, lat2 + "," + long2);
        try {
            DirectionsResult res = req.await();
            //Loop through legs and steps to get encoded polyline of each step
            if (res.routes != null && res.routes.length > 0) {
                DirectionsRoute route = res.routes[0];
                if (route.legs !=null) {
                    for(int i=0; i<route.legs.length; i++) {
                        DirectionsLeg leg = route.legs[i];
                        if (leg.steps != null) {
                            for (int j=0; j<leg.steps.length;j++){
                                DirectionsStep step = leg.steps[j];
                                if (step.steps != null && step.steps.length >0) {
                                    for (int k=0; k<step.steps.length;k++){
                                        DirectionsStep step1 = step.steps[k];
                                        EncodedPolyline points1 = step1.polyline;
                                        if (points1 != null) {
                                            //Decode polyline and add points to list of route coordinates
                                            List<com.google.maps.model.LatLng> coords1 = points1.decodePath();
                                            for (com.google.maps.model.LatLng coord1 : coords1) {
                                                path.add(new LatLng(coord1.lat, coord1.lng));
                                            }
                                        }
                                    }
                                } else {
                                    EncodedPolyline points = step.polyline;
                                    if (points != null) {
                                        //Decode polyline and add points to list of route coordinates
                                        List<com.google.maps.model.LatLng> coords = points.decodePath();
                                        for (com.google.maps.model.LatLng coord : coords) {
                                            path.add(new LatLng(coord.lat, coord.lng));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        //Draw the polyline
        if (path.size() > 0) {
            PolylineOptions opts = new PolylineOptions().addAll(path).color(Color.BLUE).width(5);
             mMap.addPolyline(opts);
            // get distance
            getDistance(path);
        }
    }

    private void getDistance(List<LatLng> path) {
        double distance = 0.0;
        Location location1 = new Location("");
        Location location2 = new Location("");
        for(int i = 0; i < path.size()-1; i++){
            location1.setLatitude(path.get(i).latitude);
            location1.setLongitude(path.get(i).longitude);
            location2.setLatitude(path.get(i+1).latitude);
            location2.setLongitude(path.get(i+1).longitude);
            distance += location1.distanceTo(location2);
        }
        distance /= 1000;// m -> km
        fragmentDistance = new DistanceFragment();
        Bundle bundle = new Bundle();
        bundle.putString("distance", String.valueOf(distance));
        bundle.putString("lat1", String.valueOf(lat1));
        bundle.putString("lat2", String.valueOf(lat2));
        bundle.putString("long1", String.valueOf(long1));
        bundle.putString("long2", String.valueOf(long2));
        bundle.putString("origin", origin);
        bundle.putString("destination", destination);
        if(isDriver){
            String phone = new Gson().fromJson(message, Message.class).getPhone();
            bundle.putString("phone", phone);
        }
        fragmentDistance.setArguments(bundle);
        fm.beginTransaction()
                .replace(R.id.container, fragmentDistance)
                .commit();
    }

    private void driverApplyContract(String mes) {
        Gson gson = new Gson();
        Message message = gson.fromJson(mes, Message.class);
        lat1 = message.getLat1();
        lat2 = message.getLat2();
        long1 = message.getLong1();
        long2 = message.getLong2();
        animateCameraAndMaker(new LatLng(lat1, long1), "Origin");
        animateCameraAndMaker(new LatLng(lat2, long2), "Destination");
        drawRoute();
        destinationEditText.setFocusable(false);
        originEditText.setFocusable(false);
        destinationEditText.setVisibility(View.GONE);
        originEditText.setVisibility(View.GONE);
        getDirection.setText("Hust Family");
        getDirection.setEnabled(false);
    }
}