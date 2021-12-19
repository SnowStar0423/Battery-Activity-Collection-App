package com.batt.data.service;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.app.Service;
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
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import static android.os.Environment.DIRECTORY_DOWNLOADS;
import static com.batt.data.MainActivity.DETECTED_ACTIVITY;

public class BackgroundService extends Service implements LocationListener {

    public Context context = this;
    public Handler handler = null;
    public static Runnable runnable = null;
    private static String TAG = "Background Service -> ";
    private StorageReference storageReference;
    String latitude, longitude, altitude;
    int ACTION_CHARGING, ACTION_DISCHARGING, BATTERY_HEALTH_COLD, BATTERY_HEALTH_DEAD, BATTERY_HEALTH_GOOD, BATTERY_HEALTH_OVERHEAT, BATTERY_HEALTH_OVER_VOLTAGE,
        BATTERY_HEALTH_UNKNOWN, BATTERY_HEALTH_UNSPECIFIED_FAILURE, BATTERY_PLUGGED_AC, BATTERY_PLUGGED_USB, BATTERY_PLUGGED_WIRELESS, BATTERY_PROPERTY_CAPACITY,
        BATTERY_PROPERTY_CHARGE_COUNTER, BATTERY_PROPERTY_STATUS, BATTERY_STATUS_CHARGING, BATTERY_STATUS_DISCHARGING,
        BATTERY_STATUS_FULL, BATTERY_STATUS_NOT_CHARGING, BATTERY_STATUS_UNKNOWN,  EXTRA_LEVEL, EXTRA_SCALE, EXTRA_VOLTAGE, EXTRA_PLUGGED, EXTRA_STATUS, EXTRA_HEALTH,
        EXTRA_BATTERY_LOW, EXTRA_ICON_SMALL,
            intProperty, IN_VEHICLE, ON_BICYCLE, ON_FOOT, RUNNING, STILL, TILTING, UNKNOWN, WALKING;
    long chargingTime, longProperty;
    float BATTERY_PROPERTY_CURRENT_NOW, EXTRA_TEMPERATURE;
    boolean _isCharging;
    protected LocationManager locationManager;
    SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private String userID, currentDate;
    private ActivityRecognitionClient mActivityRecognitionClient;
    ArrayList<DetectedActivity> temporaryList = new ArrayList<>();

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @SuppressLint("CommitPrefEdits")
    @Override
    public void onCreate() {

        prefs = getSharedPreferences("info", MODE_PRIVATE);
        editor = prefs.edit();
        userID = prefs.getString("userID", "tb1BrFsDa6bq3zomteJejOxmBJg2");
        storageReference = FirebaseStorage.getInstance().getReference();
        mActivityRecognitionClient = new ActivityRecognitionClient(this);
        /* GPS Location Initialize*/
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




        handler = new Handler();
        runnable = new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            public void run() {
//                Toast.makeText(context, "Service is still running", Toast.LENGTH_LONG).show();

                UpdatesHandler();
                new Handler().postDelayed(new Runnable() {
                    @RequiresApi(api = Build.VERSION_CODES.P)
                    @Override
                    public void run() {
                        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                        Intent batteryInfo = context.registerReceiver(null, intentFilter);



                        ACTION_CHARGING = batteryInfo.getIntExtra(BatteryManager.ACTION_CHARGING, 0);
                        ACTION_DISCHARGING = batteryInfo.getIntExtra(BatteryManager.ACTION_DISCHARGING, 0);
                        BATTERY_HEALTH_COLD = batteryInfo.getIntExtra(String.valueOf(BatteryManager.BATTERY_HEALTH_COLD), 0);
                        BATTERY_HEALTH_DEAD = batteryInfo.getIntExtra(String.valueOf(BatteryManager.BATTERY_HEALTH_DEAD), 0);
                        BATTERY_HEALTH_GOOD = batteryInfo.getIntExtra(String.valueOf(BatteryManager.BATTERY_HEALTH_GOOD), 0);
                        BATTERY_HEALTH_OVERHEAT = batteryInfo.getIntExtra(String.valueOf(BatteryManager.BATTERY_HEALTH_OVERHEAT), 0);
                        BATTERY_HEALTH_OVER_VOLTAGE = batteryInfo.getIntExtra(String.valueOf(BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE), 0);
                        BATTERY_HEALTH_UNKNOWN = batteryInfo.getIntExtra(String.valueOf(BatteryManager.BATTERY_HEALTH_UNKNOWN), 0);
                        BATTERY_HEALTH_UNSPECIFIED_FAILURE = batteryInfo.getIntExtra(String.valueOf(BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE), 0);
                        BATTERY_PLUGGED_AC = batteryInfo.getIntExtra(String.valueOf(BatteryManager.BATTERY_PLUGGED_AC), 0);
                        BATTERY_PLUGGED_USB = batteryInfo.getIntExtra(String.valueOf(BatteryManager.BATTERY_PLUGGED_USB), 0);
                        BATTERY_PLUGGED_WIRELESS = batteryInfo.getIntExtra(String.valueOf(BatteryManager.BATTERY_PLUGGED_WIRELESS), 0);
                        BATTERY_PROPERTY_CAPACITY = batteryInfo.getIntExtra(String.valueOf(BatteryManager.BATTERY_PROPERTY_CAPACITY), 0);
                        BATTERY_PROPERTY_CHARGE_COUNTER = batteryInfo.getIntExtra(String.valueOf(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER), 0);
                        BATTERY_PROPERTY_CURRENT_NOW = (float)batteryInfo.getIntExtra(String.valueOf(BatteryManager.	BATTERY_PROPERTY_CURRENT_NOW), 0);
                        BATTERY_PROPERTY_STATUS = batteryInfo.getIntExtra(String.valueOf(BatteryManager.BATTERY_PROPERTY_STATUS), 0);
                        BATTERY_STATUS_CHARGING = batteryInfo.getIntExtra(String.valueOf(BatteryManager.BATTERY_STATUS_CHARGING), 0);
                        BATTERY_STATUS_DISCHARGING = batteryInfo.getIntExtra(String.valueOf(BatteryManager.	BATTERY_STATUS_DISCHARGING), 0);
                        BATTERY_STATUS_FULL = batteryInfo.getIntExtra(String.valueOf(BatteryManager.BATTERY_STATUS_FULL), 0);
                        BATTERY_STATUS_NOT_CHARGING = batteryInfo.getIntExtra(String.valueOf(BatteryManager.BATTERY_STATUS_NOT_CHARGING), 0);
                        BATTERY_STATUS_UNKNOWN = batteryInfo.getIntExtra(String.valueOf(BatteryManager.BATTERY_STATUS_UNKNOWN), 0);
                        boolean battery_low = batteryInfo.getBooleanExtra(BatteryManager.EXTRA_BATTERY_LOW, false);
                        if (!battery_low) {
                            EXTRA_BATTERY_LOW = 0;
                        } else {
                            EXTRA_BATTERY_LOW = 1;
                        }
                        EXTRA_HEALTH = batteryInfo.getIntExtra(BatteryManager.EXTRA_HEALTH, 0);
                        if (EXTRA_HEALTH == BatteryManager.BATTERY_HEALTH_COLD) {
                            BATTERY_HEALTH_COLD = 1;
                        } else if (EXTRA_HEALTH == BatteryManager.BATTERY_HEALTH_GOOD) {
                            BATTERY_HEALTH_GOOD = 1;
                        } else if (EXTRA_HEALTH == BatteryManager.BATTERY_HEALTH_DEAD) {
                            BATTERY_HEALTH_DEAD = 1;
                        } else if (EXTRA_HEALTH == BatteryManager.BATTERY_HEALTH_OVERHEAT) {
                            BATTERY_HEALTH_OVERHEAT = 1;
                        } else if (EXTRA_HEALTH == BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE) {
                            BATTERY_HEALTH_OVER_VOLTAGE = 1;
                        } else if (EXTRA_HEALTH == BatteryManager.BATTERY_HEALTH_UNKNOWN) {
                            BATTERY_HEALTH_UNKNOWN = 1;
                        } else {
                            BATTERY_HEALTH_UNSPECIFIED_FAILURE = 1;
                        }
                        EXTRA_ICON_SMALL = batteryInfo.getIntExtra(BatteryManager.EXTRA_ICON_SMALL, 0);
                        EXTRA_LEVEL = batteryInfo.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
                        EXTRA_PLUGGED = batteryInfo.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
                        if (EXTRA_PLUGGED == BatteryManager.BATTERY_PLUGGED_AC) {
                            BATTERY_PLUGGED_AC = 1;
                        } else if (EXTRA_PLUGGED == BatteryManager.BATTERY_PLUGGED_USB) {
                            BATTERY_PLUGGED_USB = 1;
                        } else {
                            BATTERY_PLUGGED_WIRELESS = 1;
                        }
                        EXTRA_SCALE = batteryInfo.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                        EXTRA_STATUS = batteryInfo.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                        if (EXTRA_STATUS == BatteryManager.BATTERY_STATUS_CHARGING) {
                            BATTERY_STATUS_CHARGING = 1;
                        } else if (EXTRA_STATUS == BatteryManager.BATTERY_STATUS_DISCHARGING) {
                            BATTERY_STATUS_DISCHARGING = 1;
                        } else if (EXTRA_STATUS == BatteryManager.BATTERY_STATUS_FULL) {
                            BATTERY_STATUS_FULL = 1;
                        } else if (EXTRA_STATUS == BatteryManager.BATTERY_STATUS_NOT_CHARGING) {
                            BATTERY_STATUS_NOT_CHARGING = 1;
                        } else {
                            BATTERY_STATUS_UNKNOWN = 1;
                        }
                        EXTRA_TEMPERATURE = (float)((batteryInfo.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0))/10);
                        EXTRA_VOLTAGE = batteryInfo.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);

                        if (EXTRA_STATUS == BatteryManager.BATTERY_STATUS_CHARGING || EXTRA_STATUS == BatteryManager.BATTERY_STATUS_FULL) {
                            ACTION_CHARGING = 1;
                        } else {
                            ACTION_DISCHARGING = 1;
                        }



                        Log.e("CURRENT_NOW", String.valueOf(BATTERY_PROPERTY_CURRENT_NOW));
                        Log.e("EXTRA_TEMPERATURE", String.valueOf(EXTRA_TEMPERATURE));

                        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
                        LocalDateTime now = LocalDateTime.now();
                        currentDate = dtf.format(now);

                        if (userID != null) {
                            try {
                                saveExcelFile();
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }, 10000);

                //43200000
                handler.postDelayed(runnable, 1200000);
            }
        };
        handler.postDelayed(runnable, 15000);
    }

