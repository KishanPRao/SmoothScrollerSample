package com.kishanprao.smoothscroller;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * Created by Kishan P Rao on 30/10/16.
 */

public class SmoothScroller extends FrameLayout {
	private static final String TAG = SmoothScroller.class.getSimpleName();
	private static final boolean VERBOSE = false;
	private final boolean mUseScrollBy;
	//	TODO: Lesser than threshold, thinner thumb
	private static final float THRESHOLD_VISIBLE = 2.0f;
	
	private RecyclerView mRecyclerView;
	private RecyclerView.OnScrollListener mOnScrollListener;
	
	private int mCurrentAdapterItemCount;
	private float mItemSpace;
	private float mCurrentPosition = Integer.MIN_VALUE;
	
	private float mStartPosition;
	private View mThumb;
	
	private CharacterSelectionCallback mCharacterSelectionCallback;
	private PositionListener mPositionListener;
	private boolean mEnableCharacterSelection = true;
	//	private ArrayList<String> mSelectionList;
	private int mPadding = 0;
	private int mThumbWidth = 0;
	
	public interface CharacterSelectionCallback {
		void selectedCharacter(char c, float y);
		
		void startSelection();
		
		void endSelection();
		
		String stringAtPosition(int position);
	}
	
	public interface PositionListener {
		void onFirstPositionChanged(int position);
	}
	
	private final Runnable mUpdateScrollerRunnable = new Runnable() {
		@Override
		public void run() {
			if (mRecyclerView == null) {
				return;
			}
			float threshold = (float) mRecyclerView.computeVerticalScrollRange() / mRecyclerView.computeVerticalScrollExtent();
			if (threshold >= THRESHOLD_VISIBLE) {
				setVisibility(VISIBLE);
				updateScrollerPosition();
			} else {
				setVisibility(GONE);
			}
			updateCalculations();
		}
	};
	
	private final Runnable mUpdateScrollerPositionRunnable = new Runnable() {
		@Override
		public void run() {
			if (mRecyclerView != null) {
				float scrollAmount = (float) mRecyclerView.computeVerticalScrollOffset() / (mRecyclerView.computeVerticalScrollRange() - mRecyclerView.computeVerticalScrollExtent());
				mStartPosition = scrollAmount * (getHeight() - mThumb.getHeight());
				mThumb.setY(mStartPosition);
				if (getWidth() > 0) {
					mPadding = (getWidth() - mThumbWidth) / 2;
//					setPadding(mPadding, mPadding, mPadding, mPadding);
					enablePadding();
				}
			}
		}
	};
	
	public void setPositionListener(PositionListener mPositionListener) {
		this.mPositionListener = mPositionListener;
	}
	
