package com.hackthon.poseestimation;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.hackthon.poseestimation.body.Person;
import com.hackthon.poseestimation.body.Posture;
import com.hackthon.poseestimation.body.SittingPosture;
import com.hackthon.poseestimation.databinding.FragmentSecondBinding;
import com.hackthon.poseestimation.tensor.MoveNetVisualEvaluator;
import com.hackthon.poseestimation.tensor.VisualEvaluator;
import com.hackthon.poseestimation.tensor.YuvToRgbConverter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

public class SecondFragment extends Fragment {

    private FragmentSecondBinding binding;

    private CameraManager cameraManager;

    //private Camera camera;

    private CameraDevice mCameraDevice;

    private SurfaceView surfaceView;

    private SurfaceHolder surfaceHolder;

    private ImageReader imageReader;

    private String cameraId;

    private HandlerThread imageHandlerThread;

    private Handler imageReaderHandler;

    private Bitmap imageBitMap;

    /**
     * Preview Variables
     */
    private CaptureRequest.Builder previewRequestBuilder;

    private CameraCaptureSession.StateCallback previewStateCallback;

    private CameraCaptureSession cameraCaptureSession;

    private static int PREVIEW_WIDTH = 640;
    private static int PREVIEW_HEIGHT = 480;

    /**
     * Threshold for confidence score.
     */
    private static float MIN_CONFIDENCE = .2f;
    private static String TAG = "Camera Source";

    private CameraCaptureSession mCameraCaptureSession;

    private VisualEvaluator visualEvaluator;

    private Object lock;

    private YuvToRgbConverter yuvConverter;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        lock = new Object();
        binding = FragmentSecondBinding.inflate(inflater, container, false);

        surfaceView = binding.surfaceView;
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.setKeepScreenOn(true);
        yuvConverter = new YuvToRgbConverter(surfaceView.getContext());
        cameraManager = (CameraManager) surfaceView.getContext().getSystemService(Context.CAMERA_SERVICE);
        imageBitMap =
                Bitmap.createBitmap(
                        PREVIEW_WIDTH,
                        PREVIEW_HEIGHT,
                        Bitmap.Config.ARGB_8888
                );
        try {
            visualEvaluator = new MoveNetVisualEvaluator(this.getContext());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return binding.getRoot();

    }

    @Override
    public void onStart() {
        super.onStart();
        prepareCamera();
        openCamera();
    }

