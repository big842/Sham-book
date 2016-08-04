package com.ctt535.start.shambook;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.HorizontalScrollView;

/**
 * Created by imal365 on 1/26/2016.
 */


public class CustomHorizontalScrollView extends HorizontalScrollView {

    private boolean mScrollable = true;

    public CustomHorizontalScrollView(Context context) {
        super(context, null);
    }

    public CustomHorizontalScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    public void setScrollingEnabled(boolean enabled) {
        mScrollable = enabled;
    }

    public boolean isScrollable() {
        return mScrollable;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (mScrollable) return super.onTouchEvent(ev);
                return mScrollable;
            default:
                return super.onTouchEvent(ev);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!mScrollable) return false;
        else return super.onInterceptTouchEvent(ev);
    }

}
