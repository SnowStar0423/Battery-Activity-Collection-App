package com.batt.data.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.util.Log;

import com.batt.data.MainActivity;
import com.batt.data.R;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class ActivityIntentService extends IntentService {
    private Context mContext;
    protected static final String TAG = "Activity Recognition";
    //Call the super IntentService constructor with the name for the worker thread//
    public ActivityIntentService() {
        super(TAG);
    }
    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
    //Check whether the Intent contains activity recognition data//
        if (ActivityRecognitionResult.hasResult(intent)) {

    //If data is available, then extract the ActivityRecognitionResult from the Intent//
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);

    //Get an array of DetectedActivity objects//
            ArrayList<DetectedActivity> detectedActivities = (ArrayList) result.getProbableActivities();
            PreferenceManager.getDefaultSharedPreferences(this)
                    .edit()
                    .putString(MainActivity.DETECTED_ACTIVITY,
                            detectedActivitiesToJson(detectedActivities))
                    .apply();
            Log.e("Detected Activity: ", result.getProbableActivities().toString());

        }
    }

    static String getActivityString(Context context, int detectedActivityType) {
        Resources resources = context.getResources();
        switch(detectedActivityType) {
            case DetectedActivity.ON_BICYCLE:
                return resources.getString(R.string.bicycle);
            case DetectedActivity.ON_FOOT:
                return resources.getString(R.string.foot);
            case DetectedActivity.RUNNING:
                return resources.getString(R.string.running);
            case DetectedActivity.STILL:
                return resources.getString(R.string.still);
            case DetectedActivity.TILTING:
                return resources.getString(R.string.tilting);
            case DetectedActivity.WALKING:
                return resources.getString(R.string.walking);
            case DetectedActivity.IN_VEHICLE:
                return resources.getString(R.string.vehicle);
            default:
                return resources.getString(R.string.unknown_activity);
        }
    }
    public static int[] POSSIBLE_ACTIVITIES = {
            DetectedActivity.IN_VEHICLE,
            DetectedActivity.ON_BICYCLE,
            DetectedActivity.ON_FOOT,
            DetectedActivity.RUNNING,
            DetectedActivity.STILL,
            DetectedActivity.TILTING,
            DetectedActivity.UNKNOWN,
            DetectedActivity.WALKING
    };
    static String detectedActivitiesToJson(ArrayList<DetectedActivity> detectedActivitiesList) {
        Type type = new TypeToken<ArrayList<DetectedActivity>>() {}.getType();
        return new Gson().toJson(detectedActivitiesList, type);
    }
    public static ArrayList<DetectedActivity> detectedActivitiesFromJson(String jsonArray) {
        Type listType = new TypeToken<ArrayList<DetectedActivity>>(){}.getType();
        ArrayList<DetectedActivity> detectedActivities = new Gson().fromJson(jsonArray, listType);
        if (detectedActivities == null) {
            detectedActivities = new ArrayList<>();
        }
        return detectedActivities;
    }

}