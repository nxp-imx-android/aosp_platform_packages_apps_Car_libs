/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.car.ui.recyclerview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ItemAnimator;
import androidx.recyclerview.widget.RecyclerView.LayoutManager;
import androidx.recyclerview.widget.RecyclerView.OnFlingListener;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import com.android.car.ui.plugin.oemapis.recyclerview.AdapterOEMV1;
import com.android.car.ui.plugin.oemapis.recyclerview.LayoutStyleOEMV1;
import com.android.car.ui.plugin.oemapis.recyclerview.OnScrollListenerOEMV1;
import com.android.car.ui.plugin.oemapis.recyclerview.RecyclerViewOEMV1;

import java.util.ArrayList;
import java.util.List;

/**
 * AdapterV1 class for making oem implementation available for UI
 *
 * For CarUi internal usage only.
 */
public final class RecyclerViewAdapterV1 extends FrameLayout
            implements CarUiRecyclerView, OnScrollListenerOEMV1 {

    @Nullable
    private RecyclerViewOEMV1 mOEMRecyclerView;
    @Nullable
    private AdapterOEMV1 mOEMAdapter;

    @NonNull
    private final List<OnScrollListener> mScrollListeners = new ArrayList<>();
    @Nullable
    private ProxyRecyclerView mRecyclerView;
    @Nullable
    private CarUiLayoutStyle mLayoutStyle;

    public RecyclerViewAdapterV1(@NonNull Context context) {
        this(context, null);
    }

    public RecyclerViewAdapterV1(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RecyclerViewAdapterV1(@NonNull Context context, @Nullable AttributeSet attrs,
            int defStyle) {
        super(context, attrs, defStyle, 0);
    }

    /**
     * Called to pass the oem recyclerview implementation.
     * @param oemRecyclerView
     */
    public void setRecyclerViewOEMV1(@NonNull RecyclerViewOEMV1 oemRecyclerView) {
        mOEMRecyclerView = oemRecyclerView;

        // Adding this parent so androidx PreferenceFragmentCompat doesn't add the ProxyRecyclerView
        // to the view hierarchy
        ViewGroup parent = new FrameLayout(getContext());
        mRecyclerView = new ProxyRecyclerView(getContext(), this);
        parent.addView(mRecyclerView);

        mOEMRecyclerView.addOnScrollListener(this);

        ViewGroup.LayoutParams params = new MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        addView(mOEMRecyclerView.getView(), params);
    }

    @Override
    public void setLayoutManager(@Nullable LayoutManager layoutManager) {
        if (layoutManager instanceof GridLayoutManager) {
            setLayoutStyle(CarUiGridLayoutStyle.from(layoutManager));
        } else {
            setLayoutStyle(CarUiLinearLayoutStyle.from(layoutManager));
        }
    }

    @Nullable
    @Override
    public RecyclerView getRecyclerView() {
        return mRecyclerView;
    }

    private static int toInternalScrollState(int state) {
        /* default to RecyclerViewOEMV1.SCROLL_STATE_IDLE */
        int internalState = SCROLL_STATE_IDLE;
        switch (state) {
            case RecyclerViewOEMV1.SCROLL_STATE_DRAGGING:
                internalState = SCROLL_STATE_DRAGGING;
                break;
            case RecyclerViewOEMV1.SCROLL_STATE_SETTLING:
                internalState = SCROLL_STATE_SETTLING;
                break;
        }
        return internalState;
    }

    @Override
    public int getScrollState() {
        return toInternalScrollState(mOEMRecyclerView.getScrollState());
    }

    @NonNull
    @Override
    public View getView() {
        return this;
    }

    @Override
    public void invalidateItemDecorations() {}

    @Override
    public void removeItemDecoration(@NonNull RecyclerView.ItemDecoration decor) {}

    @Override
    public void removeItemDecorationAt(int index) {}

    @Nullable
    @Override
    public RecyclerView.ItemDecoration getItemDecorationAt(int index) {
        return null;
    }

    @Override
    public int getItemDecorationCount() {
        return 0;
    }

    @Override
    public void setContentDescription(CharSequence contentDescription) {
        super.setContentDescription(contentDescription);
        mOEMRecyclerView.setContentDescription(contentDescription);
    }

    @Override
    public void setAdapter(RecyclerView.Adapter<?> adapter) {
        if (adapter == null) {
            mOEMRecyclerView.setAdapter(null);
        } else {
            mOEMAdapter = new RecyclerViewAdapterAdapterV1(getContext(), adapter);
            mOEMRecyclerView.setAdapter(mOEMAdapter);
        }
    }

    @Override
    public void addItemDecoration(@NonNull RecyclerView.ItemDecoration decor) {}

    @Override
    public void addItemDecoration(@NonNull RecyclerView.ItemDecoration decor, int index) {}

    @Override
    public void addOnScrollListener(@NonNull OnScrollListener listener) {
        mScrollListeners.add(listener);
    }

    @Override
    public void removeOnScrollListener(@NonNull OnScrollListener listener) {
        mScrollListeners.remove(listener);
    }

    @Override
    public void clearOnScrollListeners() {
        mScrollListeners.clear();
        mOEMRecyclerView.clearOnScrollListeners();
    }

    @Override
    public void onScrollStateChanged(@NonNull RecyclerViewOEMV1 recyclerView, int newState) {
        for (OnScrollListener listener: mScrollListeners) {
            listener.onScrollStateChanged(this, toInternalScrollState(newState));
        }
    }

    @Override
    public void onScrolled(@NonNull RecyclerViewOEMV1 recyclerView, int dx, int dy) {
        for (OnScrollListener listener: mScrollListeners) {
            listener.onScrolled(this, dx, dy);
        }
    }

    @Override
    public void scrollToPosition(int position) {
        mOEMRecyclerView.scrollToPosition(position);
    }

    @Override
    public void smoothScrollBy(int dx, int dy) {
        mOEMRecyclerView.smoothScrollBy(dx, dy);
    }

    @Override
    public void smoothScrollToPosition(int position) {
        mOEMRecyclerView.smoothScrollToPosition(position);
    }

    @Override
    public ViewHolder findViewHolderForAdapterPosition(int position) {
        // TODO
        return null;
    }

    @Override
    public ViewHolder findViewHolderForLayoutPosition(int position) {
        // TODO
        return null;
    }

    @Override
    public Adapter<?> getAdapter() {
        // TODO
        return null;
    }

    @Override
    public void setHasFixedSize(boolean hasFixedSize) {
        mOEMRecyclerView.setHasFixedSize(hasFixedSize);
    }

    @Override
    public void setOnFlingListener(OnFlingListener listener) {
        // TODO
    }

    @Override
    public boolean hasFixedSize() {
        return mOEMRecyclerView.hasFixedSize();
    }

    /**
     * @deprecated LayoutManager will be implemented by OEMs,
     * use other available APIs to get the required data
     * @return null
     */
    @Nullable
    @Override
    @Deprecated
    public LayoutManager getLayoutManager() {
        return null;
    }

    @Override
    public CarUiLayoutStyle getLayoutStyle() {
        return mLayoutStyle;
    }

    @Override
    public void setLayoutStyle(CarUiLayoutStyle layoutStyle) {
        mLayoutStyle = layoutStyle;
        if (layoutStyle == null) mOEMRecyclerView.setLayoutStyle(null);

        final LayoutStyleOEMV1 oemLayoutStyle = new LayoutStyleOEMV1() {
            @Override
            public int getSpanCount() {
                return layoutStyle.getSpanCount();
            }

            @Override
            public int getLayoutType() {
                return layoutStyle.getLayoutType();
            }

            @Override
            public int getOrientation() {
                return layoutStyle.getOrientation();
            }

            @Override
            public boolean getReverseLayout() {
                return layoutStyle.getReverseLayout();
            }
        };

        if (mOEMRecyclerView != null) {
            if (layoutStyle instanceof CarUiGridLayoutStyle) {
                mOEMRecyclerView.setSpanSizeLookup(position ->
                        ((CarUiGridLayoutStyle) layoutStyle).getSpanSizeLookup()
                                .getSpanSize(position));
            }

            mOEMRecyclerView.setLayoutStyle(oemLayoutStyle);
        }
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        if (mOEMRecyclerView != null) {
            mOEMRecyclerView.getView().setPadding(left, top, right, bottom);
        }
    }

    @Override
    public void setPaddingRelative(int start, int top, int end, int bottom) {
        if (mOEMRecyclerView != null) {
            mOEMRecyclerView.getView().setPaddingRelative(start, top, end, bottom);
        }
    }

    @Override
    public void setClipToPadding(boolean clipToPadding) {
        if (mOEMRecyclerView != null) {
            mOEMRecyclerView.setClipToPadding(clipToPadding);
        }
    }

    @Override
    public void setItemAnimator(ItemAnimator itemAnimator) {}

    @Override
    public int findFirstCompletelyVisibleItemPosition() {
        return mOEMRecyclerView != null ? mOEMRecyclerView
                .findFirstCompletelyVisibleItemPosition() : 0;
    }

    @Override
    public int findFirstVisibleItemPosition() {
        return mOEMRecyclerView != null ? mOEMRecyclerView
                .findFirstVisibleItemPosition() : 0;
    }

    @Override
    public int findLastCompletelyVisibleItemPosition() {
        return mOEMRecyclerView != null ? mOEMRecyclerView
                .findLastCompletelyVisibleItemPosition() : 0;
    }

    @Override
    public int findLastVisibleItemPosition() {
        return mOEMRecyclerView != null ? mOEMRecyclerView
                .findLastVisibleItemPosition() : 0;
    }

    @Override
    public void setSpanSizeLookup(@NonNull SpanSizeLookup spanSizeLookup) {
        if (mLayoutStyle instanceof CarUiGridLayoutStyle) {
            ((CarUiGridLayoutStyle) mLayoutStyle).setSpanSizeLookup(spanSizeLookup);
            setLayoutStyle(mLayoutStyle);
        }
    }
}
