package com.encodex.widget;

import android.content.Context;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.VelocityTrackerCompat;
import android.support.v4.view.ViewConfigurationCompat;
import android.support.v4.widget.ScrollerCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.view.animation.Interpolator;
import android.widget.RelativeLayout;

/**
 * Created with Android Studio.
 * Author: Enex Tapper
 * Date: 15/11/2
 * Project: SlidableLayout
 * Package: com.encodex.widget
 */
public class DraggableLayoutOld extends RelativeLayout {

	private static final int INVALID_POINTER = -1;
	private static final int MAX_SETTLE_DURATION = 600; // ms
	private static final int MIN_DISTANCE_FOR_FLING = 25; // dips

	private static final Interpolator sInterpolator = new Interpolator() {
		public float getInterpolation(float t) {
			t -= 1.0f;
			return t * t * t * t * t + 1.0f;
		}
	};

	private static final int POSITION_DEFAULT = 0;
	private static final int POSITION_TOP = -1;
	private static final int POSITION_BOTTOM = 1;

	private Context mContext;

	private VelocityTracker mVelocityTracker;
	private int mMinimumVelocity;
	protected int mMaximumVelocity;
	private int mFlingDistance;
	private ScrollerCompat mScroller;

	private boolean mIsEnabled;
	private boolean mIsDraggable;
	private boolean mIsDragging;
	private boolean mIsScrolling;
	private boolean mQuickReturn;
	private boolean mIsViewInitialized;

	private float mViewWidth;
	private float mViewHeight;
	private float mActionBarHeight;

	private float mScrollY;
	private float mLastMotionY;
	private float mInitialMotionY;
	private float mLastMotionX;
	private float mTouchSlop;

	private int mCurrentPosition;

	private int mActivePointerId;

	public DraggableLayoutOld(Context context) {
		this(context, null);
	}

	public DraggableLayoutOld(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public DraggableLayoutOld(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);

		mContext = context;
		onCreateView();
	}

