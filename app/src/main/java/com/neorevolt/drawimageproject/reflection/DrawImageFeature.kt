package com.neorevolt.drawimageproject.reflection

interface DrawImageFeature {

    fun setData(refData: String)
    fun loadData(): String


    /**
     * DrawImageFeature can be instantiated in whatever way the implementer chooses,
     * we just want to have a simple method to get() an instance of it.
     */
    interface Provider {
        fun get(dependencies: Dependencies): DrawImageFeature
    }

    /**
     * Dependencies from the main app module that are required by the DrawImageFeature.
     */
    interface Dependencies {
        //Add some Dependencies that DFM need (if available)
        //Note (NeoRevolt) : Currently unavailable
    }
}