package com.key.login;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

import com.key.annotation.Autowired;
import com.key.annotation.Router;
import com.key.aroutercore.Navigator;

@Router(path = "/login/LoginActivity")
public class LoginActivity extends AppCompatActivity {
    @Autowired
    String userAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Navigator.INSTANCE.inject(this);
        TextView textView = findViewById(R.id.textView);
        textView.setText("login:account = "+userAccount);
        
    }
}