package com.shailendra.poicollection;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Point;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.AppCompatImageView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.shailendra.dataBase.DatabaseHelper;
import com.shailendra.model.MPois;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener, View.OnClickListener, GoogleMap.OnMarkerClickListener {

    private GoogleMap mMap;
    private Permission permission;
    private LocationRequestSetting locationRequestSetting;
    private EditText edtPoiName;
    private Spinner spnPoiType;
    private EditText edtOwnerName;
    private TextView edtEstablishedDate;
    private Button btnSave;
    private BottomSheetBehavior dInfoBottomSheetBehavior;
    private Location mCurrLocation;
    private LatLngBounds.Builder builder;
    private AppCompatImageButton imgShowAllPoies;
    private AppCompatImageButton imgShowTraffic;
    private AppCompatImageButton imgSync;
    private AppCompatImageButton imgCurLocation;
    private ArrayList<Marker> allMarkersList;
    private List<MPois> mPoisList;
    private Location curMarkerLocation;
    private View progressBar;
    private Location mLastLocation;
    private Marker mCurrentLocationMarker;
    private MarkerOptions markerOptions;
    /**
     * Callback for Location events.
     */
    private LocationCallback mLocationCallback;
    private View bottomSheet;
    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 15000;

    /**
     * The fastest rate for active location updates. Exact. Updates will never be more frequent
     * than this value.
     */
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    private DatabaseHelper db;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps2);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        imgShowAllPoies = findViewById(R.id.img_all_poi);
        imgShowAllPoies.setOnClickListener(this);

        imgShowTraffic = findViewById(R.id.img_traffic);
        imgShowTraffic.setOnClickListener(this);

        imgSync = findViewById(R.id.img_sync);
        imgSync.setOnClickListener(this);

        imgCurLocation = findViewById(R.id.img_cur_loc);
        imgCurLocation.setOnClickListener(this);

        bottomSheet = findViewById(R.id.bottom_sheet);
        dInfoBottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        dInfoBottomSheetBehavior.setHideable(true);
        dInfoBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        edtPoiName = findViewById(R.id.edt_name);
        spnPoiType = findViewById(R.id.spinner);
        edtOwnerName = findViewById(R.id.edt_owner);
        edtEstablishedDate = findViewById(R.id.edt_date);
        edtEstablishedDate.setOnClickListener(this);
        btnSave = findViewById(R.id.btn_submit);
        btnSave.setOnClickListener(this);

//        for permission
        permission = new Permission(this);
        permission.checkLocationPermission();