    @Override
    public void onDestroy() {
        /* IF YOU WANT THIS SERVICE KILLED WITH THE APP THEN UNCOMMENT THE FOLLOWING LINE */
        Log.e("Service: ", "Stopped");
//        handler.removeCallbacks(runnable);
//        Toast.makeText(this, "Service stopped", Toast.LENGTH_SHORT).show();
//        stopSelf();
//        super.onDestroy();
    }

    @Override
    public void onStart(Intent intent, int startid) {
        Toast.makeText(this, "Service started by user.", Toast.LENGTH_SHORT).show();
    }


    public void saveExcelFile() throws IOException {
        String path;
        File dir;
        if (!isExternalStorageAvailable() || isExternalStorageReadOnly()) {
            Log.e("Failed", "Storage not available or read only");
            return;
        }
        boolean success = false;


        path = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS).getAbsolutePath()+"/";
        dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(path, "myExcel.csv");
        FileInputStream inputStream = null;

        //New Workbook
        Workbook wb = new HSSFWorkbook();
        Sheet sheet1 = null;
        Cell c = null;

        // Generate column headings
        Row row = null;
        int k = prefs.getInt("rowNumber", 0);
        if (k == 0 || !file.exists()) {
            sheet1 = wb.createSheet("myData");
            row = sheet1.createRow(0);

            for(int j = 0; j < info_lists.length; j++){
                c = row.createCell(j);
                c.setCellValue(info_lists[j]);
                sheet1.setColumnWidth(j, (15 * 300));
            }
            row = sheet1.createRow(k+1);
            c = row.createCell(0);
            c.setCellValue(currentDate);
            c = row.createCell(1);
            c.setCellValue(ACTION_CHARGING);
            c = row.createCell(2);
            c.setCellValue(ACTION_DISCHARGING);
            c = row.createCell(3);
            c.setCellValue(BATTERY_HEALTH_COLD);
            c = row.createCell(4);
            c.setCellValue(BATTERY_HEALTH_DEAD);
            c = row.createCell(5);
            c.setCellValue(BATTERY_HEALTH_GOOD);
            c = row.createCell(6);
            c.setCellValue(BATTERY_HEALTH_OVERHEAT);
            c = row.createCell(7);
            c.setCellValue(BATTERY_HEALTH_OVER_VOLTAGE);
            c = row.createCell(8);
            c.setCellValue(BATTERY_HEALTH_UNKNOWN);
            c = row.createCell(9);
            c.setCellValue(BATTERY_HEALTH_UNSPECIFIED_FAILURE);
            c = row.createCell(10);
            c.setCellValue(BATTERY_PLUGGED_AC);
            c = row.createCell(11);
            c.setCellValue(BATTERY_PLUGGED_USB);
            c = row.createCell(12);
            c.setCellValue(BATTERY_PLUGGED_WIRELESS);
            c = row.createCell(13);
            c.setCellValue(BATTERY_PROPERTY_CAPACITY);
            c = row.createCell(14);
            c.setCellValue(BATTERY_PROPERTY_CHARGE_COUNTER);
            c = row.createCell(15);
            c.setCellValue(BATTERY_PROPERTY_CURRENT_NOW);
            c = row.createCell(16);
            c.setCellValue(BATTERY_PROPERTY_STATUS);
            c = row.createCell(17);
            c.setCellValue(BATTERY_STATUS_CHARGING);
            c = row.createCell(18);
            c.setCellValue(BATTERY_STATUS_DISCHARGING);
            c = row.createCell(19);
            c.setCellValue(BATTERY_STATUS_FULL);
            c = row.createCell(20);
            c.setCellValue(BATTERY_STATUS_NOT_CHARGING);
            c = row.createCell(21);
            c.setCellValue(BATTERY_STATUS_UNKNOWN);
            c = row.createCell(22);
            c.setCellValue(EXTRA_BATTERY_LOW);
            c = row.createCell(23);
            c.setCellValue(EXTRA_HEALTH);
            c = row.createCell(24);
            c.setCellValue(EXTRA_ICON_SMALL);
            c = row.createCell(25);
            c.setCellValue(EXTRA_LEVEL);
            c = row.createCell(26);
            c.setCellValue(EXTRA_PLUGGED);
            c = row.createCell(27);
            c.setCellValue(EXTRA_SCALE);
            c = row.createCell(28);
            c.setCellValue(EXTRA_STATUS);
            c = row.createCell(29);
            c.setCellValue(EXTRA_TEMPERATURE);
            c = row.createCell(30);
            c.setCellValue(EXTRA_VOLTAGE);
            c = row.createCell(31);
            c.setCellValue(temporaryList.get(0).getConfidence());
            c = row.createCell(32);
            c.setCellValue(temporaryList.get(1).getConfidence());
            c = row.createCell(33);
            c.setCellValue(temporaryList.get(2).getConfidence());
            c = row.createCell(34);
            c.setCellValue(temporaryList.get(3).getConfidence());
            c = row.createCell(35);
            c.setCellValue(temporaryList.get(4).getConfidence());
            c = row.createCell(36);
            c.setCellValue(temporaryList.get(5).getConfidence());
            c = row.createCell(37);
            c.setCellValue(temporaryList.get(6).getConfidence());
            c = row.createCell(38);
            c.setCellValue(temporaryList.get(7).getConfidence());
            c = row.createCell(39);
            c.setCellValue(latitude);
            c = row.createCell(40);
            c.setCellValue(longitude);
            c = row.createCell(41);
            c.setCellValue(altitude);

        } else  {
            inputStream = new FileInputStream(file);
            wb = new HSSFWorkbook(inputStream);
            sheet1 = wb.getSheet("myData");
            row = sheet1.createRow(k+1);
            c = row.createCell(0);
            c.setCellValue(currentDate);
            c = row.createCell(1);
            c.setCellValue(ACTION_CHARGING);
            c = row.createCell(2);
            c.setCellValue(ACTION_DISCHARGING);
            c = row.createCell(3);
            c.setCellValue(BATTERY_HEALTH_COLD);
            c = row.createCell(4);
            c.setCellValue(BATTERY_HEALTH_DEAD);
            c = row.createCell(5);
            c.setCellValue(BATTERY_HEALTH_GOOD);
            c = row.createCell(6);
            c.setCellValue(BATTERY_HEALTH_OVERHEAT);
            c = row.createCell(7);
            c.setCellValue(BATTERY_HEALTH_OVER_VOLTAGE);
            c = row.createCell(8);
            c.setCellValue(BATTERY_HEALTH_UNKNOWN);
            c = row.createCell(9);
            c.setCellValue(BATTERY_HEALTH_UNSPECIFIED_FAILURE);
            c = row.createCell(10);
            c.setCellValue(BATTERY_PLUGGED_AC);
            c = row.createCell(11);
            c.setCellValue(BATTERY_PLUGGED_USB);
            c = row.createCell(12);
            c.setCellValue(BATTERY_PLUGGED_WIRELESS);
            c = row.createCell(13);
            c.setCellValue(BATTERY_PROPERTY_CAPACITY);
            c = row.createCell(14);
            c.setCellValue(BATTERY_PROPERTY_CHARGE_COUNTER);
            c = row.createCell(15);
            c.setCellValue(BATTERY_PROPERTY_CURRENT_NOW);
            c = row.createCell(16);
            c.setCellValue(BATTERY_PROPERTY_STATUS);
            c = row.createCell(17);
            c.setCellValue(BATTERY_STATUS_CHARGING);
            c = row.createCell(18);
            c.setCellValue(BATTERY_STATUS_DISCHARGING);
            c = row.createCell(19);
            c.setCellValue(BATTERY_STATUS_FULL);
            c = row.createCell(20);
            c.setCellValue(BATTERY_STATUS_NOT_CHARGING);
            c = row.createCell(21);
            c.setCellValue(BATTERY_STATUS_UNKNOWN);
            c = row.createCell(22);
            c.setCellValue(EXTRA_BATTERY_LOW);
            c = row.createCell(23);
            c.setCellValue(EXTRA_HEALTH);
            c = row.createCell(24);
            c.setCellValue(EXTRA_ICON_SMALL);
            c = row.createCell(25);
            c.setCellValue(EXTRA_LEVEL);
            c = row.createCell(26);
            c.setCellValue(EXTRA_PLUGGED);
            c = row.createCell(27);
            c.setCellValue(EXTRA_SCALE);
            c = row.createCell(28);
            c.setCellValue(EXTRA_STATUS);
            c = row.createCell(29);
            c.setCellValue(EXTRA_TEMPERATURE);
            c = row.createCell(30);
            c.setCellValue(EXTRA_VOLTAGE);
            c = row.createCell(31);
            c.setCellValue(temporaryList.get(0).getConfidence());
            c = row.createCell(32);
            c.setCellValue(temporaryList.get(1).getConfidence());
            c = row.createCell(33);
            c.setCellValue(temporaryList.get(2).getConfidence());
            c = row.createCell(34);
            c.setCellValue(temporaryList.get(3).getConfidence());
            c = row.createCell(35);
            c.setCellValue(temporaryList.get(4).getConfidence());
            c = row.createCell(36);
            c.setCellValue(temporaryList.get(5).getConfidence());
            c = row.createCell(37);
            c.setCellValue(temporaryList.get(6).getConfidence());
            c = row.createCell(38);
            c.setCellValue(temporaryList.get(7).getConfidence());
            c = row.createCell(39);
            c.setCellValue(latitude);
            c = row.createCell(40);
            c.setCellValue(longitude);
            c = row.createCell(41);
            c.setCellValue(altitude);


            inputStream.close();
        }
        editor.putInt("rowNumber", k + 1 ).apply();

