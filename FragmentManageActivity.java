package com.panasonic.SmartRAC.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;

import com.panasonic.SmartRAC.R;
import com.panasonic.SmartRAC.common.log.DebugLog;
import com.panasonic.SmartRAC.common.util.UtilEolia;

import java.io.Serializable;
import java.util.Stack;

/**
 * フラグメント切り替え管理アクティビティ.
 *
 * <p>フラグメントの切り替えにより画面遷移を行うActivityです。
 *
 * @author Satoshi Itoh（Sky）
 * @version 1.0
 */
public abstract class FragmentManageActivity extends BaseActivity {
    /**
     * ログ用のタグ.
     */
    private static final String LOG_TAG = FragmentManageActivity.class.getSimpleName();

    /**
     * Activityがバックグラウンドにある場合にフラグメントの切り替えが行われた場合、復帰時に実行するために一時保存.
     */
    Fragment requestSend = null;

    /**
     * Activityがバックグラウンドにある場合はtrue.
     * */
    boolean isStop = false;
    /**
     * 初期表示指定用Intentパラメーター名.
     */
    public static final String SHOW_FRAGMENT = FragmentManageActivity.class.getSimpleName() + "ShowFragment";

    /**
     * 初期表示フラグメント用argument.
     */
    public static final String SHOW_FRAGMENT_ARGUMENT = FragmentManageActivity.class.getSimpleName() + "Argument";

    /**
     * 画面遷移履歴.
     */
    Stack<FragmentHistory> fragmentNumberHistory = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DebugLog.d(LOG_TAG, "onCreate START");
        super.onCreate(savedInstanceState);
        if(UtilEolia.isModifyPermission(this)){
            return;
        }
        fragmentNumberHistory = new Stack<>();

        if (savedInstanceState == null) {
            int showFragment = getIntent().getIntExtra(SHOW_FRAGMENT, getInitialDisplayFragmentNumber());
            Bundle argument = getIntent().getBundleExtra(SHOW_FRAGMENT_ARGUMENT);
            if (argument == null) {
                switchFragment(showFragment);
            } else {
                switchFragment(showFragment, argument);
            }
        }
        DebugLog.d(LOG_TAG, "onCreate END");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // 履歴をBundleに変換して保存
        for (int i = fragmentNumberHistory.size(); i > 0; i--) {
            outState.putBundle("fragmentNumberHistory_" + (i - 1), fragmentNumberHistory.get(i - 1).makeBundle());
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        // Bundleデータから履歴を復元
        fragmentNumberHistory.clear();
        int i = 0;
        while (true) {
            Bundle bundleData = savedInstanceState.getBundle("fragmentNumberHistory_" + i);
            if (bundleData == null) {
                break;
            }
            fragmentNumberHistory.push(makeFromBundle(bundleData));
            i++;
        }
    }

    @Override
    public void onResume() {
        DebugLog.d(LOG_TAG, "onResume START");
        super.onResume();
        isStop = false;
        if (requestSend != null) {
            replaceFragment(requestSend);
            requestSend = null;
        }
        DebugLog.d(LOG_TAG, "onResume END");
    }

    @Override
    public void onStop() {
        DebugLog.d(LOG_TAG, "onStop START");
        super.onStop();
        isStop = true;
        DebugLog.d(LOG_TAG, "onStop END");
    }


    @Override
    protected int getBaseLayoutId() {
        DebugLog.d(LOG_TAG, "getBaseLayoutId");
        return R.layout.activity_fragment_base;
    }

    /**
     * Fragmentのコンテナを指定.
     *
     * <p> フラグメントのベースになるViewIDを指定
     *     BaseLayoutIdを変更する場合は@Overrideして使用
     *
     * @return フラグメントコンテナのViewID
     */
    public int getFragmentContainerId() {
        DebugLog.d(LOG_TAG, "getFragmentContainerId");
        return R.id.fragment_container;
    }

    /**
     * 初期表示フラグメントナンバー.
     *
     * <p>指定がない場合に表示するフラグメント番号を取得します
     *
     * @return 初期表示フラグメントナンバー
     */
    protected int getInitialDisplayFragmentNumber() {
        DebugLog.d(LOG_TAG, "getInitialDisplayFragmentNumber");
        return 0;
    }

    /**
     * 画面番号からフラグメントを取得.
     *
     * @param fragmentNumber 画面番号
     */
    public abstract Fragment getFragment(int fragmentNumber);

    /**
     * フラグメント切り替えアニメーションの指定.
     */
    protected int getFragmentTransactionAnimation() {
        return FragmentTransaction.TRANSIT_FRAGMENT_FADE;
    }

