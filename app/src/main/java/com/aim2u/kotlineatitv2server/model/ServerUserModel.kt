package com.aim2u.kotlineatitv2server.model

data class ServerUserModel(
    var uid:String?=null,
    var name:String?=null,
    var phone:String?=null,
    var isActive:Boolean?=false
)