package com.valir.location;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.cardiomood.android.controls.gauge.SpeedometerGauge;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

//import com.google.android.gms.location.LocationRequest;

public class HomeActivity extends AppCompatActivity implements
        LocationListener, ActivityCompat.OnRequestPermissionsResultCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        OnMapReadyCallback {

    private static final long INTERVAL = 1000 * 60 * 1; //1 minute(Changed 20 secs )
    private static final long FASTEST_INTERVAL = 1000 * 10 * 1; // 1/6 minute
    private static final float SMALLEST_DISPLACEMENT = 100F;//0.25F; //quarter of a meter

    private final String TAG = HomeActivity.class.getSimpleName();
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private final int REQUEST_LOCATION = 2;
    private Location mCurrentLocation;
    private String mLastUpdateTime;
    private TextView mLocAddrTxt, mLocSpeedTxt,mLocDistance;
    private SpeedometerGauge speedometer;
    //private static int counter = 10;
    private GoogleMap mMap;
    private ArrayList<LatLng> points;
    private Polyline line;
    private LatLng mLatLng;
    private double mDistance = 0.0f;
    private SupportMapFragment mapFragment;
    private String mStartTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        /*Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);*/

        mapFragment = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.tick_map));

        points = new ArrayList<LatLng>();
        mLocAddrTxt = (TextView) findViewById(R.id.loc_current);
        mLocSpeedTxt = (TextView) findViewById(R.id.loc_speed);
        mLocDistance = (TextView) findViewById(R.id.loc_distance);
        // Create an instance of GoogleAPIClient.

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps();
            //enableGps();
        }
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(HomeActivity.this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }


        // Customize SpeedometerGauge
        speedometer = (SpeedometerGauge) findViewById(R.id.speedometer);

        // Add label converter
        speedometer.setLabelConverter(new SpeedometerGauge.LabelConverter() {
            @Override
            public String getLabelFor(double progress, double maxProgress) {
                return String.valueOf((int) Math.round(progress));
            }
        });

        // configure value range and ticks
        speedometer.setMaxSpeed(200);
        speedometer.setMajorTickStep(25);
        speedometer.setMinorTicks(5);
       /*speedometer.setForeground(getResources().getDrawable(android.R.drawable.dialog_holo_light_frame));*/
        //speedometer.setLayerPaint();
        /*if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            speedometer.setPointerIcon(PointerIcon.TYPE_CELL);*/
        speedometer.showContextMenu();
        // /speedometer.getDisplay();
//        speedometer.setBackground(getResources().getDrawable(android.R.drawable.dialog_holo_light_frame));
       // speedometer.la

        speedometer.setLabelTextSize(24);

        speedometer.setBackgroundColor(getResources().getColor(R.color.cast_intro_overlay_background_color));
        // Configure value range colors
