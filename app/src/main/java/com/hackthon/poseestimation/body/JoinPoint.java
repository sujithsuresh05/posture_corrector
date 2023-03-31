package com.hackthon.poseestimation.body;

import android.graphics.PointF;

public class JoinPoint {

    private final Part part;

    private PointF coordinate;

    // this score can be used to filter out the visible and not visible part
    private final float visibilityScore;


    public JoinPoint(Part part, PointF coordinate, float visibilityScore) {
        this.part = part;
        this.coordinate = coordinate;
        this.visibilityScore = visibilityScore;
    }

    public Part getPart() {
        return part.getFromPosition(part.getPosition());
    }

    public PointF getCoordinate() {
        return new PointF(coordinate.x, coordinate.y);
    }

    public float getVisibilityScore() {
        return new Float(visibilityScore);
    }

    public void setCoordinate(PointF coordinate) {
        this.coordinate = coordinate;
    }
}
