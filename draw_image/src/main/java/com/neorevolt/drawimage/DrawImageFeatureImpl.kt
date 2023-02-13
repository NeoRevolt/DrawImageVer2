package com.neorevolt.drawimage

import android.util.Log
import com.neorevolt.drawimageproject.reflection.DrawImageFeature

class DrawImageFeatureImpl(): DrawImageFeature{

    private val showDataActivity = ShowDataActivity()

    override fun setData(refData: String) {
        showDataActivity.setReflectionData(refData)
        Log.d(null, "PESAN DARI BASE :  $refData")
    }

    override fun loadData(): String {
        Log.d(null, "PESAN DARI DFM : ${showDataActivity.loadData()}")
        return showDataActivity.loadData()
    }

    companion object Provider: DrawImageFeature.Provider{
        override fun get(dependencies: DrawImageFeature.Dependencies): DrawImageFeature {
            return DrawImageFeatureImpl()
        }

    }
}