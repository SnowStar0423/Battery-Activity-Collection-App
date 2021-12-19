package com.batt.data;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.batt.data.service.ActivityIntentService;
import com.batt.data.service.BackgroundService;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import static android.os.Environment.DIRECTORY_DOWNLOADS;

public class MainActivity extends AppCompatActivity implements LocationListener, SharedPreferences.OnSharedPreferenceChangeListener {

    TextView showBatteryStatus, showPowerPlug, showBatterLevel, showBatteryScale, showBatteryHealth, showBatteryVoltage, showBatteryTemperature;
    Button start_btn, stop_btn;
    protected LocationManager locationManager;
    protected Context context;
    public String latitude, longitude;
    private Context mContext;
    public static final String DETECTED_ACTIVITY = ".DETECTED_ACTIVITY";
    private StorageReference storageReference;
    private ActivityRecognitionClient mActivityRecognitionClient;


    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = this;
        // get the Firebase  storage reference
        storageReference = FirebaseStorage.getInstance().getReference();
        mActivityRecognitionClient = new ActivityRecognitionClient(this);

        showBatteryStatus = findViewById(R.id.showBatteryStatus);
        showPowerPlug = findViewById(R.id.showPowerPlug);
        showBatterLevel = findViewById(R.id.showBatterLevel);
        showBatteryScale = findViewById(R.id.showBatteryScale);
        showBatteryHealth = findViewById(R.id.showBatteryHealth);
        showBatteryVoltage = findViewById(R.id.showBatteryVoltage);
        showBatteryTemperature = findViewById(R.id.showBatteryTemperature);
        start_btn = findViewById(R.id.start_btn);
        stop_btn = findViewById(R.id.stop_btn);

//        startAlert();

        Intent intent = new Intent(this, BackgroundService.class);
        startService(intent);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);

        Intent i = new Intent(getApplicationContext(), ActivityIntentService.class);
        startService(i);

        ArrayList<DetectedActivity> detectedActivities = ActivityIntentService.detectedActivitiesFromJson(
                PreferenceManager.getDefaultSharedPreferences(this).getString(
                        DETECTED_ACTIVITY, ""));


    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        longitude = String.valueOf(location.getLongitude());
    }

    public void requestUpdatesHandler(View view) {
        Log.e("dd", "click");
//Set the activity detection interval. I’m using 3 seconds//
        Task<Void> task = mActivityRecognitionClient.requestActivityUpdates(
                3000,
                getActivityDetectionPendingIntent());
        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                updateDetectedActivitiesList();
            }
        });
    }
    private PendingIntent getActivityDetectionPendingIntent() {
//Send the activity data to our DetectedActivitiesIntentService class//
        Intent intent = new Intent(this, ActivityIntentService.class);
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

    }

    protected void updateDetectedActivitiesList() {
        ArrayList<DetectedActivity> detectedActivities = ActivityIntentService.detectedActivitiesFromJson(
                PreferenceManager.getDefaultSharedPreferences(mContext)
                        .getString(DETECTED_ACTIVITY, ""));

        HashMap<Integer, Integer> detectedActivitiesMap = new HashMap<>();
        for (DetectedActivity activity : detectedActivities) {
            detectedActivitiesMap.put(activity.getType(), activity.getConfidence());
        }

        ArrayList<DetectedActivity> temporaryList = new ArrayList<>();
        for (int i = 0; i < ActivityIntentService.POSSIBLE_ACTIVITIES.length; i++) {
            int confidence = detectedActivitiesMap.containsKey(ActivityIntentService.POSSIBLE_ACTIVITIES[i]) ?
                    detectedActivitiesMap.get(ActivityIntentService.POSSIBLE_ACTIVITIES[i]) : 0;

        //Add the object to a temporaryList//
            temporaryList.add(new
                    DetectedActivity(ActivityIntentService.POSSIBLE_ACTIVITIES[i],
                    confidence));
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(DETECTED_ACTIVITY)) {
            updateDetectedActivitiesList();
        }
    }

    public void startAlert(){
        Intent intent = new Intent(this, LoginActivity.class);
        int mPendingIntentId = 123456;
        PendingIntent pendingIntent = PendingIntent.getActivity(this.getApplicationContext(), mPendingIntentId, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + 10000, pendingIntent);
        System.exit(0);
        Toast.makeText(this, "Alarm set in 60 seconds",Toast.LENGTH_LONG).show();
    }

}