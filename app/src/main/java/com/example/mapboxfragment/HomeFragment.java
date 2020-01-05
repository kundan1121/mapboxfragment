package com.example.mapboxfragment;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static androidx.constraintlayout.widget.Constraints.TAG;
import static com.mapbox.mapboxsdk.Mapbox.getApplicationContext;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;

public class HomeFragment extends Fragment implements OnMapReadyCallback, PermissionsListener, MapboxMap.OnMapClickListener {


    private MapView mapView;
    MapboxMap mapboxMap;
    LocationComponent locationcomponent;
    PermissionsManager permissionsManager;
    DirectionsRoute currentRoute;
    private static final String TAG = "DirectionsActivity";
    NavigationMapRoute navigationMapRoute;
    private Button button, btnstart, btnpause, btnresume, btnreset;
    EditText locationTxt;
    Handler handler;
    long MillisecondTime, StartTime, TimeBuff, UpdateTime = 0L ;
    int Seconds, Minutes, MilliSeconds ;
    TextView timer;


    //so far so good

    public HomeFragment(){

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        Mapbox.getInstance(getContext(), "sk.eyJ1Ijoia3VuZGFuMTEyMSIsImEiOiJjazMxb3g3amIwOGJrM21xZnp1ZXA5dGRxIn0.EkCOi-2P6ojg7AAhWxtWsA");
        View view = inflater .inflate(R.layout.fragment_home,container,false);

        mapView = view.findViewById(R.id.mapView);
        button = view.findViewById(R.id.button);
        btnstart = view.findViewById(R.id.start);
        btnpause = view.findViewById(R.id.pause);
        btnresume = view.findViewById(R.id.resume);
        btnreset = view.findViewById(R.id.reset);
        timer = view.findViewById(R.id.time);
        handler = new Handler();
        locationTxt = view.findViewById(R.id.loca);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        btnstart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StartTime = SystemClock.uptimeMillis();
                handler.postDelayed(runnable, 0);
                btnstart.setVisibility(View.INVISIBLE);
                btnpause.setVisibility(View.VISIBLE);

            }
        });

        btnpause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TimeBuff += MillisecondTime;
                handler.removeCallbacks(runnable);
                btnpause.setVisibility(View.INVISIBLE);
                btnresume.setVisibility(View.VISIBLE);
                btnreset.setVisibility(View.VISIBLE);

            }
        });

        btnresume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handler.postDelayed(runnable, 0);
                btnresume.setVisibility(View.INVISIBLE);
                btnreset.setVisibility(View.VISIBLE);
                btnpause.setVisibility(View.VISIBLE);

            }
        });

        btnreset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MillisecondTime = 0L ;
                StartTime = 0L ;
                TimeBuff = 0L ;
                UpdateTime = 0L ;
                Seconds = 0 ;
                Minutes = 0 ;
                MilliSeconds = 0 ;
                timer.setText("00:00:00");

                btnreset.setVisibility(View.INVISIBLE);
                btnstart.setVisibility(View.VISIBLE);
            }
        });

        return view;
    }

    public Runnable runnable = new Runnable() {

        public void run() {

            MillisecondTime = SystemClock.uptimeMillis() - StartTime;

            UpdateTime = TimeBuff + MillisecondTime;

            Seconds = (int) (UpdateTime / 1000);

            Minutes = Seconds / 60;

            Seconds = Seconds % 60;

            MilliSeconds = (int) (UpdateTime % 1000);

            timer.setText("" + Minutes + ":"
                    + String.format("%02d", Seconds) + ":"
                    + String.format("%03d", MilliSeconds));

            handler.postDelayed(this, 0);
        }

    };

    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap) {

        this.mapboxMap = mapboxMap;

        mapboxMap.setStyle(Style.MAPBOX_STREETS, new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                enableLocationComponent(style);
                addDestinationIconLayer(style);
                mapboxMap.addOnMapClickListener(HomeFragment.this);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        boolean simulateRoute = true;
                        NavigationLauncherOptions options = NavigationLauncherOptions.builder()
                                .directionsRoute(currentRoute)
                                .shouldSimulateRoute(simulateRoute)
                                .build();

                        NavigationLauncher.startNavigation(requireActivity(), options);
                        //button.setVisibility(View.INVISIBLE);

                    }
                });

