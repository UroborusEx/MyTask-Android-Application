package android.support.v7.widget;

import android.content.Context;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.support.v4.view.accessibility.AccessibilityEventCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat.CollectionItemInfoCompat;
import android.support.v4.view.accessibility.AccessibilityRecordCompat;
import android.support.v7.widget.RecyclerView.LayoutManager;
import android.support.v7.widget.RecyclerView.LayoutManager.Properties;
import android.support.v7.widget.RecyclerView.Recycler;
import android.support.v7.widget.RecyclerView.State;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.accessibility.AccessibilityEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

public class StaggeredGridLayoutManager extends LayoutManager {
    private static final boolean DEBUG = false;
    @Deprecated
    public static final int GAP_HANDLING_LAZY = 1;
    public static final int GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS = 2;
    public static final int GAP_HANDLING_NONE = 0;
    public static final int HORIZONTAL = 0;
    private static final int INVALID_OFFSET = Integer.MIN_VALUE;
    public static final String TAG = "StaggeredGridLayoutManager";
    public static final int VERTICAL = 1;
    private final AnchorInfo mAnchorInfo;
    private final Runnable mCheckForGapsRunnable;
    private int mFullSizeSpec;
    private int mGapStrategy;
    private int mHeightSpec;
    private boolean mLaidOutInvalidFullSpan;
    private boolean mLastLayoutFromEnd;
    private boolean mLastLayoutRTL;
    private LayoutState mLayoutState;
    LazySpanLookup mLazySpanLookup;
    private int mOrientation;
    private SavedState mPendingSavedState;
    int mPendingScrollPosition;
    int mPendingScrollPositionOffset;
    OrientationHelper mPrimaryOrientation;
    private BitSet mRemainingSpans;
    private boolean mReverseLayout;
    OrientationHelper mSecondaryOrientation;
    boolean mShouldReverseLayout;
    private int mSizePerSpan;
    private boolean mSmoothScrollbarEnabled;
    private int mSpanCount;
    private Span[] mSpans;
    private final Rect mTmpRect;
    private int mWidthSpec;

    /* renamed from: android.support.v7.widget.StaggeredGridLayoutManager.1 */
    class C01931 implements Runnable {
        C01931() {
        }

        public void run() {
            StaggeredGridLayoutManager.this.checkForGaps();
        }
    }

    private class AnchorInfo {
        boolean mInvalidateOffsets;
        boolean mLayoutFromEnd;
        int mOffset;
        int mPosition;

        private AnchorInfo() {
        }

        void reset() {
            this.mPosition = -1;
            this.mOffset = StaggeredGridLayoutManager.INVALID_OFFSET;
            this.mLayoutFromEnd = StaggeredGridLayoutManager.DEBUG;
            this.mInvalidateOffsets = StaggeredGridLayoutManager.DEBUG;
        }

        void assignCoordinateFromPadding() {
            this.mOffset = this.mLayoutFromEnd ? StaggeredGridLayoutManager.this.mPrimaryOrientation.getEndAfterPadding() : StaggeredGridLayoutManager.this.mPrimaryOrientation.getStartAfterPadding();
        }

        void assignCoordinateFromPadding(int addedDistance) {
            if (this.mLayoutFromEnd) {
                this.mOffset = StaggeredGridLayoutManager.this.mPrimaryOrientation.getEndAfterPadding() - addedDistance;
            } else {
                this.mOffset = StaggeredGridLayoutManager.this.mPrimaryOrientation.getStartAfterPadding() + addedDistance;
            }
        }
    }

    static class LazySpanLookup {
        private static final int MIN_SIZE = 10;
        int[] mData;
        List<FullSpanItem> mFullSpanItems;

        static class FullSpanItem implements Parcelable {
            public static final Creator<FullSpanItem> CREATOR;
            int mGapDir;
            int[] mGapPerSpan;
            boolean mHasUnwantedGapAfter;
            int mPosition;

            /* renamed from: android.support.v7.widget.StaggeredGridLayoutManager.LazySpanLookup.FullSpanItem.1 */
            static class C01941 implements Creator<FullSpanItem> {
                C01941() {
                }

                public FullSpanItem createFromParcel(Parcel in) {
                    return new FullSpanItem(in);
                }

                public FullSpanItem[] newArray(int size) {
                    return new FullSpanItem[size];
                }
            }

            public FullSpanItem(Parcel in) {
                boolean z = true;
                this.mPosition = in.readInt();
                this.mGapDir = in.readInt();
                if (in.readInt() != StaggeredGridLayoutManager.VERTICAL) {
                    z = StaggeredGridLayoutManager.DEBUG;
                }
                this.mHasUnwantedGapAfter = z;
                int spanCount = in.readInt();
                if (spanCount > 0) {
                    this.mGapPerSpan = new int[spanCount];
                    in.readIntArray(this.mGapPerSpan);
                }
            }

            int getGapForSpan(int spanIndex) {
                return this.mGapPerSpan == null ? StaggeredGridLayoutManager.HORIZONTAL : this.mGapPerSpan[spanIndex];
            }

            public void invalidateSpanGaps() {
                this.mGapPerSpan = null;
            }

            public int describeContents() {
                return StaggeredGridLayoutManager.HORIZONTAL;
            }

            public void writeToParcel(Parcel dest, int flags) {
                dest.writeInt(this.mPosition);
                dest.writeInt(this.mGapDir);
                dest.writeInt(this.mHasUnwantedGapAfter ? StaggeredGridLayoutManager.VERTICAL : StaggeredGridLayoutManager.HORIZONTAL);
                if (this.mGapPerSpan == null || this.mGapPerSpan.length <= 0) {
                    dest.writeInt(StaggeredGridLayoutManager.HORIZONTAL);
                    return;
                }
                dest.writeInt(this.mGapPerSpan.length);
                dest.writeIntArray(this.mGapPerSpan);
            }

            public String toString() {
                return "FullSpanItem{mPosition=" + this.mPosition + ", mGapDir=" + this.mGapDir + ", mHasUnwantedGapAfter=" + this.mHasUnwantedGapAfter + ", mGapPerSpan=" + Arrays.toString(this.mGapPerSpan) + '}';
            }

            static {
                CREATOR = new C01941();
            }
        }

        LazySpanLookup() {
        }

        int forceInvalidateAfter(int position) {
            if (this.mFullSpanItems != null) {
                for (int i = this.mFullSpanItems.size() - 1; i >= 0; i--) {
                    if (((FullSpanItem) this.mFullSpanItems.get(i)).mPosition >= position) {
                        this.mFullSpanItems.remove(i);
                    }
                }
            }
            return invalidateAfter(position);
        }

        int invalidateAfter(int position) {
            if (this.mData == null || position >= this.mData.length) {
                return -1;
            }
            int endPosition = invalidateFullSpansAfter(position);
            if (endPosition == -1) {
                Arrays.fill(this.mData, position, this.mData.length, -1);
                return this.mData.length;
            }
            Arrays.fill(this.mData, position, endPosition + StaggeredGridLayoutManager.VERTICAL, -1);
            return endPosition + StaggeredGridLayoutManager.VERTICAL;
        }

        int getSpan(int position) {
            if (this.mData == null || position >= this.mData.length) {
                return -1;
            }
            return this.mData[position];
        }

        void setSpan(int position, Span span) {
            ensureSize(position);
            this.mData[position] = span.mIndex;
        }

        int sizeForPosition(int position) {
            int len = this.mData.length;
            while (len <= position) {
                len *= StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS;
            }
            return len;
        }

        void ensureSize(int position) {
            if (this.mData == null) {
                this.mData = new int[(Math.max(position, MIN_SIZE) + StaggeredGridLayoutManager.VERTICAL)];
                Arrays.fill(this.mData, -1);
            } else if (position >= this.mData.length) {
                int[] old = this.mData;
                this.mData = new int[sizeForPosition(position)];
                System.arraycopy(old, StaggeredGridLayoutManager.HORIZONTAL, this.mData, StaggeredGridLayoutManager.HORIZONTAL, old.length);
                Arrays.fill(this.mData, old.length, this.mData.length, -1);
            }
        }

        void clear() {
            if (this.mData != null) {
                Arrays.fill(this.mData, -1);
            }
            this.mFullSpanItems = null;
        }

        void offsetForRemoval(int positionStart, int itemCount) {
            if (this.mData != null && positionStart < this.mData.length) {
                ensureSize(positionStart + itemCount);
                System.arraycopy(this.mData, positionStart + itemCount, this.mData, positionStart, (this.mData.length - positionStart) - itemCount);
                Arrays.fill(this.mData, this.mData.length - itemCount, this.mData.length, -1);
                offsetFullSpansForRemoval(positionStart, itemCount);
            }
        }

        private void offsetFullSpansForRemoval(int positionStart, int itemCount) {
            if (this.mFullSpanItems != null) {
                int end = positionStart + itemCount;
                for (int i = this.mFullSpanItems.size() - 1; i >= 0; i--) {
                    FullSpanItem fsi = (FullSpanItem) this.mFullSpanItems.get(i);
                    if (fsi.mPosition >= positionStart) {
                        if (fsi.mPosition < end) {
                            this.mFullSpanItems.remove(i);
                        } else {
                            fsi.mPosition -= itemCount;
                        }
                    }
                }
            }
        }

        void offsetForAddition(int positionStart, int itemCount) {
            if (this.mData != null && positionStart < this.mData.length) {
                ensureSize(positionStart + itemCount);
                System.arraycopy(this.mData, positionStart, this.mData, positionStart + itemCount, (this.mData.length - positionStart) - itemCount);
                Arrays.fill(this.mData, positionStart, positionStart + itemCount, -1);
                offsetFullSpansForAddition(positionStart, itemCount);
            }
        }

        private void offsetFullSpansForAddition(int positionStart, int itemCount) {
            if (this.mFullSpanItems != null) {
                for (int i = this.mFullSpanItems.size() - 1; i >= 0; i--) {
                    FullSpanItem fsi = (FullSpanItem) this.mFullSpanItems.get(i);
                    if (fsi.mPosition >= positionStart) {
                        fsi.mPosition += itemCount;
                    }
                }
            }
        }

        private int invalidateFullSpansAfter(int position) {
            if (this.mFullSpanItems == null) {
                return -1;
            }
            FullSpanItem item = getFullSpanItem(position);
            if (item != null) {
                this.mFullSpanItems.remove(item);
            }
            int nextFsiIndex = -1;
            int count = this.mFullSpanItems.size();
            for (int i = StaggeredGridLayoutManager.HORIZONTAL; i < count; i += StaggeredGridLayoutManager.VERTICAL) {
                if (((FullSpanItem) this.mFullSpanItems.get(i)).mPosition >= position) {
                    nextFsiIndex = i;
                    break;
                }
            }
            if (nextFsiIndex == -1) {
                return -1;
            }
            FullSpanItem fsi = (FullSpanItem) this.mFullSpanItems.get(nextFsiIndex);
            this.mFullSpanItems.remove(nextFsiIndex);
            return fsi.mPosition;
        }