	private void onCreateView(){
		setWillNotDraw(false);
		setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
		setFocusable(true);

		final Context context = getContext();
		mScroller = ScrollerCompat.create(context,sInterpolator);

		final ViewConfiguration configuration = ViewConfiguration.get(context);
		mTouchSlop = ViewConfigurationCompat.getScaledPagingTouchSlop(configuration);
		mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
		mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();

		final float density = context.getResources().getDisplayMetrics().density;
		mFlingDistance = (int) (MIN_DISTANCE_FOR_FLING * density);

		// 临时初始化
		mViewHeight = 0.0f;
		mViewWidth = 0.0f;
		mActionBarHeight = 0.0f;

		mIsEnabled = true;
		mIsDraggable = true;
		mIsDragging = false;
		mIsScrolling = false;
		mQuickReturn = false;
		mIsViewInitialized = false;

		mScrollY = 0.0f;
		mLastMotionX = mInitialMotionY = mLastMotionY = 0.0f;
		mCurrentPosition = POSITION_DEFAULT;
		mActivePointerId = INVALID_POINTER;
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {

		Log.d("DraggableLayout","Entered on intercept touch event");

		if(!mIsEnabled) return false;

		if(!mIsViewInitialized) initViewSize();

		final int action = ev.getAction() & MotionEventCompat.ACTION_MASK;

		if(action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP
				|| (action!=MotionEvent.ACTION_DOWN && !mIsDraggable)){
			endDrag();
			return false;
		}

		switch (action){
			case MotionEvent.ACTION_DOWN:
				Log.d("DraggableLayout","On intercept touch event: Action down");
				int index = MotionEventCompat.getActionIndex(ev);
				mActivePointerId = MotionEventCompat.getPointerId(ev, index);
				if (mActivePointerId == INVALID_POINTER)
					break;

				mLastMotionY = mInitialMotionY = MotionEventCompat.getY(ev, index);
				mLastMotionX = MotionEventCompat.getX(ev, index);

				if (thisTouchAllowed(ev)) {
					Log.d("DraggableLayout","On intercept touch event: Action down: Touch allowed");
					mIsDragging = false;
					mIsDraggable = true;
					// todo quick return
//					if (mIsViewDraggedUp && mViewBehind.menuTouchInQuickReturn(mContent, mCurItem, ev.getX() + mScrollX)) {
//						mQuickReturn = true;
//					}
					if (isViewDraggedUp()) {
						mQuickReturn = true;
					}
				} else {
					mIsDraggable = false;
				}
				break;
			case MotionEvent.ACTION_MOVE:
				Log.d("DraggableLayout","On intercept touch event: Action move");
				determineDrag(ev);
				break;
			case MotionEventCompat.ACTION_POINTER_UP:
				break;
		}

		if (!mIsDragging) {
			if (mVelocityTracker == null) {
				mVelocityTracker = VelocityTracker.obtain();
			}
			mVelocityTracker.addMovement(ev);
		}

		return mIsDragging || mQuickReturn;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		Log.d("DraggableLayout","Entered on touch event");

		if(!mIsEnabled) return false;

		if(!mIsDragging && !thisTouchAllowed(event)) return false;

		final int action = event.getAction() & MotionEventCompat.ACTION_MASK;

		if (mVelocityTracker == null) {
			mVelocityTracker = VelocityTracker.obtain();
		}
		mVelocityTracker.addMovement(event);

		switch (action){
			case MotionEvent.ACTION_DOWN:
				Log.d("DraggableLayout","On touch event: Action down");
				completeScroll();

				int index = MotionEventCompat.getActionIndex(event);
				mActivePointerId = MotionEventCompat.getPointerId(event, index);
				mLastMotionY = mInitialMotionY = event.getY();
				break;
			case MotionEvent.ACTION_MOVE:
				Log.d("DraggableLayout","On touch event: Action move");
				if(!mIsDragging){
					determineDrag(event);
					if(!mIsDraggable) return false;
				}

				if(mIsDragging){
					final int activePointerIndex = getPointerIndex(event, mActivePointerId);
					if (mActivePointerId == INVALID_POINTER)
						break;
					final float y = MotionEventCompat.getY(event, activePointerIndex);
					final float deltaY = mLastMotionY - y;
					mLastMotionY = y;

					Log.d("DraggableLayout","On touch event: last motion Y: "+mLastMotionY+ " deltaY: "+deltaY);

					float oldScrollY = getScrollY();
					float scrollY = oldScrollY + deltaY;
					final float topBound = getTopBound();
					final float bottomBound = getBottomBound();

					if (scrollY < topBound) {
						scrollY = topBound;
					} else if (scrollY > bottomBound) {
						scrollY = bottomBound;
					}

					Log.d("DraggableLayout","On touch event: scrollY: "+scrollY+ " topBound: "+topBound+ " bottomBound: "+bottomBound);

					mLastMotionY += scrollY - (int) scrollY;
					scrollTo(getScrollX(), (int) scrollY);
				}
				break;
			case MotionEvent.ACTION_UP:
				Log.d("DraggableLayout","On touch event: Action up");
				if(mIsDragging){
					final VelocityTracker velocityTracker = mVelocityTracker;
					velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
					int initialVelocity = (int) VelocityTrackerCompat.getXVelocity(
							velocityTracker, mActivePointerId);
					final int scrollY = getScrollY();
					final float offset = (float) (scrollY - getDestScrollY(POSITION_DEFAULT)) / getViewHeight();
					final int activePointerIndex = getPointerIndex(event, mActivePointerId);

					if (mActivePointerId != INVALID_POINTER){
						final float y = MotionEventCompat.getY(event, activePointerIndex);
						final int totalDelta = (int) (y - mInitialMotionY);
						int scrollDirection = determineScroll(offset, initialVelocity, totalDelta);
						applyScroll(scrollDirection, true, initialVelocity);
					}
				}
				// todo Quick Return
//				else if(mQuickReturn && mViewBehind.menuTouchInQuickReturn(mContent, mCurItem, ev.getX() + mScrollX)){
//					// close the menu
//					setCurrentItem(1);
//					endDrag();
//				}
				mActivePointerId = INVALID_POINTER;
				endDrag();
				break;
			case MotionEvent.ACTION_CANCEL:
				if (mIsDragging) {
					applyScroll(mCurrentPosition, true, 0);
					mActivePointerId = INVALID_POINTER;
					endDrag();
				}
				break;
			case MotionEventCompat.ACTION_POINTER_DOWN:
				break;
			case MotionEventCompat.ACTION_POINTER_UP:
				break;
		}

		return true;
	}

	@Override
	public void scrollTo(int x, int y) {
		super.scrollTo(x, y);
		mScrollY = y;
//		mViewBehind.scrollBehindTo(mContent, x, y);
//		((SlidingMenu)getParent()).manageLayers(getPercentOpen());
	}

	private void initViewSize(){
		mViewHeight = Double.valueOf(this.getHeight()).floatValue();
		mViewWidth = Double.valueOf(this.getWidth()).floatValue();

		TypedValue tv = new TypedValue();
		if (getContext().getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true))
		{
			mActionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data,getResources().getDisplayMetrics());
		}

