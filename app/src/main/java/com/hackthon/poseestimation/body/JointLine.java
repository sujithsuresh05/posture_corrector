package com.hackthon.poseestimation.body;


import java.util.Collections;
import java.util.List;

public class JointLine {

    private final List<JoinPoint> joinPointList;

    private final boolean error;

    private final JointLineName jointLineName;


    public JointLine(List<JoinPoint> joinPointList, boolean error, JointLineName jointLineName) {
        this.joinPointList = joinPointList;
        this.error = error;
        this.jointLineName = jointLineName;
    }

    public List<JoinPoint> getJoinPointList() {
        return Collections.unmodifiableList(joinPointList);
    }

    public boolean isError() {
        return error;
    }

    public JointLineName getJointLineName() {
        return jointLineName;
    }
}
