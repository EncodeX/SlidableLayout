package com.encodex.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.VelocityTrackerCompat;
import android.support.v4.view.ViewConfigurationCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.RelativeLayout;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.view.ViewHelper;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * SlidableLayout
 *
 * 灵感来源于ResideMenu
 *
 */

public class SlidableLayout extends RelativeLayout{
	private static final int MIN_FLING_DISTANCE = 25; // dips
	private static final int POINTER_INVALID = -1;

	//触摸状态
	public enum State {
		IDLE,
		TOUCH_DOWN,
		TOUCH_MOVE,
		TOUCH_MOVE_HORIZONTAL,
		TOUCH_MOVE_VERTICAL,
		SLIDING
	}

	//滑动方向
	public enum SlideDirection{
		UP,
		DOWN,
		LEFT,
		RIGHT,
		UP_DOWN,
		LEFT_RIGHT,
		FREE,
		LOCKED
	}

	//坐标位移
	private float mContentXOffset;
	private float mContentYOffset;
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

	private Context mContext;
	private State mState;
	private SlideDirection mSlideDirection;
	private List<View> mIgnoredViews;

	private AnimatorSet mSlideAnimation;

	private boolean mEnabled;
	private boolean mIsViewInitialized;
	private boolean mIsViewSlidedOut;

	private VelocityTracker mVelocityTracker;
	private int mMinimumVelocity;
	protected int mMaximumVelocity;
	private int mFlingDistance;
	private float mTouchSlop;

	private int mActivePointerId;

	private float mViewHeight;
	private float mViewWidth;
	private float mInitialMotionX;
	private float mInitialMotionY;
	private float mLastMotionX;
	private float mLastMotionY;
	private float mInitialPositionX;
	private float mInitialPositionY;
	private float mLastPositionX;
	private float mLastPositionY;

	private Animator.AnimatorListener mViewToggleListener;

	public SlidableLayout(Context context) {
		this(context, null);
	}

	public SlidableLayout(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public SlidableLayout(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);

		setWillNotDraw(false);
		setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
		setFocusable(true);

		LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		View bottomView = new View(context);
		bottomView.setClickable(true);
		addView(bottomView, 0, layoutParams);

		onCreateView(context);

		// TODO attr
		if(attrs!=null){

		}
	}

	private void onCreateView(Context context){
		mContext = context;

		final float density = context.getResources().getDisplayMetrics().density;
		mFlingDistance = (int) (MIN_FLING_DISTANCE * density);

		final ViewConfiguration configuration = ViewConfiguration.get(context);
		mTouchSlop = ViewConfigurationCompat.getScaledPagingTouchSlop(configuration);
		mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
		mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();

		mIgnoredViews = new ArrayList<>();

		mViewToggleListener = new Animator.AnimatorListener() {
			@Override
			public void onAnimationStart(Animator animation) {
				Log.d("DragHelper", "onAnimationStart");
				mState = State.SLIDING;
				toggleHardwareAcceleration(true);
			}

			@Override
			public void onAnimationEnd(Animator animation) {
				if(mIsViewSlidedOut){
					mLastPositionY = getTopBorder();
					Log.d("DragHelper", "onAnimationEnd mIsViewSlidedOut: " + mIsViewSlidedOut + " mLastPositionY: "+ mLastPositionY);
				}else{
					mLastPositionY = getBottomBorder();
					Log.d("DragHelper", "onAnimationEnd mIsViewSlidedOut: " + mIsViewSlidedOut + " mLastPositionY: "+ mLastPositionY);
				}
//				mLastPositionX = SlidableLayout.this.getLeft();
				toggleHardwareAcceleration(false);
				mState = State.IDLE;
			}

			@Override
			public void onAnimationCancel(Animator animation) {

			}

			@Override
			public void onAnimationRepeat(Animator animation) {

			}
		};

		// 临时初始化
		mEnabled = true;
		mIsViewInitialized = false;
		mIsViewSlidedOut = false;

		mState = State.IDLE;
		mSlideDirection = SlideDirection.UP;
		mContentYOffset = -0.9f;
	}

