package com.key.aroutersample;

import android.os.Bundle;
import android.os.PersistableBundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.key.annotation.Autowired;
import com.key.annotation.Router;
import com.key.aroutercore.Navigator;
import com.key.aroutersample.TestActivity$$Autowired;


//@Router(path="/app/TestActivity")
public class TestActivity extends AppCompatActivity {
    @Autowired
    String name;

    @Autowired
    int age;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Navigator.INSTANCE.inject(this);

        Log.e("==","name="+name+"age="+age);


    }

    private void test(){


    }

}
