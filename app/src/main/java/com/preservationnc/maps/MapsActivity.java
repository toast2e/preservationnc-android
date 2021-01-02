package com.preservationnc.maps;

import androidx.fragment.app.FragmentActivity;

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
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.preservationnc.R;
import com.preservationnc.representations.Location;
import com.preservationnc.representations.Property;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
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

        // Add a marker in raleigh and move the camera
        LatLng raleigh = new LatLng(35.843685, -78.78514);
        mMap.addMarker(new MarkerOptions().position(raleigh).title("Raleigh, NC"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(raleigh));
    }

    private void setupRequestQueue() {
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);
        String baseUrl ="http://10.0.2.2:8080"; // temporarily communicate to local machine
        String propertiesUrl = baseUrl + "/preservationnc/properties";

        // Request a string response from the provided URL.
        JsonArrayRequest jsonRequest = new JsonArrayRequest(Request.Method.GET, propertiesUrl, null,
            new Response.Listener<JSONArray>() {
                @Override
                public void onResponse(JSONArray response) {
                    Log.d("network", response.toString());
                    List<Property> properties = propertyNamesFromJson(response);
                    if(properties.size() > 0) {
                        mMap.clear();
                        for(Property p : properties) {
                            LatLng loc = new LatLng(p.getLocation().getLatitude(), p.getLocation().getLongitude());
                            mMap.addMarker(new MarkerOptions().position(loc).title(p.getName()));
                            mMap.moveCamera(CameraUpdateFactory.newLatLng(loc));
                        }
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

    private List<Property> propertyNamesFromJson(JSONArray json) {
        try {
            List<Property> ret = new ArrayList<>();
            for (int i = 0; i < json.length(); i++) {
                JSONObject obj = json.getJSONObject(i);
                Location l = new Location(obj.getJSONObject("location").getDouble("latitude"), obj.getJSONObject("location").getDouble("longitude"));
                Property p = new Property(obj.getString("name"), l);
                ret.add(p);
            }
            return ret;
        } catch (JSONException e) {
            Log.e("json error", e.getMessage());
        }
        return Collections.emptyList();
    }
}