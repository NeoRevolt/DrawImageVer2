package com.neorevolt.drawimage.burhanrashid52.photoeditor

import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout

/**
 * Created by Burhanuddin Rashid on 15/05/21.
 *
 * @author <https:></https:>//github.com/example>
 */
internal class GraphicManager(
    private val mPhotoEditorView: PhotoEditorView,
    private val mViewState: PhotoEditorViewState
) {
    var x: Float? = 0f
    var y: Float? = 0f
    var onPhotoEditorListener: OnPhotoEditorListener? = null
    fun addView(graphic: Graphic) {
        val view = graphic.rootView
        val params = RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
        mPhotoEditorView.addView(view, params)
        mViewState.addAddedView(view)

        onPhotoEditorListener?.onAddViewListener(
            graphic.viewType,
            mViewState.addedViewsCount
        )
    }

    fun addViewWithPos(graphic: Graphic, viewX: Float, viewY: Float) {
        val view = graphic.rootView
//        view.x += 0f
//        view.y += 0f
        x = viewX
        y = viewY
        view.translationX = viewX
        view.translationY = viewY
        Log.d("GRAPHIC MANAGER", "X = $x, | Y = $y")
        val params = RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        mPhotoEditorView.addView(view, params)
        mViewState.addAddedView(view)

        onPhotoEditorListener?.onAddViewListener(
            graphic.viewType,
            mViewState.addedViewsCount
        )
    }

    fun removeView(graphic: Graphic) {
        val view = graphic.rootView
        if (mViewState.containsAddedView(view)) {
            mPhotoEditorView.removeView(view)
            mViewState.removeAddedView(view)
            mViewState.pushRedoView(view)
            onPhotoEditorListener?.onRemoveViewListener(
                graphic.viewType,
                mViewState.addedViewsCount
            )
        }
    }

    fun updateView(view: View) {
        mPhotoEditorView.updateViewLayout(view, view.layoutParams)
        mViewState.replaceAddedView(view)
    }

    fun undoView(): Boolean {
        if (mViewState.addedViewsCount > 0) {
            val removeView = mViewState.getAddedView(
                mViewState.addedViewsCount - 1
            )
            if (removeView is DrawingView) {
                return removeView.undo()
            } else {
                mViewState.removeAddedView(mViewState.addedViewsCount - 1)
                mPhotoEditorView.removeView(removeView)
                mViewState.pushRedoView(removeView)
            }
            when (val viewTag = removeView.tag) {
                is ViewType -> onPhotoEditorListener?.onRemoveViewListener(
                    viewTag,
                    mViewState.addedViewsCount
                )
            }
        }
        return mViewState.addedViewsCount != 0
    }

    fun redoView(): Boolean {
        if (mViewState.redoViewsCount > 0) {
            val redoView = mViewState.getRedoView(
                mViewState.redoViewsCount - 1
            )
            if (redoView is DrawingView) {
                return redoView.redo()
            } else {
                mViewState.popRedoView()
                mPhotoEditorView.addView(redoView)
                mViewState.addAddedView(redoView)
            }
            when (val viewTag = redoView.tag) {
                is ViewType -> onPhotoEditorListener?.onAddViewListener(
                    viewTag,
                    mViewState.addedViewsCount
                )
            }
        }
        return mViewState.redoViewsCount != 0
    }
}