	public SmoothScroller(Context context, AttributeSet attributeSet) {
		super(context, attributeSet);
		mThumb = new View(context);
		TypedArray attributes = context.obtainStyledAttributes(attributeSet, R.styleable.SmoothScroller);
//		int thumbWidth = attributes.getDimensionPixelSize(R.styleable.SmoothScroller_thumbWidth, ViewGroup.LayoutParams.MATCH_PARENT);
		mThumbWidth = attributes.getDimensionPixelSize(R.styleable.SmoothScroller_thumbWidth, 0);
		int thumbWidth = ViewGroup.LayoutParams.MATCH_PARENT;
		int thumbHeight = attributes.getDimensionPixelSize(R.styleable.SmoothScroller_thumbHeight, 0);
		if (attributes.hasValue(R.styleable.SmoothScroller_thumbDrawable)) {
			Drawable thumbDrawable = attributes.getDrawable(R.styleable.SmoothScroller_thumbDrawable);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
				mThumb.setBackground(thumbDrawable);
			} else {
				mThumb.setBackgroundDrawable(thumbDrawable);
			}
		} else if (attributes.hasValue(R.styleable.SmoothScroller_thumbColor)) {
			int thumbColor = attributes.getColor(R.styleable.SmoothScroller_thumbColor, Color.WHITE);
			mThumb.setBackgroundColor(thumbColor);
		}
		mUseScrollBy = attributes.getBoolean(R.styleable.SmoothScroller_useScrollBy, false);
		LayoutParams layoutParams = new LayoutParams(thumbWidth, thumbHeight);
		layoutParams.gravity = Gravity.CENTER_HORIZONTAL;
		addView(mThumb, layoutParams);
		mOnScrollListener = new RecyclerView.OnScrollListener() {
			
			@Override
			public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
				float scrollAmount = (float) recyclerView.computeVerticalScrollOffset() / (recyclerView.computeVerticalScrollRange() - recyclerView.computeVerticalScrollExtent());
				mStartPosition = scrollAmount * (getHeight() - mThumb.getHeight());
				mThumb.setY(mStartPosition);
				
				if (mPositionListener != null) {
					mRecyclerView.post(new Runnable() {
						@Override
						public void run() {
							if (mPositionListener != null && mRecyclerView != null) {
								RecyclerView.LayoutManager layoutManager = mRecyclerView.getLayoutManager();
								mPositionListener.onFirstPositionChanged(((LinearLayoutManager) layoutManager).findFirstVisibleItemPosition());
							}
						}
					});
				}
				
				if (mEnableCharacterSelection && mCharacterSelectionCallback != null) {
					int position = -1;
					RecyclerView.LayoutManager layoutManager = mRecyclerView.getLayoutManager();
					if (layoutManager instanceof LinearLayoutManager) {
						position = ((LinearLayoutManager) layoutManager).findFirstCompletelyVisibleItemPosition();
//						position = (int) ((LinearLayoutManager) layoutManager).findFirstVisibleItemPosition();
					}
					if (position == -1) {
						if (VERBOSE) Log.w(TAG, "Unsupported Index!");
						return;
					}
					String string = null;
					try {
						string = mCharacterSelectionCallback.stringAtPosition(position);
					} catch (Exception e) {
						if (VERBOSE) e.printStackTrace();
					}
					if (string != null) {
						char c = ' ';
						if (string.length() > 0) {
							c = (string.toUpperCase().charAt(0));
						}
//						if (VERBOSE) Log.v(TAG, "Scroll to Pos:" + position + " " + c);
//				TODO: With y position! [Half]
						float yPos = mThumb.getY() + mThumb.getHeight() / 2;
						mCharacterSelectionCallback.selectedCharacter(c, yPos);
					}
				}
			}
		};
		setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					setSelected(true);
