package com.bignerdranch.android.draganddraw.model;

import android.graphics.PointF;

/**
 * Created by michaeltan on 2017/8/22.
 */

public class Box {
    private PointF mOrigin;
    private PointF mCurrent;
    private float angle;

    public float getAngle() {
        return angle;
    }

    public void setAngle(float angle) {
        this.angle = angle;
    }

    public Box(PointF origin) {
        mOrigin = origin;
        mCurrent = origin;
    }

    public PointF getOrigin() {
        return mOrigin;
    }

    public PointF getCurrent() {
        return mCurrent;
    }

    public void setCurrent(PointF current) {
        mCurrent = current;
    }
}
