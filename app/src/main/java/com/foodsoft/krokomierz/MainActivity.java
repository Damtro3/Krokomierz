package com.foodsoft.krokomierz;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInOptionsExtension;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Value;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.request.DataSourcesRequest;
import com.google.android.gms.fitness.request.OnDataPointListener;
import com.google.android.gms.fitness.request.SensorRequest;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    int GOOGLE_FIT_PERMISSIONS_REQUEST_CODE;
    private final int REQUEST_ID_MULTIPLE_PERMISSIONS = 1;
    private int mySteps=0;
    ProgressBar progressBar;
    TextView stepsCounter;
    private CountDownTimer readDailyStepsTimer;
    private GoogleSignInOptionsExtension fitnessOptions;
    private GoogleSignInAccount account;
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkAndRequestPermissions();
        initialize();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void initialize()
    {
        stepsCounter = findViewById(R.id.steps);
        progressBar = findViewById(R.id.progressBar);
        stepsCounter.setText("0 / 10000 kroków");
         fitnessOptions = FitnessOptions.builder()
                .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_WRITE)
                .build();
         account = GoogleSignIn.getAccountForExtension(this, fitnessOptions);
         signIn();

    }
    private void signIn()
    {
        if (!GoogleSignIn.hasPermissions(account, fitnessOptions)) {
            GoogleSignIn.requestPermissions(
                    this, // your activity
                    GOOGLE_FIT_PERMISSIONS_REQUEST_CODE, // e.g. 1
                    account,
                    fitnessOptions);
        } else {
            startReadDailyStepsTimer( account);
        }
    }
    private void startReadDailyStepsTimer( GoogleSignInAccount account) {

        Long time = 45000l;
        Long period = 5000L;
        readDailyStepsTimer= new CountDownTimer(time, period) {

            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onTick(long millisUntilFinished) {
                readDailySteps( account);
            }

            @Override
            public void onFinish() {
                start();
            }
        };
        readDailyStepsTimer.start();

    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void readDailySteps(  GoogleSignInAccount account) {
        Fitness.getHistoryClient(this, account)
                .readDailyTotal(DataType.TYPE_STEP_COUNT_DELTA)
                .addOnSuccessListener(new OnSuccessListener() {
                    @Override
                    public void onSuccess(Object o ){
                        if(o instanceof  DataSet)
                        {
                            DataSet data= (DataSet) o;
                            if(data !=null && data.getDataPoints().size()>0)
                            {
                                List<DataPoint> steps = data.getDataPoints();
                                DataPoint point = steps.get(0);
                                Value val = point.getValue(Field.FIELD_STEPS);
                                mySteps = val.asInt();
                                stepsCounter.setText(String.valueOf(mySteps)+" / 10000 kroków");

                                progressBar.setProgress(myStepsValueFormater(mySteps));
                                progressBar.setMax(10000);
                                progressBar.invalidate();
                            }
                        }
                        Log.i("Success", "OnSuccess()");
                    }})
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.i("Failure", "OnFailure()");
                    }});
    }

    private int myStepsValueFormater(int mySteps)
    {
        if(mySteps>10000 && mySteps<20000)
            mySteps=mySteps-10000;
        else if(mySteps>20000 && mySteps<30000)
            mySteps=mySteps-20000;
        else if(mySteps>30000)
            mySteps=mySteps-30000;

        return mySteps;
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        GoogleSignInAccount account = GoogleSignIn.getAccountForExtension(this, fitnessOptions);
        if(requestCode==Activity.RESULT_OK)
        {
            GOOGLE_FIT_PERMISSIONS_REQUEST_CODE=requestCode;
          readDailySteps(account);
        }
        else
        {
            signIn();
        }
    }
    private  boolean checkAndRequestPermissions() {

        int writeToStorage = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        int readPhoneState = ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION);

        List listPermissionsNeeded = new ArrayList<>();

        if (writeToStorage != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (readPhoneState != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.ACTIVITY_RECOGNITION);
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    (String[]) listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),
                    REQUEST_ID_MULTIPLE_PERMISSIONS);
            return false;
        }
        return true;
    }
}