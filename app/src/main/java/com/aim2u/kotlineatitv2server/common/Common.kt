package com.aim2u.kotlineatitv2server.common

import com.aim2u.kotlineatitv2server.model.CategoryModel
import com.aim2u.kotlineatitv2server.model.FoodModel
import com.aim2u.kotlineatitv2server.model.ServerUserModel

object Common {
    var foodSelected: FoodModel?=null
    var categorySelected: CategoryModel?=null
    val CATEGORY_REF: String = "Category"
    val SERVER_REF = "Server"
    var currentServerUser: ServerUserModel?=null
    val FULL_WIDTH_COLUMN: Int = 1
    val DEFAULT_COLUMN_COUNT: Int = 0
}