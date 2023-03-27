package com.hackthon.poseestimation.body;

import java.util.List;

public class Person {

    private final List<JoinPoint> joinPoints;

    private final float visibilityScore;


    public Person(List<JoinPoint> joinPoints, float visibilityScore) {
        this.joinPoints = joinPoints;
        this.visibilityScore = visibilityScore;
    }

    public List<JoinPoint> getJoinPoints() {
        return joinPoints;
    }

    public float getVisibilityScore() {
        return visibilityScore;
    }
}