	private void initViewSize(){
		mViewWidth = Double.valueOf(this.getWidth()).floatValue();
		mViewHeight = Double.valueOf(this.getHeight()).floatValue();

		mInitialPositionY = mLastPositionY = this.getTop();
		mInitialPositionX = mLastPositionX = this.getLeft();

		mIsViewInitialized = true;
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		if(!mEnabled) return false;
		if(!mIsViewInitialized) initViewSize();

		final int action = MotionEventCompat.getActionMasked(ev);
		createVelocityTracker(ev);


//		Log.d("DragHelper", "action: "+ action);

		switch (action) {
			case MotionEvent.ACTION_DOWN:
				Log.d("DragHelper", "dispatchTouchEvent ACTION_DOWN");

				if(mState == State.SLIDING){
					mSlideAnimation.cancel();
					mLastPositionY = ViewHelper.getY(this);
					Log.d("DragHelper", "SLIDING → DOWN mLastPositionY: "+ mLastPositionY);
					mState = State.IDLE;
				}

				if(mState != State.IDLE) break;

				int index = MotionEventCompat.getActionIndex(ev);
				mActivePointerId = MotionEventCompat.getPointerId(ev, index);
				if (mActivePointerId == POINTER_INVALID) break;

				mLastMotionY = mInitialMotionY = ev.getRawY();
				mLastMotionX = mInitialMotionX = ev.getRawX();

				if(isTouchLegal(ev)){
					mState = State.TOUCH_DOWN;
					toggleHardwareAcceleration(true);
//					return true;
//					ev.setAction(MotionEvent.ACTION_CANCEL);
				}
				break;
			case MotionEvent.ACTION_MOVE:
				Log.d("DragHelper", "dispatchTouchEvent ACTION_MOVE");

				if(mState != State.TOUCH_DOWN
						&& mState != State.TOUCH_MOVE
						&& mState != State.TOUCH_MOVE_HORIZONTAL
						&& mState != State.TOUCH_MOVE_VERTICAL) break;

				Boolean isGoodToMove = null;
				if(mState == State.TOUCH_DOWN || mState == State.TOUCH_MOVE){
					if(!(isGoodToMove = isGoodToMove(ev))) break;
				}

				// TODO 分情况讨论移动边界

				final float y = ev.getRawY();
				final float yOffset = y - mInitialMotionY;
				Log.d("DragHelper", "mLastMotionY: "+mLastMotionY + " y: "+y + " mLastPositionY: "+ mLastPositionY);
				mLastMotionY = y;

				float intentY = mLastPositionY + yOffset;
				if(intentY < getTopBorder()){
					intentY = getTopBorder();
					mInitialMotionY = y;
				}else if(intentY > getBottomBorder()){
					intentY = getBottomBorder();
					mInitialMotionY = y;
				}

				Log.d("DragHelper", "top: "+getTopBorder() + " bottom: "+getBottomBorder() + " intentY: "+ intentY);

				ViewHelper.setY(this, intentY);

				if(isGoodToMove == null){
					return true;
				}else {
					ev.setAction(MotionEvent.ACTION_CANCEL);
				}
				break;
			case MotionEvent.ACTION_UP:
				Log.d("DragHelper", "dispatchTouchEvent ACTION_UP");

				if(mState != State.TOUCH_DOWN
						&& mState != State.TOUCH_MOVE
						&& mState != State.TOUCH_MOVE_HORIZONTAL
						&& mState != State.TOUCH_MOVE_VERTICAL) break;

				switch (mState){
					case TOUCH_DOWN:
					case TOUCH_MOVE:
						toggleView(SlideDirection.LOCKED);
						mActivePointerId = POINTER_INVALID;

						if(mIsViewSlidedOut) return true;

						break;
					case TOUCH_MOVE_HORIZONTAL:
					case TOUCH_MOVE_VERTICAL:
						// TODO 分情况讨论

						final int velocity = getScrollVelocity(SlideDirection.UP_DOWN);

						getPointerIndex(ev, mActivePointerId);

						if (mActivePointerId != POINTER_INVALID){
							final int totalDelta = (int) (ev.getRawY() - mInitialMotionY);
							final int motionDelta = (int) (ev.getRawY() - mLastMotionY);

							Log.d("DragHelper", "totalDelta: "+ totalDelta+" motionDelta: "+motionDelta+" velocity: "+velocity);
							toggleView(getDirectionWithFling(SlideDirection.UP_DOWN,velocity,totalDelta));

							mActivePointerId = POINTER_INVALID;
							recycleVelocityTracker();
							return true;
						}
						break;
				}
				mActivePointerId = POINTER_INVALID;
				recycleVelocityTracker();
				break;
			case MotionEvent.ACTION_CANCEL:
				Log.d("DragHelper", "dispatchTouchEvent ACTION_CANCEL");
				if(mIsViewSlidedOut){
					toggleView(SlideDirection.UP);
					mState = State.IDLE;
					mActivePointerId = POINTER_INVALID;
				}else{
					toggleView(SlideDirection.LOCKED);
					mState = State.IDLE;
					mActivePointerId = POINTER_INVALID;
				}
				break;
		}
		return super.dispatchTouchEvent(ev);
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		return super.onInterceptTouchEvent(ev);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return super.onTouchEvent(event);
	}

	private boolean isSlidable(){
		return mState!= State.TOUCH_DOWN && mState!= State.SLIDING;
	}

	private void finishSlide(){
		// TODO
	}

	private boolean isTouchLegal(MotionEvent event){
		if(isInIgnoredViews(event)) return false;

		// TODO 边缘滑动等

		return true;
	}

