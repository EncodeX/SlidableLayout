package com.encodex.widget;

import android.content.Context;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewConfigurationCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * SlidableLayout
 *
 * 灵感来源于ResideMenu
 *
 */

public class SlidableLayout extends FrameLayout{
	private static final int MIN_FLING_DISTANCE = 25; // dips

	//触摸状态
	public enum State {
		IDLE,
		TOUCH_UP,
		TOUCH_DOWN,
		TOUCH_MOVE_HORIZONTAL,
		TOUCH_MOVE_VERTICAL,
		SLIDE
	}

	//滑动方向
	public enum SlideDirection{
		UP,
		DOWN,
		LEFT,
		RIGHT,
		UP_DOWN,
		LEFT_RIGHT,
		FREE
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

	private boolean mEnabled;
	private boolean mIsViewInitialized;

	private VelocityTracker mVelocityTracker;
	private int mMinimumVelocity;
	protected int mMaximumVelocity;
	private int mFlingDistance;
	private float mTouchSlop;

	private float mViewHeight;
	private float mViewWidth;
	private float mInitialPositionX;
	private float mInitialPositionY;
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

		setWillNotDraw(false);
		setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
		setFocusable(true);

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

		// 临时初始化
		mEnabled = true;
		mIsViewInitialized = false;

		mState = State.IDLE;
		mSlideDirection = SlideDirection.UP;
		mContentYOffset = -0.9f;
	}

	private void initViewSize(){
		mViewWidth = Double.valueOf(this.getWidth()).floatValue();
		mViewHeight = Double.valueOf(this.getHeight()).floatValue();

		mIsViewInitialized = true;
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		if(!mEnabled) return false;
		if(!mIsViewInitialized) initViewSize();

		final int action = MotionEventCompat.getActionMasked(ev);

		switch (action) {
			case MotionEvent.ACTION_DOWN:
				Log.d("DragHelper", "dispatchTouchEvent ACTION_DOWN");
				mState = State.TOUCH_DOWN;
				break;
			case MotionEvent.ACTION_MOVE:
				Log.d("DragHelper", "dispatchTouchEvent ACTION_MOVE");
				break;
			case MotionEvent.ACTION_UP:
				Log.d("DragHelper", "dispatchTouchEvent ACTION_UP");
				break;
			case MotionEvent.ACTION_CANCEL:
				Log.d("DragHelper", "dispatchTouchEvent ACTION_CANCEL");
				finishSlide();
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
		return mState!=State.TOUCH_DOWN && mState!= State.TOUCH_UP && mState!= State.SLIDE;
	}

	private void finishSlide(){
		// TODO
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
}
