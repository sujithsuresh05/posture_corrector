package com.hackthon.poseestimation.tensor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.media.Image;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class YuvToRgbConverter {

    private Context context;
    private int pixelCount = -1;
    private ByteBuffer yuvBuffer;
    private Allocation inputAllocation;
    private Allocation outputAllocation;
    private RenderScript rs;
    private ScriptIntrinsicYuvToRGB scriptYuvToRgb;

    public YuvToRgbConverter(Context context) {
        this.context = context;
        rs = RenderScript.create(context);
        scriptYuvToRgb = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));
    }

    public synchronized void yuvToRgb(Image image, Bitmap output) {
        if(yuvBuffer == null) {
            pixelCount = image.getCropRect().width() * image.getCropRect().height();
            yuvBuffer = ByteBuffer.allocateDirect(
                    pixelCount * ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8);
        }
        imageToByteBuffer(image, yuvBuffer);

        // Ensure that the RenderScript inputs and outputs are allocated
        if (inputAllocation == null) {
            inputAllocation = Allocation.createSized(rs, Element.U8(rs), yuvBuffer.array().length);
        }
        if (outputAllocation == null) {
            outputAllocation = Allocation.createFromBitmap(rs, output);
        }

        // Convert YUV to RGB
        inputAllocation.copyFrom(yuvBuffer.array());
        scriptYuvToRgb.setInput(inputAllocation);
        scriptYuvToRgb.forEach(outputAllocation);
        outputAllocation.copyTo(output);
    }




    private void imageToByteBuffer(Image image, ByteBuffer outputBuffer) {
        assert (image.getFormat() == ImageFormat.YUV_420_888);

        Rect imageCrop = image.getCropRect();
        Image.Plane[] imagePlanes = image.getPlanes();

        byte[] rowData = new byte[imagePlanes[0].getRowStride()];
        for(int i =0 ; i< imagePlanes.length; i++) {
            Image.Plane plane = imagePlanes[i];
            int outputStride = 0;
            int outputOffset = 0;
            switch (i) {
               case 0 : {
                    outputStride = 1;
                    outputOffset = 0;
                    break;
                }
                case 1 : {
                    outputStride = 2;
                    outputOffset = pixelCount + 1;
                    break;
                }
                case 2 : {
                    outputStride = 2;
                    outputOffset = pixelCount;
                    break;
                }
                default:  {
                    break;
                }
            }
            ByteBuffer buffer = plane.getBuffer();
            int rowStride = plane.getRowStride();
            int pixelStride = plane.getPixelStride();

            // We have to divide the width and height by two if it's not the Y plane
            Rect planeCrop = i == 0 ?
                imageCrop :
                new Rect(
                        imageCrop.left / 2,
                        imageCrop.top / 2,
                        imageCrop.right / 2,
                        imageCrop.bottom / 2
                );

            int planeWidth = planeCrop.width();
            int planeHeight = planeCrop.height();

            buffer.position(rowStride * planeCrop.top + pixelStride * planeCrop.left);
            for (int row =0; row <  planeHeight; row++) {
                int length;
                if (pixelStride == 1 && outputStride == 1) {

                    length = planeWidth;
                    buffer.get(outputBuffer.array(), outputOffset, length);
                    outputOffset += length;
                } else {
                    System.out.println();
                    length = (planeWidth - 1) * pixelStride + 1;
                    buffer.get(rowData, 0, length);
                    for (int col =0; col < planeWidth; col++ ) {
                        outputBuffer.array()[outputOffset] = rowData[col * pixelStride];
                        outputOffset += outputStride;
                    }
                }

                if (row < planeHeight - 1) {
                    buffer.position(buffer.position() + rowStride - length);
                }
            }
        }
    }

}