//					setPadding(0, 0, 0, 0);
					mThumb.setPressed(true);
					if (mEnableCharacterSelection && mCharacterSelectionCallback != null) {
						mCharacterSelectionCallback.startSelection();
					}
				} else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
					setSelected(false);
					enablePadding();
					mThumb.setPressed(false);
					if (mEnableCharacterSelection && mCharacterSelectionCallback != null) {
						mCharacterSelectionCallback.endSelection();
					}
				}
				float currentY = event.getY();
				scrollVerticallyTo(currentY);
				return true;
			}
		});
		attributes.recycle();
		setVisibility(GONE);
	}
	
	private void enablePadding() {
		setPadding(mPadding, 0, mPadding, 0);
	}
	
	private boolean needsUpdate() {
		return mCurrentAdapterItemCount != mRecyclerView.getAdapter().getItemCount();
	}
	
	public void setRecyclerView(RecyclerView recyclerView) {
		mRecyclerView = recyclerView;
		mRecyclerView.addOnScrollListener(mOnScrollListener);
		updateScroller();
	}
	
	public void updateScroller() {
		if (mRecyclerView == null || mRecyclerView.getAdapter() == null) {
			if (VERBOSE) Log.w(TAG, "Not Initialized yet; Cannot Update Scroller");
			return;
		}
		mRecyclerView.post(mUpdateScrollerRunnable);
	}
	
	public void scrollVerticallyTo(float y) {
		if (mRecyclerView == null || mRecyclerView.getAdapter() == null || getHeight() == 0) {
			if (VERBOSE) Log.w(TAG, "Not Initialized yet; Cannot Scroll");
			return;
		}
		if (y > getHeight()) {
			y = getHeight();
		}
		if (y < 0) {
			y = 0;
		}
		float ratio = y / getHeight();
		if (VERBOSE) Log.i(TAG, "scrollVerticallyTo:" + y + " " + ratio);
		float currentScroll = (ratio * (mRecyclerView.computeVerticalScrollRange() - mRecyclerView.computeVerticalScrollExtent())) - mRecyclerView.computeVerticalScrollOffset();
		if (VERBOSE) Log.d(TAG, "scrollVerticallyTo:" + currentScroll);
		if (VERBOSE)
			if (VERBOSE) Log.d(TAG, "scrollVerticallyTo:" + mRecyclerView.computeVerticalScrollRange() + " " + mRecyclerView.computeVerticalScrollExtent() + " " + mRecyclerView.computeVerticalScrollOffset());
		if (needsUpdate()) {
			updateCalculations();
		}
		if (mUseScrollBy) {
			mRecyclerView.scrollBy(0, (int) currentScroll);
		} else {
			if (mCurrentPosition == Integer.MIN_VALUE) {
				mCurrentPosition = ((float) mRecyclerView.computeVerticalScrollOffset() / (mRecyclerView.computeVerticalScrollRange() - mRecyclerView.computeVerticalScrollExtent())) * mRecyclerView.getAdapter().getItemCount();
			}
			if (VERBOSE) Log.v(TAG, "scrollVerticallyTo, previous:" + mCurrentPosition);
			mCurrentPosition = mCurrentPosition + (currentScroll / mItemSpace);
			if (mCurrentPosition < 0) {
				mCurrentPosition = 0;
			} else if (mCurrentPosition > (mRecyclerView.getAdapter().getItemCount() - 1)) {
				mCurrentPosition = (mRecyclerView.getAdapter().getItemCount() - 1);
			}
			mRecyclerView.scrollToPosition((int) mCurrentPosition);
			if (VERBOSE) Log.v(TAG, "scrollVerticallyTo:" + mCurrentPosition);
		}
	}
	
	public void setCharacterSelectionCallback(CharacterSelectionCallback characterSelectionCallback) {
		mCharacterSelectionCallback = characterSelectionCallback;
	}
	
	public void setEnableCharacterSelection(boolean enableCharacterSelection) {
		this.mEnableCharacterSelection = enableCharacterSelection;
	}
	
	//	public void setSelectionList(ArrayList<String> selectionList) {
//		mSelectionList = selectionList;
//	}
	
	public void updateScrollerPosition() {
		post(mUpdateScrollerPositionRunnable);
	}
	
	private void updateCalculations() {
		if (mRecyclerView == null || mRecyclerView.getAdapter() == null) {
			if (VERBOSE) Log.w(TAG, "Not Initialized yet; Cannot Update Calculations");
			return;
		}
		if (VERBOSE) Log.i(TAG, "Update Calculations");
		mCurrentAdapterItemCount = mRecyclerView.getAdapter().getItemCount();
		float numItemsVisible = ((float) mRecyclerView.computeVerticalScrollExtent() * mRecyclerView.getAdapter().getItemCount()) / ((float) mRecyclerView.computeVerticalScrollRange());
		mItemSpace = (float) mRecyclerView.computeVerticalScrollExtent() / numItemsVisible;
	}
	
	public void release() {
		if (mThumb != null) {
			if (mThumb.getBackground() != null) {
				mThumb.getBackground().setCallback(null);
			}
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
				mThumb.setBackground(null);
			} else {
				mThumb.setBackgroundDrawable(null);
			}
		}
		mRecyclerView = null;
	}
}
