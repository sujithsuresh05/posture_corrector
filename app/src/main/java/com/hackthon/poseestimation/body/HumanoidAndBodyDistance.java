package com.hackthon.poseestimation.body;

public class HumanoidAndBodyDistance {
    float maxHumanoidXDistance;
    float maxHumanoidYDistance;
    float maxBodyXDistance;
    float maxBodyYDistance;

    public HumanoidAndBodyDistance(float maxHumanoidXDistance, float maxHumanoidYDistance, float maxBodyXDistance, float maxBodyYDistance) {
        this.maxHumanoidXDistance = maxHumanoidXDistance;
        this.maxHumanoidYDistance = maxHumanoidYDistance;
        this.maxBodyXDistance = maxBodyXDistance;
        this.maxBodyYDistance = maxBodyYDistance;
    }

    public float getMaxHumanoidXDistance() {
        return maxHumanoidXDistance;
    }

    public float getMaxHumanoidYDistance() {
        return maxHumanoidYDistance;
    }

    public float getMaxBodyXDistance() {
        return maxBodyXDistance;
    }

    public float getMaxBodyYDistance() {
        return maxBodyYDistance;
    }
}
