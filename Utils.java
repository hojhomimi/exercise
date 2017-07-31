package com.panasonic.smart.eolia.common.util;

import android.app.Activity;
import android.app.AppOpsManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;
import com.panasonic.smart.eolia.common.log.DebugLog;
import com.panasonic.smart.eolia.common.preference.PanaPreferenceManager;
import com.panasonic.smart.eolia.model.HomePosition;
import com.panasonic.smart.eolia.service.GeofenceProvidersChangedBroadcastReceiver;
import com.panasonic.smart.eolia.service.GeofenceTransitionsIntentService;

import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * ユーティリティクラス.
 *
 * @author Satoshi Itoh（Sky）
 * @version 1.0
 */
public class Util {
    /**
     * ログ用のタグ.
     */
    private static final String LOG_TAG = Util.class.getSimpleName();

    private static final String MARKET_URL = "market://details?id=";
    private static final String CHECK_OP_NO_THROW = "checkOpNoThrow";
    private static final String OP_POST_NOTIFICATION = "OP_POST_NOTIFICATION";
    private static Context gContext;
    private static Gson gson = new Gson();

    /**
     * dp値をpixelに変換する.
     *
     * @param context コンテキスト
     * @param dp      Dip値
     * @return Pixel値
     */
    public static int getPxFromDp(Context context, int dp) {
        DebugLog.d(LOG_TAG, "getPxFromDp START");
        final float scale = context.getResources().getDisplayMetrics().density;
        DebugLog.d(LOG_TAG, "getPxFromDp END");
        return (int) (dp * scale);
    }