	private boolean isGoodToMove(MotionEvent event){
		final int activePointerId = mActivePointerId;
		final int pointerIndex = getPointerIndex(event, activePointerId);
		if (activePointerId == POINTER_INVALID || pointerIndex == POINTER_INVALID)
			return false;

		final float x = event.getRawX();
		final float dx = x - mLastMotionX;
		final float xOffset = Math.abs(dx);
		final float y = event.getRawY();
		final float dy = y - mLastMotionY;
		final float yOffset = Math.abs(dy);

		// TODO 分MOVE情况讨论
		if (yOffset > (mIsViewSlidedOut?mTouchSlop/2:mTouchSlop) && yOffset > xOffset){
			Log.d("DragHelper", "Slide Applied");
			mState = State.TOUCH_MOVE_VERTICAL;
			return true;
		}else{
			Log.d("DragHelper", "Slide Refused");
			mState = State.TOUCH_MOVE;
		}

		return false;
	}

	private int getPointerIndex(MotionEvent ev, int id) {
		int activePointerIndex = MotionEventCompat.findPointerIndex(ev, id);
		if (activePointerIndex == -1)
			mActivePointerId = POINTER_INVALID;
		return activePointerIndex;
	}

	private boolean isInIgnoredViews(MotionEvent event){
		Rect rect = new Rect();
		if(mIgnoredViews == null){
			return false;
		}
		for (View v : mIgnoredViews) {
			v.getGlobalVisibleRect(rect);
			if (rect.contains((int) event.getX(), (int) event.getY()))
				return true;
		}
		return false;
	}

	private int getTopBorder(){
		return (int)(-mViewHeight + mInitialPositionY + dip2px(mContext,56));
	}

	private int getBottomBorder(){
		return (int)mInitialPositionY;
	}

	private int getRightBorder(){
		return 0;
	}

	private int getLeftBorder(){
		return 0;
	}

	private void toggleView(SlideDirection direction){

		// TODO 分情况讨论
		switch (direction){
			case LOCKED:
				buildSlideAnimation(this, 0);
				mSlideAnimation.start();
				mIsViewSlidedOut = false;
				break;
			case UP:
				buildSlideAnimation(this, -mViewHeight + dip2px(mContext, 56));
				mSlideAnimation.start();
				mIsViewSlidedOut = true;
				break;
		}
	}

	private SlideDirection getDirectionWithFling(SlideDirection initialDirection, int velocity, int totalDelta){
		SlideDirection targetPosition = SlideDirection.LOCKED;
		// TODO 分情况讨论

		if (Math.abs(totalDelta) > mFlingDistance && Math.abs(velocity) > mMinimumVelocity) {
			if (velocity > 0) {
				targetPosition = SlideDirection.LOCKED;
			} else if (velocity < 0){
				targetPosition = SlideDirection.UP;
			}
		} else {
			if(mIsViewSlidedOut){
				if(totalDelta < -mViewHeight * 0.5){
					targetPosition = SlideDirection.UP;
				}else{
					targetPosition = SlideDirection.LOCKED;
				}
			}else{
				if(totalDelta > -mViewHeight * 0.3){
					targetPosition = SlideDirection.LOCKED;
				}else{
					targetPosition = SlideDirection.UP;
				}
			}
		}

		return targetPosition;
	}

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
	private int getScrollVelocity(SlideDirection direction) {
		mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
		if(direction == SlideDirection.UP_DOWN){
			return (int) VelocityTrackerCompat.getYVelocity(mVelocityTracker, mActivePointerId);
		}else if (direction == SlideDirection.LEFT_RIGHT){
			return (int) VelocityTrackerCompat.getXVelocity(mVelocityTracker, mActivePointerId);
		}
		return 0;
	}

	/**
	 * 回收Tracker
	 */
	private void recycleVelocityTracker() {
		mVelocityTracker.recycle();
		mVelocityTracker = null;
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void toggleHardwareAcceleration(boolean value){
		final int layerType = value ? View.LAYER_TYPE_HARDWARE : View.LAYER_TYPE_NONE;

		if (layerType != getLayerType()) {
			getHandler().post(new Runnable() {
				public void run() {
					setLayerType(layerType, null);
				}
			});
		}
	}

	private void buildSlideAnimation(View target, float targetPosY){
		mSlideAnimation = new AnimatorSet();
		mSlideAnimation.playTogether(
				ObjectAnimator.ofFloat(target, "translationY", targetPosY)
		);
		mSlideAnimation.setInterpolator(new DecelerateInterpolator(4.0f));

		mSlideAnimation.setDuration(700);
		mSlideAnimation.addListener(mViewToggleListener);
	}

	private static int dip2px(Context context, float dipValue){
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int)(dipValue * scale + 0.5f);
	}
}
