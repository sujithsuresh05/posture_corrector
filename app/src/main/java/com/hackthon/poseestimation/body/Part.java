package com.hackthon.poseestimation.body;

public enum Part {
    NOSE(0),
    LEFT_EYE(1),
    RIGHT_EYE(2),
    LEFT_EAR(3),
    RIGHT_EAR(4),
    LEFT_SHOULDER(5),
    RIGHT_SHOULDER(6),
    LEFT_ELBOW(7),
    RIGHT_ELBOW(8),
    LEFT_WRIST(9),
    RIGHT_WRIST(10),
    LEFT_HIP(11),
    RIGHT_HIP(12),
    LEFT_KNEE(13),
    RIGHT_KNEE(14),
    LEFT_ANKLE(15),
    RIGHT_ANKLE(16);

    private final int partPosition;

    Part(int position) {
        partPosition = position;
    }

    public int getPosition() {
        return this.partPosition;
    }

    public static Part getFromPosition(int position) {
        return values()[position];
    }
}
