package com.hackthon.poseestimation.tensor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;

import com.hackthon.poseestimation.body.HumanoidAndBodyDistance;
import com.hackthon.poseestimation.body.JoinPoint;
import com.hackthon.poseestimation.body.Part;
import com.hackthon.poseestimation.body.Person;

import org.checkerframework.checker.units.qual.A;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MoveNetVisualEvaluator implements VisualEvaluator {

    private static final String THUNDER_FILENAME = "movenet_thunder.tflite";
    //private static final String LIGHTER_FILENAME = "movenet_lightning.tflite";
    private static final int CPU_NUM_THREADS = 4;

    private static final float MIN_CROP_KEYPOINT_SCORE = .2f;

    // Parameters that control how large crop region should be expanded from previous frames'
    // body keypoints.
    private static final float TORSO_EXPANSION_RATIO = 1.9f;
    private static final float BODY_EXPANSION_RATIO = 1.2f;

    private final Context mContext;
    private Interpreter interpreter;

    private int inputWidth;

    private int inputHeight;

    private RectF cropRegion;

    public MoveNetVisualEvaluator(Context context) throws IOException {
        this.mContext = context;
        Interpreter.Options options = new Interpreter.Options();
        options.setNumThreads(CPU_NUM_THREADS);
        options.addDelegate(new GpuDelegate());
        interpreter = new Interpreter(FileUtil.loadMappedFile(
                context,
                THUNDER_FILENAME
        ));
        inputWidth = interpreter.getInputTensor(0).shape()[1];
        inputHeight = interpreter.getInputTensor(0).shape()[2];

    }

    private TensorImage processInputImage(Bitmap bitmap, int targetHeight, int targetWidth) {

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        int size = (height > width) ? width : height;
        // Initialization code
        // Create an ImageProcessor with all ops required. For more ops, please
        // refer to the ImageProcessor Architecture section in this README.
        ImageProcessor imageProcessor =
                new ImageProcessor.Builder()
                        .add(new ResizeWithCropOrPadOp(size, size))
                        .add(new ResizeOp(targetHeight, targetWidth, ResizeOp.ResizeMethod.BILINEAR))
                        .build();

        TensorImage tensorImage = new TensorImage(DataType.UINT8);

        // Analysis code for every frame
        // Preprocess the image
        tensorImage.load(bitmap);
        tensorImage = imageProcessor.process(tensorImage);
        return tensorImage;
    }

    @Override
    public List<Person> estimatePoses(Bitmap bitmap) {

        int[] outputArray = interpreter.getOutputTensor(0).shape();

        float totalScore = 0f;
        int numKeyPoints = outputArray[2];
        if (cropRegion == null) {
            cropRegion = getRectF(bitmap.getWidth(), bitmap.getHeight());
        }

        RectF rect = new RectF(
                (cropRegion.left * bitmap.getWidth()),
                (cropRegion.top * bitmap.getHeight()),
                (cropRegion.right * bitmap.getWidth()),
                (cropRegion.bottom * bitmap.getHeight())
        );
        Bitmap detectBitmap = Bitmap.createBitmap(
                (int) rect.width(),
                (int) rect.height(),
                Bitmap.Config.ARGB_8888
        );
        Canvas canvas = new Canvas(detectBitmap);
        canvas.drawBitmap(bitmap, -rect.left, -rect.top, null);

        // generate tensor image
        TensorImage inputTensorImage = processInputImage(detectBitmap, inputWidth, inputHeight);
        TensorBuffer outputTensorBuffer = TensorBuffer.createFixedSize(outputArray, DataType.FLOAT32);
        float widthRatio = ((float) detectBitmap.getWidth()) / inputWidth;
        float heightRatio = ((float) detectBitmap.getHeight()) / inputHeight;

        List<JoinPoint> jointPoints = new ArrayList<>();
        List<Float> positions = new ArrayList<>();

        interpreter.run(inputTensorImage.getTensorBuffer().getBuffer(), outputTensorBuffer.getBuffer().rewind());
        float[] output = outputTensorBuffer.getFloatArray();
        for (int id = 0; id < numKeyPoints; id++) {
            float x = output[id * 3 + 1] * inputWidth * widthRatio;
            float y = output[id * 3 + 0] * inputHeight * heightRatio;

            positions.add(x);
            positions.add(y);
            float score = output[id * 3 + 2];
            jointPoints.add(
                    new JoinPoint(
                            Part.getFromPosition(id),
                            new PointF(x, y),
                            score
                    )
            );
            totalScore += score;
        }

        Matrix matrix = new Matrix();
        float[] floatArray = new float[positions.size()];
        for (int i = 0; i < positions.size(); i++) {
            floatArray[i] = (float) positions.get(i);
        }
        matrix.postTranslate(rect.left, rect.top);
        matrix.mapPoints(floatArray);



        for (int i = 0; i < jointPoints.size(); i++) {
            jointPoints.get(i).setCoordinate(new PointF(floatArray[i * 2],
                    floatArray[i * 2 + 1]));
        }
        Person person = new Person(jointPoints, totalScore / numKeyPoints);
        List<Person> personList = new ArrayList<>();
        personList.add(person);
        //TensorImage inputTensor = processInputImage(bitmap,)
        // new crop region
        cropRegion = determineRectF(jointPoints, bitmap.getWidth(), bitmap.getHeight());
        return personList;
    }

    private RectF determineRectF(List<JoinPoint> joinPoints, Integer imageWidth, Integer imageHeight) {
        List<JoinPoint> targetJoinPoints = new ArrayList<JoinPoint>();
        joinPoints.forEach(jp -> {
            targetJoinPoints.add(new JoinPoint(jp.getPart(), new PointF(jp.getCoordinate().x, jp.getCoordinate().y), jp.getVisibilityScore()));
        });
        if (humanoidVisible(targetJoinPoints)) {
            float centerX =
                    (targetJoinPoints.get(Part.LEFT_HIP.getPosition()).getCoordinate().x +
                            targetJoinPoints.get(Part.RIGHT_HIP.getPosition()).getCoordinate().x) / 2f;
            float centerY =
                    (targetJoinPoints.get(Part.LEFT_HIP.getPosition()).getCoordinate().y +
                            targetJoinPoints.get(Part.RIGHT_HIP.getPosition()).getCoordinate().y) / 2f;

            HumanoidAndBodyDistance humanoidAndBodyDistance = determineTorsoAndBodyDistances(joinPoints, targetJoinPoints, centerX, centerY);
            Float[] bodyDistances = new Float[]{
                    humanoidAndBodyDistance.getMaxHumanoidXDistance() * TORSO_EXPANSION_RATIO,
                    humanoidAndBodyDistance.getMaxHumanoidYDistance() * TORSO_EXPANSION_RATIO,
                    humanoidAndBodyDistance.getMaxBodyXDistance() * BODY_EXPANSION_RATIO,
                    humanoidAndBodyDistance.getMaxBodyYDistance() * BODY_EXPANSION_RATIO
            };
            float cropLengthHalf = Collections.max(Arrays.asList(bodyDistances));
            Float[] tmp = new Float[]{
                    centerX, imageWidth - centerX, centerY, imageHeight - centerY
            };
            cropLengthHalf = Math.min(cropLengthHalf, Collections.max(Arrays.asList(tmp)));
            float first = centerY - cropLengthHalf;
            float second = centerX - cropLengthHalf;

            if (cropLengthHalf > Math.max(imageWidth, imageHeight) / 2f) {
                return getRectF(imageWidth, imageHeight);
            } else {
                float cropLength = cropLengthHalf * 2;
                return new RectF(
                        second / imageWidth,
                        first / imageHeight,
                        (second + cropLength) / imageWidth,
                        (first + cropLength) / imageHeight
                );
            }
        } else {
            return getRectF(imageWidth, imageHeight);
        }
    }

    private HumanoidAndBodyDistance determineTorsoAndBodyDistances(
            List<JoinPoint> joinPoints,
            List<JoinPoint> targetJoinPoints,
            float centerX,
            float centerY
    ) {
        Integer[] points = new Integer[]{
                Part.LEFT_SHOULDER.getPosition(),
                Part.RIGHT_SHOULDER.getPosition(),
                Part.LEFT_HIP.getPosition(),
                Part.RIGHT_HIP.getPosition()};
        List<Integer> torsoJoints = Arrays.asList(points);

        float maxTorsoYRange = 0f;
        float maxTorsoXRange = 0f;
        for (int joint : torsoJoints) {
            float distY = Math.abs(centerY - targetJoinPoints.get(joint).getCoordinate().y);
            float distX = Math.abs(centerX - targetJoinPoints.get(joint).getCoordinate().x);
            if (distY > maxTorsoYRange) maxTorsoYRange = distY;
            if (distX > maxTorsoXRange) maxTorsoXRange = distX;
        }

        float maxBodyYRange = 0f;
        float maxBodyXRange = 0f;
        for (int joint : torsoJoints) {
            if (joinPoints.get(joint).getVisibilityScore() < MIN_CROP_KEYPOINT_SCORE)
                continue;
            float distY = Math.abs(centerY - joinPoints.get(joint).getCoordinate().y);
            float distX = Math.abs(centerX - joinPoints.get(joint).getCoordinate().x);

            if (distY > maxBodyYRange) maxBodyYRange = distY;
            if (distX > maxBodyXRange) maxBodyXRange = distX;
        }
        return new HumanoidAndBodyDistance(
                maxTorsoYRange,
                maxTorsoXRange,
                maxBodyYRange,
                maxBodyXRange
        );
    }

    private boolean humanoidVisible(List<JoinPoint> joinPoints) {
        return ((joinPoints.get(Part.LEFT_HIP.getPosition()).getVisibilityScore() > MIN_CROP_KEYPOINT_SCORE) || (
                joinPoints.get(Part.RIGHT_HIP.getPosition()).getVisibilityScore() > MIN_CROP_KEYPOINT_SCORE))
                && (
                (joinPoints.get(Part.LEFT_SHOULDER.getPosition()).getVisibilityScore() > MIN_CROP_KEYPOINT_SCORE) || (
                        joinPoints.get(Part.RIGHT_SHOULDER.getPosition()).getVisibilityScore() > MIN_CROP_KEYPOINT_SCORE
                ));
    }

    private RectF getRectF(int imageWidth, int imageHeight) {
        float xMin;
        float yMin;
        float width;
        float height;
        if (imageWidth > imageHeight) {
            width = 1f;
            height = (float) imageWidth / imageHeight;
            xMin = 0f;
            yMin = (imageHeight / 2f - imageWidth / 2f) / imageHeight;
        } else {
            height = 1f;
            width = imageHeight / imageWidth;
            yMin = 0f;
            xMin = (imageWidth / 2f - imageHeight / 2) / imageWidth;
        }
        return new RectF(
                xMin,
                yMin,
                xMin + width,
                yMin + height
        );
    }

    public void close() {
        interpreter.close();
    }
}
