package android.support.design.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.os.Build.VERSION;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.C0000R;
import android.support.design.widget.SwipeDismissBehavior.OnDismissListener;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorListenerAdapter;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewParent;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.datetimepicker.time.TimePickerDialog;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public final class Snackbar {
    private static final int ANIMATION_DURATION = 250;
    private static final int ANIMATION_FADE_DURATION = 180;
    public static final int LENGTH_INDEFINITE = -2;
    public static final int LENGTH_LONG = 0;
    public static final int LENGTH_SHORT = -1;
    private static final int MSG_DISMISS = 1;
    private static final int MSG_SHOW = 0;
    private static final Handler sHandler;
    private Callback mCallback;
    private final Context mContext;
    private int mDuration;
    private final Callback mManagerCallback;
    private final ViewGroup mTargetParent;
    private final SnackbarLayout mView;

    /* renamed from: android.support.design.widget.Snackbar.10 */
    class AnonymousClass10 implements AnimationListener {
        final /* synthetic */ int val$event;

        AnonymousClass10(int i) {
            this.val$event = i;
        }

        public void onAnimationEnd(Animation animation) {
            Snackbar.this.onViewHidden(this.val$event);
        }

        public void onAnimationStart(Animation animation) {
        }

        public void onAnimationRepeat(Animation animation) {
        }
    }

    /* renamed from: android.support.design.widget.Snackbar.1 */
    static class C00061 implements android.os.Handler.Callback {
        C00061() {
        }

        public boolean handleMessage(Message message) {
            switch (message.what) {
                case Snackbar.LENGTH_LONG /*0*/:
                    ((Snackbar) message.obj).showView();
                    return true;
                case Snackbar.MSG_DISMISS /*1*/:
                    ((Snackbar) message.obj).hideView(message.arg1);
                    return true;
                default:
                    return false;
            }
        }
    }

    /* renamed from: android.support.design.widget.Snackbar.2 */
    class C00072 implements OnClickListener {
        final /* synthetic */ OnClickListener val$listener;

        C00072(OnClickListener onClickListener) {
            this.val$listener = onClickListener;
        }

        public void onClick(View view) {
            this.val$listener.onClick(view);
            Snackbar.this.dispatchDismiss(Snackbar.MSG_DISMISS);
        }
    }

    /* renamed from: android.support.design.widget.Snackbar.8 */
    class C00098 implements AnimationListener {
        C00098() {
        }

        public void onAnimationEnd(Animation animation) {
            if (Snackbar.this.mCallback != null) {
                Snackbar.this.mCallback.onShown(Snackbar.this);
            }
            SnackbarManager.getInstance().onShown(Snackbar.this.mManagerCallback);
        }

        public void onAnimationStart(Animation animation) {
        }

        public void onAnimationRepeat(Animation animation) {
        }
    }

    public static abstract class Callback {
        public static final int DISMISS_EVENT_ACTION = 1;
        public static final int DISMISS_EVENT_CONSECUTIVE = 4;
        public static final int DISMISS_EVENT_MANUAL = 3;
        public static final int DISMISS_EVENT_SWIPE = 0;
        public static final int DISMISS_EVENT_TIMEOUT = 2;

        @Retention(RetentionPolicy.SOURCE)
        public @interface DismissEvent {
        }

        public void onDismissed(Snackbar snackbar, int event) {
        }

        public void onShown(Snackbar snackbar) {
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface Duration {
    }

    public static class SnackbarLayout extends LinearLayout {
        private Button mActionView;
        private int mMaxInlineActionWidth;
        private int mMaxWidth;
        private TextView mMessageView;
        private OnAttachStateChangeListener mOnAttachStateChangeListener;
        private OnLayoutChangeListener mOnLayoutChangeListener;

        interface OnAttachStateChangeListener {
            void onViewAttachedToWindow(View view);

            void onViewDetachedFromWindow(View view);
        }

        interface OnLayoutChangeListener {
            void onLayoutChange(View view, int i, int i2, int i3, int i4);
        }

        public SnackbarLayout(Context context) {
            this(context, null);
        }

        public SnackbarLayout(Context context, AttributeSet attrs) {
            super(context, attrs);
            TypedArray a = context.obtainStyledAttributes(attrs, C0000R.styleable.SnackbarLayout);
            this.mMaxWidth = a.getDimensionPixelSize(C0000R.styleable.SnackbarLayout_android_maxWidth, Snackbar.LENGTH_SHORT);
            this.mMaxInlineActionWidth = a.getDimensionPixelSize(C0000R.styleable.SnackbarLayout_maxActionInlineWidth, Snackbar.LENGTH_SHORT);
            if (a.hasValue(C0000R.styleable.SnackbarLayout_elevation)) {
                ViewCompat.setElevation(this, (float) a.getDimensionPixelSize(C0000R.styleable.SnackbarLayout_elevation, Snackbar.LENGTH_LONG));
            }
            a.recycle();
            setClickable(true);
            LayoutInflater.from(context).inflate(C0000R.layout.design_layout_snackbar_include, this);
            ViewCompat.setAccessibilityLiveRegion(this, Snackbar.MSG_DISMISS);
        }

        protected void onFinishInflate() {
            super.onFinishInflate();
            this.mMessageView = (TextView) findViewById(C0000R.id.snackbar_text);
            this.mActionView = (Button) findViewById(C0000R.id.snackbar_action);
        }

        TextView getMessageView() {
            return this.mMessageView;
        }

        Button getActionView() {
            return this.mActionView;
        }

        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            boolean isMultiLine;
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            if (this.mMaxWidth > 0 && getMeasuredWidth() > this.mMaxWidth) {
                widthMeasureSpec = MeasureSpec.makeMeasureSpec(this.mMaxWidth, 1073741824);
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            }
            int multiLineVPadding = getResources().getDimensionPixelSize(C0000R.dimen.design_snackbar_padding_vertical_2lines);
            int singleLineVPadding = getResources().getDimensionPixelSize(C0000R.dimen.design_snackbar_padding_vertical);
            if (this.mMessageView.getLayout().getLineCount() > Snackbar.MSG_DISMISS) {
                isMultiLine = true;
            } else {
                isMultiLine = false;
            }
            boolean remeasure = false;
            if (!isMultiLine || this.mMaxInlineActionWidth <= 0 || this.mActionView.getMeasuredWidth() <= this.mMaxInlineActionWidth) {
                int messagePadding;
                if (isMultiLine) {
                    messagePadding = multiLineVPadding;
                } else {
                    messagePadding = singleLineVPadding;
                }
                if (updateViewsWithinLayout(Snackbar.LENGTH_LONG, messagePadding, messagePadding)) {
                    remeasure = true;
                }
            } else if (updateViewsWithinLayout(Snackbar.MSG_DISMISS, multiLineVPadding, multiLineVPadding - singleLineVPadding)) {
                remeasure = true;
            }
            if (remeasure) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            }
        }

        void animateChildrenIn(int delay, int duration) {
            ViewCompat.setAlpha(this.mMessageView, 0.0f);
            ViewCompat.animate(this.mMessageView).alpha(1.0f).setDuration((long) duration).setStartDelay((long) delay).start();
            if (this.mActionView.getVisibility() == 0) {
                ViewCompat.setAlpha(this.mActionView, 0.0f);
                ViewCompat.animate(this.mActionView).alpha(1.0f).setDuration((long) duration).setStartDelay((long) delay).start();
            }
        }

        void animateChildrenOut(int delay, int duration) {
            ViewCompat.setAlpha(this.mMessageView, 1.0f);
            ViewCompat.animate(this.mMessageView).alpha(0.0f).setDuration((long) duration).setStartDelay((long) delay).start();
            if (this.mActionView.getVisibility() == 0) {
                ViewCompat.setAlpha(this.mActionView, 1.0f);
                ViewCompat.animate(this.mActionView).alpha(0.0f).setDuration((long) duration).setStartDelay((long) delay).start();
            }
        }

        protected void onLayout(boolean changed, int l, int t, int r, int b) {
            super.onLayout(changed, l, t, r, b);
            if (changed && this.mOnLayoutChangeListener != null) {
                this.mOnLayoutChangeListener.onLayoutChange(this, l, t, r, b);
            }
        }

        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            if (this.mOnAttachStateChangeListener != null) {
                this.mOnAttachStateChangeListener.onViewAttachedToWindow(this);
            }
        }

        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            if (this.mOnAttachStateChangeListener != null) {
                this.mOnAttachStateChangeListener.onViewDetachedFromWindow(this);
            }
        }

        void setOnLayoutChangeListener(OnLayoutChangeListener onLayoutChangeListener) {
            this.mOnLayoutChangeListener = onLayoutChangeListener;
        }

        void setOnAttachStateChangeListener(OnAttachStateChangeListener listener) {
            this.mOnAttachStateChangeListener = listener;
        }

        private boolean updateViewsWithinLayout(int orientation, int messagePadTop, int messagePadBottom) {
            boolean changed = false;
            if (orientation != getOrientation()) {
                setOrientation(orientation);
                changed = true;
            }
            if (this.mMessageView.getPaddingTop() == messagePadTop && this.mMessageView.getPaddingBottom() == messagePadBottom) {
                return changed;
            }
            updateTopBottomPadding(this.mMessageView, messagePadTop, messagePadBottom);
            return true;
        }

        private static void updateTopBottomPadding(View view, int topPadding, int bottomPadding) {
            if (ViewCompat.isPaddingRelative(view)) {
                ViewCompat.setPaddingRelative(view, ViewCompat.getPaddingStart(view), topPadding, ViewCompat.getPaddingEnd(view), bottomPadding);
            } else {
                view.setPadding(view.getPaddingLeft(), topPadding, view.getPaddingRight(), bottomPadding);
            }
        }
    }

    /* renamed from: android.support.design.widget.Snackbar.3 */
    class C02363 implements Callback {
        C02363() {
        }

        public void show() {
            Snackbar.sHandler.sendMessage(Snackbar.sHandler.obtainMessage(Snackbar.LENGTH_LONG, Snackbar.this));
        }

        public void dismiss(int event) {
            Snackbar.sHandler.sendMessage(Snackbar.sHandler.obtainMessage(Snackbar.MSG_DISMISS, event, Snackbar.LENGTH_LONG, Snackbar.this));
        }
    }

    /* renamed from: android.support.design.widget.Snackbar.4 */
    class C02374 implements OnDismissListener {
        C02374() {
        }

        public void onDismiss(View view) {
            Snackbar.this.dispatchDismiss(Snackbar.LENGTH_LONG);
        }

        public void onDragStateChanged(int state) {
            switch (state) {
                case Snackbar.LENGTH_LONG /*0*/:
                    SnackbarManager.getInstance().restoreTimeout(Snackbar.this.mManagerCallback);
                case Snackbar.MSG_DISMISS /*1*/:
                case TimePickerDialog.AMPM_INDEX /*2*/:
                    SnackbarManager.getInstance().cancelTimeout(Snackbar.this.mManagerCallback);
                default:
            }
        }
    }

    /* renamed from: android.support.design.widget.Snackbar.5 */
    class C02385 implements OnAttachStateChangeListener {

        /* renamed from: android.support.design.widget.Snackbar.5.1 */
        class C00081 implements Runnable {
            C00081() {
            }

            public void run() {
                Snackbar.this.onViewHidden(3);
            }
        }

        C02385() {
        }

        public void onViewAttachedToWindow(View v) {
        }

        public void onViewDetachedFromWindow(View v) {
            if (Snackbar.this.isShownOrQueued()) {
                Snackbar.sHandler.post(new C00081());
            }
        }
    }

    /* renamed from: android.support.design.widget.Snackbar.6 */
    class C02396 implements OnLayoutChangeListener {
        C02396() {
        }

        public void onLayoutChange(View view, int left, int top, int right, int bottom) {
            Snackbar.this.animateViewIn();
            Snackbar.this.mView.setOnLayoutChangeListener(null);
        }
    }

    /* renamed from: android.support.design.widget.Snackbar.7 */
    class C03067 extends ViewPropertyAnimatorListenerAdapter {
        C03067() {
        }

        public void onAnimationStart(View view) {
            Snackbar.this.mView.animateChildrenIn(70, Snackbar.ANIMATION_FADE_DURATION);
        }

        public void onAnimationEnd(View view) {
            if (Snackbar.this.mCallback != null) {
                Snackbar.this.mCallback.onShown(Snackbar.this);
            }
            SnackbarManager.getInstance().onShown(Snackbar.this.mManagerCallback);
        }
    }

    /* renamed from: android.support.design.widget.Snackbar.9 */
    class C03079 extends ViewPropertyAnimatorListenerAdapter {
        final /* synthetic */ int val$event;

        C03079(int i) {
            this.val$event = i;
        }

        public void onAnimationStart(View view) {
            Snackbar.this.mView.animateChildrenOut(Snackbar.LENGTH_LONG, Snackbar.ANIMATION_FADE_DURATION);
        }

        public void onAnimationEnd(View view) {
            Snackbar.this.onViewHidden(this.val$event);
        }
    }

    final class Behavior extends SwipeDismissBehavior<SnackbarLayout> {
        Behavior() {
        }

        public boolean canSwipeDismissView(View child) {
            return child instanceof SnackbarLayout;
        }

        public boolean onInterceptTouchEvent(CoordinatorLayout parent, SnackbarLayout child, MotionEvent event) {
            if (parent.isPointInChildBounds(child, (int) event.getX(), (int) event.getY())) {
                switch (event.getActionMasked()) {
                    case Snackbar.LENGTH_LONG /*0*/:
                        SnackbarManager.getInstance().cancelTimeout(Snackbar.this.mManagerCallback);
                        break;
                    case Snackbar.MSG_DISMISS /*1*/:
                    case TimePickerDialog.ENABLE_PICKER_INDEX /*3*/:
                        SnackbarManager.getInstance().restoreTimeout(Snackbar.this.mManagerCallback);
                        break;
                }
            }
            return super.onInterceptTouchEvent(parent, child, event);
        }
    }

    static {
        sHandler = new Handler(Looper.getMainLooper(), new C00061());
    }

    private Snackbar(ViewGroup parent) {
        this.mManagerCallback = new C02363();
        this.mTargetParent = parent;
        this.mContext = parent.getContext();
        ThemeUtils.checkAppCompatTheme(this.mContext);
        this.mView = (SnackbarLayout) LayoutInflater.from(this.mContext).inflate(C0000R.layout.design_layout_snackbar, this.mTargetParent, false);
    }

    @NonNull
    public static Snackbar make(@NonNull View view, @NonNull CharSequence text, int duration) {
        Snackbar snackbar = new Snackbar(findSuitableParent(view));
        snackbar.setText(text);
        snackbar.setDuration(duration);
        return snackbar;
    }

    @NonNull
    public static Snackbar make(@NonNull View view, @StringRes int resId, int duration) {
        return make(view, view.getResources().getText(resId), duration);
    }

    private static ViewGroup findSuitableParent(View view) {
        ViewGroup fallback = null;
        while (!(view instanceof CoordinatorLayout)) {
            if (view instanceof FrameLayout) {
                if (view.getId() == 16908290) {
                    return (ViewGroup) view;
                }
                fallback = (ViewGroup) view;
            }
            if (view != null) {
                ViewParent parent = view.getParent();
                if (parent instanceof View) {
                    view = (View) parent;
                    continue;
                } else {
                    view = null;
                    continue;
                }
            }
            if (view == null) {
                return fallback;
            }
        }
        return (ViewGroup) view;
    }

    @NonNull
    public Snackbar setAction(@StringRes int resId, OnClickListener listener) {
        return setAction(this.mContext.getText(resId), listener);
    }

    @NonNull
    public Snackbar setAction(CharSequence text, OnClickListener listener) {
        TextView tv = this.mView.getActionView();
        if (TextUtils.isEmpty(text) || listener == null) {
            tv.setVisibility(8);
            tv.setOnClickListener(null);
        } else {
            tv.setVisibility(LENGTH_LONG);
            tv.setText(text);
            tv.setOnClickListener(new C00072(listener));
        }
        return this;
    }

    @NonNull
    public Snackbar setActionTextColor(ColorStateList colors) {
        this.mView.getActionView().setTextColor(colors);
        return this;
    }

    @NonNull
    public Snackbar setActionTextColor(@ColorInt int color) {
        this.mView.getActionView().setTextColor(color);
        return this;
    }

    @NonNull
    public Snackbar setText(@NonNull CharSequence message) {
        this.mView.getMessageView().setText(message);
        return this;
    }

    @NonNull
    public Snackbar setText(@StringRes int resId) {
        return setText(this.mContext.getText(resId));
    }

    @NonNull
    public Snackbar setDuration(int duration) {
        this.mDuration = duration;
        return this;
    }

    public int getDuration() {
        return this.mDuration;
    }

    @NonNull
    public View getView() {
        return this.mView;
    }

    public void show() {
        SnackbarManager.getInstance().show(this.mDuration, this.mManagerCallback);
    }

    public void dismiss() {
        dispatchDismiss(3);
    }

    private void dispatchDismiss(int event) {
        SnackbarManager.getInstance().dismiss(this.mManagerCallback, event);
    }

    @NonNull
    public Snackbar setCallback(Callback callback) {
        this.mCallback = callback;
        return this;
    }

    public boolean isShown() {
        return SnackbarManager.getInstance().isCurrent(this.mManagerCallback);
    }

    public boolean isShownOrQueued() {
        return SnackbarManager.getInstance().isCurrentOrNext(this.mManagerCallback);
    }

    final void showView() {
        if (this.mView.getParent() == null) {
            LayoutParams lp = this.mView.getLayoutParams();
            if (lp instanceof CoordinatorLayout.LayoutParams) {
                Behavior behavior = new Behavior();
                behavior.setStartAlphaSwipeDistance(0.1f);
                behavior.setEndAlphaSwipeDistance(0.6f);
                behavior.setSwipeDirection(LENGTH_LONG);
                behavior.setListener(new C02374());
                ((CoordinatorLayout.LayoutParams) lp).setBehavior(behavior);
            }
            this.mTargetParent.addView(this.mView);
        }
        this.mView.setOnAttachStateChangeListener(new C02385());
        if (ViewCompat.isLaidOut(this.mView)) {
            animateViewIn();
        } else {
            this.mView.setOnLayoutChangeListener(new C02396());
        }
    }

    private void animateViewIn() {
        if (VERSION.SDK_INT >= 14) {
            ViewCompat.setTranslationY(this.mView, (float) this.mView.getHeight());
            ViewCompat.animate(this.mView).translationY(0.0f).setInterpolator(AnimationUtils.FAST_OUT_SLOW_IN_INTERPOLATOR).setDuration(250).setListener(new C03067()).start();
            return;
        }
        Animation anim = AnimationUtils.loadAnimation(this.mView.getContext(), C0000R.anim.design_snackbar_in);
        anim.setInterpolator(AnimationUtils.FAST_OUT_SLOW_IN_INTERPOLATOR);
        anim.setDuration(250);
        anim.setAnimationListener(new C00098());
        this.mView.startAnimation(anim);
    }

    private void animateViewOut(int event) {
        if (VERSION.SDK_INT >= 14) {
            ViewCompat.animate(this.mView).translationY((float) this.mView.getHeight()).setInterpolator(AnimationUtils.FAST_OUT_SLOW_IN_INTERPOLATOR).setDuration(250).setListener(new C03079(event)).start();
            return;
        }
        Animation anim = AnimationUtils.loadAnimation(this.mView.getContext(), C0000R.anim.design_snackbar_out);
        anim.setInterpolator(AnimationUtils.FAST_OUT_SLOW_IN_INTERPOLATOR);
        anim.setDuration(250);
        anim.setAnimationListener(new AnonymousClass10(event));
        this.mView.startAnimation(anim);
    }

    final void hideView(int event) {
        if (this.mView.getVisibility() != 0 || isBeingDragged()) {
            onViewHidden(event);
        } else {
            animateViewOut(event);
        }
    }

    private void onViewHidden(int event) {
        SnackbarManager.getInstance().onDismissed(this.mManagerCallback);
        if (this.mCallback != null) {
            this.mCallback.onDismissed(this, event);
        }
        ViewParent parent = this.mView.getParent();
        if (parent instanceof ViewGroup) {
            ((ViewGroup) parent).removeView(this.mView);
        }
    }

    private boolean isBeingDragged() {
        LayoutParams lp = this.mView.getLayoutParams();
        if (!(lp instanceof CoordinatorLayout.LayoutParams)) {
            return false;
        }
        android.support.design.widget.CoordinatorLayout.Behavior behavior = ((CoordinatorLayout.LayoutParams) lp).getBehavior();
        if (!(behavior instanceof SwipeDismissBehavior) || ((SwipeDismissBehavior) behavior).getDragState() == 0) {
            return false;
        }
        return true;
    }
}
