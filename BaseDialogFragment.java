package com.panasonic.smart.eolia.common.view.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;

import com.panasonic.smart.eolia.R;
import com.panasonic.smart.eolia.common.log.DebugLog;
import com.panasonic.smart.eolia.common.util.Util;

import static com.panasonic.smart.eolia.common.log.DebugLog.LOG_TAG;

/**
 * Created by songyb on 2017/03/30.
 */

public class BaseDialogFragment extends DialogFragment {

    private ImageView iv;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.Anim_fade);

    }

    @Override
    public void onStart() {
        super.onStart();
        dialogEffect(getActivity(),getDialog());
    }

    /**
     * ダイアログ背景にブラーをかける.
     *
     * @param activity 表示のベースのなるActivity
     * @param dialog   表示するダイアログ
     */
    public void dialogEffect(Activity activity, Dialog dialog) {
        DebugLog.d(LOG_TAG, "dialogEffect START");

        if (dialog.getWindow() != null) {
            WindowManager.LayoutParams lp = dialog.getWindow().getAttributes();
            lp.dimAmount = 0.0f;
            dialog.getWindow().setAttributes(lp);
        }

        //Activityの表示内容をViewとして取得
        View decorView = activity.getWindow().getDecorView();
        decorView.setDrawingCacheEnabled(true);
        decorView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);
        // Viewのキャッシュを取得
        final Bitmap cache = decorView.getDrawingCache(true);
        if (cache != null) {

            iv = new ImageView(activity);

            //コピーした画像を加工してImageViewに貼り付ける
            iv.setImageBitmap(Util.blur(activity, cache));
            //加工したActivityの表示内容をViewとして貼り付ける
            ((ViewGroup) decorView).addView(iv);
        } else {
            DebugLog.d(LOG_TAG, "DrawingCache取得失敗");
        }

        DebugLog.d(LOG_TAG, "dialogEffect END");
    }


    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (iv != null) {
            try {
                ((ViewGroup) iv.getParent()).removeView(iv);
            } catch (IllegalArgumentException exception) {
                DebugLog.e(LOG_TAG, exception);
            }
        }
    }
}