/*
        speedometer.addColoredRange(30, 140, Color.GREEN);
        speedometer.addColoredRange(140, 180, Color.YELLOW);
        speedometer.addColoredRange(180, 300, Color.RED);
*/

        speedometer.addColoredRange(20, 60, Color.GREEN);
        speedometer.addColoredRange(60, 100, Color.YELLOW);
        speedometer.addColoredRange(100, 200, Color.RED);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Log.i(TAG, "KMS Marshmallow Version ");
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
            } else {
                Log.i(TAG, "KMS Permission Granted");
                //lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_UPDATE_FREQUENCY, 0, MapActivity.this);
                createLocationRequest();
            }
        } else {
            Log.i(TAG, "KMS other than marshmallow  ");
            //lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_UPDATE_FREQUENCY, 0, MapActivity.this);
            createLocationRequest();
        }

        /*FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/
    }

    public void enableGps() {
        Intent intent = new Intent("android.location.GPS_ENABLED_CHANGE");
        intent.putExtra("enabled", true);
        sendBroadcast(intent);
    }

    public void disableGps() {
        Intent intent = new Intent("android.location.GPS_ENABLED_CHANGE");
        intent.putExtra("enabled", false);
        sendBroadcast(intent);
    }

    protected void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
            //LocationServices.FusedLocationApi.requestLocationUpdates( mGoogleApiClient, mLocationRequest, this);
        } else {
            //Log.i(TAG, "Permission Granted");
            //lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_UPDATE_FREQUENCY, 0, MapActivity.this);
            LocationServices.FusedLocationApi.requestLocationUpdates( mGoogleApiClient, mLocationRequest, this);

        }

    }

   /* @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }*/

    protected void createLocationRequest() {
        mapFragment.getMapAsync(this);

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setSmallestDisplacement(SMALLEST_DISPLACEMENT);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        //if (mRequestingLocationUpdates) {
        mStartTime = DateFormat.getTimeInstance().format(new Date());
        startLocationUpdates();
        //}
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "KMS onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(TAG, "KMS onConnectionFailed");
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mGoogleApiClient.disconnect();
        disableGps();
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i(TAG, "KMS onLocationChanged");
        mLatLng = new LatLng(location.getLatitude(), location.getLongitude()); //you already have this

        points.add(mLatLng); //added

        mCurrentLocation = location;
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());

        updateUI(getAddressFromLatLang(location.getLatitude(), location.getLongitude()));
        redrawLine();
    }

    private void updateUI(String address) {
        float convetTokmhr = 3.6f;
        if (address != null && !address.contains("null"))
            mLocAddrTxt.setText(address);
        else
            mLocAddrTxt.setText("Fetching address...");
        int speed =  Math.round(mCurrentLocation.getSpeed() * convetTokmhr);
        mLocSpeedTxt.setText(/*mLastUpdateTime + */" Speed :" +speed + " Km/Hr");
        Log.i(TAG,"KMS speed :"+speed);
        if(speed > 0)
            speedometer.setSpeed(speed);
    }

    /*protected void getSpeed(){
            // Acquire a reference to the system Location Manager
            LocationManager locationManager = (LocationManager) this
                    .getSystemService(Context.LOCATION_SERVICE);

            // Define a listener that responds to location updates
            LocationListener locationListener = new LocationListener() {
                public void onLocationChanged(Location location) {
                    location.getLatitude();
                    Toast.makeText(HomeActivity.this, "Current speed:" + location.getSpeed(),
                            Toast.LENGTH_SHORT).show();

                }

                public void onStatusChanged(String provider, int status,
                                            Bundle extras) {
                }

                public void onProviderEnabled(String provider) {
                }

                public void onProviderDisabled(String provider) {
                }
            };
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0,
                    0, locationListener);

        }*/
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_UPDATE_FREQUENCY, 0, MapActivity.this);
                    //createLocationRequest();
                    startLocationUpdates();

                } else {
                    finish();
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    /*check whether the current location settings are satisfied:*/

    public String getAddressFromLatLang(double latitude, double longitude) {
        Geocoder geocoder;
        List<Address> addresses;
        String address = "Fetching Adress...";
        String city = "";
        String knownName = "";

        try {
            geocoder = new Geocoder(this, Locale.getDefault());
            addresses = geocoder.getFromLocation(latitude, longitude, 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
            if(addresses.size() >0) {
                address = addresses.get(0).getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
                city = addresses.get(0).getLocality();
            /*String state = addresses.get(0).getAdminArea();
            String country = addresses.get(0).getCountryName();
            String postalCode = addresses.get(0).getPostalCode();*/
                knownName = addresses.get(0).getFeatureName(); // Only if available else return NULL
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return address + "," + city + "," + knownName;

    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                        finish();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();

    }

    public int getZoomLevel(Circle circle) {
        int zoomLevel =0;
        if (circle != null){
            double radius = circle.getRadius();
            double scale = radius / 500;
            zoomLevel =(int) (16 - Math.log(scale) / Math.log(2));
        }
        return zoomLevel;
    }

    private void redrawLine() {

       /*Circle circle = mMap.addCircle(new CircleOptions().center(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()))
                .radius(getRadiusInMeters()).strokeColor(Color.RED));
        circle.setVisible(true);
        getZoomLevel(circle);*/

        mMap.clear();  //clears all Markers and Polylines
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(mLatLng)      // Sets the center of the map to location user
                .zoom(20)                   // Sets the zoom
                //.bearing(90)                // Sets the orientation of the camera to east
                //.tilt(40)                   // Sets the tilt of the camera to 30 degrees
                .build();                   // Creates a CameraPosition from the builder
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        PolylineOptions options = new PolylineOptions().width(5).color(Color.GREEN).geodesic(false);
        options.addAll(points);
       /* for (int i = 0; i < points.size(); i++) {
            LatLng point = points.get(i);
            options.add(point);
        }*/
        Log.i(TAG,"KMS Points Size :"+points.size());
        //addMarker(); //add Marker in current position
        line = mMap.addPolyline(options); //add Polyline
        LatLng latlngStart = null;
        if(options.getPoints().size() < 2)
            latlngStart =  options.getPoints().get(0);
        else
            latlngStart =  options.getPoints().get(options.getPoints().size()-2);
        Location startLocation  = new Location("");
        startLocation.setLatitude(latlngStart.latitude);
        startLocation.setLongitude(latlngStart.longitude);
        mDistance  = mDistance + (startLocation.distanceTo(mCurrentLocation)/1000);
        DecimalFormat df = new DecimalFormat("#.##");
        Log.i(TAG,"KMS Distance :"+mDistance);
        mLocDistance.setText(""+ Double.valueOf(df.format(mDistance))+" Kms");

        mMap.addMarker(new MarkerOptions().position(options.getPoints().get(0)).title("Start"));
        mMap.addMarker(new MarkerOptions().position(mLatLng).title("Me"));
    }

    @Override
    @SuppressWarnings("MissingPermission")
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setMyLocationEnabled(true);
        mMap.setTrafficEnabled(false);
        mMap.setIndoorEnabled(true);
        mMap.setBuildingsEnabled(true);
       // mMap.getUiSettings().setZoomControlsEnabled(true);
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(mLatLng));
        /*// Add a marker in Sydney, Australia, and move the camera.
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        ;*/
    }
}
