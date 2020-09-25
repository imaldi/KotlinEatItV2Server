package com.aim2u.kotlineatitv2server.eventbus

import com.aim2u.kotlineatitv2server.model.SizeModel

class UpdateSizeModel {
    var sizeModelList: List<SizeModel>? = null
    constructor()
    constructor(sizeModelList: List<SizeModel>?){
        this.sizeModelList = sizeModelList
    }

}