    @SuppressLint("MissingPermission")
    private void openCamera() {
        try {
            //imageReader = ImageReader.newInstance(PREVIEW_WIDTH, PREVIEW_HEIGHT, ImageFormat.YUV_420_888, 3);
            HandlerThread imageReaderthread = new HandlerThread("CameraVideo");
            imageReaderthread.start();
            imageReaderHandler = new Handler(imageReaderthread.getLooper());
            imageReader = ImageReader.newInstance(PREVIEW_WIDTH, PREVIEW_HEIGHT, ImageFormat.YUV_420_888, 3);

            imageReader.setOnImageAvailableListener(reader -> {
                Image image = reader.acquireLatestImage();
                if (image != null) {
//                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
//                    byte[] bytes = new byte[buffer.remaining()];
//                    buffer.get(bytes);
//                    final Bitmap imageBitMap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    Bitmap imageBitMap =
                            Bitmap.createBitmap(
                                    PREVIEW_WIDTH,
                                    PREVIEW_HEIGHT,
                                    Bitmap.Config.ARGB_8888
                            );
                   if (imageBitMap != null) {

                        synchronized(lock) {
                            yuvConverter.yuvToRgb(image, imageBitMap);



                            Matrix rotateMatrix = new Matrix();
                            rotateMatrix.postRotate(90.0f);
                            Bitmap rotatedBitmap = Bitmap.createBitmap(
                                    imageBitMap, 0, 0, PREVIEW_WIDTH, PREVIEW_HEIGHT,
                                    rotateMatrix, false
                            );
                            List<Person> persons = visualEvaluator.estimatePoses(rotatedBitmap);
                            Posture sittingPosture = new SittingPosture(rotatedBitmap, persons);
//                        for(Person person : persons) {
//                            person.getJoinPoints().stream().forEach( jt -> {
//                                System.out.println(jt.getPart());
//                                System.out.println(jt.getCoordinate());
//                                System.out.println(jt.getVisibilityScore());
//                            });;
//                        }
                            showImage(sittingPosture);
                        }

                    }
                    image.close();
                }

            }, imageReaderHandler);
            Surface surface = imageReader.getSurface();
            cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice cameraDevice) {
                    mCameraDevice = cameraDevice;
                    takePreview(surface);
                }


                @Override
                public void onDisconnected(@NonNull CameraDevice cameraDevice) {
                    cameraDevice.close();
                }

                @Override
                public void onError(@NonNull CameraDevice cameraDevice, int i) {
                    cameraDevice.close();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        previewStateCallback =
                new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                        if (null == mCameraDevice) return;
                        // Begin to preview
                        mCameraCaptureSession = cameraCaptureSession;
                        try {
                            // Camera2 functions
                            // Turn on Auto Focus
                            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                            ;
                            // Show up
                            CaptureRequest previewRequest = previewRequestBuilder.build();
                            mCameraCaptureSession.setRepeatingRequest(previewRequest, null, imageReaderHandler);
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {

                    }
                };
    }


    private void showImage(Posture posture) {
        Bitmap outputBitmap = posture.getBitMap();
        Canvas canvas = surfaceHolder.lockCanvas();
        int screenWidth = canvas.getWidth();
        int screenHeight = canvas.getHeight();
        int left = 0;
        int top = 0;

//        if (screenHeight > screenWidth) {
//            float ratio = ((float) outputBitmap.getHeight()) / outputBitmap.getWidth();
//            screenWidth = canvas.getWidth();
//            left = 0;
//            screenHeight = (int) (canvas.getWidth() * ratio);
//            top = (canvas.getHeight() - screenHeight) / 2;
//        } else {
//            float ratio = ((float) outputBitmap.getWidth()) / outputBitmap.getHeight();
//            screenHeight = canvas.getHeight();
//            top = 0;
//            screenWidth = (int) (canvas.getHeight() * ratio);
//            left = (canvas.getWidth() - screenWidth) / 2;
//        }
        int right = left + screenWidth;
        int bottom = top + screenHeight;

        canvas.drawBitmap(
                outputBitmap, new Rect(0, 0, outputBitmap.getWidth(), outputBitmap.getHeight()),
                new Rect(left, top, right, bottom), null
        );
        surfaceHolder.unlockCanvasAndPost(canvas);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

//        binding.buttonSecond.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                NavHostFragment.findNavController(SecondFragment.this)
//                        .navigate(R.id.action_SecondFragment_to_FirstFragment);
//            }
//        });
    }


    private void prepareCamera() {
        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                Integer cameraDirection = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
                if (cameraDirection != null && cameraDirection == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }
                this.cameraId = cameraId;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        initiateCameraHandler();
    }

    @Override
    public void onPause() {
        super.onPause();
        closeCameraOperation();
    }

    private void initiateCameraHandler() {
        imageHandlerThread = new HandlerThread("imageReaderThread");
        imageHandlerThread.start();
        Looper looper = imageHandlerThread.getLooper();
        imageReaderHandler = new Handler(looper);
    }

    private void closeCameraOperation() {
//        session?.close()
//        session = null
        if (mCameraDevice != null)
            mCameraDevice.close();
        mCameraDevice = null;
        if (imageHandlerThread != null)
            closeImageThreadReader();

        if (imageReader != null)
            imageReader.close();

        visualEvaluator.close();
        visualEvaluator = null;
    }

    private void closeImageThreadReader() {
        imageHandlerThread.interrupt();
        imageHandlerThread = null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }


    /**
     * Begin to preview
     */
    private void takePreview(Surface surface) {
        try {

            // CaptureRequest.Builder
            previewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            // surface of SurfaceView will be the object of CaptureRequest.Builder
            previewRequestBuilder.addTarget(surface);
            // Create CameraCaptureSession to take care of preview and photo shooting.
            mCameraDevice.createCaptureSession(
                    Arrays.asList(
                            imageReader.getSurface()),
                    previewStateCallback,
                    imageReaderHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


}