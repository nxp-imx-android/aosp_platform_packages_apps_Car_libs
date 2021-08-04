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
package com.chassis.car.ui.plugin.recyclerview;

import static androidx.recyclerview.widget.RecyclerView.VERTICAL;

import static com.android.car.ui.plugin.oemapis.recyclerview.RecyclerViewAttributesOEMV1.SIZE_LARGE;
import static com.android.car.ui.plugin.oemapis.recyclerview.RecyclerViewAttributesOEMV1.SIZE_MEDIUM;
import static com.android.car.ui.plugin.oemapis.recyclerview.RecyclerViewAttributesOEMV1.SIZE_SMALL;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.car.ui.plugin.oemapis.recyclerview.AdapterOEMV1;
import com.android.car.ui.plugin.oemapis.recyclerview.LayoutStyleOEMV1;
import com.android.car.ui.plugin.oemapis.recyclerview.OnScrollListenerOEMV1;
import com.android.car.ui.plugin.oemapis.recyclerview.RecyclerViewAttributesOEMV1;
import com.android.car.ui.plugin.oemapis.recyclerview.RecyclerViewOEMV1;
import com.android.car.ui.plugin.oemapis.recyclerview.SpanSizeLookupOEMV1;
import com.android.car.ui.plugin.oemapis.recyclerview.ViewHolderOEMV1;

import com.chassis.car.ui.plugin.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Reference OEM implementation for RecyclerView
 */
public final class RecyclerViewImpl extends FrameLayout implements RecyclerViewOEMV1 {

    @NonNull
    private final RecyclerView mRecyclerView;

    @NonNull
    private final List<OnScrollListenerOEMV1> mScrollListeners = new ArrayList<>();

