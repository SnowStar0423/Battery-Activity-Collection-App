package com.batt.data;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;


public class LoginActivity extends AppCompatActivity {
    EditText inputEmail, inputPassword;
    TextView register;
    Button login_btn;
    ProgressBar progressBar;
    private FirebaseAuth auth;
    SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    String userID;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        if (!Settings.canDrawOverlays(getApplicationContext())) {
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION));
            Toast.makeText(this, "app is been restarting now", Toast.LENGTH_SHORT).show();
        }

        prefs = getSharedPreferences("info", MODE_PRIVATE);
        editor = prefs.edit();
        requestPermission();

        auth = FirebaseAuth.getInstance();
        authUser();

        inputEmail = findViewById(R.id.email);
        inputPassword = findViewById(R.id.password);
        login_btn = findViewById(R.id.login_btn);
        progressBar = findViewById(R.id.progressBar);
        register = findViewById(R.id.register);

        login_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = inputEmail.getText().toString();
                final String password = inputPassword.getText().toString();

                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(getApplicationContext(), "Enter email address!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(password)) {
                    Toast.makeText(getApplicationContext(), "Enter password!", Toast.LENGTH_SHORT).show();
                    return;
                }

                progressBar.setVisibility(View.VISIBLE);

                auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                progressBar.setVisibility(View.GONE);
                                if (!task.isSuccessful()) {
                                    // there was an error
                                    if (password.length() < 6) {
                                        inputPassword.setError(getString(R.string.minimum_password));
                                    } else {
                                        Toast.makeText(LoginActivity.this, getString(R.string.auth_failed), Toast.LENGTH_LONG).show();
                                    }
                                } else {
                                    editor.putString("userID", auth.getCurrentUser().getUid() ).apply();
                                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                    startActivity(intent);
                                    finish();
                                }
                            }
                        });
            }
        });

        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void authUser() {
        userID = prefs.getString("userID", null);
        if (userID != null) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        }
    }

    private void requestPermission(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if ((ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)  != PackageManager.PERMISSION_GRANTED) &&
                    (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACTIVITY_RECOGNITION)  != PackageManager.PERMISSION_GRANTED) &&
                    (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE)  != PackageManager.PERMISSION_GRANTED) &&
                    (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)  != PackageManager.PERMISSION_GRANTED) &&
                    (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECEIVE_BOOT_COMPLETED)  != PackageManager.PERMISSION_GRANTED) ) {
                requestPermissions(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACTIVITY_RECOGNITION,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.RECEIVE_BOOT_COMPLETED,
                }, 0);
            }
        }
    }
}