    /**
     * 指定されたView以下のImageViewの開放.
     *
     * @param view 親ビュー
     */
    public static void cleanUpImageView(View view) {
        if (view instanceof ImageView) {
            ((ImageView) view).setImageDrawable(null);
        } else if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                cleanUpImageView(((ViewGroup) view).getChildAt(i));
            }
        }
    }

    /**
     * Stringをクリップボードに保存する.
     *
     * @param context コンテキスト
     * @param message メッセージ
     */
    public static void putClipBoard(Context context, String message) {
        DebugLog.d(LOG_TAG, "putClipBoard START");
        ClipData.Item item = new ClipData.Item(message);
        String[] mimeType = new String[1];
        mimeType[0] = ClipDescription.MIMETYPE_TEXT_PLAIN;
        ClipData cd = new ClipData(new ClipDescription("text_data", mimeType), item);
        ClipboardManager cm = (ClipboardManager)
                context.getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(cd);
        DebugLog.d(LOG_TAG, "putClipBoard END");
    }

    /**
     * パッケージ名からGooglePlayのURLを取得.
     *
     * @param packageName APPパッケージ名
     * @return Google PlayのURL
     */
    public static String getMarketUrl(String packageName) {
        DebugLog.d(LOG_TAG, "getMarketUrl START");
        return MARKET_URL + packageName;
    }


    /**
     * 通知の設定状態を取得.
     *
     * @param context コンテキスト
     * @return 通知の権限状態がONならtrue、OFFならfals
     */
    public static boolean isNotificationEnabled(Context context) {
        DebugLog.d(LOG_TAG, "isNotificationEnabled START");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            AppOpsManager appOps = (AppOpsManager)
                    context.getSystemService(Context.APP_OPS_SERVICE);

            ApplicationInfo appInfo = context.getApplicationInfo();

            String pkg = context.getApplicationContext().getPackageName();

            int uid = appInfo.uid;

            try {
                Class appOpsClass = Class.forName(AppOpsManager.class.getName());

                Method checkOpNoThrowMethod = appOpsClass.getMethod(CHECK_OP_NO_THROW,
                        Integer.TYPE, Integer.TYPE, String.class);

                Field opPostNotificationValue = appOpsClass.getDeclaredField(OP_POST_NOTIFICATION);
                int value = (int) opPostNotificationValue.get(Integer.class);

                return ((int) checkOpNoThrowMethod.invoke(appOps, value, uid, pkg)
                        == AppOpsManager.MODE_ALLOWED);

            } catch (Exception exception) {
                DebugLog.e(LOG_TAG, exception);
            }
        } else {
            return true;
        }

        DebugLog.d(LOG_TAG, "isNotificationEnabled END");
        return false;
    }

    /**
     * GPS機能が有効です。
     *
     * @param context
     * @return
     */


    public static final boolean isGpsOPen(final Context context) {
        LocationManager locationManager
                = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        // 通过GPS卫星定位，定位级别可以精确到街（通过24颗卫星定位，在室外和空旷的地方定位准确、速度快）
        boolean gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        // 通过WLAN或移动网络(3G/2G)确定的位置（也称作AGPS，辅助GPS定位。主要用于在室内或遮盖物（建筑群或茂密的深林等）密集的地方定位）
        boolean network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (gps || network) {
            return true;
        }

        return false;
    }


    /**
     * GPS open
     *
     * @param context
     */
    public static final void openGPS(Context context) {
        Intent GPSIntent = new Intent();
        GPSIntent.setClassName("com.android.settings",
                "com.android.settings.widget.SettingsAppWidgetProvider");
        GPSIntent.addCategory("android.intent.category.ALTERNATIVE");
        GPSIntent.setData(Uri.parse("custom:3"));
        try {
            PendingIntent.getBroadcast(context, 0, GPSIntent, 0).send();
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
    }

    /**
     * 指定された機能が存在するかをチェック.
     *
     * @param context    コンテキスト
     * @param featureStr 指定された機能
     * @return 存在する:true 存在しない:false
     */
    public static final boolean isHasHardware(Context context, String featureStr) {
        //PackageManager.FEATURE_NFC
        PackageManager pm = context.getPackageManager();
        boolean feature = pm
                .hasSystemFeature(featureStr);

        return feature;
    }



    /**
     * 指定されたパッケージ名のAppが存在するかをチェック.
     *
     * @param context     コンテキスト
     * @param packageName パッケージ名
     * @return 存在する:true 存在しない:false
     */
    public static boolean existPackage(Context context, String packageName) {
        DebugLog.d(LOG_TAG, "existPackage START");
        List<ApplicationInfo> appInfoList = context.getPackageManager()
                .getInstalledApplications(PackageManager.GET_META_DATA);
        for (ApplicationInfo info : appInfoList) {
            if (packageName.equals(info.processName)) {
                return true;
            }
        }
        DebugLog.d(LOG_TAG, "existPackage END");
        return false;
    }

    /**
     * ObjectへJsonに転換する。
     *
     * @param o 　転換成功のjson
     * @return
     */

    public static String toJson(Object o) {

        synchronized (gson) {
            return gson.toJson(o);
        }
    }

    /**
     * JsonへObjectに転換する。
     *
     * @param  　転換成功のObject
     * @return
     */

    public static <T> T fromJson(String objString, Class<T> classOfT) {
        synchronized (gson) {
            return gson.fromJson(objString, classOfT);
        }
    }


    /**
     * Provides the entry point to Google Play services.
     */
    protected static GoogleApiClient mGoogleApiClient;

    /**
     * The list of geofences used in this sample.
     */
    protected static ArrayList<Geofence> mGeofenceList;

    /**
     * Used when requesting to add or remove geofences.
     */
    private static PendingIntent mGeofencePendingIntent;

    /**
     * Builds a GoogleApiClient. Uses the {@code #addApi} method to request the LocationServices API.
     */
    protected static synchronized void buildGoogleApiClient(Context context) {

        DebugLog.i("MyApplication", "buildGoogleApiClient");
        synchronized (gContext) {
            if (mGoogleApiClient != null) {
                DebugLog.i("MyApplication", "mGoogleApiClient != null");
                if (mGoogleApiClient.isConnected()) {
                    DebugLog.i("MyApplication", "mGoogleApiClient.isConnected() == true");
                    addGeofences();
                } else {
                    mGoogleApiClient.connect();
                }
                return;
            }
        }
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        DebugLog.i("MyApplication", "Connected to GoogleApiClient: " + connectionHint);
                        //Toast.makeText(mContext, "Connected to GoogleApiClient", Toast.LENGTH_SHORT).show();
                        addGeofences();
                    }

                    @Override
                    public void onConnectionSuspended(int cause) {
                        DebugLog.d("MyApplication", "onConnectionSuspended: " + cause);
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        DebugLog.i("MyApplication", "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
                    }
                })
                .addApi(LocationServices.API)
                .build();

        mGoogleApiClient.connect();

    }


    protected static synchronized void addGeofences() {
        DebugLog.i("MyApplication", "addGeofences");
        try {
            LocationServices.GeofencingApi.addGeofences(
                    mGoogleApiClient,
                    // The GeofenceRequest object.
                    getGeofencingRequest(),
                    getGeofencePendingIntent()
            ).setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(Status status) {
                    DebugLog.i("MyApplication", "LocationServices.GeofencingApi.addGeofences onResult");
                    if (status.isSuccess()) {
                        setRegionObserving(true, gContext);
                        GeofenceProvidersChangedBroadcastReceiver.checkRetryFlag = false;
//                        GeofenceProvidersChangedBroadcastReceiver.handler.removeCallbacks(GeofenceProvidersChangedBroadcastReceiver.runnable);
                    } else {
                        if (GeofenceProvidersChangedBroadcastReceiver.checkRetryFlag) {
//                            GeofenceProvidersChangedBroadcastReceiver.handler.postDelayed(GeofenceProvidersChangedBroadcastReceiver.runnable, 5000);
                        }
                        DebugLog.e("MyApplication", "status.getStatusCode()=" + status.getStatusCode());
                    }
                }

            }); // Result processed in onResult().

        } catch (SecurityException securityException) {
            // Catch exception generated if the app does not use ACCESS_FINE_LOCATION permission.
            DebugLog.e("MyApplication", "Invalid location permission. " + "You need to use ACCESS_FINE_LOCATION with geofences");
        }
    }

    /**
     * Builds and returns a GeofencingRequest. Specifies the list of geofences to be monitored.
     * Also specifies how the geofence notifications are initially triggered.
     */
    private static GeofencingRequest getGeofencingRequest() {
        DebugLog.i("MyApplication", "getGeofencingRequest");
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();

        // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
        // GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
        // is already inside that geofence.
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_EXIT | GeofencingRequest.INITIAL_TRIGGER_ENTER);

        // Add the geofences to be monitored by geofencing service.
        builder.addGeofences(mGeofenceList);

        // Return a GeofencingRequest.
        return builder.build();
    }

    /**
     * Gets a PendingIntent to send with the request to add or remove Geofences. Location Services
     * issues the Intent inside this PendingIntent whenever a geofence transition occurs for the
     * current list of geofences.
     *
     * @return A PendingIntent for the IntentService that handles geofence transitions.
     */
    private static PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(gContext, GeofenceTransitionsIntentService.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        mGeofencePendingIntent = PendingIntent.getService(gContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);//xq
        return mGeofencePendingIntent;
    }

    /**
     * 監視状態を設定する
     *
     * @param context
     */
    public static void setRegionObserving(boolean flag, Context context) {
        DebugLog.i("MyApplication", "setRegionObserving(), flag=" + flag);
        String localStorageRegionObserve = PanaPreferenceManager.loadRegionObserve(context);
        if ("undefined".equals(localStorageRegionObserve)) {
            localStorageRegionObserve = "{}";
        }
        try {
            JSONObject RegionObserveStorage = new JSONObject(localStorageRegionObserve);
            RegionObserveStorage.put("isRegionObserving", flag);
            PanaPreferenceManager.saveRegionObserve(context, RegionObserveStorage.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 監視開始
     */
    public static void startRegionObservation(HomePosition jsonString, Context context) {
        DebugLog.i("MyApplication", "startRegionObservation() jsonString=" + jsonString);
        gContext = context;
        HomePosition.LocationItem locationJSONObj = null;
        float radius = 0;
        if (jsonString != null) {
            locationJSONObj = jsonString.location;
            radius = jsonString.radius.floatValue();

        }
        double latitude = 0;
        double longitude = 0;

        if (locationJSONObj != null) {
            latitude = locationJSONObj.latitude.doubleValue();
            longitude = locationJSONObj.longitude.doubleValue();

        }
        DebugLog.i("MyApplication", "latitude=" + latitude);
        DebugLog.i("MyApplication", "longitude=" + longitude);
        DebugLog.i("MyApplication", "radius=" + radius);
//        if (SysBootedReceiver.isSysBooted == false) {
//            String localStorageRegionObserve = PanaPreferenceManager.loadRegionObserve(context);
//            if ("undefined".equals(localStorageRegionObserve)) {
//                localStorageRegionObserve = "{}";
//            }
//
//            JSONObject RegionObserveStorage = null;
//            try {
//                RegionObserveStorage = new JSONObject(localStorageRegionObserve);
//                RegionObserveStorage.put("RegionObserveState", "-1");
//                PanaPreferenceManager.saveRegionObserve(context, RegionObserveStorage.toString());
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//        SysBootedReceiver.isSysBooted = false;
        // Empty list for storing geofences.
        mGeofenceList = new ArrayList<Geofence>();

        // Initially set the PendingIntent used in addGeofences() and removeGeofences() to null.
        mGeofencePendingIntent = null;
        // Get the geofences used. Geofence data is hard coded in this sample.
        populateGeofenceList(latitude, longitude, radius);

        // Kick off the request to build GoogleApiClient.
        buildGoogleApiClient(context);
    }


    /**
     * 　GeofenceList生成
     *
     * @param latitude  経度
     * @param longitude 　緯度
     * @param radius    　半径
     */
    private static void populateGeofenceList(double latitude, double longitude, float radius) {
        DebugLog.i("MyApplication", "populateGeofenceList,latitude=" + latitude + "longitude=" + longitude + "radius=" + radius);
        mGeofenceList.add(new Geofence.Builder()
                // Set the request ID of the geofence. This is a string to identify this
                // geofence.
                .setRequestId(String.valueOf(latitude))

                // Set the circular region of this geofence.
                .setCircularRegion(latitude, longitude, radius)

                // Set the expiration duration of the geofence. This geofence gets automatically
                // removed after this period of time.
                .setExpirationDuration(Geofence.NEVER_EXPIRE)

                // Set the transition types of interest. Alerts are only generated for these
                // transition. We track entry and exit transitions in this sample.
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                        Geofence.GEOFENCE_TRANSITION_EXIT)

                // Create the geofence.
                .build());
    }


    /**
     * 監視停止
     */
    public static void stopRegionObservation(final HomePosition jsonString) {
        DebugLog.i("MyApplication", "stopRegionObservation()");
        if (mGoogleApiClient == null) {
            DebugLog.i("MyApplication", "mGoogleApiClient == null");
            if (gContext != null) {
                setRegionObserving(false, gContext);
                if ((jsonString != null) && (!jsonString.equals("undefined"))) {
                    startRegionObservation(jsonString, gContext);
                }
            }
            return;
        } else if (mGoogleApiClient.isConnected() == false) {
            DebugLog.i("MyApplication", "mGoogleApiClient.isConnected() == false");
            setRegionObserving(false, gContext);
            if ((jsonString != null) && (!jsonString.equals("undefined"))) {
                startRegionObservation(jsonString, gContext);
            }
            return;
        }
        try {
            // Remove geofences.
            LocationServices.GeofencingApi.removeGeofences(
                    mGoogleApiClient,
                    // This is the same pending intent that was used in addGeofences().
                    getGeofencePendingIntent()
            ).setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(Status status) {
                    DebugLog.i("MyApplication", "LocationServices.GeofencingApi.removeGeofences onResult");
                    if (status.isSuccess()) {
                        //Toast.makeText(gContext, "removeGeofences onResult = isSuccess", Toast.LENGTH_SHORT).show();
                        DebugLog.i("MyApplication", "onResult = isSuccess");
                        setRegionObserving(false, gContext);
                        if ((jsonString != null) && (!jsonString.equals("undefined"))) {
                            startRegionObservation(jsonString, gContext);
                        }
                    } else {
                        DebugLog.e("MyApplication", "status.getStatusCode()=" + status.getStatusCode());
                    }
                }

            }); // Result processed in onResult().

        } catch (SecurityException securityException) {
            // Catch exception generated if the app does not use ACCESS_FINE_LOCATION permission.
            DebugLog.e("MyApplication", "Invalid location permission. " +
                    "You need to use ACCESS_FINE_LOCATION with geofences");
        }
    }

    /**
     * 監視状態を取得する
     *
     * @param context
     */
    public static boolean isRegionObserving(Context context) {
        DebugLog.i("MyApplication", "isRegionObserving()");
        String localStorageRegionObserve = PanaPreferenceManager.loadRegionObserve(context);
        if ("undefined".equals(localStorageRegionObserve)) {
            localStorageRegionObserve = "{}";
        }

        boolean isRegionObservingFlg = false;
        try {
            JSONObject RegionObserveStorage = new JSONObject(localStorageRegionObserve);
            isRegionObservingFlg = RegionObserveStorage.getBoolean("isRegionObserving");
//            if (((SettingReminderActivity) context).isTaskRoot() && (mGoogleApiClient == null)) {
//                isRegionObservingFlg = false;
//            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        DebugLog.i("MyApplication", "isRegionObserving = " + isRegionObservingFlg);
        return isRegionObservingFlg;
    }


    /**
     * RenderScriptを使用してBitMapをBlur処理を行う。.
     *
     * @param context コンテキスト
     * @param bitmap  Blur処理を行う画像
     * @return Blur処理を行った画像
     */
    public static Bitmap blur(Context context, Bitmap bitmap) {
        DebugLog.d(LOG_TAG, "blur START");

        RenderScript rs = RenderScript.create(context);
        if (Build.VERSION.SDK_INT >= 17) {
            ScriptIntrinsicBlur theIntrinsic = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
            Allocation tmpIn = Allocation.createFromBitmap(rs, bitmap);
            Bitmap outputBitmap = Bitmap.createBitmap(bitmap);
            Allocation tmpOut = Allocation.createFromBitmap(rs, outputBitmap);
            theIntrinsic.setRadius(10f);
            theIntrinsic.setInput(tmpIn);
            theIntrinsic.forEach(tmpOut);
            tmpOut.copyTo(outputBitmap);
            DebugLog.d(LOG_TAG, "blur END");
            return outputBitmap;
        } else {
            DebugLog.d(LOG_TAG, "blur END");
            return bitmap;
        }
    }


    /**
     * ダイアログ背景にブラーをかける.
     *
     * @param activity 表示のベースのなるActivity
     * @param dialog   表示するダイアログ
     */
    public static void dialogEffect(Activity activity, Dialog dialog) {
        DebugLog.d(LOG_TAG, "dialogEffect START");

        if (dialog.getWindow() != null) {
            WindowManager.LayoutParams lp = dialog.getWindow().getAttributes();
            lp.dimAmount = 0.0f;
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            dialog.getWindow().setAttributes(lp);
        }

        //Activityの表示内容をViewとして取得
        View decorView = activity.getWindow().getDecorView();
        decorView.setDrawingCacheEnabled(true);
        decorView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);
        // Viewのキャッシュを取得
        final Bitmap cache = decorView.getDrawingCache(true);
        if (cache != null) {

            final ImageView iv = new ImageView(activity);

            //コピーした画像を加工してImageViewに貼り付ける
            iv.setImageBitmap(Util.blur(activity, cache));
            //加工したActivityの表示内容をViewとして貼り付ける
            ((ViewGroup) decorView).addView(iv);
            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialogInterface) {
                    try {
                        ((ViewGroup) iv.getParent()).removeView(iv);
                    } catch (IllegalArgumentException exception) {
                        DebugLog.e(LOG_TAG, exception);
                    }
                }
            });
        } else {
            DebugLog.d(LOG_TAG, "DrawingCache取得失敗");
        }
        decorView.setDrawingCacheEnabled(false);

        DebugLog.d(LOG_TAG, "dialogEffect END");
    }
      public static void showNotification(String serviceID, String applianceID, String msg, String titletext, Context context) {
        DebugLog.d(LOG_TAG, "showNotification() serviceID=" + serviceID + ",applianceID=" + applianceID + ",msg=" + msg + ",titletext=" + titletext);

        // 通知欄メッセージのタイトル
        String title = "通知";
        if(StringUtil.isNotEmpty(titletext)) {
            title = titletext;
        }
        // 通知のＩＤを作成する「日＋時＋分＋秒」
        Date currentDate = new Date();
        String notifyId = String.valueOf((int)(currentDate.getTime() % 1000000));
        DebugLog.d(LOG_TAG, "notifyId=" + notifyId);


// CHG-ST #30718:IT1_項目外_Push通知バナーをタップした後の動作がアンドロイドとiOSで差異がある。 PSDCD徐 2017-06-27
        Intent newIntent = new Intent(context, InitNotification.class);
// CHG-ED #30718:IT1_項目外_Push通知バナーをタップした後の動作がアンドロイドとiOSで差異がある。 PSDCD徐 2017-06-27
        //newIntent.setAction(ACTION_PUSH_MESSAGE_FCM);
        // PUSH通知サービス種別
        newIntent.putExtra(PUSH_RECEIVE_APPLIANCE_ID, applianceID);
        // 機器ＩＤ
        newIntent.putExtra(PUSH_RECEIVE_MESSAGE, msg);
        // ポップアップ表示用メッセージ
        newIntent.putExtra(PUSH_RECEIVE_SERVICE_ID, serviceID);

        newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        newIntent.setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        newIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        newIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        newIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, Integer.parseInt(notifyId), newIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = null;
/*        if (android.os.Build.VERSION.SDK_INT < 23){
            DebugLog.d(LOG_TAG, "SDK_INT<23");
            notification = new Notification(R.mipmap.ic_launcher, title, System.currentTimeMillis());
            //notification.setLatestEventInfo(context, "appName", title, pendingIntent);
        } else {
*/
        // 通知の作成(Heads-up通知を利用する)
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.mipmap.app_android)
                .setContentTitle(title)
                .setContentText(msg)
                .setTicker(title)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setFullScreenIntent(PendingIntent.getActivity(context, 0, new Intent(), 0), true);

