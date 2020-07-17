package com.key.aroutersample

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.key.aroutercore.Navigator
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tv.setOnClickListener {

//            val bundle = Bundle()
////            bundle.putString("name","key")
////            bundle.putInt("age",26)
////            startActivity(Intent(this,TestActivity::class.java).putExtras(bundle))
            Navigator.build("/personal/PersonalActivity").withString("name","key").navigate(this)

        }
    }
}