        public void addFullSpanItem(FullSpanItem fullSpanItem) {
            if (this.mFullSpanItems == null) {
                this.mFullSpanItems = new ArrayList();
            }
            int size = this.mFullSpanItems.size();
            for (int i = StaggeredGridLayoutManager.HORIZONTAL; i < size; i += StaggeredGridLayoutManager.VERTICAL) {
                FullSpanItem other = (FullSpanItem) this.mFullSpanItems.get(i);
                if (other.mPosition == fullSpanItem.mPosition) {
                    this.mFullSpanItems.remove(i);
                }
                if (other.mPosition >= fullSpanItem.mPosition) {
                    this.mFullSpanItems.add(i, fullSpanItem);
                    return;
                }
            }
            this.mFullSpanItems.add(fullSpanItem);
        }

        public FullSpanItem getFullSpanItem(int position) {
            if (this.mFullSpanItems == null) {
                return null;
            }
            for (int i = this.mFullSpanItems.size() - 1; i >= 0; i--) {
                FullSpanItem fsi = (FullSpanItem) this.mFullSpanItems.get(i);
                if (fsi.mPosition == position) {
                    return fsi;
                }
            }
            return null;
        }

        public FullSpanItem getFirstFullSpanItemInRange(int minPos, int maxPos, int gapDir, boolean hasUnwantedGapAfter) {
            if (this.mFullSpanItems == null) {
                return null;
            }
            int limit = this.mFullSpanItems.size();
            for (int i = StaggeredGridLayoutManager.HORIZONTAL; i < limit; i += StaggeredGridLayoutManager.VERTICAL) {
                FullSpanItem fsi = (FullSpanItem) this.mFullSpanItems.get(i);
                if (fsi.mPosition >= maxPos) {
                    return null;
                }
                if (fsi.mPosition >= minPos) {
                    if (gapDir == 0 || fsi.mGapDir == gapDir) {
                        return fsi;
                    }
                    if (hasUnwantedGapAfter && fsi.mHasUnwantedGapAfter) {
                        return fsi;
                    }
                }
            }
            return null;
        }
    }

    public static class SavedState implements Parcelable {
        public static final Creator<SavedState> CREATOR;
        boolean mAnchorLayoutFromEnd;
        int mAnchorPosition;
        List<FullSpanItem> mFullSpanItems;
        boolean mLastLayoutRTL;
        boolean mReverseLayout;
        int[] mSpanLookup;
        int mSpanLookupSize;
        int[] mSpanOffsets;
        int mSpanOffsetsSize;
        int mVisibleAnchorPosition;

        /* renamed from: android.support.v7.widget.StaggeredGridLayoutManager.SavedState.1 */
        static class C01951 implements Creator<SavedState> {
            C01951() {
            }

            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        }

        SavedState(Parcel in) {
            boolean z;
            boolean z2 = true;
            this.mAnchorPosition = in.readInt();
            this.mVisibleAnchorPosition = in.readInt();
            this.mSpanOffsetsSize = in.readInt();
            if (this.mSpanOffsetsSize > 0) {
                this.mSpanOffsets = new int[this.mSpanOffsetsSize];
                in.readIntArray(this.mSpanOffsets);
            }
            this.mSpanLookupSize = in.readInt();
            if (this.mSpanLookupSize > 0) {
                this.mSpanLookup = new int[this.mSpanLookupSize];
                in.readIntArray(this.mSpanLookup);
            }
            this.mReverseLayout = in.readInt() == StaggeredGridLayoutManager.VERTICAL ? true : StaggeredGridLayoutManager.DEBUG;
            if (in.readInt() == StaggeredGridLayoutManager.VERTICAL) {
                z = true;
            } else {
                z = StaggeredGridLayoutManager.DEBUG;
            }
            this.mAnchorLayoutFromEnd = z;
            if (in.readInt() != StaggeredGridLayoutManager.VERTICAL) {
                z2 = StaggeredGridLayoutManager.DEBUG;
            }
            this.mLastLayoutRTL = z2;
            this.mFullSpanItems = in.readArrayList(FullSpanItem.class.getClassLoader());
        }

        public SavedState(SavedState other) {
            this.mSpanOffsetsSize = other.mSpanOffsetsSize;
            this.mAnchorPosition = other.mAnchorPosition;
            this.mVisibleAnchorPosition = other.mVisibleAnchorPosition;
            this.mSpanOffsets = other.mSpanOffsets;
            this.mSpanLookupSize = other.mSpanLookupSize;
            this.mSpanLookup = other.mSpanLookup;
            this.mReverseLayout = other.mReverseLayout;
            this.mAnchorLayoutFromEnd = other.mAnchorLayoutFromEnd;
            this.mLastLayoutRTL = other.mLastLayoutRTL;
            this.mFullSpanItems = other.mFullSpanItems;
        }

        void invalidateSpanInfo() {
            this.mSpanOffsets = null;
            this.mSpanOffsetsSize = StaggeredGridLayoutManager.HORIZONTAL;
            this.mSpanLookupSize = StaggeredGridLayoutManager.HORIZONTAL;
            this.mSpanLookup = null;
            this.mFullSpanItems = null;
        }

        void invalidateAnchorPositionInfo() {
            this.mSpanOffsets = null;
            this.mSpanOffsetsSize = StaggeredGridLayoutManager.HORIZONTAL;
            this.mAnchorPosition = -1;
            this.mVisibleAnchorPosition = -1;
        }

        public int describeContents() {
            return StaggeredGridLayoutManager.HORIZONTAL;
        }

        public void writeToParcel(Parcel dest, int flags) {
            int i;
            int i2 = StaggeredGridLayoutManager.VERTICAL;
            dest.writeInt(this.mAnchorPosition);
            dest.writeInt(this.mVisibleAnchorPosition);
            dest.writeInt(this.mSpanOffsetsSize);
            if (this.mSpanOffsetsSize > 0) {
                dest.writeIntArray(this.mSpanOffsets);
            }
            dest.writeInt(this.mSpanLookupSize);
            if (this.mSpanLookupSize > 0) {
                dest.writeIntArray(this.mSpanLookup);
            }
            if (this.mReverseLayout) {
                i = StaggeredGridLayoutManager.VERTICAL;
            } else {
                i = StaggeredGridLayoutManager.HORIZONTAL;
            }
            dest.writeInt(i);
            if (this.mAnchorLayoutFromEnd) {
                i = StaggeredGridLayoutManager.VERTICAL;
            } else {
                i = StaggeredGridLayoutManager.HORIZONTAL;
            }
            dest.writeInt(i);
            if (!this.mLastLayoutRTL) {
                i2 = StaggeredGridLayoutManager.HORIZONTAL;
            }
            dest.writeInt(i2);
            dest.writeList(this.mFullSpanItems);
        }

        static {
            CREATOR = new C01951();
        }
    }

    class Span {
        static final int INVALID_LINE = Integer.MIN_VALUE;
        int mCachedEnd;
        int mCachedStart;
        int mDeletedSize;
        final int mIndex;
        private ArrayList<View> mViews;

        private Span(int index) {
            this.mViews = new ArrayList();
            this.mCachedStart = INVALID_LINE;
            this.mCachedEnd = INVALID_LINE;
            this.mDeletedSize = StaggeredGridLayoutManager.HORIZONTAL;
            this.mIndex = index;
        }

        int getStartLine(int def) {
            if (this.mCachedStart != INVALID_LINE) {
                return this.mCachedStart;
            }
            if (this.mViews.size() == 0) {
                return def;
            }
            calculateCachedStart();
            return this.mCachedStart;
        }

        void calculateCachedStart() {
            View startView = (View) this.mViews.get(StaggeredGridLayoutManager.HORIZONTAL);
            LayoutParams lp = getLayoutParams(startView);
            this.mCachedStart = StaggeredGridLayoutManager.this.mPrimaryOrientation.getDecoratedStart(startView);
            if (lp.mFullSpan) {
                FullSpanItem fsi = StaggeredGridLayoutManager.this.mLazySpanLookup.getFullSpanItem(lp.getViewLayoutPosition());
                if (fsi != null && fsi.mGapDir == -1) {
                    this.mCachedStart -= fsi.getGapForSpan(this.mIndex);
                }
            }
        }

        int getStartLine() {
            if (this.mCachedStart != INVALID_LINE) {
                return this.mCachedStart;
            }
            calculateCachedStart();
            return this.mCachedStart;
        }

        int getEndLine(int def) {
            if (this.mCachedEnd != INVALID_LINE) {
                return this.mCachedEnd;
            }
            if (this.mViews.size() == 0) {
                return def;
            }
            calculateCachedEnd();
            return this.mCachedEnd;
        }

        void calculateCachedEnd() {
            View endView = (View) this.mViews.get(this.mViews.size() - 1);
            LayoutParams lp = getLayoutParams(endView);
            this.mCachedEnd = StaggeredGridLayoutManager.this.mPrimaryOrientation.getDecoratedEnd(endView);
            if (lp.mFullSpan) {
                FullSpanItem fsi = StaggeredGridLayoutManager.this.mLazySpanLookup.getFullSpanItem(lp.getViewLayoutPosition());
                if (fsi != null && fsi.mGapDir == StaggeredGridLayoutManager.VERTICAL) {
                    this.mCachedEnd += fsi.getGapForSpan(this.mIndex);
                }
            }
        }

        int getEndLine() {
            if (this.mCachedEnd != INVALID_LINE) {
                return this.mCachedEnd;
            }
            calculateCachedEnd();
            return this.mCachedEnd;
        }

        void prependToSpan(View view) {
            LayoutParams lp = getLayoutParams(view);
            lp.mSpan = this;
            this.mViews.add(StaggeredGridLayoutManager.HORIZONTAL, view);
            this.mCachedStart = INVALID_LINE;
            if (this.mViews.size() == StaggeredGridLayoutManager.VERTICAL) {
                this.mCachedEnd = INVALID_LINE;
            }
            if (lp.isItemRemoved() || lp.isItemChanged()) {
                this.mDeletedSize += StaggeredGridLayoutManager.this.mPrimaryOrientation.getDecoratedMeasurement(view);
            }
        }

        void appendToSpan(View view) {
            LayoutParams lp = getLayoutParams(view);
            lp.mSpan = this;
            this.mViews.add(view);
            this.mCachedEnd = INVALID_LINE;
            if (this.mViews.size() == StaggeredGridLayoutManager.VERTICAL) {
                this.mCachedStart = INVALID_LINE;
            }
            if (lp.isItemRemoved() || lp.isItemChanged()) {
                this.mDeletedSize += StaggeredGridLayoutManager.this.mPrimaryOrientation.getDecoratedMeasurement(view);
            }
        }