    /**
     * 画面番号からフラグメントを取得し画面を切り替え.
     *
     * <p>画面番号を指定してフラグメントを切り替える。履歴に保存する。
     *
     * @param fragmentNumber 画面番号
     */
    public void switchFragment(int fragmentNumber) {
        DebugLog.d(LOG_TAG, "switchFragment START");
        switchFragment(fragmentNumber, null, false);
        DebugLog.d(LOG_TAG, "switchFragment END");
    }

    /**
     * 画面番号からフラグメントを取得し画面を切り替え.
     *
     * <p>画面番号を指定してフラグメントを切り替える。履歴に保存する。
     *
     * @param fragmentNumber 画面番号
     * @param isDeleteHistory 履歴の削除フラグ
     */
    public void switchFragment(int fragmentNumber, Bundle args, boolean isDeleteHistory) {
        DebugLog.d(LOG_TAG, "switchFragment START " + isDeleteHistory);
        for (FragmentHistory x : fragmentNumberHistory) {
            removeFragment(getFragment(x.fragmentNumber));
        }
        if (isDeleteHistory) {
            fragmentNumberHistory.clear();
        }
        FragmentHistory history = new FragmentHistory();
        history.fragmentNumber = fragmentNumber;
        history.args = args;
        fragmentNumberHistory.push(history);
        Fragment fragment = getFragment(fragmentNumber);
        if (fragment != null) {
            fragment.setArguments(args);
            if (isDeleteHistory) {
                replaceFragment(fragment);
            } else {
                addFragment(fragment);
            }
        }
        DebugLog.d(LOG_TAG, "switchFragment END");
    }

    /**
     * 画面番号とパラメーターからフラグメントを取得し画面を切り替え.
     *
     * <p>画面番号とパラメーターを指定してフラグメントを切り替える。履歴に保存する。
     *
     * @param fragmentNumber 画面番号
     * @param args パラメーター
     */
    public void switchFragment(int fragmentNumber, Bundle args) {
        DebugLog.d(LOG_TAG, "switchFragment START");
        switchFragment(fragmentNumber, args, false);
        DebugLog.d(LOG_TAG, "switchFragment END");
    }

    /**
     * 画面番号とパラメーターからフラグメントを取得し画面を切り替え.
     *
     * <p>画面番号とパラメーターを指定してフラグメントを切り替える。履歴に保存する。
     *
     * @param fragmentNumber 画面番号
     * @param args パラメーター
     */
    public void switchFragment(int fragmentNumber, boolean args) {
        DebugLog.d(LOG_TAG, "switchFragment START");
        switchFragment(fragmentNumber, null, args);
        DebugLog.d(LOG_TAG, "switchFragment END");
    }

    /**
     * フラグメント切り替え.
     *
     * @param fragment フラグメント(v4)
     */
    public void replaceFragment(final Fragment fragment) {
        DebugLog.d(LOG_TAG, "changeFragment START");
        // Activityがバックグラウンドにある場合は、再度フォアグラウンドに復帰した際に実行する
        if (!isStop) {
            mainHandler.post(new Runnable() {
                @SuppressWarnings("WrongConstant")
                @Override
                public void run() {
                    FragmentManager fragmentManager = getSupportFragmentManager();
                    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                    fragmentTransaction.replace(getFragmentContainerId(), fragment);
                    fragmentTransaction.setTransition(getFragmentTransactionAnimation());
                    try {
                        fragmentTransaction.commit();
                    } catch (IllegalStateException exception) {
                        DebugLog.e(LOG_TAG, exception);
                    }
                }
            });
        } else {
            requestSend = fragment;
        }
        DebugLog.d(LOG_TAG, "changeFragment END");
    }

    /**
     * フラグメント切り替え.
     *
     * @param fragment フラグメント(v4)
     */
    public void addFragment(final Fragment fragment) {
        DebugLog.d(LOG_TAG, "changeFragment START");
        // Activityがバックグラウンドにある場合は、再度フォアグラウンドに復帰した際に実行する
        if (!isStop) {
            mainHandler.post(new Runnable() {
                @SuppressWarnings("WrongConstant")
                @Override
                public void run() {
                    FragmentManager fragmentManager = getSupportFragmentManager();
                    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                    fragmentTransaction.add(getFragmentContainerId(), fragment);
                    fragmentTransaction.setTransition(getFragmentTransactionAnimation());
                    try {
                        fragmentTransaction.commit();
                    } catch (IllegalStateException exception) {
                        DebugLog.e(LOG_TAG, exception);
                    }
                }
            });
        } else {
            requestSend = fragment;
        }
        DebugLog.d(LOG_TAG, "changeFragment END");
    }


