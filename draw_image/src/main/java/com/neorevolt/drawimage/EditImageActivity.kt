package com.neorevolt.drawimage

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnticipateOvershootInterpolator
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.neorevolt.drawimage.filters.FilterViewAdapter
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.io.File
import java.io.IOException
import java.lang.Exception
import androidx.annotation.RequiresPermission
import androidx.cardview.widget.CardView
import androidx.lifecycle.ViewModelProvider
import com.neorevolt.drawimage.base.BaseActivity
import com.neorevolt.drawimage.burhanrashid52.photoeditor.*
import com.neorevolt.drawimage.burhanrashid52.photoeditor.shape.ShapeBuilder
import com.neorevolt.drawimage.burhanrashid52.photoeditor.shape.ShapeType
import com.neorevolt.drawimage.data.offline.LayoutViewModel
import com.neorevolt.drawimage.data.offline.entity.TransactionEntity
import com.neorevolt.drawimage.filters.FilterListener
import com.neorevolt.drawimage.tools.EditingToolsAdapter
import com.neorevolt.drawimage.tools.ToolType
import com.neorevolt.drawimage.ui.toolsfragments.*
import com.neorevolt.drawimage.utils.FileSaveHelper
import com.neorevolt.drawimage.utils.uriToFile
import java.lang.Float.max
import java.lang.Float.min
import java.util.concurrent.Executors
import android.view.ScaleGestureDetector
import android.view.View.OnTouchListener
import android.widget.SeekBar
import android.widget.Toast
import androidx.transition.Visibility
import com.google.android.play.core.splitcompat.SplitCompat
import com.neorevolt.drawimageproject.MainActivity

/**
 * Modified by NeoRevolt on 3/1/2022.
 */
