package com.hackthon.poseestimation.body;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.List;

public class SittingPosture implements Posture {

    /** Radius of circle used to draw keypoints.  */
    private float CIRCLE_RADIUS = 6f;

    /** Width of line used to connected two keypoints.  */
    private float LINE_WIDTH = 4f;

    /** The text size of the person id that will be displayed when the tracker is available.  */
    private float PERSON_ID_TEXT_SIZE = 30f;

    private Bitmap inputBitmap;

    private Bitmap outputBitMap;

    private List<JointLine> jointLines;



    private Integer lineColorNormal;

    private Integer lineColorError;

    private List<Person> personList;

    private Paint paintCircle;

    private Paint paintLineNormal;

    private Paint paintLineError;

    public SittingPosture(@NonNull Bitmap inputBitmap, @NonNull List<Person> personList) {
        this.inputBitmap = inputBitmap;
        this.personList = personList;
        this.outputBitMap = Bitmap.createBitmap(inputBitmap);
        lineColorNormal = Color.GREEN;
        lineColorError = Color.RED;

        paintCircle = new Paint();
        paintCircle.setStrokeWidth(CIRCLE_RADIUS);
        paintCircle.setColor(Color.MAGENTA);
        paintCircle.setStyle(Paint.Style.FILL);

        paintLineNormal = new Paint();
        paintLineNormal.setStrokeWidth(CIRCLE_RADIUS);
        paintLineNormal.setColor(Color.MAGENTA);
        paintLineNormal.setStyle(Paint.Style.STROKE);

        paintLineError = new Paint();
        paintLineError.setStrokeWidth(CIRCLE_RADIUS);
        paintLineError.setColor(Color.MAGENTA);
        paintLineError.setStyle(Paint.Style.STROKE);
        this.outputBitMap = inputBitmap.copy(Bitmap.Config.ARGB_8888, true);
        processBitMap(inputBitmap, personList);
    }

    private void processBitMap(Bitmap inputBitmap, List<Person> personList) {
        Canvas originalSizeCanvas = new Canvas(outputBitMap);
        personList.stream()
                //.filter(pl -> {
                //return pl.getVisibilityScore() > Posture.MIN_CONFIDENCE;
                //})
                .forEach(pl -> {
                            Arrays.stream(JointLineName.values()).forEach(jl -> {
                                Part[] parts = jl.getParts();
                                List<JoinPoint> joinPoints = pl.getJoinPoints();
                                JoinPoint joinPoint1 = joinPoints.get(parts[0].getPosition());
                                JoinPoint joinPoint2 = joinPoints.get(parts[1].getPosition());
                                if (joinPoint1.getVisibilityScore() > Posture.MIN_CONFIDENCE && joinPoint2.getVisibilityScore() > Posture.MIN_CONFIDENCE) {
                                    originalSizeCanvas.drawLine(joinPoint1.getCoordinate().x, joinPoint1.getCoordinate().y,
                                            joinPoint2.getCoordinate().x, joinPoint2.getCoordinate().y, paintLineNormal);
                                }
                            });
                        }
                );
    }

    @Override
    public Bitmap getBitMap() {
        return outputBitMap;
    }

}