//        enable to location of user
        locationRequestSetting = new LocationRequestSetting(MapsActivity.this,
                UPDATE_INTERVAL_IN_MILLISECONDS,FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        db = new DatabaseHelper(this);

        builder = new LatLngBounds.Builder();

        progressBar = findViewById(R.id.progressBar);
    }

    @Override
    protected void onResume() {
        super.onResume();
        locationRequestSetting.startLocationUpdates();

//      for location callback
        locationRequestSetting.mLocationCallback = new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                mCurrLocation = locationResult.getLastLocation();


                float bearing = 0;
                if (mLastLocation != null && mCurrLocation != null) {
                    bearing = mLastLocation.bearingTo(mCurrLocation);
                }
                mLastLocation = mCurrLocation;
                LatLng curLatLong = new LatLng(mCurrLocation.getLatitude(), mCurrLocation.getLongitude());
                if (mCurrentLocationMarker != null && mCurrentLocationMarker.isVisible()) {
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(curLatLong));
                    animateMarker(mCurrentLocationMarker, curLatLong, false, bearing);

                } else {
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(curLatLong));
                    markerOptions = new MarkerOptions();
                    markerOptions.position(curLatLong);
                    markerOptions.anchor(0.5f, 0.5f);
                    markerOptions.rotation(bearing);
                    markerOptions.title("Current Location");
                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_current_loc_marker));
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(curLatLong, 16));
                    mCurrentLocationMarker = mMap.addMarker(markerOptions);
                    mCurrentLocationMarker.setPosition(curLatLong);
                }
            }
        };
    }

    @Override
    protected void onPause() {
        super.onPause();
        locationRequestSetting.stopLocationUpdates();
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
        mMap = googleMap;
        mMap.setOnMarkerClickListener(this);

//        for user current location
        /*if (permission.checkLocationPermission()) {
            mMap.setMyLocationEnabled(true);
        }*/

/*//        for direction
        mMap.getUiSettings().setCompassEnabled(true);*/

//      for adding marker on long clik in map
        mMap.setOnMapLongClickListener(this);
       /* // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));*/
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        drawMarker(latLng);
        curMarkerLocation = new Location("cur Marker");
        curMarkerLocation.setLatitude(latLng.latitude);
        curMarkerLocation.setLongitude(latLng.longitude);

        dInfoBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);

    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id){
            case R.id.btn_submit:
                String poiName = edtPoiName.getText().toString();
                String poiType = spnPoiType.getSelectedItem().toString();
                poiType += "-";
                poiType += spnPoiType.getSelectedItemPosition();
                String ownerName = edtOwnerName.getText().toString();
                String establishedDate = edtEstablishedDate.getText().toString();
                String curLat = String.valueOf(curMarkerLocation.getLatitude());
                String culLng = String.valueOf(curMarkerLocation.getLongitude());
                if (TextUtils.isEmpty(poiName)) {
                    edtPoiName.setError(getString(R.string.error_field_required));
                    edtPoiName.requestFocus();
                }else if (TextUtils.isEmpty(ownerName)){
                    edtOwnerName.setError(getString(R.string.error_field_required));
                    edtOwnerName.requestFocus();
                }else if (TextUtils.isEmpty(establishedDate)){
                    edtEstablishedDate.setError(getString(R.string.error_field_required));
                    edtEstablishedDate.requestFocus();
                }
                if (mCurrLocation == null){
                    Toast.makeText(this,R.string.location_error,Toast.LENGTH_SHORT).show();
                }else {
                    long insert = db.insertNote(poiName, poiType, ownerName, establishedDate, curLat, culLng, 0);
                    if (insert > -1) {
                        Toast.makeText(this, R.string.markerInserted, Toast.LENGTH_SHORT).show();
                        dInfoBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                        resetViews();
                    }
                }
                break;
            case R.id.edt_date:
                showDialog(1);
                break;

            case R.id.img_all_poi:
                mPoisList = db.getAllPoies();
                drawAllPois(mPoisList);
                allStopFocus();
                break;

            case R.id.img_traffic:
                if (mMap.isTrafficEnabled()){
                    mMap.setTrafficEnabled(false);
                    Toast.makeText(this,R.string.trafic_disabled,Toast.LENGTH_SHORT).show();
                }else {
                    mMap.setTrafficEnabled(true);
                    Toast.makeText(this,R.string.trafic_enabled,Toast.LENGTH_SHORT).show();

                }
                break;

            case R.id.img_sync:
                new UploadMarker().execute();
                break;

            case R.id.img_cur_loc:
                if (permission.checkLocationPermission() && mCurrLocation != null) {
                    LatLng curLatLng = new LatLng(mCurrLocation.getLatitude(), mCurrLocation.getLongitude());
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(curLatLng, 16));
                }
                break;
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);
                return new DatePickerDialog(MapsActivity.this,
                        to_date_listener,year,month,day);
    }

    DatePickerDialog.OnDateSetListener to_date_listener = new DatePickerDialog.OnDateSetListener() {
        @Override
        public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
            String fromdate2 = String .valueOf(dayOfMonth)+ "/" +String.valueOf(month+1)
                    +"/"+String .valueOf(year);
            edtEstablishedDate.setText(fromdate2);
        }
    };

    void resetViews(){
        edtPoiName.setText("");
        edtOwnerName.setText("");
        edtPoiName.setText("");
    }

    void drawAllPois(List<MPois> mPoisList){
        mMap.clear();
        allMarkersList = new ArrayList<>();
        for (int i = 0 ; i < mPoisList.size() ; i++){
            MPois mPois = mPoisList.get(i);
            LatLng latLng = new LatLng(Double.valueOf(mPois.getLat()),Double.valueOf(mPois.getLng()));
            drawMarker(latLng,i);
        }
    }

    void drawMarker(LatLng latLng){
        mMap.addMarker(new MarkerOptions()
                .position(latLng)
                .icon(BitmapDescriptorFactory.defaultMarker()));
    }
    void drawMarker(LatLng latLng,int i){
        Marker m = mMap.addMarker(new MarkerOptions()
                .position(latLng)
                .icon(BitmapDescriptorFactory.defaultMarker()));
        m.setTag(i);
        allMarkersList.add(m);
    }
//    for focusing all marker
    private void allStopFocus() {
        for (Marker m : allMarkersList) {
            builder.include(m.getPosition());
        }
        if (allMarkersList.size() != 0) {
            LatLngBounds bounds = builder.build();
            int padding = 80;
            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
            mMap.moveCamera(cu);
            mMap.animateCamera(cu);
        }else{
            Toast.makeText(MapsActivity.this,R.string.no_marker_saved,Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        try {
            Log.e("marker", marker.getTag().toString());
            dInfoBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            MPois mPois = mPoisList.get((Integer) marker.getTag());
            edtPoiName.setText(mPois.getPoiName());
            String poiType = mPois.getPoiType();
            String[] poiIndex = poiType.split("-");
            spnPoiType.setSelection(Integer.valueOf(poiIndex[1]));
            edtOwnerName.setText(mPois.getOwnerName());
            edtEstablishedDate.setText(mPois.getEstimateDate());
        }catch (NullPointerException npe){
            npe.printStackTrace();
        }
        return false;
    }


    public class UploadMarker extends AsyncTask<Void, Void, Boolean> {

        UploadMarker() {

        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showProgress(true);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // TODO: attempt authentication against a network service.

            try {
                // Simulate network access.
                Thread.sleep(2000);

//                we can update the row her after successfuly upload to server
//                db.updatePoi()


            } catch (InterruptedException e) {
                return false;
            }



            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            showProgress(false);


            if (success) {
            } else {

            }
        }

        @Override
        protected void onCancelled() {
            showProgress(false);
        }
    }

    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);


            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            progressBar.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    // for camera animation
    public void animateMarker(final Marker marker, final LatLng toPosition,
                              final boolean hideMarker, final float bearing) {
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
                marker.setRotation(bearing);
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

}
