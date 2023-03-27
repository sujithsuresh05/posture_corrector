package com.hackthon.poseestimation.tensor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;

import com.hackthon.poseestimation.body.JoinPoint;
import com.hackthon.poseestimation.body.Part;
import com.hackthon.poseestimation.body.Person;

import org.checkerframework.checker.units.qual.A;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MoveNetVisualEvaluator implements VisualEvaluator {

    private static final String THUNDER_FILENAME = "movenet_thunder.tflite";
    private static final String LIGHTER_FILENAME = "movenet_lightning.tflite";
    private static final int CPU_NUM_THREADS = 4;

    private final Context mContext;
    private Interpreter interpreter;

    private int inputWidth;

    private int inputHeight;

    public MoveNetVisualEvaluator(Context context) throws IOException {
        this.mContext = context;
        Interpreter.Options options = new Interpreter.Options();
        options.setNumThreads(CPU_NUM_THREADS);
        interpreter = new Interpreter(FileUtil.loadMappedFile(
                context,
                LIGHTER_FILENAME
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
        RectF cropRegion = getRectF(bitmap.getWidth(), bitmap.getHeight());
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
        float widthRatio = (float) detectBitmap.getWidth() / inputWidth;
        float heightRatio = (float) detectBitmap.getHeight() / inputHeight;

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
        Float[] points = positions.toArray(new Float[positions.size()]);
        matrix.postTranslate(rect.left, rect.top);

        Person person = new Person(jointPoints, totalScore / numKeyPoints);
        List<Person> personList = new ArrayList<>();
        personList.add(person);
        //TensorImage inputTensor = processInputImage(bitmap,)
        return personList;
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

    public void close()  {
        interpreter.close();
    }
}