        FileOutputStream outputStream = null;

        try {
            outputStream = new FileOutputStream(file);
            wb.write(outputStream);
            wb.close();
            outputStream.close();
            Log.w("FileUtils", "Writing file" + file +  ":" + UUID.randomUUID().toString());
            StorageReference ref = storageReference.child("excel/" + userID + "/myExcel.csv");
            // adding listeners on upload
            // or failure of image
            ref.putFile(Uri.fromFile(file))
                    .addOnSuccessListener(
                            new OnSuccessListener<UploadTask.TaskSnapshot>() {

                                @Override
                                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot)
                                {
                                    Log.e("File Upload", "done");
                                    Toast.makeText(getApplicationContext(), "The data has been successfully uploaded to storage", Toast.LENGTH_SHORT).show();
                                }
                            })

                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e)
                        {
                            Toast.makeText(getApplicationContext(), "Failed " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnProgressListener(
                            new OnProgressListener<UploadTask.TaskSnapshot>() {

                                // Progress Listener for loading
                                // percentage on the dialog box
                                @Override
                                public void onProgress(
                                        UploadTask.TaskSnapshot taskSnapshot)
                                {
                                    double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
//                                    progressDialog.setMessage("Uploaded "  + (int)progress + "%");
                                }
                            });
            success = true;
        } catch (IOException e) {
            Log.w("FileUtils", "Error writing " + file, e);
        } catch (Exception e) {
            Log.w("FileUtils", "Failed to save file", e);
        } finally {
            try {
                if (null != outputStream)
                    Log.e("file created", "success");
//                    ou.close();
            } catch (Exception ex) {
            }
        }
        return;
    }

    public static boolean isExternalStorageReadOnly() {
        String extStorageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(extStorageState)) {
            return true;
        }
        return false;
    }

    public static boolean isExternalStorageAvailable() {
        String extStorageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(extStorageState)) {
            return true;
        }
        return false;
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        latitude = String.valueOf(location.getLatitude());
        longitude = String.valueOf(location.getLongitude());
        altitude = String.valueOf(location.getAltitude());
    }

    public void UpdatesHandler() {
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
        temporaryList.clear();
        ArrayList<DetectedActivity> detectedActivities = ActivityIntentService.detectedActivitiesFromJson(
                PreferenceManager.getDefaultSharedPreferences(context)
                        .getString(DETECTED_ACTIVITY, "")
        );

        HashMap<Integer, Integer> detectedActivitiesMap = new HashMap<>();
        for (DetectedActivity activity : detectedActivities) {
            detectedActivitiesMap.put(activity.getType(), activity.getConfidence());
        }


        for (int i = 0; i < ActivityIntentService.POSSIBLE_ACTIVITIES.length; i++) {
            int confidence = detectedActivitiesMap.containsKey(ActivityIntentService.POSSIBLE_ACTIVITIES[i]) ?
                    detectedActivitiesMap.get(ActivityIntentService.POSSIBLE_ACTIVITIES[i]) : 0;

            //Add the object to a temporaryList//
            temporaryList.add(new
                    DetectedActivity(ActivityIntentService.POSSIBLE_ACTIVITIES[i],
                    confidence));
        }
        Log.e("ddddddddddd ", (String.valueOf(temporaryList)));
    }

    public static String[] info_lists = {
            "Date",
            "ACTION_CHARGING",
            "ACTION_DISCHARGING",
            "BATTERY_HEALTH_COLD",
            "BATTERY_HEALTH_DEAD",
            "BATTERY_HEALTH_GOOD",
            "BATTERY_HEALTH_OVERHEAT",
            "BATTERY_HEALTH_OVER_VOLTAGE",
            "BATTERY_HEALTH_UNKNOWN",
            "BATTERY_HEALTH_UNSPECIFIED",
            "BATTERY_PLUGGED_AC",
            "BATTERY_PLUGGED_USB",
            "BATTERY_PLUGGED_WIRELESS",
            "BATTERY_PROPERTY_CAPACITY",
            "BATTERY_PROPERTY_CHARGE_COUNTER",
            "BATTERY_PROPERTY_CURRENT_NOW",
            "BATTERY_PROPERTY_STATUS",
            "BATTERY_STATUS_CHARGING",
            "BATTERY_STATUS_DISCHARGING",
            "BATTERY_STATUS_FULL",
            "BATTERY_STATUS_NOT_CHARGING",
            "BATTERY_STATUS_UNKNOWN",
            "EXTRA_BATTERY_LOW",
            "EXTRA_HEALTH",
            "EXTRA_ICON_SMALL",
            "EXTRA_LEVEL",
            "EXTRA_PLUGGED",
            "EXTRA_SCALE",
            "EXTRA_STATUS",
            "EXTRA_TEMPERATURE",
            "EXTRA_VOLTAGE",
            "IN_VEHICLE",
            "ON_BICYCLE",
            "ON_FOOT",
            "RUNNING",
            "STILL",
            "TILTING",
            "UNKNOWN",
            "WALKING",
            "Longitude",
            "Latitude",
            "Altitude"
    };

    public static String[] POSSIBLE_ACTIVITIES = {
            "STILL",
            "ON_FOOT",
            "WALKING",
            "RUNNING",
            "IN_VEHICLE",
            "ON_BICYCLE",
            "TILTING",
            "UNKNOWN"
    };
}