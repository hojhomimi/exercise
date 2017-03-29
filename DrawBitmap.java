package com.panasonic.smart.eolia.common.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.Log;

/**
 *
 *
 * Created by mac on 2016/12/18.
 */
public class DrawBitmap {
    public Bitmap setLinearGradientV(int[] colors,boolean flag){
        int[] colorsShader;
        if(flag){
            colorsShader  =  new int[]{colors[1], colors[0]};
        }else{
            colorsShader = new int[]{colors[0],colors[0]};
        }
        LinearGradient shader = new LinearGradient(
                0, 0,
                0, 100,
                colorsShader,
                new float[]{0.08f,0.2f},
                Shader.TileMode.MIRROR);
        return getBitmap(shader);
    }
    public Bitmap setLinearGradientH(int[] colors,boolean flag){
        int[] colorsShader;
        if(flag){
            colorsShader  =  new int[]{colors[0], colors[1]};
        }else{
            colorsShader = new int[]{colors[1],colors[1]};
        }

        LinearGradient shader = new LinearGradient(
                0, 0,
                0, 100,
                colorsShader,
                new float[]{0.08f,0.2f},
                Shader.TileMode.MIRROR);
        return getBitmap(shader);
    }
    private Bitmap getBitmap( LinearGradient shader){
        int width = 100,height = 100;
        Bitmap bitmap = Bitmap.createBitmap(width,height, Bitmap.Config.ARGB_4444);
        Canvas canvas = new Canvas(bitmap);
        Paint paint=new Paint();  //定义一个Paint
        paint.setShader(shader);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
        canvas.drawCircle(width/2, height/2, 40,
                paint);
        canvas.save();
        return bitmap;
    }

    public Bitmap setArcShaderBlueGreen(int[] colors){
        LinearGradient shader;
        if (Color.rgb(196, 221, 224)==colors[0]) {
            shader = new LinearGradient(
                    0, 0,
                    0, 100,
                    colors,
                    new float[]{0.1f, 1f},
                    Shader.TileMode.MIRROR);
        }else if(colors.length==3){
            shader = new LinearGradient(
                    0, 0,
                    100, 0,
                    colors,
                    new float[]{0f,0.5f,1f},
                    Shader.TileMode.MIRROR);
        }else {
            shader = new LinearGradient(
                    0, 0,
                    100, 0,
                    colors,
                    new float[]{0f,1f},
                    Shader.TileMode.MIRROR);
        }


        return getArcBackGround(shader);
    }
    private Bitmap getArcBackGround( LinearGradient shader){
        int width = 100,height = 100;
        RectF mArcRectF = new RectF();
        mArcRectF.set(10,10,width-10,height-10);
        Bitmap bitmap = Bitmap.createBitmap(width,height, Bitmap.Config.ARGB_4444);
        Canvas canvas = new Canvas(bitmap);
        Paint paint=new Paint();  //定义一个Paint
        paint.setShader(shader);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(8);
        paint.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawArc(mArcRectF, 140f, 260, false, paint);
        canvas.save();
        return bitmap;
    }

    public Bitmap getBackGroundText(String text,int height){
        float constant = 1776f;
        int bitmapHeight = (int)(60f/constant*height);
        Paint paint=new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
//        paint.setStrokeWidth(1);
        paint.setTextSize(52f/constant*height);
        paint.setColor(Color.rgb(105,114,147));
        paint.setTextAlign(Paint.Align.CENTER);
        if(text==null){
            text = "";
        }
        int textWidth = (int)paint.measureText(text)+1;

        Bitmap bitmap = Bitmap.createBitmap(textWidth,(int)paint.getTextSize(), Bitmap.Config.ARGB_4444);
        Canvas canvas = new Canvas(bitmap);

        canvas.drawText(text,textWidth/2,paint.getTextSize()*0.85f,paint);
        canvas.save();
        return bitmap;
    }

}
