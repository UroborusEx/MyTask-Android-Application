package android.support.v13.app;

import android.app.Fragment;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build.VERSION;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import java.util.Arrays;

public class FragmentCompat {
    static final FragmentCompatImpl IMPL;

    interface FragmentCompatImpl {
        void requestPermissions(Fragment fragment, String[] strArr, int i);

        void setMenuVisibility(Fragment fragment, boolean z);

        void setUserVisibleHint(Fragment fragment, boolean z);

        boolean shouldShowRequestPermissionRationale(Fragment fragment, String str);
    }

    public interface OnRequestPermissionsResultCallback {
        void onRequestPermissionsResult(int i, @NonNull String[] strArr, @NonNull int[] iArr);
    }

    static class BaseFragmentCompatImpl implements FragmentCompatImpl {

        /* renamed from: android.support.v13.app.FragmentCompat.BaseFragmentCompatImpl.1 */
        class C00181 implements Runnable {
            final /* synthetic */ Fragment val$fragment;
            final /* synthetic */ String[] val$permissions;
            final /* synthetic */ int val$requestCode;

            C00181(String[] strArr, Fragment fragment, int i) {
                this.val$permissions = strArr;
                this.val$fragment = fragment;
                this.val$requestCode = i;
            }

            public void run() {
                int[] grantResults = new int[this.val$permissions.length];
                Context context = this.val$fragment.getActivity();
                if (context != null) {
                    PackageManager packageManager = context.getPackageManager();
                    String packageName = context.getPackageName();
                    int permissionCount = this.val$permissions.length;
                    for (int i = 0; i < permissionCount; i++) {
                        grantResults[i] = packageManager.checkPermission(this.val$permissions[i], packageName);
                    }
                } else {
                    Arrays.fill(grantResults, -1);
                }
                ((OnRequestPermissionsResultCallback) this.val$fragment).onRequestPermissionsResult(this.val$requestCode, this.val$permissions, grantResults);
            }
        }

        BaseFragmentCompatImpl() {
        }

        public void setMenuVisibility(Fragment f, boolean visible) {
        }

        public void setUserVisibleHint(Fragment f, boolean deferStart) {
        }

        public void requestPermissions(Fragment fragment, String[] permissions, int requestCode) {
            new Handler(Looper.getMainLooper()).post(new C00181(permissions, fragment, requestCode));
        }

        public boolean shouldShowRequestPermissionRationale(Fragment fragment, String permission) {
            return false;
        }
    }

    static class ICSFragmentCompatImpl extends BaseFragmentCompatImpl {
        ICSFragmentCompatImpl() {
        }

        public void setMenuVisibility(Fragment f, boolean visible) {
            FragmentCompatICS.setMenuVisibility(f, visible);
        }
    }

    static class ICSMR1FragmentCompatImpl extends ICSFragmentCompatImpl {
        ICSMR1FragmentCompatImpl() {
        }

        public void setUserVisibleHint(Fragment f, boolean deferStart) {
            FragmentCompatICSMR1.setUserVisibleHint(f, deferStart);
        }
    }

    static class MncFragmentCompatImpl extends ICSMR1FragmentCompatImpl {
        MncFragmentCompatImpl() {
        }

        public void requestPermissions(Fragment fragment, String[] permissions, int requestCode) {
            FragmentCompat23.requestPermissions(fragment, permissions, requestCode);
        }

        public boolean shouldShowRequestPermissionRationale(Fragment fragment, String permission) {
            return FragmentCompat23.shouldShowRequestPermissionRationale(fragment, permission);
        }
    }

    static {
        if (VERSION.SDK_INT >= 23) {
            IMPL = new MncFragmentCompatImpl();
        } else if (VERSION.SDK_INT >= 15) {
            IMPL = new ICSMR1FragmentCompatImpl();
        } else if (VERSION.SDK_INT >= 14) {
            IMPL = new ICSFragmentCompatImpl();
        } else {
            IMPL = new BaseFragmentCompatImpl();
        }
    }

    public static void setMenuVisibility(Fragment f, boolean visible) {
        IMPL.setMenuVisibility(f, visible);
    }

    public static void setUserVisibleHint(Fragment f, boolean deferStart) {
        IMPL.setUserVisibleHint(f, deferStart);
    }

    public static void requestPermissions(@NonNull Fragment fragment, @NonNull String[] permissions, int requestCode) {
        IMPL.requestPermissions(fragment, permissions, requestCode);
    }

    public static boolean shouldShowRequestPermissionRationale(@NonNull Fragment fragment, @NonNull String permission) {
        return IMPL.shouldShowRequestPermissionRationale(fragment, permission);
    }
}