        void cacheReferenceLineAndClear(boolean reverseLayout, int offset) {
            int reference;
            if (reverseLayout) {
                reference = getEndLine(INVALID_LINE);
            } else {
                reference = getStartLine(INVALID_LINE);
            }
            clear();
            if (reference != INVALID_LINE) {
                if (reverseLayout && reference < StaggeredGridLayoutManager.this.mPrimaryOrientation.getEndAfterPadding()) {
                    return;
                }
                if (reverseLayout || reference <= StaggeredGridLayoutManager.this.mPrimaryOrientation.getStartAfterPadding()) {
                    if (offset != INVALID_LINE) {
                        reference += offset;
                    }
                    this.mCachedEnd = reference;
                    this.mCachedStart = reference;
                }
            }
        }

        void clear() {
            this.mViews.clear();
            invalidateCache();
            this.mDeletedSize = StaggeredGridLayoutManager.HORIZONTAL;
        }

        void invalidateCache() {
            this.mCachedStart = INVALID_LINE;
            this.mCachedEnd = INVALID_LINE;
        }

        void setLine(int line) {
            this.mCachedStart = line;
            this.mCachedEnd = line;
        }

        void popEnd() {
            int size = this.mViews.size();
            View end = (View) this.mViews.remove(size - 1);
            LayoutParams lp = getLayoutParams(end);
            lp.mSpan = null;
            if (lp.isItemRemoved() || lp.isItemChanged()) {
                this.mDeletedSize -= StaggeredGridLayoutManager.this.mPrimaryOrientation.getDecoratedMeasurement(end);
            }
            if (size == StaggeredGridLayoutManager.VERTICAL) {
                this.mCachedStart = INVALID_LINE;
            }
            this.mCachedEnd = INVALID_LINE;
        }

        void popStart() {
            View start = (View) this.mViews.remove(StaggeredGridLayoutManager.HORIZONTAL);
            LayoutParams lp = getLayoutParams(start);
            lp.mSpan = null;
            if (this.mViews.size() == 0) {
                this.mCachedEnd = INVALID_LINE;
            }
            if (lp.isItemRemoved() || lp.isItemChanged()) {
                this.mDeletedSize -= StaggeredGridLayoutManager.this.mPrimaryOrientation.getDecoratedMeasurement(start);
            }
            this.mCachedStart = INVALID_LINE;
        }

        public int getDeletedSize() {
            return this.mDeletedSize;
        }

        LayoutParams getLayoutParams(View view) {
            return (LayoutParams) view.getLayoutParams();
        }

        void onOffset(int dt) {
            if (this.mCachedStart != INVALID_LINE) {
                this.mCachedStart += dt;
            }
            if (this.mCachedEnd != INVALID_LINE) {
                this.mCachedEnd += dt;
            }
        }

        int getNormalizedOffset(int dt, int targetStart, int targetEnd) {
            if (this.mViews.size() == 0) {
                return StaggeredGridLayoutManager.HORIZONTAL;
            }
            if (dt < 0) {
                int endSpace = getEndLine() - targetEnd;
                if (endSpace <= 0) {
                    return StaggeredGridLayoutManager.HORIZONTAL;
                }
                if ((-dt) > endSpace) {
                    return -endSpace;
                }
                return dt;
            }
            int startSpace = targetStart - getStartLine();
            if (startSpace <= 0) {
                return StaggeredGridLayoutManager.HORIZONTAL;
            }
            if (startSpace >= dt) {
                startSpace = dt;
            }
            return startSpace;
        }

        boolean isEmpty(int start, int end) {
            int count = this.mViews.size();
            for (int i = StaggeredGridLayoutManager.HORIZONTAL; i < count; i += StaggeredGridLayoutManager.VERTICAL) {
                View view = (View) this.mViews.get(i);
                if (StaggeredGridLayoutManager.this.mPrimaryOrientation.getDecoratedStart(view) < end && StaggeredGridLayoutManager.this.mPrimaryOrientation.getDecoratedEnd(view) > start) {
                    return StaggeredGridLayoutManager.DEBUG;
                }
            }
            return true;
        }

        public int findFirstVisibleItemPosition() {
            return StaggeredGridLayoutManager.this.mReverseLayout ? findOneVisibleChild(this.mViews.size() - 1, -1, StaggeredGridLayoutManager.DEBUG) : findOneVisibleChild(StaggeredGridLayoutManager.HORIZONTAL, this.mViews.size(), StaggeredGridLayoutManager.DEBUG);
        }

        public int findFirstCompletelyVisibleItemPosition() {
            return StaggeredGridLayoutManager.this.mReverseLayout ? findOneVisibleChild(this.mViews.size() - 1, -1, true) : findOneVisibleChild(StaggeredGridLayoutManager.HORIZONTAL, this.mViews.size(), true);
        }

        public int findLastVisibleItemPosition() {
            return StaggeredGridLayoutManager.this.mReverseLayout ? findOneVisibleChild(StaggeredGridLayoutManager.HORIZONTAL, this.mViews.size(), StaggeredGridLayoutManager.DEBUG) : findOneVisibleChild(this.mViews.size() - 1, -1, StaggeredGridLayoutManager.DEBUG);
        }

        public int findLastCompletelyVisibleItemPosition() {
            return StaggeredGridLayoutManager.this.mReverseLayout ? findOneVisibleChild(StaggeredGridLayoutManager.HORIZONTAL, this.mViews.size(), true) : findOneVisibleChild(this.mViews.size() - 1, -1, true);
        }

        int findOneVisibleChild(int fromIndex, int toIndex, boolean completelyVisible) {
            int next;
            int start = StaggeredGridLayoutManager.this.mPrimaryOrientation.getStartAfterPadding();
            int end = StaggeredGridLayoutManager.this.mPrimaryOrientation.getEndAfterPadding();
            if (toIndex > fromIndex) {
                next = StaggeredGridLayoutManager.VERTICAL;
            } else {
                next = -1;
            }
            for (int i = fromIndex; i != toIndex; i += next) {
                View child = (View) this.mViews.get(i);
                int childStart = StaggeredGridLayoutManager.this.mPrimaryOrientation.getDecoratedStart(child);
                int childEnd = StaggeredGridLayoutManager.this.mPrimaryOrientation.getDecoratedEnd(child);
                if (childStart < end && childEnd > start) {
                    if (!completelyVisible) {
                        return StaggeredGridLayoutManager.this.getPosition(child);
                    }
                    if (childStart >= start && childEnd <= end) {
                        return StaggeredGridLayoutManager.this.getPosition(child);
                    }
                }
            }
            return -1;
        }
    }

    public static class LayoutParams extends android.support.v7.widget.RecyclerView.LayoutParams {
        public static final int INVALID_SPAN_ID = -1;
        boolean mFullSpan;
        Span mSpan;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(MarginLayoutParams source) {
            super(source);
        }

        public LayoutParams(android.view.ViewGroup.LayoutParams source) {
            super(source);
        }

        public LayoutParams(android.support.v7.widget.RecyclerView.LayoutParams source) {
            super(source);
        }

        public void setFullSpan(boolean fullSpan) {
            this.mFullSpan = fullSpan;
        }

        public boolean isFullSpan() {
            return this.mFullSpan;
        }

        public final int getSpanIndex() {
            if (this.mSpan == null) {
                return INVALID_SPAN_ID;
            }
            return this.mSpan.mIndex;
        }
    }

    /* renamed from: android.support.v7.widget.StaggeredGridLayoutManager.2 */
    class C03242 extends LinearSmoothScroller {
        C03242(Context x0) {
            super(x0);
        }

        public PointF computeScrollVectorForPosition(int targetPosition) {
            int direction = StaggeredGridLayoutManager.this.calculateScrollDirectionForPosition(targetPosition);
            if (direction == 0) {
                return null;
            }
            if (StaggeredGridLayoutManager.this.mOrientation == 0) {
                return new PointF((float) direction, 0.0f);
            }
            return new PointF(0.0f, (float) direction);
        }
    }

