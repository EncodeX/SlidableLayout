package com.encodex.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.view.ViewHelper;

import java.util.List;

/**
 *
 * SlidableLayout
 *
 * 灵感来源于ResideMenu
 *
 */

public class SlidableLayout extends FrameLayout{

	//触摸状态
	public enum TouchState{
		TOUCH_UP,
		TOUCH_DOWN,
		TOUCH_MOVE_HORIZONTAL,
		TOUCH_MOVE_VERTICAL
	}

	//滑动方向
	public enum SlideDirection{
		UP,
		DOWN,
		LEFT,
		RIGHT
	}

	//坐标位移
	private float mContentXOffset;
	private float mContentYOffset = -0.9f;
	private int mCoveredViewXOffset;
	private int mCoveredViewYOffset;

	//缩放
	private float mContentScaleX;
	private float mContentScaleY;
	private float mCoveredViewScaleX;
	private float mCoveredViewScaleY;

	//旋转
	private int mContentRotationX;
	private int mContentRotationY;
	private int mCoveredViewRotationX;
	private int mCoveredViewRotationY;

	private boolean mEnabledSwitch = true;
	private TouchState mTouchState = TouchState.TOUCH_UP;
	private SlideDirection mSlideDirection = SlideDirection.RIGHT;
	private VelocityTracker mVelocityTracker;
	private List<View> mIgnoredViews;
	private boolean mIsSlided;
	private boolean mIsInitialized = false;

	private float mViewHeight;
	private float mViewWidth;
	private float mLastTouchPositionX;
	private float mLastTouchPositionY;
	private float mLastPositionX;
	private float mLastPositionY;

	public SlidableLayout(Context context) {
		this(context, null);
	}

	public SlidableLayout(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public SlidableLayout(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		if(!mIsInitialized) initView();

		createVelocityTracker(ev);
		switch (ev.getAction()){
			case MotionEvent.ACTION_DOWN:
				mTouchState = TouchState.TOUCH_DOWN;

				mLastTouchPositionX = ev.getRawX();
				mLastTouchPositionY = ev.getRawY();

				break;
			case MotionEvent.ACTION_MOVE:

				float xOffset = ev.getRawX() - mLastTouchPositionX;
				float yOffset = ev.getRawY() - mLastTouchPositionY;

				switch(mTouchState){
					case TOUCH_DOWN:
						//判断方向
						if(Math.abs(xOffset) > 25){
							mTouchState = TouchState.TOUCH_MOVE_HORIZONTAL;
							break;
						}else if(Math.abs(yOffset) > 50){
							mTouchState = TouchState.TOUCH_MOVE_VERTICAL;
							ev.setAction(MotionEvent.ACTION_CANCEL);
						}
						break;
					case TOUCH_MOVE_VERTICAL:
						if(mLastPositionY + yOffset >= 0){
							ViewHelper.setY(this, 0);
						}else if(mLastPositionY + yOffset <= mContentYOffset * mViewHeight){
							ViewHelper.setY(this, mContentYOffset * mViewHeight);
						}else{
							ViewHelper.setY(this, mLastPositionY + yOffset);
						}
						return true;
				}

				break;
			case MotionEvent.ACTION_UP:
				if(mTouchState == TouchState.TOUCH_DOWN) {mTouchState=TouchState.TOUCH_UP;break;}
				if(mTouchState != TouchState.TOUCH_MOVE_VERTICAL) break;
				mTouchState = TouchState.TOUCH_UP;

				mLastPositionY +=ev.getRawY() - mLastTouchPositionY;

				if(getScrollVelocity()>200){
					recycleVelocityTracker();
					if((ev.getRawY() - mLastTouchPositionY)<0){
						slideUp();
					}else {
						slideDown();
					}
				}else if(mIsSlided){
					if(mLastPositionY>-mViewHeight*0.5){
						slideDown();
					}else{
						slideUp();
					}
					recycleVelocityTracker();
				}else{
					if(mLastPositionY<-mViewHeight*0.3){
						slideUp();
					}else{
						slideDown();
					}
					recycleVelocityTracker();
				}

				return true;
		}



		return super.dispatchTouchEvent(ev);
	}

	private void initView(){
		mViewWidth = Double.valueOf(this.getWidth()).floatValue();
		mViewHeight = Double.valueOf(this.getHeight()).floatValue();

		mIsInitialized = true;
	}

	// 遍历所有子View(暂不用到)
//	private void traversalView(ViewGroup viewGroup){
//		int count = viewGroup.getChildCount();
//		for (int i = 0; i < count; i++) {
//			View view = viewGroup.getChildAt(i);
//			if (view instanceof ViewGroup) {
//				traversalView((ViewGroup) view);
//			} else {
//				// do something
//			}
//		}
//	}

	/**
	 * 创建Tracker
	 * @param ev
	 */
	private void createVelocityTracker(MotionEvent ev){
		if(mVelocityTracker ==null){
			mVelocityTracker = VelocityTracker.obtain();
		}
		mVelocityTracker.addMovement(ev);
	}

	/**
	 * 获得瞬时速度
	 * @return
	 */
	private int getScrollVelocity() {
		mVelocityTracker.computeCurrentVelocity(1000);
		return (int) Math.abs(mVelocityTracker.getXVelocity());
	}

	/**
	 * 回收Tracker
	 */
	private void recycleVelocityTracker() {
		mVelocityTracker.recycle();
		mVelocityTracker = null;
	}

	//临时函数
	private void slideUp(){
		AnimatorSet slideUp = buildSlideAnimation(this,mContentYOffset * mViewHeight);
		mLastPositionY = mContentYOffset * mViewHeight;
		slideUp.start();
	}

	//临时函数
	private void slideDown(){
		AnimatorSet slideDown = buildSlideAnimation(this,0);
		mLastPositionY = 0;
		slideDown.start();
	}

	/**
	 * 滑动动画
	 * @param target
	 * @param targetPosY
	 * @return
	 */
	private AnimatorSet buildSlideAnimation(View target, float targetPosY){

		AnimatorSet slideAnimation = new AnimatorSet();
		slideAnimation.playTogether(
				ObjectAnimator.ofFloat(target, "translationY", targetPosY)
		);
		slideAnimation.setInterpolator(new DecelerateInterpolator(4.0f));

		slideAnimation.setDuration(500);
		return slideAnimation;
	}
}
