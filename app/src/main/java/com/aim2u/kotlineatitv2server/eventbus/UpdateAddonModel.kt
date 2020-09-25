package com.aim2u.kotlineatitv2server.eventbus

import com.aim2u.kotlineatitv2server.model.AddonModel

class UpdateAddonModel {
    var addonModelList: List<AddonModel>? = null
    constructor()
    constructor(addonModelList: List<AddonModel>?){
        this.addonModelList = addonModelList
    }
}
