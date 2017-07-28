package com.panasonic.jp.SmartRAC.activity.apptopmenu.settingreminder;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;
import com.panasonic.jp.SmartRAC.R;
import com.panasonic.jp.SmartRAC.activity.BaseActivity;
import com.panasonic.jp.SmartRAC.common.configs.Config;
import com.panasonic.jp.SmartRAC.common.log.DebugLog;
import com.panasonic.jp.SmartRAC.common.preference.PanaPreferenceManager;
import com.panasonic.jp.SmartRAC.common.util.Util;
import com.panasonic.jp.SmartRAC.common.util.UtilEolia;
import com.panasonic.jp.SmartRAC.common.util.asynctask.ProgressAsyncTask;
import com.panasonic.jp.SmartRAC.common.view.dialog.ErrorDialogFragment;
import com.panasonic.jp.SmartRAC.common.view.dialog.NetWorkOperationPriorityDialogFragment;
import com.panasonic.jp.SmartRAC.model.GoogleResponse;
import com.panasonic.jp.SmartRAC.model.HomePosition;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 自宅設定画面.
 * <p>
 * <p>自宅設定を行うActivityです。
 *
 * @author songyb on 2017/03/02
 * @version 1.0
 */

public class HomePinActivity extends BaseActivity implements OnMyLocationButtonClickListener,OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    final private int REQUEST_CODE_ASK_PERMISSIONS = 123;
    /**
     * ログ用のタグ.
     */
    private static final String LOG_TAG = HomePinActivity.class.getSimpleName();
    /**
     * Default Zoom scale
     */
    private static final int DEFAULT_ZOOM = 14;
    private static final float INIT_ZOOM = 3.38f;
    private static final float RADIUS = 1000;
    private static final int INDEX = 0;
    private static final float CIRCLE_STROKE_WIDTH = 3f;
    private GoogleMap mMap;
    private LinearLayout btnOK;
    private Marker mMarker;
    private Circle mCircle;
    //アドレス更新用
    private String mAddrString;
    private Location myLocation;
    /**
     * 設定した位置を保存する。
     */
    private Location mSetLocation;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest locationRequest;

    @Override
    protected int getBaseLayoutId() {
        return R.layout.activity_setting_home_pin;
    }

    @Override
    public int getCustomActionBarLayout() {
        DebugLog.d(LOG_TAG, "getCustomActionBarLayout");
        return R.layout.toolbar_normal;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DebugLog.d(LOG_TAG, "onCreate start");
        super.onCreate(savedInstanceState);
        if(UtilEolia.isModifyPermission(this)){
            return;
        }
        setToolBarTitle(getString(R.string.lbHomeSetting));
        btnOK = (LinearLayout) findViewById(R.id.btnSetting);
        btnOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setData();
            }
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
// ADD-ST #30510:自宅設定を行った時にGoogle Mapの画面上でタップするとエラーが発生する PSDCD林 2017-06-20
        if (mSetLocation == null) {
            //初期化処理を追加する
            mSetLocation = new Location("Panasonic");
            mSetLocation.setLatitude(37.511d);
            mSetLocation.setLongitude(139.197d);
        }
