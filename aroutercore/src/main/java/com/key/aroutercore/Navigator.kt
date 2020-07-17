package com.key.aroutercore

import android.content.Context
import android.content.Intent
import android.util.Log
import android.util.LruCache
import com.key.annotation.model.Type
import java.lang.Exception
import java.lang.IllegalArgumentException
import java.lang.NullPointerException


object Navigator {

    private val groupCache = LruCache<String, ArouterGroupLoad>(100)
    private val pathCache = LruCache<String, ArouterPathLoad>(100)
    private var group = ""
    private var path = ""
    private val pathSufix = "ARouter\$\$Group\$\$"
    fun build(path:String?):BundleManager{
        checkNotNull(path){
            throw NullPointerException("path is empty")
        }
        if (!path.startsWith("/")){
            throw IllegalArgumentException("path should start with /,for example:/app/MainActivity")
        }
        val finalGroup  = path.substring(1,path.indexOf("/",1))
        if (finalGroup.isNullOrEmpty()){
            throw IllegalArgumentException("path error,for example:/app/MainActivity")
        }
        this.group = finalGroup
        this.path = path
        return BundleManager()

    }
    fun inject(target: Any?){
        checkNotNull(target){
            "target is null"
        }
        try {
            val clazz = Class.forName(target.javaClass.name+"\$\$Autowired").newInstance()

            if (clazz is IAutowired){
                val autowired = clazz as IAutowired
                autowired.loadParameter(target)
            }
        }catch (e:Exception){
            Log.e("==","error:$e")
        }

    }


   internal fun navigate(context:Context, bundleManager: BundleManager):Any?{

        val groupClassName = "${context.packageName}.apt.${pathSufix}$group"
        try {
            var groupLoad = groupCache[group]
            if (groupLoad==null){
                val clazz = Class.forName(groupClassName)
                groupLoad = clazz.newInstance() as ArouterGroupLoad
                groupCache.put(groupClassName,groupLoad)
            }
            if (groupLoad.loadGroup().isNullOrEmpty()){
                throw IllegalArgumentException("router error")
            }

            var pathLoad = pathCache[path]

            if (pathLoad ==null){
                val pathLoadClazz = groupLoad.loadGroup()[group] ?: throw IllegalArgumentException("router error")
                pathLoad = pathLoadClazz!!.newInstance() as ArouterPathLoad
                val routerBean = pathLoad.loadPath()[path]

                if (routerBean?.type == Type.ACTIVITY){
                    context.startActivity(Intent(context,routerBean?.clazz).putExtras(bundleManager.bundle))
                }else if (routerBean?.type == Type.FRAGMENT){
                    val fragmentClazz = routerBean?.clazz?:throw NullPointerException("fragment is null")
                    val constructor = fragmentClazz.getDeclaredConstructor()
                    constructor.isAccessible = true
                    return constructor.newInstance()
                }else{
                    //扩展
                }

            }

        }catch (e:Exception){
            Log.e("==","navigation error:${e}")
        }
        return null
    }

}