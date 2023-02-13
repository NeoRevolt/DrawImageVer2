package com.neorevolt.drawimageproject.reflection

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.neorevolt.drawimageproject.R

private const val DRAWIMAGE_MODULE = "module_drawimage"

abstract class AbstractMainViewModel(app: Application) : AndroidViewModel(app) {

    private val manager = SplitInstallManagerFactory.create(getApplication())

    var drawImageModule: DrawImageFeature? = null

    private val _refData = MutableLiveData<String>()
    val refData: LiveData<String> = _refData

    private fun isDrawImageInstalled() =
        manager.installedModules.contains(DRAWIMAGE_MODULE)

    fun setReflectionData(msg: String) {
        initializeDrawImageFeature()
        drawImageModule?.setData(msg)
        Toast.makeText(getApplication(), "Data has ben set to Feature", Toast.LENGTH_SHORT).show()
    }

    fun loadReflectionData(){
        if (isDrawImageInstalled()){
            initializeDrawImageFeature()
        }
        _refData.value = drawImageModule?.loadData() ?: "No Data Loaded"
    }

    protected abstract fun initializeDrawImageFeature()
}