    /**
     * フラグメント切り替え.
     *
     * @param fragment フラグメント(v4)
     */
    public void showFragment(final Fragment fragment) {
        DebugLog.d(LOG_TAG, "changeFragment START");
        // Activityがバックグラウンドにある場合は、再度フォアグラウンドに復帰した際に実行する
        if (!isStop) {
            mainHandler.post(new Runnable() {
                @SuppressWarnings("WrongConstant")
                @Override
                public void run() {
                    FragmentManager fragmentManager = getSupportFragmentManager();
                    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                    fragmentTransaction.show(fragment);
                    fragmentTransaction.setTransition(getFragmentTransactionAnimation());
                    try {
                        fragmentTransaction.commit();
                    } catch (IllegalStateException exception) {
                        DebugLog.e(LOG_TAG, exception);
                    }
                }
            });
        } else {
            requestSend = fragment;
        }
        DebugLog.d(LOG_TAG, "changeFragment END");
    }


    /**
     * フラグメント切り替え.
     *
     * @param fragment フラグメント(v4)
     */
    public void removeFragment(final Fragment fragment) {
        DebugLog.d(LOG_TAG, "changeFragment START");
        // Activityがバックグラウンドにある場合は、再度フォアグラウンドに復帰した際に実行する
        if (!isStop) {
            mainHandler.post(new Runnable() {
                @SuppressWarnings("WrongConstant")
                @Override
                public void run() {
                    FragmentManager fragmentManager = getSupportFragmentManager();
                    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                    fragmentTransaction.hide(fragment);
                    fragmentTransaction.setTransition(getFragmentTransactionAnimation());
                    try {
                        fragmentTransaction.commit();
                    } catch (IllegalStateException exception) {
                        DebugLog.e(LOG_TAG, exception);
                    }
                }
            });
        } else {
            requestSend = fragment;
        }
        DebugLog.d(LOG_TAG, "changeFragment END");
    }

    @Override
    protected void onBackButton() {
        DebugLog.d(LOG_TAG, "onBackButton START");
        if (!onHistoryBack()) {
            finish();
        }
        DebugLog.d(LOG_TAG, "onBackButton END");
    }

    /**
     * 一つ前の画面に戻る.
     *
     * @return 一つ前の画面に戻れた場合はtrue、最初期画面などで戻れない場合はfalseを返却する
     */
    public boolean onHistoryBack() {
        DebugLog.d(LOG_TAG, "onHistoryBack START");
        boolean stat = false;
        if (!fragmentNumberHistory.empty()) {
            FragmentHistory lastHistoryClear =  fragmentNumberHistory.pop();
            Fragment fragmentClear =   getFragment(lastHistoryClear.fragmentNumber);
            removeFragment(fragmentClear);
            if (!fragmentNumberHistory.empty()) {
                FragmentHistory lastHistory = fragmentNumberHistory.peek();
                Fragment fragment = getFragment(lastHistory.fragmentNumber);
                if (fragment != null) {
                    if (lastHistory.args != null) {
                        fragment.setArguments(lastHistory.args);
                    }
                    showFragment(fragment);
                    stat = true;
                }
            }
        }
        DebugLog.d(LOG_TAG, "onHistoryBack END" + stat);
        return stat;
    }

    /**
     * 複数指定数、前の画面に戻る.
     * @param historyCount 指定回数
     * @return 指定回数戻れた場合はtrue、最初期画面などで戻れない場合はfalseを返却する
     */
    public boolean onHistoryBack(int historyCount) {
        DebugLog.d(LOG_TAG, "onHistoryBack START");
        if (historyCount <= 0) {
            DebugLog.d(LOG_TAG, "onHistoryBack EMD" + true);
            return true;
        } else {
            if (onHistoryBack()) {
                DebugLog.d(LOG_TAG, "onHistoryBack EMD");
                return onHistoryBack(historyCount - 1);
            } else {
                DebugLog.d(LOG_TAG, "onHistoryBack EMD" + false);
                return false;
            }
        }
    }

    @Override
    public void onBackPressed() {
        DebugLog.d(LOG_TAG, "onBackPressed START");
        if (toolBar != null && toolBar.getVisibility() == View.VISIBLE
                && backButton != null && backButton.getVisibility() == View.VISIBLE) {
            if (!onHistoryBack()) {
                super.onBackPressed();
            }
        }
        DebugLog.d(LOG_TAG, "onBackPressed END");
    }

    private static class FragmentHistory implements Serializable {
        int fragmentNumber = 0;
        Bundle args = null;

        Bundle makeBundle() {
            Bundle bundle = new Bundle();
            bundle.putInt("fragmentNumber", fragmentNumber);
            bundle.putBundle("args", args);
            return bundle;
        }
    }

    static FragmentHistory makeFromBundle(Bundle bundle) {
        FragmentHistory history = new FragmentHistory();
        history.fragmentNumber = bundle.getInt("fragmentNumber");
        history.args = bundle.getBundle("args");
        return history;
    }
}