// Map is set up and the style has loaded. Now you can add data or make other map adjustments.


            }
        });
    }

    private void addDestinationIconLayer(Style loadedMapStyle) {
        loadedMapStyle.addImage( "destination-icon-id",
                BitmapFactory.decodeResource(this.getResources(),R.drawable.mapbox_marker_icon_default));

        GeoJsonSource geoJsonSource = new GeoJsonSource("destination-symbol-layer-id");
        loadedMapStyle.addSource(geoJsonSource);

        SymbolLayer destinationSymbolLayer = new SymbolLayer("destination-symbol-layer-id","destination-source-id");
        destinationSymbolLayer.withProperties(iconImage("destination-icon-id"),iconAllowOverlap(true),
                iconIgnorePlacement(true));

        loadedMapStyle.addLayer(destinationSymbolLayer);
    }

    @Override
    public boolean onMapClick(@NonNull LatLng point) {
        Point destinationPoint = Point.fromLngLat(point.getLongitude(),point.getLatitude());
        Point originPoint = Point.fromLngLat(locationcomponent.getLastKnownLocation().getLongitude(),
                locationcomponent.getLastKnownLocation().getLatitude());
        Point destinationPoint1 = Point.fromLngLat(point.getLongitude(),point.getLatitude());

        GeoJsonSource source = mapboxMap.getStyle().getSourceAs("destination-source-id");

        if (source!=null)
        {
            source.setGeoJson(Feature.fromGeometry(destinationPoint));
            source.setGeoJson(Feature.fromGeometry(destinationPoint1));
        }
        getRoute(originPoint,destinationPoint);
        button.setEnabled(true);
        //button.setBackgroundResource(R.color.mapboxBlue);
        //double coo = locationcomponent.getLastKnownLocation().getLongitude();
        String lon = Double.toString(locationcomponent.getLastKnownLocation().getLongitude());
        String lat = Double.toString(locationcomponent.getLastKnownLocation().getLatitude());
        locationTxt.setText("longitude is : "+lon+" latitude is : " +lat);
        return true;
    }

    private void getRoute(Point originPoint, Point destinationPoint) {

        NavigationRoute.builder(requireActivity())
                .accessToken(Mapbox.getAccessToken())
                .origin(originPoint)
                .destination(destinationPoint)
                .build()
                .getRoute(new Callback<DirectionsResponse>() {
                    @Override
                    public void onResponse(@NotNull Call<DirectionsResponse> call, @NotNull Response<DirectionsResponse> response) {
                        Log.d(TAG, "Response code: " + response.code());

                        if (response.body() == null) {
                            Log.e(TAG, "No routes found, make sure you set the right user and access token.");
                            return;
                        } else if (response.body().routes().size() < 1) {
                            Log.e(TAG, "No routes found");
                            return;
                        }

                        currentRoute = response.body().routes().get(0);

                        if (navigationMapRoute != null) {
                            navigationMapRoute.removeRoute();
                        } else {
                            navigationMapRoute = new NavigationMapRoute(null, mapView, mapboxMap, R.style.NavigationMapRoute);
                        }
                        navigationMapRoute.addRoute(currentRoute);
                    }

                    @Override
                    public void onFailure(@NotNull Call<DirectionsResponse> call, Throwable throwable) {
                        Log.e(TAG, "Error: " + throwable.getMessage());


                    }
                });
    }


    private void enableLocationComponent(Style loadedMapStyle) {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(getApplicationContext())) {

// Get an instance of the component
            locationcomponent = mapboxMap.getLocationComponent();

// Activate with options
            locationcomponent.activateLocationComponent(
                    LocationComponentActivationOptions.builder(getApplicationContext(), loadedMapStyle).build());

// Enable to make component visible
            locationcomponent.setLocationComponentEnabled(true);

// Set the component's camera mode
            locationcomponent.setCameraMode(CameraMode.TRACKING);

// Set the component's render mode
            locationcomponent.setRenderMode(RenderMode.COMPASS);
        } else {
            permissionsManager = new PermissionsManager((PermissionsListener) this);
            permissionsManager.requestLocationPermissions(getActivity());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }


    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();

    }


    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(getActivity(),"This app needs location permissions to show its functionality.", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted)
        {
            enableLocationComponent(mapboxMap.getStyle() );
        }
        else
        {
            Toast.makeText(getApplicationContext(),  "user_location_permission_not_granted", Toast.LENGTH_LONG).show();
            getActivity().finish();
        }

    }



}