    public StaggeredGridLayoutManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        this.mSpanCount = -1;
        this.mReverseLayout = DEBUG;
        this.mShouldReverseLayout = DEBUG;
        this.mPendingScrollPosition = -1;
        this.mPendingScrollPositionOffset = INVALID_OFFSET;
        this.mLazySpanLookup = new LazySpanLookup();
        this.mGapStrategy = GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS;
        this.mTmpRect = new Rect();
        this.mAnchorInfo = new AnchorInfo();
        this.mLaidOutInvalidFullSpan = DEBUG;
        this.mSmoothScrollbarEnabled = true;
        this.mCheckForGapsRunnable = new C01931();
        Properties properties = LayoutManager.getProperties(context, attrs, defStyleAttr, defStyleRes);
        setOrientation(properties.orientation);
        setSpanCount(properties.spanCount);
        setReverseLayout(properties.reverseLayout);
    }

    public StaggeredGridLayoutManager(int spanCount, int orientation) {
        this.mSpanCount = -1;
        this.mReverseLayout = DEBUG;
        this.mShouldReverseLayout = DEBUG;
        this.mPendingScrollPosition = -1;
        this.mPendingScrollPositionOffset = INVALID_OFFSET;
        this.mLazySpanLookup = new LazySpanLookup();
        this.mGapStrategy = GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS;
        this.mTmpRect = new Rect();
        this.mAnchorInfo = new AnchorInfo();
        this.mLaidOutInvalidFullSpan = DEBUG;
        this.mSmoothScrollbarEnabled = true;
        this.mCheckForGapsRunnable = new C01931();
        this.mOrientation = orientation;
        setSpanCount(spanCount);
    }

    private boolean checkForGaps() {
        if (getChildCount() == 0 || this.mGapStrategy == 0 || !isAttachedToWindow()) {
            return DEBUG;
        }
        int minPos;
        int maxPos;
        if (this.mShouldReverseLayout) {
            minPos = getLastChildPosition();
            maxPos = getFirstChildPosition();
        } else {
            minPos = getFirstChildPosition();
            maxPos = getLastChildPosition();
        }
        if (minPos == 0 && hasGapsToFix() != null) {
            this.mLazySpanLookup.clear();
            requestSimpleAnimationsInNextLayout();
            requestLayout();
            return true;
        } else if (!this.mLaidOutInvalidFullSpan) {
            return DEBUG;
        } else {
            int invalidGapDir;
            if (this.mShouldReverseLayout) {
                invalidGapDir = -1;
            } else {
                invalidGapDir = VERTICAL;
            }
            FullSpanItem invalidFsi = this.mLazySpanLookup.getFirstFullSpanItemInRange(minPos, maxPos + VERTICAL, invalidGapDir, true);
            if (invalidFsi == null) {
                this.mLaidOutInvalidFullSpan = DEBUG;
                this.mLazySpanLookup.forceInvalidateAfter(maxPos + VERTICAL);
                return DEBUG;
            }
            FullSpanItem validFsi = this.mLazySpanLookup.getFirstFullSpanItemInRange(minPos, invalidFsi.mPosition, invalidGapDir * -1, true);
            if (validFsi == null) {
                this.mLazySpanLookup.forceInvalidateAfter(invalidFsi.mPosition);
            } else {
                this.mLazySpanLookup.forceInvalidateAfter(validFsi.mPosition + VERTICAL);
            }
            requestSimpleAnimationsInNextLayout();
            requestLayout();
            return true;
        }
    }

    public void onScrollStateChanged(int state) {
        if (state == 0) {
            checkForGaps();
        }
    }

    public void onDetachedFromWindow(RecyclerView view, Recycler recycler) {
        removeCallbacks(this.mCheckForGapsRunnable);
        for (int i = HORIZONTAL; i < this.mSpanCount; i += VERTICAL) {
            this.mSpans[i].clear();
        }
    }

    View hasGapsToFix() {
        int firstChildIndex;
        int childLimit;
        int endChildIndex = getChildCount() - 1;
        BitSet mSpansToCheck = new BitSet(this.mSpanCount);
        mSpansToCheck.set(HORIZONTAL, this.mSpanCount, true);
        int i = this.mOrientation;
        int preferredSpanDir = (r0 == VERTICAL && isLayoutRTL()) ? VERTICAL : -1;
        if (this.mShouldReverseLayout) {
            firstChildIndex = endChildIndex;
            childLimit = HORIZONTAL - 1;
        } else {
            firstChildIndex = HORIZONTAL;
            childLimit = endChildIndex + VERTICAL;
        }
        int nextChildDiff = firstChildIndex < childLimit ? VERTICAL : -1;
        int i2 = firstChildIndex;
        while (i2 != childLimit) {
            View child = getChildAt(i2);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            if (mSpansToCheck.get(lp.mSpan.mIndex)) {
                if (checkSpanForGap(lp.mSpan)) {
                    return child;
                }
                mSpansToCheck.clear(lp.mSpan.mIndex);
            }
            if (!(lp.mFullSpan || i2 + nextChildDiff == childLimit)) {
                View nextChild = getChildAt(i2 + nextChildDiff);
                boolean compareSpans = DEBUG;
                if (this.mShouldReverseLayout) {
                    int myEnd = this.mPrimaryOrientation.getDecoratedEnd(child);
                    int nextEnd = this.mPrimaryOrientation.getDecoratedEnd(nextChild);
                    if (myEnd < nextEnd) {
                        return child;
                    }
                    if (myEnd == nextEnd) {
                        compareSpans = true;
                    }
                } else {
                    int myStart = this.mPrimaryOrientation.getDecoratedStart(child);
                    int nextStart = this.mPrimaryOrientation.getDecoratedStart(nextChild);
                    if (myStart > nextStart) {
                        return child;
                    }
                    if (myStart == nextStart) {
                        compareSpans = true;
                    }
                }
                if (compareSpans) {
                    Object obj;
                    Object obj2;
                    if (lp.mSpan.mIndex - ((LayoutParams) nextChild.getLayoutParams()).mSpan.mIndex < 0) {
                        obj = VERTICAL;
                    } else {
                        obj = HORIZONTAL;
                    }
                    if (preferredSpanDir < 0) {
                        obj2 = VERTICAL;
                    } else {
                        obj2 = null;
                    }
                    if (obj != obj2) {
                        return child;
                    }
                } else {
                    continue;
                }
            }
            i2 += nextChildDiff;
        }
        return null;
    }

    private boolean checkSpanForGap(Span span) {
        if (this.mShouldReverseLayout) {
            if (span.getEndLine() < this.mPrimaryOrientation.getEndAfterPadding()) {
                return true;
            }
        } else if (span.getStartLine() > this.mPrimaryOrientation.getStartAfterPadding()) {
            return true;
        }
        return DEBUG;
    }

    public void setSpanCount(int spanCount) {
        assertNotInLayoutOrScroll(null);
        if (spanCount != this.mSpanCount) {
            invalidateSpanAssignments();
            this.mSpanCount = spanCount;
            this.mRemainingSpans = new BitSet(this.mSpanCount);
            this.mSpans = new Span[this.mSpanCount];
            for (int i = HORIZONTAL; i < this.mSpanCount; i += VERTICAL) {
                this.mSpans[i] = new Span(i, null);
            }
            requestLayout();
        }
    }

    public void setOrientation(int orientation) {
        if (orientation == 0 || orientation == VERTICAL) {
            assertNotInLayoutOrScroll(null);
            if (orientation != this.mOrientation) {
                this.mOrientation = orientation;
                if (!(this.mPrimaryOrientation == null || this.mSecondaryOrientation == null)) {
                    OrientationHelper tmp = this.mPrimaryOrientation;
                    this.mPrimaryOrientation = this.mSecondaryOrientation;
                    this.mSecondaryOrientation = tmp;
                }
                requestLayout();
                return;
            }
            return;
        }
        throw new IllegalArgumentException("invalid orientation.");
    }

    public void setReverseLayout(boolean reverseLayout) {
        assertNotInLayoutOrScroll(null);
        if (!(this.mPendingSavedState == null || this.mPendingSavedState.mReverseLayout == reverseLayout)) {
            this.mPendingSavedState.mReverseLayout = reverseLayout;
        }
        this.mReverseLayout = reverseLayout;
        requestLayout();
    }

    public int getGapStrategy() {
        return this.mGapStrategy;
    }

    public void setGapStrategy(int gapStrategy) {
        assertNotInLayoutOrScroll(null);
        if (gapStrategy != this.mGapStrategy) {
            if (gapStrategy == 0 || gapStrategy == GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS) {
                this.mGapStrategy = gapStrategy;
                requestLayout();
                return;
            }
            throw new IllegalArgumentException("invalid gap strategy. Must be GAP_HANDLING_NONE or GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS");
        }
    }

    public void assertNotInLayoutOrScroll(String message) {
        if (this.mPendingSavedState == null) {
            super.assertNotInLayoutOrScroll(message);
        }
    }

    public int getSpanCount() {
        return this.mSpanCount;
    }

    public void invalidateSpanAssignments() {
        this.mLazySpanLookup.clear();
        requestLayout();
    }

    private void ensureOrientationHelper() {
        if (this.mPrimaryOrientation == null) {
            this.mPrimaryOrientation = OrientationHelper.createOrientationHelper(this, this.mOrientation);
            this.mSecondaryOrientation = OrientationHelper.createOrientationHelper(this, 1 - this.mOrientation);
            this.mLayoutState = new LayoutState();
        }
    }

    private void resolveShouldLayoutReverse() {
        boolean z = true;
        if (this.mOrientation == VERTICAL || !isLayoutRTL()) {
            this.mShouldReverseLayout = this.mReverseLayout;
            return;
        }
        if (this.mReverseLayout) {
            z = DEBUG;
        }
        this.mShouldReverseLayout = z;
    }

    boolean isLayoutRTL() {
        return getLayoutDirection() == VERTICAL ? true : DEBUG;
    }

    public boolean getReverseLayout() {
        return this.mReverseLayout;
    }

    public void onLayoutChildren(Recycler recycler, State state) {
        boolean needToCheckForGaps = true;
        ensureOrientationHelper();
        AnchorInfo anchorInfo = this.mAnchorInfo;
        anchorInfo.reset();
        if (!(this.mPendingSavedState == null && this.mPendingScrollPosition == -1) && state.getItemCount() == 0) {
            removeAndRecycleAllViews(recycler);
            return;
        }
        if (this.mPendingSavedState != null) {
            applyPendingSavedState(anchorInfo);
        } else {
            resolveShouldLayoutReverse();
            anchorInfo.mLayoutFromEnd = this.mShouldReverseLayout;
        }
        updateAnchorInfoForLayout(state, anchorInfo);
        if (this.mPendingSavedState == null && !(anchorInfo.mLayoutFromEnd == this.mLastLayoutFromEnd && isLayoutRTL() == this.mLastLayoutRTL)) {
            this.mLazySpanLookup.clear();
            anchorInfo.mInvalidateOffsets = true;
        }
        if (getChildCount() > 0 && (this.mPendingSavedState == null || this.mPendingSavedState.mSpanOffsetsSize < VERTICAL)) {
            int i;
            if (anchorInfo.mInvalidateOffsets) {
                for (i = HORIZONTAL; i < this.mSpanCount; i += VERTICAL) {
                    this.mSpans[i].clear();
                    if (anchorInfo.mOffset != INVALID_OFFSET) {
                        this.mSpans[i].setLine(anchorInfo.mOffset);
                    }
                }
            } else {
                for (i = HORIZONTAL; i < this.mSpanCount; i += VERTICAL) {
                    this.mSpans[i].cacheReferenceLineAndClear(this.mShouldReverseLayout, anchorInfo.mOffset);
                }
            }
        }
        detachAndScrapAttachedViews(recycler);
        this.mLaidOutInvalidFullSpan = DEBUG;
        updateMeasureSpecs();
        updateLayoutState(anchorInfo.mPosition, state);
        if (anchorInfo.mLayoutFromEnd) {
            setLayoutStateDirection(-1);
            fill(recycler, this.mLayoutState, state);
            setLayoutStateDirection(VERTICAL);
            this.mLayoutState.mCurrentPosition = anchorInfo.mPosition + this.mLayoutState.mItemDirection;
            fill(recycler, this.mLayoutState, state);
        } else {
            setLayoutStateDirection(VERTICAL);
            fill(recycler, this.mLayoutState, state);
            setLayoutStateDirection(-1);
            this.mLayoutState.mCurrentPosition = anchorInfo.mPosition + this.mLayoutState.mItemDirection;
            fill(recycler, this.mLayoutState, state);
        }
        if (getChildCount() > 0) {
            if (this.mShouldReverseLayout) {
                fixEndGap(recycler, state, true);
                fixStartGap(recycler, state, DEBUG);
            } else {
                fixStartGap(recycler, state, true);
                fixEndGap(recycler, state, DEBUG);
            }
        }
        if (!state.isPreLayout()) {
            if (this.mGapStrategy == 0 || getChildCount() <= 0 || (!this.mLaidOutInvalidFullSpan && hasGapsToFix() == null)) {
                needToCheckForGaps = DEBUG;
            }
            if (needToCheckForGaps) {
                removeCallbacks(this.mCheckForGapsRunnable);
                postOnAnimation(this.mCheckForGapsRunnable);
            }
            this.mPendingScrollPosition = -1;
            this.mPendingScrollPositionOffset = INVALID_OFFSET;
        }
        this.mLastLayoutFromEnd = anchorInfo.mLayoutFromEnd;
        this.mLastLayoutRTL = isLayoutRTL();
        this.mPendingSavedState = null;
    }

    private void applyPendingSavedState(AnchorInfo anchorInfo) {
        if (this.mPendingSavedState.mSpanOffsetsSize > 0) {
            if (this.mPendingSavedState.mSpanOffsetsSize == this.mSpanCount) {
                for (int i = HORIZONTAL; i < this.mSpanCount; i += VERTICAL) {
                    this.mSpans[i].clear();
                    int line = this.mPendingSavedState.mSpanOffsets[i];
                    if (line != INVALID_OFFSET) {
                        if (this.mPendingSavedState.mAnchorLayoutFromEnd) {
                            line += this.mPrimaryOrientation.getEndAfterPadding();
                        } else {
                            line += this.mPrimaryOrientation.getStartAfterPadding();
                        }
                    }
                    this.mSpans[i].setLine(line);
                }
            } else {
                this.mPendingSavedState.invalidateSpanInfo();
                this.mPendingSavedState.mAnchorPosition = this.mPendingSavedState.mVisibleAnchorPosition;
            }
        }
        this.mLastLayoutRTL = this.mPendingSavedState.mLastLayoutRTL;
        setReverseLayout(this.mPendingSavedState.mReverseLayout);
        resolveShouldLayoutReverse();
        if (this.mPendingSavedState.mAnchorPosition != -1) {
            this.mPendingScrollPosition = this.mPendingSavedState.mAnchorPosition;
            anchorInfo.mLayoutFromEnd = this.mPendingSavedState.mAnchorLayoutFromEnd;
        } else {
            anchorInfo.mLayoutFromEnd = this.mShouldReverseLayout;
        }
        if (this.mPendingSavedState.mSpanLookupSize > VERTICAL) {
            this.mLazySpanLookup.mData = this.mPendingSavedState.mSpanLookup;
            this.mLazySpanLookup.mFullSpanItems = this.mPendingSavedState.mFullSpanItems;
        }
    }

    void updateAnchorInfoForLayout(State state, AnchorInfo anchorInfo) {
        if (!updateAnchorFromPendingData(state, anchorInfo) && !updateAnchorFromChildren(state, anchorInfo)) {
            anchorInfo.assignCoordinateFromPadding();
            anchorInfo.mPosition = HORIZONTAL;
        }
    }

    private boolean updateAnchorFromChildren(State state, AnchorInfo anchorInfo) {
        anchorInfo.mPosition = this.mLastLayoutFromEnd ? findLastReferenceChildPosition(state.getItemCount()) : findFirstReferenceChildPosition(state.getItemCount());
        anchorInfo.mOffset = INVALID_OFFSET;
        return true;
    }

    boolean updateAnchorFromPendingData(State state, AnchorInfo anchorInfo) {
        boolean z = DEBUG;
        if (state.isPreLayout() || this.mPendingScrollPosition == -1) {
            return DEBUG;
        }
        if (this.mPendingScrollPosition < 0 || this.mPendingScrollPosition >= state.getItemCount()) {
            this.mPendingScrollPosition = -1;
            this.mPendingScrollPositionOffset = INVALID_OFFSET;
            return DEBUG;
        } else if (this.mPendingSavedState == null || this.mPendingSavedState.mAnchorPosition == -1 || this.mPendingSavedState.mSpanOffsetsSize < VERTICAL) {
            View child = findViewByPosition(this.mPendingScrollPosition);
            if (child != null) {
                anchorInfo.mPosition = this.mShouldReverseLayout ? getLastChildPosition() : getFirstChildPosition();
                if (this.mPendingScrollPositionOffset != INVALID_OFFSET) {
                    if (anchorInfo.mLayoutFromEnd) {
                        anchorInfo.mOffset = (this.mPrimaryOrientation.getEndAfterPadding() - this.mPendingScrollPositionOffset) - this.mPrimaryOrientation.getDecoratedEnd(child);
                        return true;
                    }
                    anchorInfo.mOffset = (this.mPrimaryOrientation.getStartAfterPadding() + this.mPendingScrollPositionOffset) - this.mPrimaryOrientation.getDecoratedStart(child);
                    return true;
                } else if (this.mPrimaryOrientation.getDecoratedMeasurement(child) > this.mPrimaryOrientation.getTotalSpace()) {
                    anchorInfo.mOffset = anchorInfo.mLayoutFromEnd ? this.mPrimaryOrientation.getEndAfterPadding() : this.mPrimaryOrientation.getStartAfterPadding();
                    return true;
                } else {
                    int startGap = this.mPrimaryOrientation.getDecoratedStart(child) - this.mPrimaryOrientation.getStartAfterPadding();
                    if (startGap < 0) {
                        anchorInfo.mOffset = -startGap;
                        return true;
                    }
                    int endGap = this.mPrimaryOrientation.getEndAfterPadding() - this.mPrimaryOrientation.getDecoratedEnd(child);
                    if (endGap < 0) {
                        anchorInfo.mOffset = endGap;
                        return true;
                    }
                    anchorInfo.mOffset = INVALID_OFFSET;
                    return true;
                }
            }
            anchorInfo.mPosition = this.mPendingScrollPosition;
            if (this.mPendingScrollPositionOffset == INVALID_OFFSET) {
                if (calculateScrollDirectionForPosition(anchorInfo.mPosition) == VERTICAL) {
                    z = true;
                }
                anchorInfo.mLayoutFromEnd = z;
                anchorInfo.assignCoordinateFromPadding();
            } else {
                anchorInfo.assignCoordinateFromPadding(this.mPendingScrollPositionOffset);
            }
            anchorInfo.mInvalidateOffsets = true;
            return true;
        } else {
            anchorInfo.mOffset = INVALID_OFFSET;
            anchorInfo.mPosition = this.mPendingScrollPosition;
            return true;
        }
    }

    void updateMeasureSpecs() {
        this.mSizePerSpan = this.mSecondaryOrientation.getTotalSpace() / this.mSpanCount;
        this.mFullSizeSpec = MeasureSpec.makeMeasureSpec(this.mSecondaryOrientation.getTotalSpace(), 1073741824);
        if (this.mOrientation == VERTICAL) {
            this.mWidthSpec = MeasureSpec.makeMeasureSpec(this.mSizePerSpan, 1073741824);
            this.mHeightSpec = MeasureSpec.makeMeasureSpec(HORIZONTAL, HORIZONTAL);
            return;
        }
        this.mHeightSpec = MeasureSpec.makeMeasureSpec(this.mSizePerSpan, 1073741824);
        this.mWidthSpec = MeasureSpec.makeMeasureSpec(HORIZONTAL, HORIZONTAL);
    }

    public boolean supportsPredictiveItemAnimations() {
        return this.mPendingSavedState == null ? true : DEBUG;
    }

    public int[] findFirstVisibleItemPositions(int[] into) {
        if (into == null) {
            into = new int[this.mSpanCount];
        } else if (into.length < this.mSpanCount) {
            throw new IllegalArgumentException("Provided int[]'s size must be more than or equal to span count. Expected:" + this.mSpanCount + ", array size:" + into.length);
        }
        for (int i = HORIZONTAL; i < this.mSpanCount; i += VERTICAL) {
            into[i] = this.mSpans[i].findFirstVisibleItemPosition();
        }
        return into;
    }

    public int[] findFirstCompletelyVisibleItemPositions(int[] into) {
        if (into == null) {
            into = new int[this.mSpanCount];
        } else if (into.length < this.mSpanCount) {
            throw new IllegalArgumentException("Provided int[]'s size must be more than or equal to span count. Expected:" + this.mSpanCount + ", array size:" + into.length);
        }
        for (int i = HORIZONTAL; i < this.mSpanCount; i += VERTICAL) {
            into[i] = this.mSpans[i].findFirstCompletelyVisibleItemPosition();
        }
        return into;
    }

    public int[] findLastVisibleItemPositions(int[] into) {
        if (into == null) {
            into = new int[this.mSpanCount];
        } else if (into.length < this.mSpanCount) {
            throw new IllegalArgumentException("Provided int[]'s size must be more than or equal to span count. Expected:" + this.mSpanCount + ", array size:" + into.length);
        }
        for (int i = HORIZONTAL; i < this.mSpanCount; i += VERTICAL) {
            into[i] = this.mSpans[i].findLastVisibleItemPosition();
        }
        return into;
    }

    public int[] findLastCompletelyVisibleItemPositions(int[] into) {
        if (into == null) {
            into = new int[this.mSpanCount];
        } else if (into.length < this.mSpanCount) {
            throw new IllegalArgumentException("Provided int[]'s size must be more than or equal to span count. Expected:" + this.mSpanCount + ", array size:" + into.length);
        }
        for (int i = HORIZONTAL; i < this.mSpanCount; i += VERTICAL) {
            into[i] = this.mSpans[i].findLastCompletelyVisibleItemPosition();
        }
        return into;
    }

    public int computeHorizontalScrollOffset(State state) {
        return computeScrollOffset(state);
    }

    private int computeScrollOffset(State state) {
        boolean z = DEBUG;
        if (getChildCount() == 0) {
            return HORIZONTAL;
        }
        ensureOrientationHelper();
        OrientationHelper orientationHelper = this.mPrimaryOrientation;
        View findFirstVisibleItemClosestToStart = findFirstVisibleItemClosestToStart(!this.mSmoothScrollbarEnabled ? true : DEBUG, true);
        if (!this.mSmoothScrollbarEnabled) {
            z = true;
        }
        return ScrollbarHelper.computeScrollOffset(state, orientationHelper, findFirstVisibleItemClosestToStart, findFirstVisibleItemClosestToEnd(z, true), this, this.mSmoothScrollbarEnabled, this.mShouldReverseLayout);
    }

    public int computeVerticalScrollOffset(State state) {
        return computeScrollOffset(state);
    }

    public int computeHorizontalScrollExtent(State state) {
        return computeScrollExtent(state);
    }

    private int computeScrollExtent(State state) {
        boolean z = DEBUG;
        if (getChildCount() == 0) {
            return HORIZONTAL;
        }
        ensureOrientationHelper();
        OrientationHelper orientationHelper = this.mPrimaryOrientation;
        View findFirstVisibleItemClosestToStart = findFirstVisibleItemClosestToStart(!this.mSmoothScrollbarEnabled ? true : DEBUG, true);
        if (!this.mSmoothScrollbarEnabled) {
            z = true;
        }
        return ScrollbarHelper.computeScrollExtent(state, orientationHelper, findFirstVisibleItemClosestToStart, findFirstVisibleItemClosestToEnd(z, true), this, this.mSmoothScrollbarEnabled);
    }

    public int computeVerticalScrollExtent(State state) {
        return computeScrollExtent(state);
    }

    public int computeHorizontalScrollRange(State state) {
        return computeScrollRange(state);
    }

    private int computeScrollRange(State state) {
        boolean z = DEBUG;
        if (getChildCount() == 0) {
            return HORIZONTAL;
        }
        ensureOrientationHelper();
        OrientationHelper orientationHelper = this.mPrimaryOrientation;
        View findFirstVisibleItemClosestToStart = findFirstVisibleItemClosestToStart(!this.mSmoothScrollbarEnabled ? true : DEBUG, true);
        if (!this.mSmoothScrollbarEnabled) {
            z = true;
        }
        return ScrollbarHelper.computeScrollRange(state, orientationHelper, findFirstVisibleItemClosestToStart, findFirstVisibleItemClosestToEnd(z, true), this, this.mSmoothScrollbarEnabled);
    }

    public int computeVerticalScrollRange(State state) {
        return computeScrollRange(state);
    }

    private void measureChildWithDecorationsAndMargin(View child, LayoutParams lp) {
        if (lp.mFullSpan) {
            if (this.mOrientation == VERTICAL) {
                measureChildWithDecorationsAndMargin(child, this.mFullSizeSpec, getSpecForDimension(lp.height, this.mHeightSpec));
            } else {
                measureChildWithDecorationsAndMargin(child, getSpecForDimension(lp.width, this.mWidthSpec), this.mFullSizeSpec);
            }
        } else if (this.mOrientation == VERTICAL) {
            measureChildWithDecorationsAndMargin(child, this.mWidthSpec, getSpecForDimension(lp.height, this.mHeightSpec));
        } else {
            measureChildWithDecorationsAndMargin(child, getSpecForDimension(lp.width, this.mWidthSpec), this.mHeightSpec);
        }
    }

    private int getSpecForDimension(int dim, int defaultSpec) {
        return dim < 0 ? defaultSpec : MeasureSpec.makeMeasureSpec(dim, 1073741824);
    }

    private void measureChildWithDecorationsAndMargin(View child, int widthSpec, int heightSpec) {
        calculateItemDecorationsForChild(child, this.mTmpRect);
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        child.measure(updateSpecWithExtra(widthSpec, lp.leftMargin + this.mTmpRect.left, lp.rightMargin + this.mTmpRect.right), updateSpecWithExtra(heightSpec, lp.topMargin + this.mTmpRect.top, lp.bottomMargin + this.mTmpRect.bottom));
    }

    private int updateSpecWithExtra(int spec, int startInset, int endInset) {
        if (startInset == 0 && endInset == 0) {
            return spec;
        }
        int mode = MeasureSpec.getMode(spec);
        if (mode == INVALID_OFFSET || mode == 1073741824) {
            return MeasureSpec.makeMeasureSpec(Math.max(HORIZONTAL, (MeasureSpec.getSize(spec) - startInset) - endInset), mode);
        }
        return spec;
    }

    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof SavedState) {
            this.mPendingSavedState = (SavedState) state;
            requestLayout();
        }
    }

    public Parcelable onSaveInstanceState() {
        if (this.mPendingSavedState != null) {
            return new SavedState(this.mPendingSavedState);
        }
        Parcelable state = new SavedState();
        state.mReverseLayout = this.mReverseLayout;
        state.mAnchorLayoutFromEnd = this.mLastLayoutFromEnd;
        state.mLastLayoutRTL = this.mLastLayoutRTL;
        if (this.mLazySpanLookup == null || this.mLazySpanLookup.mData == null) {
            state.mSpanLookupSize = HORIZONTAL;
        } else {
            state.mSpanLookup = this.mLazySpanLookup.mData;
            state.mSpanLookupSize = state.mSpanLookup.length;
            state.mFullSpanItems = this.mLazySpanLookup.mFullSpanItems;
        }
        if (getChildCount() > 0) {
            ensureOrientationHelper();
            state.mAnchorPosition = this.mLastLayoutFromEnd ? getLastChildPosition() : getFirstChildPosition();
            state.mVisibleAnchorPosition = findFirstVisibleItemPositionInt();
            state.mSpanOffsetsSize = this.mSpanCount;
            state.mSpanOffsets = new int[this.mSpanCount];
            for (int i = HORIZONTAL; i < this.mSpanCount; i += VERTICAL) {
                int line;
                if (this.mLastLayoutFromEnd) {
                    line = this.mSpans[i].getEndLine(INVALID_OFFSET);
                    if (line != INVALID_OFFSET) {
                        line -= this.mPrimaryOrientation.getEndAfterPadding();
                    }
                } else {
                    line = this.mSpans[i].getStartLine(INVALID_OFFSET);
                    if (line != INVALID_OFFSET) {
                        line -= this.mPrimaryOrientation.getStartAfterPadding();
                    }
                }
                state.mSpanOffsets[i] = line;
            }
            return state;
        }
        state.mAnchorPosition = -1;
        state.mVisibleAnchorPosition = -1;
        state.mSpanOffsetsSize = HORIZONTAL;
        return state;
    }

    public void onInitializeAccessibilityNodeInfoForItem(Recycler recycler, State state, View host, AccessibilityNodeInfoCompat info) {
        android.view.ViewGroup.LayoutParams lp = host.getLayoutParams();
        if (lp instanceof LayoutParams) {
            LayoutParams sglp = (LayoutParams) lp;
            if (this.mOrientation == 0) {
                info.setCollectionItemInfo(CollectionItemInfoCompat.obtain(sglp.getSpanIndex(), sglp.mFullSpan ? this.mSpanCount : VERTICAL, -1, -1, sglp.mFullSpan, DEBUG));
                return;
            } else {
                info.setCollectionItemInfo(CollectionItemInfoCompat.obtain(-1, -1, sglp.getSpanIndex(), sglp.mFullSpan ? this.mSpanCount : VERTICAL, sglp.mFullSpan, DEBUG));
                return;
            }
        }
        super.onInitializeAccessibilityNodeInfoForItem(host, info);
    }

    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        if (getChildCount() > 0) {
            AccessibilityRecordCompat record = AccessibilityEventCompat.asRecord(event);
            View start = findFirstVisibleItemClosestToStart(DEBUG, true);
            View end = findFirstVisibleItemClosestToEnd(DEBUG, true);
            if (start != null && end != null) {
                int startPos = getPosition(start);
                int endPos = getPosition(end);
                if (startPos < endPos) {
                    record.setFromIndex(startPos);
                    record.setToIndex(endPos);
                    return;
                }
                record.setFromIndex(endPos);
                record.setToIndex(startPos);
            }
        }
    }

    int findFirstVisibleItemPositionInt() {
        View first = this.mShouldReverseLayout ? findFirstVisibleItemClosestToEnd(true, true) : findFirstVisibleItemClosestToStart(true, true);
        return first == null ? -1 : getPosition(first);
    }

    public int getRowCountForAccessibility(Recycler recycler, State state) {
        if (this.mOrientation == 0) {
            return this.mSpanCount;
        }
        return super.getRowCountForAccessibility(recycler, state);
    }

    public int getColumnCountForAccessibility(Recycler recycler, State state) {
        if (this.mOrientation == VERTICAL) {
            return this.mSpanCount;
        }
        return super.getColumnCountForAccessibility(recycler, state);
    }

    View findFirstVisibleItemClosestToStart(boolean fullyVisible, boolean acceptPartiallyVisible) {
        ensureOrientationHelper();
        int boundsStart = this.mPrimaryOrientation.getStartAfterPadding();
        int boundsEnd = this.mPrimaryOrientation.getEndAfterPadding();
        int limit = getChildCount();
        View partiallyVisible = null;
        for (int i = HORIZONTAL; i < limit; i += VERTICAL) {
            View child = getChildAt(i);
            int childStart = this.mPrimaryOrientation.getDecoratedStart(child);
            if (this.mPrimaryOrientation.getDecoratedEnd(child) > boundsStart && childStart < boundsEnd) {
                if (childStart >= boundsStart || !fullyVisible) {
                    return child;
                }
                if (acceptPartiallyVisible && partiallyVisible == null) {
                    partiallyVisible = child;
                }
            }
        }
        return partiallyVisible;
    }

    View findFirstVisibleItemClosestToEnd(boolean fullyVisible, boolean acceptPartiallyVisible) {
        ensureOrientationHelper();
        int boundsStart = this.mPrimaryOrientation.getStartAfterPadding();
        int boundsEnd = this.mPrimaryOrientation.getEndAfterPadding();
        View partiallyVisible = null;
        for (int i = getChildCount() - 1; i >= 0; i--) {
            View child = getChildAt(i);
            int childStart = this.mPrimaryOrientation.getDecoratedStart(child);
            int childEnd = this.mPrimaryOrientation.getDecoratedEnd(child);
            if (childEnd > boundsStart && childStart < boundsEnd) {
                if (childEnd <= boundsEnd || !fullyVisible) {
                    return child;
                }
                if (acceptPartiallyVisible && partiallyVisible == null) {
                    partiallyVisible = child;
                }
            }
        }
        return partiallyVisible;
    }

    private void fixEndGap(Recycler recycler, State state, boolean canOffsetChildren) {
        int gap = this.mPrimaryOrientation.getEndAfterPadding() - getMaxEnd(this.mPrimaryOrientation.getEndAfterPadding());
        if (gap > 0) {
            gap -= -scrollBy(-gap, recycler, state);
            if (canOffsetChildren && gap > 0) {
                this.mPrimaryOrientation.offsetChildren(gap);
            }
        }
    }

    private void fixStartGap(Recycler recycler, State state, boolean canOffsetChildren) {
        int gap = getMinStart(this.mPrimaryOrientation.getStartAfterPadding()) - this.mPrimaryOrientation.getStartAfterPadding();
        if (gap > 0) {
            gap -= scrollBy(gap, recycler, state);
            if (canOffsetChildren && gap > 0) {
                this.mPrimaryOrientation.offsetChildren(-gap);
            }
        }
    }

    private void updateLayoutState(int anchorPosition, State state) {
        boolean z = DEBUG;
        this.mLayoutState.mAvailable = HORIZONTAL;
        this.mLayoutState.mCurrentPosition = anchorPosition;
        int startExtra = HORIZONTAL;
        int endExtra = HORIZONTAL;
        if (isSmoothScrolling()) {
            int targetPos = state.getTargetScrollPosition();
            if (targetPos != -1) {
                boolean z2 = this.mShouldReverseLayout;
                if (targetPos < anchorPosition) {
                    z = true;
                }
                if (z2 == z) {
                    endExtra = this.mPrimaryOrientation.getTotalSpace();
                } else {
                    startExtra = this.mPrimaryOrientation.getTotalSpace();
                }
            }
        }
        if (getClipToPadding()) {
            this.mLayoutState.mStartLine = this.mPrimaryOrientation.getStartAfterPadding() - startExtra;
            this.mLayoutState.mEndLine = this.mPrimaryOrientation.getEndAfterPadding() + endExtra;
            return;
        }
        this.mLayoutState.mEndLine = this.mPrimaryOrientation.getEnd() + endExtra;
        this.mLayoutState.mStartLine = -startExtra;
    }

    private void setLayoutStateDirection(int direction) {
        int i = VERTICAL;
        this.mLayoutState.mLayoutDirection = direction;
        LayoutState layoutState = this.mLayoutState;
        if (this.mShouldReverseLayout != (direction == -1 ? VERTICAL : DEBUG)) {
            i = -1;
        }
        layoutState.mItemDirection = i;
    }

    public void offsetChildrenHorizontal(int dx) {
        super.offsetChildrenHorizontal(dx);
        for (int i = HORIZONTAL; i < this.mSpanCount; i += VERTICAL) {
            this.mSpans[i].onOffset(dx);
        }
    }

    public void offsetChildrenVertical(int dy) {
        super.offsetChildrenVertical(dy);
        for (int i = HORIZONTAL; i < this.mSpanCount; i += VERTICAL) {
            this.mSpans[i].onOffset(dy);
        }
    }

    public void onItemsRemoved(RecyclerView recyclerView, int positionStart, int itemCount) {
        handleUpdate(positionStart, itemCount, GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS);
    }

    public void onItemsAdded(RecyclerView recyclerView, int positionStart, int itemCount) {
        handleUpdate(positionStart, itemCount, VERTICAL);
    }

    public void onItemsChanged(RecyclerView recyclerView) {
        this.mLazySpanLookup.clear();
        requestLayout();
    }

    public void onItemsMoved(RecyclerView recyclerView, int from, int to, int itemCount) {
        handleUpdate(from, to, 8);
    }

    public void onItemsUpdated(RecyclerView recyclerView, int positionStart, int itemCount, Object payload) {
        handleUpdate(positionStart, itemCount, 4);
    }

    private void handleUpdate(int positionStart, int itemCountOrToPosition, int cmd) {
        int affectedRangeStart;
        int affectedRangeEnd;
        int minPosition = this.mShouldReverseLayout ? getLastChildPosition() : getFirstChildPosition();
        if (cmd != 8) {
            affectedRangeStart = positionStart;
            affectedRangeEnd = positionStart + itemCountOrToPosition;
        } else if (positionStart < itemCountOrToPosition) {
            affectedRangeEnd = itemCountOrToPosition + VERTICAL;
            affectedRangeStart = positionStart;
        } else {
            affectedRangeEnd = positionStart + VERTICAL;
            affectedRangeStart = itemCountOrToPosition;
        }
        this.mLazySpanLookup.invalidateAfter(affectedRangeStart);
        switch (cmd) {
            case VERTICAL /*1*/:
                this.mLazySpanLookup.offsetForAddition(positionStart, itemCountOrToPosition);
                break;
            case GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS /*2*/:
                this.mLazySpanLookup.offsetForRemoval(positionStart, itemCountOrToPosition);
                break;
            case ItemTouchHelper.RIGHT /*8*/:
                this.mLazySpanLookup.offsetForRemoval(positionStart, VERTICAL);
                this.mLazySpanLookup.offsetForAddition(itemCountOrToPosition, VERTICAL);
                break;
        }
        if (affectedRangeEnd > minPosition) {
            if (affectedRangeStart <= (this.mShouldReverseLayout ? getFirstChildPosition() : getLastChildPosition())) {
                requestLayout();
            }
        }
    }

    private int fill(Recycler recycler, LayoutState layoutState, State state) {
        int targetLine;
        int diff;
        this.mRemainingSpans.set(HORIZONTAL, this.mSpanCount, true);
        if (layoutState.mLayoutDirection == VERTICAL) {
            targetLine = layoutState.mEndLine + layoutState.mAvailable;
        } else {
            targetLine = layoutState.mStartLine - layoutState.mAvailable;
        }
        updateAllRemainingSpans(layoutState.mLayoutDirection, targetLine);
        int defaultNewViewLine = this.mShouldReverseLayout ? this.mPrimaryOrientation.getEndAfterPadding() : this.mPrimaryOrientation.getStartAfterPadding();
        boolean added = DEBUG;
        while (layoutState.hasMore(state) && !this.mRemainingSpans.isEmpty()) {
            Span currentSpan;
            int start;
            int end;
            FullSpanItem fullSpanItem;
            View view = layoutState.next(recycler);
            LayoutParams lp = (LayoutParams) view.getLayoutParams();
            int position = lp.getViewLayoutPosition();
            int spanIndex = this.mLazySpanLookup.getSpan(position);
            boolean assignSpan = spanIndex == -1 ? true : DEBUG;
            if (assignSpan) {
                currentSpan = lp.mFullSpan ? this.mSpans[HORIZONTAL] : getNextSpan(layoutState);
                this.mLazySpanLookup.setSpan(position, currentSpan);
            } else {
                currentSpan = this.mSpans[spanIndex];
            }
            lp.mSpan = currentSpan;
            if (layoutState.mLayoutDirection == VERTICAL) {
                addView(view);
            } else {
                addView(view, HORIZONTAL);
            }
            measureChildWithDecorationsAndMargin(view, lp);
            if (layoutState.mLayoutDirection == VERTICAL) {
                start = lp.mFullSpan ? getMaxEnd(defaultNewViewLine) : currentSpan.getEndLine(defaultNewViewLine);
                end = start + this.mPrimaryOrientation.getDecoratedMeasurement(view);
                if (assignSpan && lp.mFullSpan) {
                    fullSpanItem = createFullSpanItemFromEnd(start);
                    fullSpanItem.mGapDir = -1;
                    fullSpanItem.mPosition = position;
                    this.mLazySpanLookup.addFullSpanItem(fullSpanItem);
                }
            } else {
                end = lp.mFullSpan ? getMinStart(defaultNewViewLine) : currentSpan.getStartLine(defaultNewViewLine);
                start = end - this.mPrimaryOrientation.getDecoratedMeasurement(view);
                if (assignSpan && lp.mFullSpan) {
                    fullSpanItem = createFullSpanItemFromStart(end);
                    fullSpanItem.mGapDir = VERTICAL;
                    fullSpanItem.mPosition = position;
                    this.mLazySpanLookup.addFullSpanItem(fullSpanItem);
                }
            }
            if (lp.mFullSpan && layoutState.mItemDirection == -1) {
                if (assignSpan) {
                    this.mLaidOutInvalidFullSpan = true;
                } else {
                    boolean hasInvalidGap = layoutState.mLayoutDirection == VERTICAL ? !areAllEndsEqual() ? true : DEBUG : !areAllStartsEqual() ? true : DEBUG;
                    if (hasInvalidGap) {
                        fullSpanItem = this.mLazySpanLookup.getFullSpanItem(position);
                        if (fullSpanItem != null) {
                            fullSpanItem.mHasUnwantedGapAfter = true;
                        }
                        this.mLaidOutInvalidFullSpan = true;
                    }
                }
            }
            attachViewToSpans(view, lp, layoutState);
            int otherStart = lp.mFullSpan ? this.mSecondaryOrientation.getStartAfterPadding() : (currentSpan.mIndex * this.mSizePerSpan) + this.mSecondaryOrientation.getStartAfterPadding();
            int otherEnd = otherStart + this.mSecondaryOrientation.getDecoratedMeasurement(view);
            if (this.mOrientation == VERTICAL) {
                layoutDecoratedWithMargins(view, otherStart, start, otherEnd, end);
            } else {
                layoutDecoratedWithMargins(view, start, otherStart, end, otherEnd);
            }
            if (lp.mFullSpan) {
                updateAllRemainingSpans(this.mLayoutState.mLayoutDirection, targetLine);
            } else {
                updateRemainingSpans(currentSpan, this.mLayoutState.mLayoutDirection, targetLine);
            }
            recycle(recycler, this.mLayoutState);
            added = true;
        }
        if (!added) {
            recycle(recycler, this.mLayoutState);
        }
        if (this.mLayoutState.mLayoutDirection == -1) {
            diff = this.mPrimaryOrientation.getStartAfterPadding() - getMinStart(this.mPrimaryOrientation.getStartAfterPadding());
        } else {
            diff = getMaxEnd(this.mPrimaryOrientation.getEndAfterPadding()) - this.mPrimaryOrientation.getEndAfterPadding();
        }
        return diff > 0 ? Math.min(layoutState.mAvailable, diff) : HORIZONTAL;
    }

    private FullSpanItem createFullSpanItemFromEnd(int newItemTop) {
        FullSpanItem fsi = new FullSpanItem();
        fsi.mGapPerSpan = new int[this.mSpanCount];
        for (int i = HORIZONTAL; i < this.mSpanCount; i += VERTICAL) {
            fsi.mGapPerSpan[i] = newItemTop - this.mSpans[i].getEndLine(newItemTop);
        }
        return fsi;
    }

    private FullSpanItem createFullSpanItemFromStart(int newItemBottom) {
        FullSpanItem fsi = new FullSpanItem();
        fsi.mGapPerSpan = new int[this.mSpanCount];
        for (int i = HORIZONTAL; i < this.mSpanCount; i += VERTICAL) {
            fsi.mGapPerSpan[i] = this.mSpans[i].getStartLine(newItemBottom) - newItemBottom;
        }
        return fsi;
    }

    private void attachViewToSpans(View view, LayoutParams lp, LayoutState layoutState) {
        if (layoutState.mLayoutDirection == VERTICAL) {
            if (lp.mFullSpan) {
                appendViewToAllSpans(view);
            } else {
                lp.mSpan.appendToSpan(view);
            }
        } else if (lp.mFullSpan) {
            prependViewToAllSpans(view);
        } else {
            lp.mSpan.prependToSpan(view);
        }
    }

    private void recycle(Recycler recycler, LayoutState layoutState) {
        if (layoutState.mAvailable == 0) {
            if (layoutState.mLayoutDirection == -1) {
                recycleFromEnd(recycler, layoutState.mEndLine);
            } else {
                recycleFromStart(recycler, layoutState.mStartLine);
            }
        } else if (layoutState.mLayoutDirection == -1) {
            scrolled = layoutState.mStartLine - getMaxStart(layoutState.mStartLine);
            if (scrolled < 0) {
                line = layoutState.mEndLine;
            } else {
                line = layoutState.mEndLine - Math.min(scrolled, layoutState.mAvailable);
            }
            recycleFromEnd(recycler, line);
        } else {
            scrolled = getMinEnd(layoutState.mEndLine) - layoutState.mEndLine;
            if (scrolled < 0) {
                line = layoutState.mStartLine;
            } else {
                line = layoutState.mStartLine + Math.min(scrolled, layoutState.mAvailable);
            }
            recycleFromStart(recycler, line);
        }
    }

    private void appendViewToAllSpans(View view) {
        for (int i = this.mSpanCount - 1; i >= 0; i--) {
            this.mSpans[i].appendToSpan(view);
        }
    }

    private void prependViewToAllSpans(View view) {
        for (int i = this.mSpanCount - 1; i >= 0; i--) {
            this.mSpans[i].prependToSpan(view);
        }
    }

    private void layoutDecoratedWithMargins(View child, int left, int top, int right, int bottom) {
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        layoutDecorated(child, left + lp.leftMargin, top + lp.topMargin, right - lp.rightMargin, bottom - lp.bottomMargin);
    }

    private void updateAllRemainingSpans(int layoutDir, int targetLine) {
        for (int i = HORIZONTAL; i < this.mSpanCount; i += VERTICAL) {
            if (!this.mSpans[i].mViews.isEmpty()) {
                updateRemainingSpans(this.mSpans[i], layoutDir, targetLine);
            }
        }
    }

    private void updateRemainingSpans(Span span, int layoutDir, int targetLine) {
        int deletedSize = span.getDeletedSize();
        if (layoutDir == -1) {
            if (span.getStartLine() + deletedSize <= targetLine) {
                this.mRemainingSpans.set(span.mIndex, DEBUG);
            }
        } else if (span.getEndLine() - deletedSize >= targetLine) {
            this.mRemainingSpans.set(span.mIndex, DEBUG);
        }
    }

    private int getMaxStart(int def) {
        int maxStart = this.mSpans[HORIZONTAL].getStartLine(def);
        for (int i = VERTICAL; i < this.mSpanCount; i += VERTICAL) {
            int spanStart = this.mSpans[i].getStartLine(def);
            if (spanStart > maxStart) {
                maxStart = spanStart;
            }
        }
        return maxStart;
    }

    private int getMinStart(int def) {
        int minStart = this.mSpans[HORIZONTAL].getStartLine(def);
        for (int i = VERTICAL; i < this.mSpanCount; i += VERTICAL) {
            int spanStart = this.mSpans[i].getStartLine(def);
            if (spanStart < minStart) {
                minStart = spanStart;
            }
        }
        return minStart;
    }

    boolean areAllEndsEqual() {
        int end = this.mSpans[HORIZONTAL].getEndLine(INVALID_OFFSET);
        for (int i = VERTICAL; i < this.mSpanCount; i += VERTICAL) {
            if (this.mSpans[i].getEndLine(INVALID_OFFSET) != end) {
                return DEBUG;
            }
        }
        return true;
    }

    boolean areAllStartsEqual() {
        int start = this.mSpans[HORIZONTAL].getStartLine(INVALID_OFFSET);
        for (int i = VERTICAL; i < this.mSpanCount; i += VERTICAL) {
            if (this.mSpans[i].getStartLine(INVALID_OFFSET) != start) {
                return DEBUG;
            }
        }
        return true;
    }

    private int getMaxEnd(int def) {
        int maxEnd = this.mSpans[HORIZONTAL].getEndLine(def);
        for (int i = VERTICAL; i < this.mSpanCount; i += VERTICAL) {
            int spanEnd = this.mSpans[i].getEndLine(def);
            if (spanEnd > maxEnd) {
                maxEnd = spanEnd;
            }
        }
        return maxEnd;
    }

    private int getMinEnd(int def) {
        int minEnd = this.mSpans[HORIZONTAL].getEndLine(def);
        for (int i = VERTICAL; i < this.mSpanCount; i += VERTICAL) {
            int spanEnd = this.mSpans[i].getEndLine(def);
            if (spanEnd < minEnd) {
                minEnd = spanEnd;
            }
        }
        return minEnd;
    }

    private void recycleFromStart(Recycler recycler, int line) {
        while (getChildCount() > 0) {
            View child = getChildAt(HORIZONTAL);
            if (this.mPrimaryOrientation.getDecoratedEnd(child) <= line) {
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (lp.mFullSpan) {
                    int j = HORIZONTAL;
                    while (j < this.mSpanCount) {
                        if (this.mSpans[j].mViews.size() != VERTICAL) {
                            j += VERTICAL;
                        } else {
                            return;
                        }
                    }
                    for (j = HORIZONTAL; j < this.mSpanCount; j += VERTICAL) {
                        this.mSpans[j].popStart();
                    }
                } else if (lp.mSpan.mViews.size() != VERTICAL) {
                    lp.mSpan.popStart();
                } else {
                    return;
                }
                removeAndRecycleView(child, recycler);
            } else {
                return;
            }
        }
    }

    private void recycleFromEnd(Recycler recycler, int line) {
        int i = getChildCount() - 1;
        while (i >= 0) {
            View child = getChildAt(i);
            if (this.mPrimaryOrientation.getDecoratedStart(child) >= line) {
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (lp.mFullSpan) {
                    int j = HORIZONTAL;
                    while (j < this.mSpanCount) {
                        if (this.mSpans[j].mViews.size() != VERTICAL) {
                            j += VERTICAL;
                        } else {
                            return;
                        }
                    }
                    for (j = HORIZONTAL; j < this.mSpanCount; j += VERTICAL) {
                        this.mSpans[j].popEnd();
                    }
                } else if (lp.mSpan.mViews.size() != VERTICAL) {
                    lp.mSpan.popEnd();
                } else {
                    return;
                }
                removeAndRecycleView(child, recycler);
                i--;
            } else {
                return;
            }
        }
    }

    private boolean preferLastSpan(int layoutDir) {
        if (this.mOrientation == 0) {
            boolean z;
            if (layoutDir == -1) {
                z = true;
            } else {
                z = HORIZONTAL;
            }
            if (z != this.mShouldReverseLayout) {
                return true;
            }
            return DEBUG;
        }
        if (((layoutDir == -1 ? true : HORIZONTAL) == this.mShouldReverseLayout ? true : HORIZONTAL) != isLayoutRTL()) {
            return DEBUG;
        }
        return true;
    }

    private Span getNextSpan(LayoutState layoutState) {
        int startIndex;
        int endIndex;
        int diff;
        if (preferLastSpan(layoutState.mLayoutDirection)) {
            startIndex = this.mSpanCount - 1;
            endIndex = -1;
            diff = -1;
        } else {
            startIndex = HORIZONTAL;
            endIndex = this.mSpanCount;
            diff = VERTICAL;
        }
        int defaultLine;
        int i;
        Span other;
        int otherLine;
        if (layoutState.mLayoutDirection == VERTICAL) {
            Span min = null;
            int minLine = ActivityChooserViewAdapter.MAX_ACTIVITY_COUNT_UNLIMITED;
            defaultLine = this.mPrimaryOrientation.getStartAfterPadding();
            for (i = startIndex; i != endIndex; i += diff) {
                other = this.mSpans[i];
                otherLine = other.getEndLine(defaultLine);
                if (otherLine < minLine) {
                    min = other;
                    minLine = otherLine;
                }
            }
            return min;
        }
        Span max = null;
        int maxLine = INVALID_OFFSET;
        defaultLine = this.mPrimaryOrientation.getEndAfterPadding();
        for (i = startIndex; i != endIndex; i += diff) {
            other = this.mSpans[i];
            otherLine = other.getStartLine(defaultLine);
            if (otherLine > maxLine) {
                max = other;
                maxLine = otherLine;
            }
        }
        return max;
    }

    public boolean canScrollVertically() {
        return this.mOrientation == VERTICAL ? true : DEBUG;
    }

    public boolean canScrollHorizontally() {
        return this.mOrientation == 0 ? true : DEBUG;
    }

    public int scrollHorizontallyBy(int dx, Recycler recycler, State state) {
        return scrollBy(dx, recycler, state);
    }

    public int scrollVerticallyBy(int dy, Recycler recycler, State state) {
        return scrollBy(dy, recycler, state);
    }

    private int calculateScrollDirectionForPosition(int position) {
        int i = -1;
        if (getChildCount() != 0) {
            if ((position < getFirstChildPosition() ? VERTICAL : DEBUG) == this.mShouldReverseLayout) {
                i = VERTICAL;
            }
            return i;
        } else if (this.mShouldReverseLayout) {
            return VERTICAL;
        } else {
            return -1;
        }
    }

    public void smoothScrollToPosition(RecyclerView recyclerView, State state, int position) {
        LinearSmoothScroller scroller = new C03242(recyclerView.getContext());
        scroller.setTargetPosition(position);
        startSmoothScroll(scroller);
    }

    public void scrollToPosition(int position) {
        if (!(this.mPendingSavedState == null || this.mPendingSavedState.mAnchorPosition == position)) {
            this.mPendingSavedState.invalidateAnchorPositionInfo();
        }
        this.mPendingScrollPosition = position;
        this.mPendingScrollPositionOffset = INVALID_OFFSET;
        requestLayout();
    }

    public void scrollToPositionWithOffset(int position, int offset) {
        if (this.mPendingSavedState != null) {
            this.mPendingSavedState.invalidateAnchorPositionInfo();
        }
        this.mPendingScrollPosition = position;
        this.mPendingScrollPositionOffset = offset;
        requestLayout();
    }

    int scrollBy(int dt, Recycler recycler, State state) {
        int layoutDir;
        int referenceChildPosition;
        int totalScroll;
        ensureOrientationHelper();
        if (dt > 0) {
            layoutDir = VERTICAL;
            referenceChildPosition = getLastChildPosition();
        } else {
            layoutDir = -1;
            referenceChildPosition = getFirstChildPosition();
        }
        updateLayoutState(referenceChildPosition, state);
        setLayoutStateDirection(layoutDir);
        this.mLayoutState.mCurrentPosition = this.mLayoutState.mItemDirection + referenceChildPosition;
        int absDt = Math.abs(dt);
        this.mLayoutState.mAvailable = absDt;
        int consumed = fill(recycler, this.mLayoutState, state);
        if (absDt < consumed) {
            totalScroll = dt;
        } else if (dt < 0) {
            totalScroll = -consumed;
        } else {
            totalScroll = consumed;
        }
        this.mPrimaryOrientation.offsetChildren(-totalScroll);
        this.mLastLayoutFromEnd = this.mShouldReverseLayout;
        return totalScroll;
    }

    private int getLastChildPosition() {
        int childCount = getChildCount();
        return childCount == 0 ? HORIZONTAL : getPosition(getChildAt(childCount - 1));
    }

    private int getFirstChildPosition() {
        if (getChildCount() == 0) {
            return HORIZONTAL;
        }
        return getPosition(getChildAt(HORIZONTAL));
    }

    private int findFirstReferenceChildPosition(int itemCount) {
        int limit = getChildCount();
        for (int i = HORIZONTAL; i < limit; i += VERTICAL) {
            int position = getPosition(getChildAt(i));
            if (position >= 0 && position < itemCount) {
                return position;
            }
        }
        return HORIZONTAL;
    }

    private int findLastReferenceChildPosition(int itemCount) {
        for (int i = getChildCount() - 1; i >= 0; i--) {
            int position = getPosition(getChildAt(i));
            if (position >= 0 && position < itemCount) {
                return position;
            }
        }
        return HORIZONTAL;
    }

    public android.support.v7.widget.RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(-2, -2);
    }

    public android.support.v7.widget.RecyclerView.LayoutParams generateLayoutParams(Context c, AttributeSet attrs) {
        return new LayoutParams(c, attrs);
    }

    public android.support.v7.widget.RecyclerView.LayoutParams generateLayoutParams(android.view.ViewGroup.LayoutParams lp) {
        if (lp instanceof MarginLayoutParams) {
            return new LayoutParams((MarginLayoutParams) lp);
        }
        return new LayoutParams(lp);
    }

    public boolean checkLayoutParams(android.support.v7.widget.RecyclerView.LayoutParams lp) {
        return lp instanceof LayoutParams;
    }

    public int getOrientation() {
        return this.mOrientation;
    }
}
