package com.aim2u.kotlineatitv2server.callback

import com.aim2u.kotlineatitv2server.model.CategoryModel

interface ICategoryCallbackListener {
    fun onCategoryLoadSuccess(categoriesList:List<CategoryModel>)
    fun onCategoryLoadFailed(message:String)
}