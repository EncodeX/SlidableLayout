package com.encodex.widget;

import android.support.v4.widget.ViewDragHelper;
import android.util.Log;
import android.view.View;

/**
 * Created with Android Studio.
 * Author: Enex Tapper
 * Date: 15/10/18
 * Project: SlidableLayout
 * Package: com.encodex.widget
 */
public class DragHelperCallback extends ViewDragHelper.Callback {
	@Override
	public boolean tryCaptureView(View child, int pointerId) {
		Log.d("DragHelper","tryCaptureView");
		return true;
	}

	@Override
	public int clampViewPositionVertical(View child, int top, int dy) {
		Log.d("DragHelper","clampViewPositionVertical");
		Log.d("DragHelper","top: "+ top + " dy:"+ dy);
		return top;
	}

	@Override
	public int clampViewPositionHorizontal(View child, int left, int dx) {
		Log.d("DragHelper","clampViewPositionHorizontal");
		return super.clampViewPositionHorizontal(child, left, dx);
	}

	@Override
	public void onViewDragStateChanged(int state) {
		Log.d("DragHelper","onViewDragStateChanged");
		super.onViewDragStateChanged(state);
	}
}
