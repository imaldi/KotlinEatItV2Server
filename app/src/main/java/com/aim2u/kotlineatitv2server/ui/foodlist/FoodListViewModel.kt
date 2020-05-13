package com.aim2u.kotlineatitv2server.ui.foodlist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.aim2u.kotlineatitv2server.common.Common
import com.aim2u.kotlineatitv2server.model.FoodModel

class FoodListViewModel : ViewModel() {

    private var mutableFoodModelListData: MutableLiveData<List<FoodModel>>?= null

    fun getMutableFoodModelListData():MutableLiveData<List<FoodModel>>{
        if(mutableFoodModelListData == null)
            mutableFoodModelListData = MutableLiveData()

        mutableFoodModelListData!!.value = Common.categorySelected!!.foods
        return mutableFoodModelListData!!
    }
}