// ADD-ED #30510:自宅設定を行った時にGoogle Mapの画面上でタップするとエラーが発生する PSDCD林 2017-06-20
        requestPermissions();
        DebugLog.d(LOG_TAG, "onCreate end");
    }

    /**
     *　選択した位置をHomePositionに設定する
     */

    private void setData() {
        DebugLog.d(LOG_TAG, "setData start");
        HomePosition homePosition = new HomePosition();
        homePosition.radius = RADIUS;
        HomePosition.LocationItem locationItem = new HomePosition.LocationItem();

        if (mSetLocation != null){
            locationItem.latitude = Util.encode(String.valueOf(mSetLocation.getLatitude()));
            locationItem.longitude = Util.encode(String.valueOf(mSetLocation.getLongitude()));
            homePosition.location = locationItem;
            PanaPreferenceManager.saveHomePosition(HomePinActivity.this,homePosition);

        }else{
            DebugLog.d(LOG_TAG, "mSetLocation is null");
        }
        finish();
        DebugLog.d(LOG_TAG, "setData end");
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        DebugLog.d(LOG_TAG, "onMapReady start");
        mMap = googleMap;
        connect();
        mMap.setOnMyLocationButtonClickListener(this);
        enableMyLocation();
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.setOnMapClickListener(new OnMapClickListener(){
            @Override
            public void onMapClick(LatLng point) {
                //Toast.makeText(getApplicationContext(), "タップ位置\n緯度：" + point.latitude + "\n経度:" + point.longitude, Toast.LENGTH_LONG).show();
                setMarker(point);
                mSetLocation.setLatitude(point.latitude);
                mSetLocation.setLongitude(point.longitude);
            }
        });
        /**
         * Customize InfoWindow, default infoWindow cannot wrapper content
         */
        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker arg0) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {

                Context context = getApplicationContext();

                LinearLayout info = new LinearLayout(context);
                info.setOrientation(LinearLayout.VERTICAL);

                TextView title = new TextView(context);
                title.setTextColor(Color.BLACK);
                title.setGravity(Gravity.CENTER);
                title.setTypeface(null, Typeface.BOLD);
                title.setText(marker.getTitle());

                TextView snippet = new TextView(context);
                snippet.setTextColor(Color.GRAY);
                snippet.setText(marker.getSnippet());

                info.addView(title);
                info.addView(snippet);

                return info;
            }
        });
        DebugLog.d(LOG_TAG, "onMapReady end");

    }

    private void setMarker(LatLng point) {
        //draw Marker
        if (null == mMarker) {
            mMarker = mMap.addMarker(new MarkerOptions()
                    .position(point)
                    .snippet(getAddress(point))
                    .title("自宅位置"));
        }else{
            mMarker.setPosition(point);
            mMarker.setSnippet(getAddress(point));
        }
        //move camera
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(point,DEFAULT_ZOOM));
        //draw Circle
        if (null == mCircle) {
            mCircle = mMap.addCircle(new CircleOptions()
                    .center(point)
                    .strokeColor(Color.rgb(176,224,230))
                    .strokeWidth(CIRCLE_STROKE_WIDTH)
                    .fillColor(Color.argb(100,197,228,252))
                    .radius(RADIUS).visible(true));
        }else{
            mCircle.setCenter(point);
        }
    }
    private String getAddress(LatLng point) {
        //位置座標からアドレスを取得処理
        String positionString = (new StringBuilder())
                .append("latlng=")
                .append(point.latitude)
                .append(",")
                .append(point.longitude)
                .append("&key=")
                .append(getString(R.string.google_maps_key))
                .toString();
        final String requestUrl = "https://maps.googleapis.com/maps/api/geocode/json?"+positionString+"&language=ja";

        ProgressAsyncTask progressAsyncTask = new ProgressAsyncTask<String, Void, String>(getBaseContext()) {
            @Override
            protected String catchException(Exception exception) {
                return null;
            }

            @Override
            protected String doBackground(String param) throws Exception {
                Log.d(LOG_TAG, "HTTP request:"+requestUrl);
                URL url = new URL(requestUrl);
                HttpURLConnection con = (HttpURLConnection)url.openConnection();
                try {
                    con.setRequestProperty("Accept", "application/json");
                    con.setRequestProperty("Content-Type",
                            "application/json;charset=UTF-8");
                    con.setConnectTimeout(5 * 1000);
                    con.setRequestMethod("GET");
                    if (con.getResponseCode() != 200) throw new RuntimeException("Request url failed");
                    String str = InputStreamToString(con.getInputStream());
                    GoogleResponse response = new Gson().fromJson(str, GoogleResponse.class);
                    //取得したアドレスを更新する
                    synchronized (this) {
                        mAddrString = response.getResults(INDEX).getFormattedAddress();
                    }
                    Log.d(LOG_TAG, "HTTP response:"+mAddrString);
                } finally {
                    con.disconnect();
                }
                return mAddrString;
            }

            @Override
            protected void onPostedExecute(String result) {
                //set snippet message
                mMarker.setSnippet(result);
                //show infowindow
                mMarker.showInfoWindow();
            }

        };
        progressAsyncTask.execute();

        return mAddrString;
    }
    static String InputStreamToString(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is, Config.DEFAULT_CHARSET));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        br.close();
        return sb.toString();
    }
    /**
     * Enables the My Location layer if the fine location permission has been granted.
     */
    private void enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED) {
            //Toast.makeText(this, "Permission not granted,設定->アプリ->Eolia->許可", Toast.LENGTH_SHORT).show();
            return;
        } else if (mMap != null) {
            // Access to the location has been granted to the app.
            mMap.setMyLocationEnabled(true);
        }
    }

    public void connect() {
        DebugLog.d(LOG_TAG, "connect start");
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
        DebugLog.d(LOG_TAG, "connect end");
    }

    private void updateToNewLocation(Location location) {
        DebugLog.d(LOG_TAG, "updateToNewLocation start");
        if (null != location) {
            this.mSetLocation = location;
            LatLng newLocation = new LatLng(location.getLatitude(), location.getLongitude());
            setMarker(newLocation);
        }
        DebugLog.d(LOG_TAG, "updateToNewLocation end");
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        DebugLog.d(LOG_TAG, "onConnected start");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //Toast.makeText(this, "Permission not granted,設定->アプリ->Eolia->許可", Toast.LENGTH_SHORT).show();
            return;
        }
        myLocation =
                LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (myLocation != null) {
            DebugLog.d(LOG_TAG, "myLocation:" + myLocation);
            updateToNewLocation(myLocation);
//            LatLng newLocation = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());
//            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newLocation,DEFAULT_ZOOM));
        }

        //現在の位置を取得する
        startLocation();
        DebugLog.d(LOG_TAG, "onConnected end");
    }

    @Override
    public void onConnectionSuspended(int i) {
        DebugLog.d(LOG_TAG, "onConnectionSuspended start");
        DebugLog.d(LOG_TAG, "onConnectionSuspended end");
    }

    public void startLocation() {
        DebugLog.d(LOG_TAG, "startLocation mGoogleApiClient.isConnected():" + mGoogleApiClient.isConnected());
        if (mGoogleApiClient.isConnected()) {
            createLocationRequest();
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                //Toast.makeText(this, "Permission not granted,設定->アプリ->Eolia->許可", Toast.LENGTH_SHORT).show();
                return;
            }
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, locationRequest, this);
// CHG-ST #30330:IT1_2-119-7_1-27_「OSのアプリの自宅位置設定権限誘導ポップアップ画面」で、許可を押下すると、地図が現在位置とならない。 PSDCD林 2017-06-09
            myLocation =
                    LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (myLocation != null) {
                DebugLog.d(LOG_TAG, "myLocation:" + myLocation);
                //カメラ移動、マーカ設置
                updateToNewLocation(myLocation);
                //現在位置を有効
                enableMyLocation();
            } else {
// ADD-ST #30510:自宅設定を行った時にGoogle Mapの画面上でタップするとエラーが発生する PSDCD林 2017-06-22
                DebugLog.d(LOG_TAG, "myLocation:" + myLocation);
                LatLng newLocation = new LatLng(mSetLocation.getLatitude(), mSetLocation.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newLocation,INIT_ZOOM));
