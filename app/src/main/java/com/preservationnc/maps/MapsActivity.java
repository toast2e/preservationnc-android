package com.preservationnc.maps;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.preservationnc.R;
import com.preservationnc.representations.Location;
import com.preservationnc.representations.Property;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    private LocationManager mLocationManager;

    private List<Marker> propertyMarkers = new ArrayList<>();

    private static String CHANNEL_ID = "PreservationNC";

    private NotificationCompat.Builder mNotifcationBuilder;

    private NotificationManagerCompat mNotificationManager;


    private final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull android.location.Location location) {
            LatLng current = new LatLng(location.getLatitude(), location.getLongitude());
            for(Marker m : propertyMarkers) {
                float[] results = new float[3];
                android.location.Location.distanceBetween(location.getLatitude(), location.getLongitude(), m.getPosition().latitude, m.getPosition().longitude, results);
                //Log.i("distanceToProperty", "Distance to property (" + m.getTitle() + ") is " + results[0] + " m");
                if (results[0] < 20*1000) {
                    Log.i("YOUCLOSEBRO", "YOU ARE WITHIN 20 KM OF A PRESERVATION NC HOUSE: " + m.getTitle());
                    Notification n = mNotifcationBuilder
                            .setSmallIcon(R.drawable.notification_icon)
                            .setContentTitle("Near a PreservationNC property")
                            .setContentText("YOU ARE WITHIN 20 KM OF A PRESERVATION NC HOUSE: " + m.getTitle())
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .build();
                    mNotificationManager.notify((int)results[0], n);
                }
            }
        }
    };

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //CharSequence name = getString(R.string.channel_name);
            //String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "PreservationNC", importance);
            channel.setDescription("Notifications about PreservationNC properties");
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            //NotificationManager notificationManager = getSystemService(NotificationManager.class);
            mNotificationManager.createNotificationChannel(channel);
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        mNotifcationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID);
        mNotificationManager = NotificationManagerCompat.from(this);
        createNotificationChannel();
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        setupRequestQueue();

        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 6);
        } else {
            mMap.setMyLocationEnabled(true);
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10*1000,
                    1000, mLocationListener);
        }
    }

    private void setupRequestQueue() {
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);
        String baseUrl ="http://10.0.2.2:8080"; // temporarily communicate to local machine
        String propertiesUrl = baseUrl + "/preservationnc/properties";

        // Request a json response from the provided URL.
        JsonArrayRequest jsonRequest = new JsonArrayRequest(Request.Method.GET, propertiesUrl, null,
            new Response.Listener<JSONArray>() {
                @Override
                public void onResponse(JSONArray response) {
                    Log.d("network", response.toString());
                    List<Property> properties = propertiesFromJson(response);
                    if(properties.size() > 0) {
                        mMap.clear();
                        for(Property p : properties) {
                            LatLng loc = null;
                            if (p.getLocation().getLatitude() != null && p.getLocation().getLongitude() != null) {
                                loc = new LatLng(p.getLocation().getLatitude(), p.getLocation().getLongitude());
                            }

                            if (loc != null) {
                                propertyMarkers.add(mMap.addMarker(new MarkerOptions().position(loc).title(p.getName())));
                            }
                        }
                        LatLngBounds.Builder builder = new LatLngBounds.Builder();
                        for (Marker marker : propertyMarkers) {
                            builder.include(marker.getPosition());
                        }
                        LatLngBounds bounds = builder.build();
                        int padding = 200; // offset from edges of the map in pixels
                        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);

                        mMap.animateCamera(cu);
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e("network error", error.getMessage() == null ? error.toString() : error.getMessage());
                    Log.e("network error", Log.getStackTraceString(error));
                }
        });

        // Add the request to the RequestQueue.
        queue.add(jsonRequest);
    }

    private List<Property> propertiesFromJson(JSONArray json) {
        try {
            List<Property> ret = new ArrayList<>();
            for (int i = 0; i < json.length(); i++) {
                JSONObject obj = json.getJSONObject(i);
                Location l = new Location();
                if (obj.getJSONObject("location").has("latitude")) {
                    l.setLatitude(obj.getJSONObject("location").getDouble("latitude"));
                }
                if (obj.getJSONObject("location").has("longitude")) {
                    l.setLongitude(obj.getJSONObject("location").getDouble("longitude"));
                }
                if (obj.getJSONObject("location").has("address")) {
                    l.setAddress(obj.getJSONObject("location").getString("address"));
                }
                if (obj.getJSONObject("location").has("city")) {
                    l.setCity(obj.getJSONObject("location").getString("city"));
                }
                if (obj.getJSONObject("location").has("county")) {
                    l.setCounty(obj.getJSONObject("location").getString("county"));
                }
                if (obj.getJSONObject("location").has("state")) {
                    l.setState(obj.getJSONObject("location").getString("state"));
                }
                if (obj.getJSONObject("location").has("zip")) {
                    l.setZip(obj.getJSONObject("location").getString("zip"));
                }

                Property p = new Property(obj.getString("name"), l);
                if (obj.has("description")) {
                    p.setDescription(obj.getString("description"));
                }
                if (obj.has("price")) {
                    p.setPrice(obj.getLong("price"));
                }
                ret.add(p);
            }
            return ret;
        } catch (JSONException e) {
            Log.e("json error", e.getMessage());
        }
        return Collections.emptyList();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults)
    {
        if (requestCode == 6) {
            if (permissions.length > 0) {
                if (permissions[0] == Manifest.permission.ACCESS_FINE_LOCATION && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                    mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 60*1000,
                            1000, mLocationListener);
                }
            }
            else {
                return;
            }
        }
    }
}