//ADD-ST #10461:PUSH通知受信時のアイコン（Android5以上用） PSDCD徐 2017-07-25
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            //Android 5 以上
            builder.setSmallIcon(R.mipmap.notification_icon);
        }
//ADD-ED #10461:PUSH通知受信時のアイコン（Android5以上用） PSDCD徐 2017-07-25
        notification = builder.build();
//        }
        notification.defaults = Notification.DEFAULT_SOUND;
        notification.flags = Notification.FLAG_AUTO_CANCEL;

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(
                android.content.Context.NOTIFICATION_SERVICE);
        notificationManager.notify(Integer.parseInt(notifyId), notification);

        KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        if (km.inKeyguardRestrictedInputMode()) {
            DebugLog.d(LOG_TAG, "WakeLock startActivity");
            Intent alarmIntent = new Intent(context, AlarmHandlerActivity.class);
            alarmIntent.putExtra("title", title);
            alarmIntent.putExtra(PUSH_RECEIVE_APPLIANCE_ID, applianceID);
            // 機器ＩＤ
            alarmIntent.putExtra(PUSH_RECEIVE_MESSAGE, msg);
            // ポップアップ表示用メッセージ
            alarmIntent.putExtra(PUSH_RECEIVE_SERVICE_ID, serviceID);
            alarmIntent.putExtra("notifyId", notifyId);
            alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            context.startActivity(alarmIntent);
        } else {
            final PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (!pm.isScreenOn()) {
                final PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP |
                        PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "bright");
                DebugLog.d(LOG_TAG, "WakeLock ScreenOn");
                wl.acquire();
                int result = 15000;
                try {
                    result  = Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT);
                } catch (Exception e){
                    e.printStackTrace();
                }
                DebugLog.i(LOG_TAG, "screen off timeout = " + result);

                new Handler().postDelayed(new Runnable() {
                    public void run() {
                        wl.release();
                    }
                }, result);
            }

        }
    }


}
