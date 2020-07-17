package com.key.aroutercore

import android.content.Context
import android.os.Bundle

class BundleManager {
     var bundle = Bundle()
        private set


    fun with(bundle: Bundle):BundleManager{
        this.bundle = bundle
        return this
    }
    fun withString(key:String,value: String):BundleManager{
        bundle.putString(key, value)
        return this
    }
    fun withInt(key:String,value: Int):BundleManager{
        bundle.putInt(key, value)
        return this
    }

    fun navigate(context: Context):Any?{
        return Navigator.navigate(context,this)
    }
}