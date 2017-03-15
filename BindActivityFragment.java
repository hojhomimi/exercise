package com.panasonic.smart.eolia.activity;

import android.content.Context;

public abstract class BindActivityFragment<T extends FragmentManageActivity> extends BaseFragment {

    protected T activity = null;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = (T) context;
    }
    protected abstract int getLayoutId() ;
}
