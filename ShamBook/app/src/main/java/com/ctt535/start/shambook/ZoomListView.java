package com.ctt535.start.shambook;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ListView;


/**
 * Created by imal365 on 1/28/2016.
 */



public class ZoomListView extends ListView {

    private ScaleGestureDetector SGD;

    private View TouchedView = null;  // this is the view which is touched when you pinch zoom
    private int currentYscroll;  //this is the Y scroll of the List View when you start pinch zoom
    private int InitialXscroll; // this is the x scroll amout of the Horizontal Scroll view
    private int currentChildIndex; // this is the current pinch zoomin childs index in the list view

    private float bef_x;  //Pinch zoom center x calculated relative to the touched Item top corner
    private float bef_y;  //Pinch zoom center y calculated relative to the touched Item top corner

    private int YScroll_After_anEvent = 0;  //I use these variable to keep the Y scroll constatnt during a single pinch zoom event
    private int XScroll_After_anEvent = 0;

    private float scale = 1f;   // current Scale of the Elements
    private float previousScale = -1f; // previous scaled amount of the elements
    private float pinchScale = 1f; //for a scale gesture event how much the elements zoomed (thinking that previous zoom is 1 )

    private int prev_fomTop = 0;
    private int prev_fromLeft = 0;

    private final float MIN_ZOOM = 1.0f;
    private final float MAX_ZOOM = 4.0f;
    private boolean ScrollLock = false;

    private ZoomListView.PinchZoomListner pinchZoomListner;


    private CustomHorizontalScrollView scrollview;

    public ZoomListView(Context context) {
        super(context, null);
        onReady(context);
    }

    public ZoomListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        onReady(context);
    }

    public void onReady(Context context){

        SGD = new ScaleGestureDetector(context,new ScaleListener());

        this.setOnTouchListener(
                new OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent motionEvent) {
                        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN || motionEvent.getAction() == MotionEvent.ACTION_UP) {
                            Rect rect = new Rect();
                            int childCount = ZoomListView.this.getChildCount();
                            int[] listViewCoords = new int[2];
                            ZoomListView.this.getLocationOnScreen(listViewCoords);
                            int x = (int) motionEvent.getRawX() - listViewCoords[0];
                            int y = (int) motionEvent.getRawY() - listViewCoords[1];
                            View child;
                            for (int i = 0; i < childCount; i++) {
                                child = ZoomListView.this.getChildAt(i);
                                child.getHitRect(rect);
                                if (rect.contains(x, y)) {
                                    if (child != null) {
                                        TouchedView = child;
                                    }
                                    break;
                                }
                            }

                            if (TouchedView != null && ZoomListView.this != null) {
                                currentYscroll = TouchedView.getTop();
                                currentChildIndex = ZoomListView.this.getPositionForView(TouchedView);
                            }
                        }
                        if (scrollview != null) {
                            InitialXscroll = scrollview.getScrollX();
                        }
                        SGD.onTouchEvent(motionEvent);
                        return ScrollLock;
                    }
                }

        );
    }


    public View getTouchedView(){
        return TouchedView;
    }

    public int getCurrentYScroll(){
        return currentYscroll;
    }

    public void setCustomHorizontalScrollView(CustomHorizontalScrollView scrollview){
        this.scrollview = scrollview;
    }

    public void setPinchScaleListner(ZoomListView.PinchZoomListner listner){
        this.pinchZoomListner = listner;
    }



    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {


        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            super.onScaleEnd(detector);
            scrollview.setScrollingEnabled(true);
            pinchScale = 1f;
            YScroll_After_anEvent = TouchedView.getTop();
            XScroll_After_anEvent = scrollview.getScrollX();
            if(pinchZoomListner != null){
                pinchZoomListner.onPinchEnd();
            }
            ScrollLock = false;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            bef_y = detector.getFocusY();
            bef_x = detector.getFocusX();
            YScroll_After_anEvent = TouchedView == null ? currentYscroll : TouchedView.getTop();
            XScroll_After_anEvent = scrollview.getScrollX();

            prev_fomTop  = 0;
            prev_fromLeft = 0;
            pinchScale = 1f;
            System.out.println("Touched Point X : "+ bef_x);
            System.out.println("Touched Point Y : "+bef_y);
            scrollview.setScrollingEnabled(false);
            if(pinchZoomListner != null){
                pinchZoomListner.onPinchStarted();
            }
            return super.onScaleBegin(detector);
        }




        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float newscale = scale * detector.getScaleFactor();
            newscale = Math.max(MIN_ZOOM, Math.min(newscale, MAX_ZOOM));
            if(newscale != previousScale) {
                scale *= detector.getScaleFactor();
                scale = Math.max(MIN_ZOOM, Math.min(scale, MAX_ZOOM));
                previousScale = scale;
                pinchScale *= detector.getScaleFactor();
                pinchScale = MIN_ZOOM < scale ? Math.min(pinchScale, MAX_ZOOM) : MIN_ZOOM;

                if(pinchZoomListner != null){
                    pinchZoomListner.onPinchZoom(scale);
                }


                final int frmtop = - Math.round((bef_y * pinchScale - bef_y)) + Math.round(YScroll_After_anEvent * pinchScale);
                if (prev_fomTop != frmtop) {
                    prev_fomTop = frmtop;
                    ZoomListView.this.setSelectionFromTop(currentChildIndex, frmtop);
                }
                int frmLeft = Math.round((bef_x-XScroll_After_anEvent) * pinchScale - (bef_x-XScroll_After_anEvent)) + Math.round(XScroll_After_anEvent * pinchScale);
                if (prev_fromLeft != frmLeft) {
                    scrollview.setScrollX(frmLeft);
                }

               }else{
                    ScrollLock = true;
                }
            return true;
        }

        public int recal(int previous, float zoom){
            return (int)(previous * zoom);
        }


    }

    public static abstract class PinchZoomListner{
        public abstract void onPinchZoom(float zoom);
        public void onPinchStarted(){}
        public void onPinchEnd(){}

    }

}
