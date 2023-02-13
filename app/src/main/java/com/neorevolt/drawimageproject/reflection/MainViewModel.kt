package com.neorevolt.drawimageproject.reflection

import android.app.Application
import android.util.Log

const val PROVIDER_CLASS = "com.neorevolt.drawimage.DrawImageFeatureImpl\$Provider"

class MainViewModel(app: Application) : AbstractMainViewModel(app) {
    override fun initializeDrawImageFeature() {

        val dependencies = object : DrawImageFeature.Dependencies {
            //Add some Dependencies that DFM need (if available)
            //Note (NeoRevolt) : Currently unavailable
        }

        val drawImageProvider = Class.forName(PROVIDER_CLASS).kotlin.objectInstance as DrawImageFeature.Provider

        drawImageModule = drawImageProvider.get(dependencies)
        Log.d(null, "Loaded Draw Image feature via reflection")
    }

}