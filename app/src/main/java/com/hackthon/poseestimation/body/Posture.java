package com.hackthon.poseestimation.body;

import android.graphics.Bitmap;
import android.graphics.Color;

import java.util.List;

public interface Posture {

     static final float MIN_CONFIDENCE = .2f;

     Bitmap getBitMap();

}