class EditImageActivity : BaseActivity(), OnPhotoEditorListener, View.OnClickListener,
    PropertiesBSFragment.Properties, ShapeBSFragment.Properties, EmojiBSFragment.EmojiListener,
    StickerBSFragment.StickerListener,
    EditingToolsAdapter.OnItemSelected, FilterListener, GestureDetector.OnDoubleTapListener, GestureDetector.OnGestureListener, OnTouchListener{

    var mPhotoEditor: PhotoEditor? = null
    private var mPhotoEditorView: PhotoEditorView? = null
    private var mPropertiesBSFragment: PropertiesBSFragment? = null
    private var mShapeBSFragment: ShapeBSFragment? = null
    private var mShapeBuilder: ShapeBuilder? = null
    private var mEmojiBSFragment: EmojiBSFragment? = null
    private var mStickerBSFragment: StickerBSFragment? = null
    private var mTxtCurrentTool: TextView? = null
    private var mTxtDone: TextView? = null
    private var mRvTools: RecyclerView? = null
    private var mRvFilters: RecyclerView? = null
    private val mEditingToolsAdapter = EditingToolsAdapter(this)
    private val mFilterViewAdapter = FilterViewAdapter(this)
    private var mRootView: ConstraintLayout? = null
    private val mConstraintSet = ConstraintSet()
    private var mIsFilterVisible = false

    private var desiredImage: Bitmap? = null

    private var getFile: File? = null
    private lateinit var progressBar: ProgressBar
    private lateinit var mTransactions: LayoutViewModel

    private lateinit var seekBar: SeekBar
    private lateinit var mScaleGestureDetector: ScaleGestureDetector
    private var mGestureDetector: GestureDetector? = null
    private var mScaleFactor = 1.0f
    private var inputSize: Int = 100

    private var xStart = 0.0f
    private var yStart = 0.0f
    private var oriScaleX = 0.0f
    private var oriScaleY = 0.0f
    private var xSticker = 0.0f
    private var xStickerStart = 0.0f
    private var xStickerDiff = 0.0f
    private var ySticker = 0.0f
    private var yStickerStart = 0.0f
    private var yStickerDiff = 0.0f
    var scaledX = 0.0f
    var scaledY = 0.0f
    private var mode: String = ""
    private var doubleTapScaleValue = 0f
    private var doubleTapCount = 0

    @VisibleForTesting
    var mSaveImageUri: Uri? = null
    private var mSaveFileHelper: FileSaveHelper? = null

    //CODE FROM BASE
    private var reqCode : String? = null
    private var remoteImage : Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        makeFullScreen()
        setContentView(R.layout.activity_edit_image)

        fitToScreen()

        progressBar = findViewById(R.id.progress_bar)
        seekBar = findViewById(R.id.sbStickerSize)

        initViews()
        handleIntentImage(mPhotoEditorView?.source)
        mPropertiesBSFragment = PropertiesBSFragment()
        mStickerBSFragment = StickerBSFragment()
        mShapeBSFragment = ShapeBSFragment()
        mStickerBSFragment?.setStickerListener(this)
        mEmojiBSFragment?.setEmojiListener(this)
        mPropertiesBSFragment?.setPropertiesChangeListener(this)
        mShapeBSFragment?.setPropertiesChangeListener(this)
        val llmTools = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        mRvTools?.layoutManager = llmTools
        mRvTools?.adapter = mEditingToolsAdapter
        val llmFilters = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        mRvFilters?.layoutManager = llmFilters
        mRvFilters?.adapter = mFilterViewAdapter

        // Used to set integration testing parameters to PhotoEditor
        val pinchTextScalable = intent.getBooleanExtra(PINCH_TEXT_SCALABLE_INTENT_KEY, true)

        // Get Image URL from Detail Activity
//        val photoUrl = intent.getStringExtra(EXTRA_PHOTO)
//        val requestCode = intent.getStringExtra(EXTRA_REQ)
        val bundle: Bundle? = intent.extras
        val requestCode: String? = bundle?.getString("extra_req")
        val photoUrl: String? = bundle?.getString("extra_photo")

        mPhotoEditor = mPhotoEditorView?.run {
            PhotoEditor.Builder(this@EditImageActivity, this)
                .setPinchTextScalable(pinchTextScalable) // set flag to make text scalable when pinch
                .build() // build photo editor sdk
        }
        mPhotoEditor?.setOnPhotoEditorListener(this)

        //Get RequestCode and Image from Intent
        if (requestCode == "gallery" || reqCode == "gallery") {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_REQUEST)
            showLoading(false)
        } else if (requestCode == "remote") {
            val executor = Executors.newSingleThreadExecutor()
            val handler = Handler(Looper.getMainLooper())
            var image: Bitmap? = null
            executor.execute {
                try {
                    showLoading(true)
                    val `in` = java.net.URL(photoUrl).openStream()
                    image = BitmapFactory.decodeStream(`in`)
                    handler.post {
                        showLoading(false)
                        remoteImage = image
                        mPhotoEditorView?.source?.setImageBitmap(image)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        showLoading(false)
        mPhotoEditorView?.source?.setImageResource(R.drawable.blank_image)
        mSaveFileHelper = FileSaveHelper(this)
        mScaleGestureDetector = ScaleGestureDetector(this, ScaleListener())
        mGestureDetector = GestureDetector(this, this)
        if (mPhotoEditorView != null) {
            oriScaleX = mPhotoEditorView?.scaleX!!
            oriScaleY = mPhotoEditorView?.scaleY!!
        }
    }
    var x = 0f
    var y = 0f
    var xS = 0f
    var yS = 0f
    override fun onTouchEvent(event: MotionEvent?): Boolean {

//        Log.d("MODE",mode)
        if (event != null) {
            mTxtCurrentTool?.text = ""
            mScaleGestureDetector.onTouchEvent(event)
            onTouch(mPhotoEditorView, event)
//            mGestureDetector?.onTouchEvent(event)
            showFilter(false)
            setVisibility(false)
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    x = event.x
                    y = event.y

                    xSticker = event.x - 150f
                    ySticker = event.y - 150f
                    xS = xStart
                    yS = yStart

//                    if (doubleTapCount > 1){
//                        xStickerDiff = (xStickerStart - event.x) / doubleTapScaleValue
//                        yStickerDiff = (yStickerStart - event.y) / doubleTapScaleValue
//                        xSticker.plus(xStickerDiff)
//                        ySticker.plus(yStickerDiff)
//                    }
                    Log.d("ACTION DOWN", "event.x = ${event.x}, event.y = ${event.y} \n" +
                            "PhotoEditorViewX = ${mPhotoEditorView?.matrix}, PhotoEditorViewY = ${mPhotoEditorView?.matrix}\n" +
                            "Sticker x= $xSticker, Sticker y = $ySticker")
                }
                MotionEvent.ACTION_MOVE -> if (mode == "DRAG" && doubleTapCount >= 1) {
//                    Log.d("MODE",mode)

                    var dx = event.x - x
                    var dy = event.y - y

                    mPhotoEditorView?.x = (mPhotoEditorView?.x?.plus(dx) ?: mPhotoEditorView?.x) as Float
                    mPhotoEditorView?.y = (mPhotoEditorView?.y?.plus(dy) ?: mPhotoEditorView?.y) as Float
                    x = event.x
                    y = event.y


                    if (doubleTapCount > 1){
                        xStickerDiff = (event.x - xS)
                        yStickerDiff = (event.y - yS)
                        xSticker.plus(xStickerDiff)
                        ySticker.plus(yStickerDiff)
                    }
                    Log.d("ACTION MOVE", "event.x = ${event.x}, event.y = ${event.y} \n" +
                            "PhotoEditorViewX = ${mPhotoEditorView?.x}, PhotoEditorViewY = ${mPhotoEditorView?.y}\n" +
                            "DX = $dx, DY = $dy")
                }
                MotionEvent.ACTION_POINTER_UP -> mode = ""
            }
        }


        return super.onTouchEvent(event)
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener(), OnTouchListener {

        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            mPhotoEditorView?.x = 0f
            mPhotoEditorView?.y = -0f
            mScaleFactor = 1.0f
            return true
        }
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            mScaleFactor *= detector.scaleFactor
            mScaleFactor = max(0.1f, min(mScaleFactor, 3.0f))
            mPhotoEditorView?.scaleX = scaledX + mScaleFactor
            mPhotoEditorView?.scaleY = scaledY + mScaleFactor
            mode = "DRAG"
            doubleTapCount = 3 //MAX
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            mode = ""
            doubleTapCount = 0 //MAX
            fitToScreen()
        }

        override fun onTouch(v: View?, event: MotionEvent?): Boolean {
            mPhotoEditorView?.x = event?.x!!
            mPhotoEditorView?.y = event?.y!!
            return true
        }
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        if (event != null) {
            try {
                mGestureDetector!!.onTouchEvent(event)
            } catch (e: Exception) {
//                Toast.makeText(this,"ERROR", Toast.LENGTH_SHORT).show()
                //TODO (NeoRevolt): Exception with GestureDetector
                Log.d(TAG, "Exception : $e")
            }
        }
        return true
    }

    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        if (doubleTapScaleValue > 1f){
//            mPhotoEditorView?.pivotX = e.x
//            mPhotoEditorView?.pivotY = e.y
//            mPhotoEditorView?.animate()?.translationX(-(e.x/2.0f))?.translationY(-(e.y/2.0f))

        }
        return true
    }

    override fun onDoubleTap(e: MotionEvent): Boolean {

        Log.d(TAG,"DOUBLE TAP SCALE = $doubleTapScaleValue")
        Log.d(TAG,"DOUBLE TAP COUNT = $doubleTapCount")
        when (e.action){
            MotionEvent.ACTION_DOWN -> {
                if (doubleTapCount < 3){

                    mode = "DRAG"
                    doubleTapScaleValue += 1.0f

                    val animScaleX = oriScaleY + doubleTapScaleValue
                    val animScaleY = oriScaleY + doubleTapScaleValue
                    scaledX = animScaleX
                    scaledY = animScaleY
                    mPhotoEditorView?.animate()?.scaleX(animScaleX)?.scaleY(animScaleY)

                    if(doubleTapCount < 1){
                        xStart = e.x
                        yStart = e.y
                        mPhotoEditorView?.pivotX = xStart
                        mPhotoEditorView?.pivotY = yStart

//                        xSticker = e.x - 150f
//                        ySticker = e.y - 150f

                    }
//                    else{
//                        //TODO (NeoRevolt) : Try to animate position
//                        val xDiff = (e.x - xStart) / doubleTapScaleValue
//                        val yDiff = (e.y - yStart) / doubleTapScaleValue
//                        val xCurrent = mPhotoEditorView?.pivotX!! + xDiff
//                        val yCurrent = mPhotoEditorView?.pivotY!! + yDiff
////                        mPhotoEditorView?.pivotX = (mPhotoEditorView?.pivotX!! + xCurrent)
////                        mPhotoEditorView?.pivotY = (mPhotoEditorView?.pivotY!! + yCurrent)
//                        mPhotoEditorView?.pivotX = xCurrent
//                        mPhotoEditorView?.pivotY = yCurrent
//                        Log.d("D","xCurrent = ${e.x} - yCurrent = ${e.y}")
//                    }

                    doubleTapCount += 1

                } else {
                    mode = ""
                    fitToScreen()
                }
            }
        }

        return true
    }

    private fun fitToScreen() {
        doubleTapScaleValue = 1f
        doubleTapCount = 0
        mScaleFactor = 1.0f
        scaledX = 0.0f
        scaledY = 0.0f
        x = 0.0f
        y = 0.0f
        xStart = 0.0f
        yStart = 0.0f
        mPhotoEditorView?.x = 0.0f
        mPhotoEditorView?.y = 0.0f
        mPhotoEditorView?.animate()?.x(0f)?.y(0f)
        mPhotoEditorView?.animate()?.scaleX(oriScaleX)?.scaleY(oriScaleY)
    }

    override fun onDoubleTapEvent(e: MotionEvent): Boolean {
        return false
    }


    //TODO (NEW)
    override fun onDown(e: MotionEvent): Boolean {
//        if (doubleTapCount > 1){
//            xSticker = e.x
//            ySticker = e.y
//            Log.d("DOUBLE EVENT", "X = $xSticker | Y = $ySticker")
//        }
//        return true
        return false
    }

    override fun onShowPress(e: MotionEvent) {
    }

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        return false
    }

    override fun onScroll(
        e1: MotionEvent,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        return false
    }

    override fun onLongPress(e: MotionEvent) {

    }

    override fun onFling(
        e1: MotionEvent,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        return false
    }

    private fun handleIntentImage(source: ImageView?) {
        if (intent == null) {
            return;
        }

        when (intent.action) {
            Intent.ACTION_EDIT, ACTION_NEXTGEN_EDIT -> {
                try {
                    val uri = intent.data
                    val bitmap = MediaStore.Images.Media.getBitmap(
                        contentResolver, uri
                    )
                    source?.setImageBitmap(bitmap)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            else -> {
                val intentType = intent.type
                if (intentType != null && intentType.startsWith("image/")) {
                    val imageUri = intent.data
                    if (imageUri != null) {
                        source?.setImageURI(imageUri)
                    }
                }
            }
        }
    }

    private fun initViews() {
        mPhotoEditorView = findViewById(R.id.photoEditorView)
        mTxtCurrentTool = findViewById(R.id.txtCurrentTool)
        mTxtDone = findViewById(R.id.brush_done_tv)
        mRvTools = findViewById(R.id.rvConstraintTools)
        mRvFilters = findViewById(R.id.rvFilterView)
        mRootView = findViewById(R.id.rootView)

        val imgUndo: ImageView = findViewById(R.id.imgUndo)
        imgUndo.setOnClickListener(this)
        val imgRedo: ImageView = findViewById(R.id.imgRedo)
        imgRedo.setOnClickListener(this)
        val imgCamera: CardView = findViewById(R.id.imgCamera)
        imgCamera.setOnClickListener(this)
        val imgGallery: CardView = findViewById(R.id.imgGallery)
        imgGallery.setOnClickListener(this)
        val imgSave: ImageView = findViewById(R.id.imgSave)
        imgSave.setOnClickListener(this)
        val imgClose: ImageView = findViewById(R.id.imgClose)
        imgClose.setOnClickListener(this)
        val imgShare: ImageView = findViewById(R.id.imgShare)
        imgShare.setOnClickListener(this)
    }

    override fun onEditTextChangeListener(rootView: View?, text: String?, colorCode: Int) {
        val textEditorDialogFragment =
            TextEditorDialogFragment.show(this, text.toString(), colorCode)
        textEditorDialogFragment.setOnTextEditorListener(object :
            TextEditorDialogFragment.TextEditorListener {
            override fun onDone(inputText: String?, colorCode: Int) {
                val styleBuilder = TextStyleBuilder()
                styleBuilder.withTextColor(colorCode)
                if (rootView != null) {
                    mPhotoEditor?.editText(rootView, inputText, styleBuilder)
                }
                mTxtCurrentTool?.setText(R.string.label_text)
            }
        })
    }

    override fun onStickerChangeListener(rootView: View?, inputSize: Int?) {
        seekBar.progress = inputSize ?: 5
    }

    override fun onAddViewListener(viewType: ViewType?, numberOfAddedViews: Int) {
        Log.d(
            TAG,
            "onAddViewListener() called with: viewType = [$viewType], numberOfAddedViews = [$numberOfAddedViews]"
        )
    }

    override fun onRemoveViewListener(viewType: ViewType?, numberOfAddedViews: Int) {
        Log.d(
            TAG,
            "onRemoveViewListener() called with: viewType = [$viewType], numberOfAddedViews = [$numberOfAddedViews]"
        )
    }

    override fun onStartViewChangeListener(viewType: ViewType?) {
        Log.d(TAG, "onStartViewChangeListener() called with: viewType = [$viewType]")
        mode = ""
    }

    override fun onStopViewChangeListener(viewType: ViewType?) {
        Log.d(TAG, "onStopViewChangeListener() called with: viewType = [$viewType]")
        mode = ""
    }

    override fun onTouchSourceImage(event: MotionEvent?) {
//        Log.d(TAG, "onTouchView() called with: event = [$event]")
        if (event != null){
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    mode = "DRAG"
                }
                else -> {
                    mode = ""
//                    doubleTapCount = 3
                }
            }
        }
    }

    @SuppressLint("NonConstantResourceId", "MissingPermission")
    override fun onClick(view: View) {
        when (view.id) {
            R.id.imgUndo -> mPhotoEditor?.undo()
            R.id.imgRedo -> mPhotoEditor?.redo()
            R.id.imgSave -> saveImage()
            R.id.imgClose -> onBackPressed()
            //TODO (NeoRevolt) : Try to send data to BASE (DONE)
            R.id.imgShare -> sharePicture()
            R.id.imgCamera -> getFromCamera()
            R.id.imgGallery -> getFromGallery()
        }
    }

    fun getFromCamera(){
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(cameraIntent, CAMERA_REQUEST)
        Log.d(null,"Get From Camera")
        fitToScreen()
    }

     fun getFromGallery(){
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_REQUEST)
         fitToScreen()

     }

    fun setCode(reqCode: String){
        this.reqCode = reqCode
        Toast.makeText(this,reqCode,Toast.LENGTH_SHORT).show()
    }

    // Note : Share to different app
    private fun shareImage() {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "image/*"
        val saveImageUri = mSaveImageUri
        if (saveImageUri == null) {
            showSnackbar(getString(R.string.msg_save_image_to_share))
            return
        }
        intent.putExtra(Intent.EXTRA_STREAM, buildFileProviderUri(saveImageUri))
        startActivity(Intent.createChooser(intent, getString(R.string.msg_share_image)))
    }

    private fun buildFileProviderUri(uri: Uri): Uri {
        if (FileSaveHelper.isSdkHigherThan28()) {
            return uri
        }
        val path: String = uri.path ?: throw IllegalArgumentException("URI Path Expected")

        return FileProvider.getUriForFile(
            this,
            FILE_PROVIDER_AUTHORITY,
            File(path)
        )
    }


    // Save Image
    @RequiresPermission(allOf = [Manifest.permission.WRITE_EXTERNAL_STORAGE])
    private fun saveImage() {
        val fileName = System.currentTimeMillis().toString() + ".png"
        val hasStoragePermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        if (hasStoragePermission || FileSaveHelper.isSdkHigherThan28()) {
            showLoading("Saving...")
            mSaveFileHelper?.createFile(fileName, object : FileSaveHelper.OnFileCreateResult {

                @RequiresPermission(allOf = [Manifest.permission.WRITE_EXTERNAL_STORAGE])
                override fun onFileCreateResult(
                    created: Boolean,
                    filePath: String?,
                    error: String?,
                    uri: Uri?
                ) {
                    if (created && filePath != null) {
                        val saveSettings = SaveSettings.Builder()
                            .setClearViewsEnabled(true)
                            .setTransparencyEnabled(true)
                            .build()

                        mPhotoEditor?.saveAsFile(
                            filePath,
                            saveSettings,
                            object : PhotoEditor.OnSaveListener {
                                override fun onSuccess(imagePath: String) {
                                    mSaveFileHelper?.notifyThatFileIsNowPubliclyAvailable(
                                        contentResolver
                                    )
                                    hideLoading()
                                    showSnackbar("Image Saved Successfully")
                                    mSaveImageUri = uri
                                    mPhotoEditorView?.source?.setImageURI(mSaveImageUri)
                                    val myFile =
                                        mSaveImageUri?.let { uriToFile(it, this@EditImageActivity) }
                                    getFile = myFile
                                }

                                override fun onFailure(exception: Exception) {
                                    hideLoading()
                                    showSnackbar("Failed to save Image")
                                }
                            })
                    } else {
                        hideLoading()
                        error?.let { showSnackbar(error) }
                    }
                }
            })
        } else {
            requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    private fun shareImageToBase(imageUri: Uri?) {
//        Log.d(null, "INI DI TO SHOW : $imageUri")
        Intent(this, MainActivity::class.java).also {
//            it.putExtra(MainActivity.EXTRA_PHOTO, imageUri?.toString())
            it.data = imageUri
            startActivity(it)
        }
    }

    private fun sharePicture() {
        val fileName = System.currentTimeMillis().toString() + ".png"
        val hasStoragePermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        if (hasStoragePermission || FileSaveHelper.isSdkHigherThan28()) {
            showLoading("Saving...")
            mSaveFileHelper?.createFile(fileName, object : FileSaveHelper.OnFileCreateResult {

                @RequiresPermission(allOf = [Manifest.permission.WRITE_EXTERNAL_STORAGE])
                override fun onFileCreateResult(
                    created: Boolean,
                    filePath: String?,
                    error: String?,
                    uri: Uri?
                ) {
                    if (created && filePath != null) {
                        val saveSettings = SaveSettings.Builder()
                            .setClearViewsEnabled(true)
                            .setTransparencyEnabled(true)
                            .build()

                        mPhotoEditor?.saveAsFile(
                            filePath,
                            saveSettings,
                            object : PhotoEditor.OnSaveListener {
                                override fun onSuccess(imagePath: String) {
                                    mSaveFileHelper?.notifyThatFileIsNowPubliclyAvailable(
                                        contentResolver
                                    )
                                    hideLoading()
                                    showSnackbar("Image Saved Successfully")
                                    mSaveImageUri = uri
                                    mPhotoEditorView?.source?.setImageURI(mSaveImageUri)
                                    Log.d(null, "INI ALAMATNYA DI SHARE : $mSaveImageUri")
                                    val myFile =
                                        mSaveImageUri?.let { uriToFile(it, this@EditImageActivity) }
                                    getFile = myFile

                                    //Share Image to Base
                                    shareImageToBase(mSaveImageUri)

                                    mTransactions = ViewModelProvider(this@EditImageActivity).get(
                                        LayoutViewModel::class.java
                                    )
                                    mTransactions.addTransaction(
                                        TransactionEntity(
                                            getFile.toString(),
                                            getFile?.name.toString(),
                                            null
                                        )
                                    )
                                }

                                override fun onFailure(exception: Exception) {
                                    hideLoading()
                                    showSnackbar("Failed to save Image")
                                }
                            })
                    } else {
                        hideLoading()
                        error?.let { showSnackbar(error) }
                    }
                }
            })
        } else {
            requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                CAMERA_REQUEST -> {
                    mPhotoEditor?.clearAllViews()
                    val photo = data?.extras?.get("data") as Bitmap?
                    mPhotoEditorView?.source?.setImageBitmap(photo)
                }
                PICK_REQUEST -> try {
                    mPhotoEditor?.clearAllViews()
                    val uri = data?.data

                    val selectedImg: Uri = data?.data as Uri
                    val myFile = uriToFile(selectedImg, this)

                    val bitmap = MediaStore.Images.Media.getBitmap(
                        contentResolver, uri
                    )
                    mPhotoEditorView?.source?.setImageBitmap(bitmap)
                    getFile = myFile
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onColorChanged(colorCode: Int) {
        mPhotoEditor?.setShape(mShapeBuilder?.withShapeColor(colorCode))
        mTxtCurrentTool?.setText(R.string.label_brush)
    }

    override fun onOpacityChanged(opacity: Int) {
        mPhotoEditor?.setShape(mShapeBuilder?.withShapeOpacity(opacity))
        mTxtCurrentTool?.setText(R.string.label_brush)
    }

    override fun onShapeSizeChanged(shapeSize: Int) {
        mPhotoEditor?.setShape(mShapeBuilder?.withShapeSize(shapeSize.toFloat()))
        mTxtCurrentTool?.setText(R.string.label_brush)
    }

    override fun onShapePicked(shapeType: ShapeType?) {
        mPhotoEditor?.setShape(mShapeBuilder?.withShapeType(shapeType))
    }

    override fun onEmojiClick(emojiUnicode: String?) {
        mPhotoEditor?.addEmoji(emojiUnicode)
        mTxtCurrentTool?.setText(R.string.label_emoji)
    }

    override fun onStickerClick(bitmap: Bitmap?, size: Int) {
        mPhotoEditor?.addImage(bitmap, size)
        mTxtCurrentTool?.setText(R.string.label_sticker)
        desiredImage = bitmap
        mode = ""
        fitToScreen()
    }

    override fun onStickerClickWithPost(bitmap: Bitmap?, size: Int, xClick: Float, yClick: Float) {
        val x = xSticker
        val y = ySticker
        Log.d("SESUDAH", "X = $x, - Y = $y")
        if (x == 0.0f && y == 0.0f) {
            mPhotoEditor?.addImage(bitmap, size)
            fitToScreen()
        }
        else{
            mPhotoEditor?.addImageWithPost(bitmap, size, x, y)
            xSticker = 0f
            ySticker = 0f
        }
        mTxtCurrentTool?.setText(R.string.label_sticker)
        desiredImage = bitmap
        mode = ""

//        fitToScreen()
    }

    override fun onSizeChange(stickerSize: Int) {
//        mPhotoEditor?.setStickerSize(stickerSize)
        inputSize = stickerSize
        Log.d("TAG", "radio size $stickerSize")
        mTxtCurrentTool?.setText(R.string.label_sticker)
    }

    @SuppressLint("MissingPermission")
    override fun isPermissionGranted(isGranted: Boolean, permission: String?) {
        if (isGranted) {
            saveImage()
        }
    }

    @SuppressLint("MissingPermission")
    private fun showSaveDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(getString(R.string.msg_save_image))
        builder.setPositiveButton("Save") { _: DialogInterface?, _: Int -> saveImage() }
        builder.setNegativeButton("Cancel") { dialog: DialogInterface, _: Int -> dialog.dismiss() }
        builder.setNeutralButton("Discard") { _: DialogInterface?, _: Int -> finish() }
        builder.create().show()
    }

    override fun onFilterSelected(photoFilter: PhotoFilter?) {
        mPhotoEditor?.setFilterEffect(photoFilter)
    }

    override fun onToolSelected(toolType: ToolType?) {
        when (toolType) {
            ToolType.BRUSH -> {
                mPhotoEditor?.setBrushDrawingMode(true)
                mShapeBuilder = ShapeBuilder()
                mPhotoEditor?.setShape(mShapeBuilder)
                mTxtCurrentTool?.setText(R.string.label_shape)
                clearFocusButton(true)
                showBottomSheetDialogFragment(mShapeBSFragment)
            }
            ToolType.TEXT -> {
                mTxtDone?.visibility = View.GONE
                val textEditorDialogFragment = TextEditorDialogFragment.show(this)
                textEditorDialogFragment.setOnTextEditorListener(object :
                    TextEditorDialogFragment.TextEditorListener {
                    override fun onDone(inputText: String?, colorCode: Int) {
                        val styleBuilder = TextStyleBuilder()
                        styleBuilder.withTextColor(colorCode)
                        mPhotoEditor?.addText(inputText, styleBuilder)
                        mTxtCurrentTool?.setText(R.string.label_text)
                    }
                })
                mPhotoEditor?.setBrushDrawingMode(false)
            }
            ToolType.ERASER -> {
                mPhotoEditor?.brushEraser(true)
                mTxtCurrentTool?.setText(R.string.label_eraser_mode)
                clearFocusButton(true)
            }
            ToolType.ADJUST -> {
                mTxtCurrentTool?.setText(R.string.label_filter)
                showFilter(true)
                mPhotoEditor?.setBrushDrawingMode(false)
            }
            ToolType.EMOJI -> {
                showBottomSheetDialogFragment(mEmojiBSFragment)
                mPhotoEditor?.setBrushDrawingMode(false)
                clearFocusButton(false)
            }
            ToolType.STICKER -> {
                showBottomSheetDialogFragment(mStickerBSFragment)
                mPhotoEditor?.setBrushDrawingMode(false)
                clearFocusButton(false)
            }
            else -> {
                mPhotoEditor?.setBrushDrawingMode(false)
                clearFocusButton(false)

            }
        }
    }

    private fun showBottomSheetDialogFragment(fragment: BottomSheetDialogFragment?) {
        if (fragment == null || fragment.isAdded) {
            return
        }
        fragment.show(supportFragmentManager, fragment.tag)
    }

    private fun showFilter(isVisible: Boolean) {
        mIsFilterVisible = isVisible
        mConstraintSet.clone(mRootView)
        val rvFilterId: Int =
            mRvFilters?.id ?: throw IllegalArgumentException("RV Filter ID Expected")
        if (isVisible) {
            mConstraintSet.clear(rvFilterId, ConstraintSet.START)
            mConstraintSet.connect(
                rvFilterId, ConstraintSet.START,
                ConstraintSet.PARENT_ID, ConstraintSet.START
            )
            mConstraintSet.connect(
                rvFilterId, ConstraintSet.END,
                ConstraintSet.PARENT_ID, ConstraintSet.END
            )
        } else {
            mConstraintSet.connect(
                rvFilterId, ConstraintSet.START,
                ConstraintSet.PARENT_ID, ConstraintSet.END
            )
            mConstraintSet.clear(rvFilterId, ConstraintSet.END)
        }
        val changeBounds = ChangeBounds()
        changeBounds.duration = 350
        changeBounds.interpolator = AnticipateOvershootInterpolator(1.0f)
        mRootView?.let { TransitionManager.beginDelayedTransition(it, changeBounds) }
        mConstraintSet.applyTo(mRootView)
    }

    override fun onBackPressed() {
        val isCacheEmpty =
            mPhotoEditor?.isCacheEmpty ?: throw IllegalArgumentException("isCacheEmpty Expected")

        if (mIsFilterVisible) {
            showFilter(false)
            mTxtCurrentTool?.setText(R.string.app_name)
        } else if (!isCacheEmpty) {
            showSaveDialog()
        } else {
            super.onBackPressed()
        }
    }

    private fun clearFocusButton(visible: Boolean){
        if (visible){
            mTxtDone?.visibility = View.VISIBLE
            mTxtDone?.setOnClickListener {
                mPhotoEditor?.setBrushDrawingMode(false)
                mTxtDone?.visibility = View.GONE
                mPhotoEditor?.brushEraser(false)
            }
        }else{
            mTxtDone?.visibility = View.GONE
        }

    }

    private fun showLoading(state: Boolean) {
        if (state) {
            progressBar.visibility = View.VISIBLE
        } else {
            progressBar.visibility = View.GONE
        }
    }

    //TODO (NeoRevolt) : ADD THISS SO IT CAN BE USED IN BASE !!!
    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase)
        SplitCompat.install(this)
    }

    private fun setVisibility(state: Boolean){
        if (state){
            seekBar.visibility = View.VISIBLE
        } else {
            seekBar.visibility = View.GONE
        }
    }


    companion object {
        private val TAG = EditImageActivity::class.java.simpleName
        const val FILE_PROVIDER_AUTHORITY = "com.neorevolt.drawimagedemo.fileprovider"
        private const val CAMERA_REQUEST = 52
        private const val PICK_REQUEST = 53
        const val ACTION_NEXTGEN_EDIT = "action_nextgen_edit"
        const val PINCH_TEXT_SCALABLE_INTENT_KEY = "PINCH_TEXT_SCALABLE"
        const val EXTRA_PHOTO = "extra_photo"
        const val EXTRA_REQ = "extra_req"
    }
}