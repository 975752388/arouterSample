package com.key.annotation.model

import javax.lang.model.element.Element

class ArouterBean {
    var type: Type?=null
    var element:Element?=null
    var clazz:Class<*>?=null
    var group:String?=""
    var path:String?=null


    companion object{
        fun create(type: Type, clazz: Class<*>, group:String, path:String): ArouterBean {
            return ArouterBean().apply {
                this.type =type
                this.clazz = clazz
                this.group = group
                this.path = path
            }
        }
    }

    override fun toString(): String {
        return "ArouterBean(type=$type, element=$element, clazz=$clazz, group=$group, path=$path)"
    }

}