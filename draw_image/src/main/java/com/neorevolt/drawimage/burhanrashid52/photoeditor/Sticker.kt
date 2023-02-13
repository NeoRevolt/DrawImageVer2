package com.neorevolt.drawimage.burhanrashid52.photoeditor

import android.graphics.Bitmap
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import com.neorevolt.drawimage.R

/**
 * Created by Burhanuddin Rashid on 14/05/21.
 *
 * @author <https:></https:>//github.com/example>
 */
internal class Sticker(
    private val mPhotoEditorView: PhotoEditorView,
    private val mMultiTouchListener: MultiTouchListener,
    private val mViewState: PhotoEditorViewState,
    graphicManager: GraphicManager?
) : Graphic(
    context = mPhotoEditorView.context,
    graphicManager = graphicManager,
    viewType = ViewType.IMAGE,
    layoutId = R.layout.view_photo_editor_image
) {
    private var imageView: ImageView? = null
    private var mSeekBar: SeekBar? = null
    fun buildView(desiredImage: Bitmap?, size: Int, stickerStyleBuilder: StickerStyleBuilder?) {
        imageView?.apply {
            val resized = desiredImage?.let { Bitmap.createScaledBitmap(it,size,size,true) }
            setImageBitmap(resized)
            resized.apply {
                this?.let { stickerStyleBuilder?.applyStyle(it,size.toFloat()) }
            }
        }
    }

    private fun setupGesture() {
        val onGestureControl = buildGestureController(mPhotoEditorView, mViewState)
        mMultiTouchListener.setOnGestureControl(onGestureControl)
        val rootView = rootView
        rootView.setOnTouchListener(mMultiTouchListener)
    }

    override fun setupView(rootView: View) {
        imageView = rootView.findViewById(R.id.imgPhotoEditorImage)
        mSeekBar = rootView.findViewById(R.id.sbStickerSize)
    }

    override fun updateView(view: View?) {
        val inputSize = mSeekBar?.progress
        val photoEditorListener = graphicManager?.onPhotoEditorListener
        photoEditorListener?.onStickerChangeListener(view,inputSize)
    }

    init {
        setupGesture()
    }
}