    @NonNull
    private final RecyclerView.OnScrollListener mOnScrollListener =
            new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            for (OnScrollListenerOEMV1 listener: mScrollListeners) {
                listener.onScrolled(RecyclerViewImpl.this, dx, dy);
            }
        }

        @Override
        public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
            for (OnScrollListenerOEMV1 listener: mScrollListeners) {
                listener.onScrollStateChanged(RecyclerViewImpl.this,
                        toInternalScrollState(newState));
            }
        }
    };

    @Nullable
    private LayoutStyleOEMV1 mLayoutStyle;

    public RecyclerViewImpl(@NonNull Context context) {
        this(context, null);
    }

    public RecyclerViewImpl(@NonNull Context context,
                            @Nullable RecyclerViewAttributesOEMV1 attrs) {
        super(context);

        boolean mScrollBarEnabled = context.getResources().getBoolean(R.bool.scrollbar_enable);
        @LayoutRes int layout = R.layout.recycler_view_no_scrollbar;
        if (mScrollBarEnabled && attrs != null) {
            switch (attrs.getSize()) {
                case SIZE_SMALL:
                    // Small layout is always rendered without scrollbar
                    mScrollBarEnabled = false;
                    layout = R.layout.recycler_view_no_scrollbar;
                    break;
                case SIZE_MEDIUM:
                    layout = R.layout.recycler_view_medium;
                    break;
                case SIZE_LARGE:
                    layout = R.layout.recycler_view;
            }
        }

        LayoutInflater factory = LayoutInflater.from(context);
        View rootView = factory.inflate(layout, this, true);
        mRecyclerView = rootView.requireViewById(R.id.recycler_view);

        // Set to false so the items below the toolbar are visible.
        mRecyclerView.setClipToPadding(false);

        setLayoutStyle(attrs == null ? null : attrs.getLayoutStyle());

        if (!mScrollBarEnabled) {
            return;
        }

        mRecyclerView.setVerticalScrollBarEnabled(false);
        mRecyclerView.setHorizontalScrollBarEnabled(false);

        DefaultScrollBar mScrollBar = new DefaultScrollBar();
        mScrollBar.initialize(context, mRecyclerView, rootView.requireViewById(R.id.scroll_bar));
    }

    @Override
    public <V extends ViewHolderOEMV1> void setAdapter(AdapterOEMV1<V> adapterV1) {
        if (adapterV1 == null) {
            mRecyclerView.setAdapter(null);
        } else {
            mRecyclerView.setAdapter(new AdapterWrapper(adapterV1));
        }
    }

    @Override
    public void addOnScrollListener(OnScrollListenerOEMV1 listener) {
        if (listener == null) {
            return;
        }
        if (mScrollListeners.isEmpty()) {
            mRecyclerView.addOnScrollListener(mOnScrollListener);
        }
        mScrollListeners.add(listener);
    }

    @Override
    public void removeOnScrollListener(OnScrollListenerOEMV1 listener) {
        if (listener == null) {
            return;
        }
        mScrollListeners.remove(listener);
        if (mScrollListeners.isEmpty()) {
            mRecyclerView.removeOnScrollListener(mOnScrollListener);
        }
    }

    @Override
    public void clearOnScrollListeners() {
        if (!mScrollListeners.isEmpty()) {
            mScrollListeners.clear();
            mRecyclerView.clearOnScrollListeners();
        }
    }

    @Override
    public void scrollToPosition(int position) {
        mRecyclerView.scrollToPosition(position);
    }

    @Override
    public void smoothScrollBy(int dx, int dy) {
        mRecyclerView.smoothScrollBy(dx, dy);
    }

    @Override
    public void smoothScrollToPosition(int position) {
        mRecyclerView.smoothScrollToPosition(position);
    }

    @Override
    public void setHasFixedSize(boolean hasFixedSize) {
        mRecyclerView.setHasFixedSize(hasFixedSize);
    }

    @Override
    public boolean hasFixedSize() {
        return mRecyclerView.hasFixedSize();
    }

    @Override
    public void setLayoutStyle(@Nullable LayoutStyleOEMV1 layoutStyle) {
        mLayoutStyle = layoutStyle;

        int orientation = layoutStyle == null ? VERTICAL : layoutStyle.getOrientation();
        boolean reverseLayout  = layoutStyle != null && layoutStyle.getReverseLayout();

        if (layoutStyle == null
                || layoutStyle.getLayoutType() == LayoutStyleOEMV1.LAYOUT_TYPE_LINEAR) {
            mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(),
                    orientation,
                    reverseLayout));
        } else {
            mRecyclerView.setLayoutManager(new GridLayoutManager(getContext(),
                    layoutStyle.getSpanCount(),
                    orientation,
                    reverseLayout));
        }
    }

    @Override
    public LayoutStyleOEMV1 getLayoutStyle() {
        return mLayoutStyle;
    }

    public View getView() {
        return this;
    }

    @Override
    public int findFirstCompletelyVisibleItemPosition() {
        RecyclerView.LayoutManager layoutManager = mRecyclerView.getLayoutManager();
        if (layoutManager instanceof LinearLayoutManager) {
            return ((LinearLayoutManager) layoutManager)
                    .findFirstCompletelyVisibleItemPosition();
        }
        return 0;
    }

    @Override
    public int findFirstVisibleItemPosition() {
        RecyclerView.LayoutManager layoutManager = mRecyclerView.getLayoutManager();
        if (layoutManager instanceof LinearLayoutManager) {
            return ((LinearLayoutManager) layoutManager)
                    .findFirstVisibleItemPosition();
        }
        return 0;
    }

    @Override
    public int findLastCompletelyVisibleItemPosition() {
        RecyclerView.LayoutManager layoutManager = mRecyclerView.getLayoutManager();
        if (layoutManager instanceof LinearLayoutManager) {
            return ((LinearLayoutManager) layoutManager)
                    .findLastCompletelyVisibleItemPosition();
        }
        return 0;
    }

    @Override
    public int findLastVisibleItemPosition() {
        RecyclerView.LayoutManager layoutManager = mRecyclerView.getLayoutManager();
        if (layoutManager instanceof LinearLayoutManager) {
            return ((LinearLayoutManager) layoutManager)
                    .findLastVisibleItemPosition();
        }
        return 0;
    }

    @Override
    public void setSpanSizeLookup(@NonNull SpanSizeLookupOEMV1 spanSizeLookup) {
        RecyclerView.LayoutManager layoutManager = mRecyclerView.getLayoutManager();
        if (layoutManager instanceof GridLayoutManager) {
            ((GridLayoutManager) layoutManager).setSpanSizeLookup(new SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    return spanSizeLookup.getSpanSize(position);
                }
            });
        }
    }

    private static int toInternalScrollState(int state) {
        /* default to RecyclerView.SCROLL_STATE_IDLE */
        int internalState = RecyclerViewOEMV1.SCROLL_STATE_IDLE;
        switch (state) {
            case RecyclerView.SCROLL_STATE_DRAGGING:
                internalState = RecyclerViewOEMV1.SCROLL_STATE_DRAGGING;
                break;
            case RecyclerView.SCROLL_STATE_SETTLING:
                internalState = RecyclerViewOEMV1.SCROLL_STATE_SETTLING;
                break;
        }
        return internalState;
    }

    @Override
    public int getScrollState() {
        return toInternalScrollState(mRecyclerView.getScrollState());
    }
}
