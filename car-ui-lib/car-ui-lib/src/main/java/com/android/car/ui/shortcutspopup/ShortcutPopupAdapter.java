/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.car.ui.shortcutspopup;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleAdapter;

import androidx.annotation.NonNull;

import com.android.car.ui.R;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Used by {@link CarUiShortcutsPopup}
 * Determines if an Item has an Arrow and also decides the gravity of the attached arrow
 * based on {@link ShortcutPopupAdapter#mReverse}, {@link ShortcutPopupAdapter#mArrowGravityRight}
 * and position of the view in the {@link ShortcutPopupAdapter#mAdapterList}
 * @see ShortcutPopupAdapter#getView(int, View, ViewGroup)
 */
class ShortcutPopupAdapter extends SimpleAdapter {

    private boolean mReverse;
    private boolean mArrowGravityRight;
    @NonNull
    private final List<Map<String, ?>> mAdapterList;
    @NonNull
    private final ItemClickListener mItemClickListener;

    ShortcutPopupAdapter(@NonNull Context context,
            @NonNull ItemClickListener itemClickListener,
            @NonNull List<Map<String, ?>> data, int resource,
            @NonNull String[] from, @NonNull int[] to) {
        super(context, data, resource, from, to);
        mAdapterList = data;
        mItemClickListener = itemClickListener;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        CarUiArrowContainerView containerView = (CarUiArrowContainerView) super.getView(position,
                convertView, parent);
        boolean hasArrow =
                (position == 0 && !mReverse) || (position == mAdapterList.size() - 1 && mReverse);
        containerView.setHasArrow(hasArrow);
        containerView.setArrowGravityTop(!mReverse);
        containerView.setArrowGravityLeft(!mArrowGravityRight);

        //set the click listener to contentView rather than parent for rotary support
        View contentView = containerView.findViewById(R.id.car_ui_shortcuts_contentView);
        if (contentView != null) {
            contentView.setOnClickListener(v-> {
                mItemClickListener.onClick((int) getItemId(position));
            });
        }
        containerView.setEnabled(mItemClickListener.isEnabled((int) getItemId(position)));
        return containerView;
    }


    @Override
    public long getItemId(int position) {
        int actualPosition = position;
        if (mReverse) {
            actualPosition = mAdapterList.size() - 1 - actualPosition;
        }
        return super.getItemId(actualPosition);
    }

    @Override
    public int getItemViewType(int position) {
        return super.getItemViewType(position);
    }

    /**
     * Updates {@link ShortcutPopupAdapter#mReverse} field, and reverses the adapter list.
     *
     * @param isReverse - true: last item {@code mAdapter.size()-1} shows the arrow.
     *                  false: first item shows the arrow.
     */
    public void setReverse(boolean isReverse) {
        if (isReverse == mReverse) {
            return;
        }
        this.mReverse = isReverse;
        Collections.reverse(mAdapterList);
        notifyDataSetChanged();
    }

    public void setArrowGravityRight(boolean arrowGravityRight) {
        mArrowGravityRight = arrowGravityRight;
    }

    interface ItemClickListener {
        void onClick(int position);

        boolean isEnabled(int position);
    }
}
