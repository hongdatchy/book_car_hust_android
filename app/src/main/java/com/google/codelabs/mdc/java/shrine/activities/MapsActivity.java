package com.google.codelabs.mdc.java.shrine.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import com.google.codelabs.mdc.java.shrine.entities.ApiService;
import com.google.codelabs.mdc.java.shrine.entities.Message;
import com.google.codelabs.mdc.java.shrine.fragments.DistanceFragment;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.maps.android.PolyUtil;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.google.android.gms.maps.GoogleMap.MAP_TYPE_HYBRID;
import static com.google.android.gms.maps.GoogleMap.MAP_TYPE_NORMAL;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private SupportMapFragment mapFragment;
    LocationRequest locationRequest;
    FusedLocationProviderClient fusedLocationProviderClient;
    private final static int REQUEST_CODE = 101;
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
    ActivityResultLauncher activityResultLauncher;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
//        get gg map
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.fragment);
        assert mapFragment != null;
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
                progressDialog = new ProgressDialog(this);
                progressDialog.show();
                progressDialog.setContentView(R.layout.progress_dialog);
                progressDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                drawRoute();
                progressDialog.dismiss();
            }
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
        isDriver = message != null;

//        auto place google api
        Places.initialize(getApplicationContext(), getResources().getString(R.string.google_maps_key));
        originEditText.setFocusable(false);
        originEditText.setOnClickListener(v -> {
            type = "Origin";
            List<Place.Field> fields = Arrays.asList(Place.Field.ADDRESS, Place.Field.LAT_LNG);
            Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                    .build(MapsActivity.this);
            activityResultLauncher.launch(intent);
        });
        destinationEditText.setFocusable(false);
        destinationEditText.setOnClickListener(v -> {
            type = "Destination";
            List<Place.Field> fields = Arrays.asList(Place.Field.ADDRESS, Place.Field.LAT_LNG);
            Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                    .build(MapsActivity.this);
            activityResultLauncher.launch(intent);
        });
        activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Intent data = result.getData();
                    //            delete distance fragment
                    if(fragmentDistance != null){
                        fm.beginTransaction()
                                .remove(fragmentDistance)
                                .commit();
                    }
                    try {
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
                            moveCamera(new LatLng(lat1, long1));
                        }else{
                            destinationEditText.setText(place.getAddress());
                            String sDestination = String.valueOf(place.getLatLng());
                            sDestination = sDestination.replaceAll("lat/lng: ", "");
                            sDestination = sDestination.replace("(", "");
                            sDestination = sDestination.replace(")", "");
                            String[] split = sDestination.split(",");
                            lat2 = Double.parseDouble(split[0]);
                            long2 = Double.parseDouble(split[1]);
                            moveCamera(new LatLng(lat2, long2));
                        }
                        mMap.clear();
                        addMaker(new LatLng(lat1, long1), "Origin");
                        addMaker(new LatLng(lat2, long2),"Destination");
                    }catch (Exception ignored){
//                        nếu exception thi khong phai loi, chi la do nhan vao cho trong luc tim kiem
                    }
                }
        );
    }

    @Override
    public void onMapReady(@NotNull GoogleMap googleMap) {
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
                        mapFragment.getMapAsync(googleMap -> {
                            gotoMyLocation(location);
                        });
                    }
                }else{
                    LocationRequest locationRequest = LocationRequest.create().setInterval(10000)
                            .setFastestInterval(1000)
                            .setNumUpdates(1)
                            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);;
                    LocationCallback locationCallback = new LocationCallback(){
                        @Override
                        public void onLocationResult(LocationResult locationResult) {
                            Location location =  locationResult.getLastLocation();
                            if(!isDriver){
                                gotoMyLocation(location);
                            }
                        }
                    };
                    fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
                }
            });
        }else{
            turnOnGPS();
        }
    }

    public void gotoMyLocation(Location location){
        mMap.clear();
        lat1 = location.getLatitude();
        long1 = location.getLongitude();
        addMaker(new LatLng(lat1, long1), "Origin");
        if(lat2 != 0 && long2 != 0){
            addMaker(new LatLng(lat2, long2), "Destination");
        }
        moveCamera(new LatLng(lat1, long1));
        originEditText.setText("Your current Location");
    }

    public void turnOnGPS(){
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(1000);
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

    public void addMaker(LatLng latLng, String type){
        Geocoder geocoder;
        String address;
        MarkerOptions markerOptions ;
        geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> mapsActivities = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if(mapsActivities.size() != 0){
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

    void moveCamera(LatLng latLng){
        mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
    }

    public void drawRoute(){
        ApiService.apiService.getDirection(lat1 +","+long1,
                lat2 + "," + long2,
                "driving",
                getResources().getString(R.string.google_maps_key))
                .enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                List<LatLng> points = new ArrayList<>();
                JSONArray jRoutes;
                JSONArray jLegs;
                JSONArray jSteps;
                String distance ="";
                try {
                    JsonObject jsonObject = JsonParser.parseString(new Gson().toJson(response.body())).getAsJsonObject();
                    jRoutes = new JSONObject(jsonObject.toString()).getJSONArray("routes");
                    for (int i = 0; i < jRoutes.length(); i++) {
                        jLegs = ((JSONObject) jRoutes.get(i)).getJSONArray("legs");
                        for (int j = 0; j < jLegs.length(); j++) {
                            jSteps = ((JSONObject) jLegs.get(j)).getJSONArray("steps");
                            distance = ((JSONObject) jLegs.get(j)).getJSONObject("distance").getString("text");
                            for (int k = 0; k < jSteps.length(); k++) {
                                String polyline = "";
                                polyline = (String) ((JSONObject) ((JSONObject) jSteps.get(k)).get("polyline")).get("points");
                                List<LatLng> list = PolyUtil.decode(polyline);
                                for (int l = 0; l < list.size(); l++) {
                                    points.add(new LatLng(list.get(l).latitude, list.get(l).longitude));
                                }
                            }
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (points.size() > 0) {
                    PolylineOptions opts = new PolylineOptions().addAll(points).color(Color.BLUE).width(10);
                    mMap.addPolyline(opts);
                    getDistance(distance);

                }
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {

            }
        });
    }

    private void getDistance(String d) {
        double distance = Double.parseDouble(d.split(" ")[0].replaceAll(",","."));
        if(d.split(" ")[1].equals("mi")){
            distance *= 1.609344;
        }
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
        addMaker(new LatLng(lat1, long1), "Origin");
        addMaker(new LatLng(lat2, long2), "Destination");
        moveCamera(new LatLng(lat2, long2));
        drawRoute();
        destinationEditText.setFocusable(false);
        originEditText.setFocusable(false);
        destinationEditText.setVisibility(View.GONE);
        originEditText.setVisibility(View.GONE);
        getDirection.setText("Hust Family");
        getDirection.setEnabled(false);
    }
}