		mIsViewInitialized = true;
	}

	private void startDrag() {
		mIsDragging = true;
		mQuickReturn = false;
	}

	private void endDrag(){
		mQuickReturn = false;
		mIsDragging = false;
		mIsDraggable = true;
		mActivePointerId = INVALID_POINTER;

		if (mVelocityTracker != null) {
			mVelocityTracker.recycle();
			mVelocityTracker = null;
		}
	}

	private boolean thisTouchAllowed(MotionEvent ev) {
		int y = (int) (ev.getY() + mScrollY);
		// Todo 暂且全部放行
		return true;
//		if (mIsViewDraggedUp) {
//			return mViewBehind.menuOpenTouchAllowed(mContent, mCurItem, x);
//		} else {
//			switch (mTouchMode) {
//				case SlidingMenu.TOUCHMODE_FULLSCREEN:
//					return !isInIgnoredView(ev);
//				case SlidingMenu.TOUCHMODE_NONE:
//					return false;
//				case SlidingMenu.TOUCHMODE_MARGIN:
//					return mViewBehind.marginTouchAllowed(mContent, x);
//			}
//		}
//		return false;
	}

	private void completeScroll() {
		if (mIsScrolling) {
			// Done with scroll, no longer want to cache view drawing.
//			setScrollingCacheEnabled(false);
			mScroller.abortAnimation();
			int oldX = getScrollX();
			int oldY = getScrollY();
			int x = mScroller.getCurrX();
			int y = mScroller.getCurrY();
			if (oldX != x || oldY != y) {
				scrollTo(x, y);
			}
//			if (mIsViewDraggedUp) {
//				if (mOpenedListener != null)
//					mOpenedListener.onOpened();
//			} else {
//				if (mClosedListener != null)
//					mClosedListener.onClosed();
//			}
		}
		mIsScrolling = false;
	}

	private void determineDrag(MotionEvent ev) {
		final int activePointerId = mActivePointerId;
		final int pointerIndex = getPointerIndex(ev, activePointerId);
		if (activePointerId == INVALID_POINTER || pointerIndex == INVALID_POINTER)
			return;
		final float x = MotionEventCompat.getX(ev, pointerIndex);
		final float dx = x - mLastMotionX;
		final float xDiff = Math.abs(dx);
		final float y = MotionEventCompat.getY(ev, pointerIndex);
		final float dy = y - mLastMotionY;
		final float yDiff = Math.abs(dy);
		if (yDiff > (isViewDraggedUp()?mTouchSlop/2:mTouchSlop) && yDiff > xDiff && thisSlideAllowed(dy)) {
			Log.d("DraggableLayout","Determine Drag: allowed");
			startDrag();
			mLastMotionX = x;
			mLastMotionY = y;
		} else if (xDiff > mTouchSlop) {
			Log.d("DraggableLayout","Determine Drag: stopped");
			mIsDraggable = false;
		}
	}

	private boolean isViewDraggedUp(){
		return mCurrentPosition != POSITION_DEFAULT;
	}

	private boolean thisSlideAllowed(float dy) {
		// TODO 暂且全部放行
		boolean allowed = false;
//		if (mIsViewDraggedUp) {
//			allowed = mViewBehind.menuOpenSlideAllowed(dy);
//		} else {
//			allowed = mViewBehind.menuClosedSlideAllowed(dy);
//		}
		return true;
	}

	private int getPointerIndex(MotionEvent ev, int id) {
		int activePointerIndex = MotionEventCompat.findPointerIndex(ev, id);
		if (activePointerIndex == -1)
			mActivePointerId = INVALID_POINTER;
		return activePointerIndex;
	}

	private int getTopBound() {
		// todo 获得上边缘

		return 0;
	}

	private int getBottomBound() {
		// todo 获得下边缘

		return (int)(mViewHeight - mActionBarHeight);
	}

	private int getDestScrollY(int position) {
		switch (position) {
			case POSITION_DEFAULT:
				return getBottomBound();
			case POSITION_TOP:
				return getTopBound();
		}
		// Todo 滚动目标点
		return 0;
	}

	private int getViewHeight(){
		// todo View高度

		return (int)mViewHeight;
	}

	private int determineScroll(float offset, int velocity, int deltaY){
		int targetPosition = POSITION_DEFAULT;

		if (Math.abs(deltaY) > mFlingDistance && Math.abs(velocity) > mMinimumVelocity) {
			if (velocity > 0 && deltaY > 0) {
				targetPosition = POSITION_DEFAULT;
			} else if (velocity < 0 && deltaY < 0){
				targetPosition = POSITION_TOP;
			}
		} else {
			targetPosition = Math.round(offset);
		}

		return targetPosition;
	}

	void applyScroll(int position, boolean smoothScroll, int velocity) {

		final int destY = getDestScrollY(position);

		mCurrentPosition = position;

		Log.d("DraggableLayout","Apply Scroll, position: "+ position + ", destY: "+ destY);

		if (smoothScroll) {
			smoothScrollTo(0, destY, velocity);
		} else {
			completeScroll();
			scrollTo(0, destY);
		}
	}

	void smoothScrollTo(int x, int y, int velocity) {
		int sx = getScrollX();
		int sy = getScrollY();
		int dx = x - sx;
		int dy = y - sy;
		Log.d("DraggableLayout","Smooth Scroll, scrollY: "+ getScrollY());
		if (dx == 0 && dy == 0) {
			completeScroll();
			if (isViewDraggedUp()) {
				// Todo
//				if (mOpenedListener != null)
//					mOpenedListener.onOpened();
			} else {
//				if (mClosedListener != null)
//					mClosedListener.onClosed();
			}
			return;
		}

		mIsScrolling = true;

		final int height = getViewHeight();
		final int halfHeight = height / 2;
		final float distanceRatio = Math.min(1f, 1.0f * Math.abs(dy) / height);
		final float distance = halfHeight + halfHeight * distanceInfluenceForSnapDuration(distanceRatio);

		int duration = 0;
		velocity = Math.abs(velocity);
		if (velocity > 0) {
			duration = 4 * Math.round(1000 * Math.abs(distance / velocity));
		} else {
			final float pageDelta = (float) Math.abs(dy) / height;
			duration = (int) ((pageDelta + 1) * 100);
			duration = MAX_SETTLE_DURATION;
		}
		duration = Math.min(duration, MAX_SETTLE_DURATION);
		Log.d("DraggableLayout","Smooth Scroll "+ sx+" "+ sy+" "+ dx+" "+ dy+" "+duration);

		mScroller.startScroll(sx, sy, dx, dy, duration);
		invalidate();
	}

	private static float distanceInfluenceForSnapDuration(float f) {
		f -= 0.5f; // center the values about 0.
		f *= 0.3f * Math.PI / 2.0f;
		return (float) Math.sin(f);
	}

	private static int dip2px(Context context, float dipValue){
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int)(dipValue * scale + 0.5f);
	}
}
