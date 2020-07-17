package com.key.personal;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.key.annotation.Autowired;
import com.key.annotation.Router;
import com.key.aroutercore.Navigator;



@Router(path = "/personal/PersonalActivity")
public class PersonalActivity extends AppCompatActivity {
    @Autowired
    String name;
    private FrameLayout container;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal);
        Navigator.INSTANCE.inject(this);
        TextView tv = findViewById(R.id.tv);
        container = findViewById(R.id.container);
        tv.setText("personal:name="+name);
        showFragment();

        tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Navigator.INSTANCE.build("/login/LoginActivity").withString("userAccount","123456").navigate(PersonalActivity.this);
            }
        });
    }
    public void showFragment(){
        Fragment fragment = (Fragment) Navigator.INSTANCE.build("/login/MyFragment").navigate(this);
//        Fragment fragment = TestFragment.getInstance();
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = manager.beginTransaction();
        fragmentTransaction.add(R.id.container,fragment);
        fragmentTransaction.commit();

    }
}