package com.hackthon.poseestimation.body;

public enum JointLineName {

    NOSE_LEFT_EYE("NOSE LEFT EYE", new Part[]{Part.NOSE, Part.LEFT_EYE}),
    NOSE_RIGHT_EYE("NOSE RIGHT EYE", new Part[]{Part.NOSE, Part.RIGHT_EYE}),
    LEFT_SHOULDER_LEFT_ELBOW("LEFT SHOULDER LEFT ELBOW", new Part[]{Part.LEFT_SHOULDER, Part.LEFT_ELBOW}),
    RIGHT_SHOULDER_RIGHT_ELBOW("RIGHT SHOULDER RIGHT ELBOW", new Part[]{Part.RIGHT_SHOULDER, Part.RIGHT_ELBOW}),
    LEFT_ELBOW_LEFT_WRIST("LEFT ELBOW LEFT WRIST", new Part[]{Part.LEFT_ELBOW, Part.LEFT_WRIST}),
    RIGHT_ELBOW_RIGHT_WRIST("RIGHT ELBOW RIGHT WRIST", new Part[]{Part.RIGHT_ELBOW, Part.RIGHT_WRIST}),
    RIGHT_ELBOW_LEFT_ELBOW("RIGHT ELBOW LEFT ELBOW", new Part[]{Part.RIGHT_ELBOW, Part.LEFT_ELBOW}),
    RIGHT_ELBOW_RIGHT_HIP("RIGHT ELBOW RIGHT HIP", new Part[]{Part.RIGHT_ELBOW, Part.RIGHT_HIP}),
    LEFT_ELBOW_LEFT_HIP("RIGHT HIP LEFT HIP", new Part[]{Part.RIGHT_HIP, Part.LEFT_HIP}),
    RIGHT_HIP_RIGHT_KNEE("RIGHT HIP RIGHT KNEE", new Part[]{Part.RIGHT_HIP, Part.RIGHT_KNEE}),
    RIGHT_KNEE_RIGHT_ANKLE("RIGHT KNEE RIGHT ANKLE", new Part[]{Part.RIGHT_KNEE, Part.RIGHT_ANKLE}),
    LEFT_HIP_LEFT_KNEE("LEFT HIP LEFT KNEE", new Part[]{Part.LEFT_HIP, Part.LEFT_ANKLE}),
    LEFT_KNEE_LEFT_ANKLE("LEFT KNEE LEFT ANKLE", new Part[]{Part.LEFT_KNEE, Part.LEFT_ANKLE});

    private final String mLineName;

    private final Part[] parts;

    JointLineName(String lineName,Part[] parts) {
        this.mLineName = lineName;
        this.parts = parts;
    }

    public Part[] getParts() {
        return this.parts;
    }

    public String getLineName() {
        return this.mLineName;
    }
}
