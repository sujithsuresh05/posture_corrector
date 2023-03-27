package com.hackthon.poseestimation.tensor;

import android.graphics.Bitmap;

import com.hackthon.poseestimation.body.Person;

import java.util.List;

public interface VisualEvaluator {

     List<Person> estimatePoses(Bitmap bitmap);

     void close() ;
}