// ADD-ST #30510:自宅設定を行った時にGoogle Mapの画面上でタップするとエラーが発生する PSDCD林 2017-06-22
            }
// CHG-ED #30330:IT1_2-119-7_1-27_「OSのアプリの自宅位置設定権限誘導ポップアップ画面」で、許可を押下すると、地図が現在位置とならない。 PSDCD林 2017-06-09
        }

        DebugLog.d(LOG_TAG, "startLocation end");
    }

    private void createLocationRequest() {
        DebugLog.d(LOG_TAG, "createLocationRequest start");
        locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        DebugLog.d(LOG_TAG, "createLocationRequest end");
    }

    @Override
    public void onLocationChanged(Location location) {
        DebugLog.d(LOG_TAG, "onLocationChanged start");
        if (location != null) {
            //updateToNewLocation(location);
        } else {
            DebugLog.d(LOG_TAG, "定位失敗.....");
        }
        DebugLog.d(LOG_TAG, "onLocationChanged end");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        DebugLog.d(LOG_TAG, "onConnectionFailed start");
        DebugLog.d(LOG_TAG, "onConnectionFailed end");
    }

    @Override
    public boolean onMyLocationButtonClick() {
        //Toast.makeText(this, "MyLocation button clicked", Toast.LENGTH_SHORT).show();
//ADD-ST #30895:IT1_2-19-7_1-34_現在地が取れない場合の「自宅設定画面：MAINMENU-005」での動作に齟齬がある。 PSDCD宋 2017-07-05
        if (myLocation == null) {
            ErrorDialogFragment confirmDialogFragment =
                    ErrorDialogFragment.newInstanceSingleButton(getString(R.string.close),
                            getString(R.string.message_location_error_get), null);
            confirmDialogFragment.show(getFragmentManager(), null);
        }
//ADD-ED #30895:IT1_2-19-7_1-34_現在地が取れない場合の「自宅設定画面：MAINMENU-005」での動作に齟齬がある。 PSDCD宋 2017-07-05
        return false;
    }

    /**
     * 権限を取得する。
     */

    private void requestPermissions() {

        if (Build.VERSION.SDK_INT < 23) {
            return;
        }

        int hasWriteContactsPermission = checkSelfPermission(
                Manifest.permission.ACCESS_COARSE_LOCATION);
        if (hasWriteContactsPermission != PackageManager.PERMISSION_GRANTED) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)) {


                NetWorkOperationPriorityDialogFragment confirmDialogFragment = NetWorkOperationPriorityDialogFragment.newInstancePDFPermission(
                        getString(R.string.btnSetting)
                        ,  getString(R.string.reminder_notification_dialog_content), new NetWorkOperationPriorityDialogFragment.OnErrorConfirmListener() {
                            @Override
                            public void onClick() {
                                startActivity(getAppDetailSettingIntent());
                                finish();
                            }

                            @Override
                            public void onBack() {
                                finish();
                            }

                        });
                confirmDialogFragment.show(getFragmentManager(), null);
            } else {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        REQUEST_CODE_ASK_PERMISSIONS);
            }

            return;
        }

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        DebugLog.d(LOG_TAG, "onRequestPermissionsResult" + requestCode);
        if (requestCode == REQUEST_CODE_ASK_PERMISSIONS) {
// CHG-ST #30330:IT1_2-119-7_1-27_「OSのアプリの自宅位置設定権限誘導ポップアップ画面」で、許可を押下すると、地図が現在位置とならない。 PSDCD宋 2017-06-08
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED){
                finish();
            }else {
                //権限取得後、位置を取得する。
                startLocation();
            }
// CHG-ED #30330:IT1_2-119-7_1-27_「OSのアプリの自宅位置設定権限誘導ポップアップ画面」で、許可を押下すると、地図が現在位置とならない。 PSDCD宋 2017-06-08
        }
    }

    /**
     * アプリ詳細設定intent
     */
    private Intent getAppDetailSettingIntent() {
        Intent localIntent = new Intent();
        localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT >= 9) {
            localIntent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
            localIntent.setData(Uri.fromParts("package", getPackageName(), null));
        } else if (Build.VERSION.SDK_INT <= 8) {
            localIntent.setAction(Intent.ACTION_VIEW);
            localIntent.setClassName("com.android.settings",
                    "com.android.settings.InstalledAppDetails");
            localIntent.putExtra("com.android.settings.ApplicationPkgName", getPackageName());
        }
        return localIntent;
    }
}
