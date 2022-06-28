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
import android.graphics.Rect;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.ListPopupWindow;

import com.android.car.ui.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is a Helper class which uses {@link ListPopupWindow} to draw a Popup anchored at
 * {@link CarUiShortcutsPopup#mAnchorView}
 */
public class CarUiShortcutsPopup implements ShortcutPopupAdapter.ItemClickListener {

    private static final String TAG = CarUiShortcutsPopup.class.getSimpleName();
    private static final String CAR_UI_CONTENT_LEFT_DRAWABLE = "car_ui_content_left_drawable";
    private static final String CAR_UI_CONTENT_NAME = "car_ui_content_name";

    @NonNull
    private final List<ShortcutItem> mShortcuts;
    @NonNull
    private final ListPopupWindow mListPopupWindow;
    @NonNull
    private final ShortcutPopupAdapter mShortcutPopupAdapter;
    @NonNull
    private final View mAnchorView;
    @NonNull
    private final Context mContext;
    private final int mArrowCenterOffsetFromBoundary;
    private boolean mIsAboveAnchor;

    @NonNull
    private final Animation mCollapseAnimationAboveAnchor;
    @NonNull
    private final Animation mCollapseAnimationBelowAnchor;

    public CarUiShortcutsPopup(@NonNull Context context, @NonNull View anchorView,
            @NonNull List<ShortcutItem> shortcuts) {
        mShortcuts = shortcuts;
        mContext = context;
        mAnchorView = anchorView;
        CarUiArrowContainerView arrowView = (CarUiArrowContainerView) LayoutInflater.from(
                mContext).inflate(R.layout.car_ui_shortcut_popup_item, /* root= */ null);
        // offset of the anchor view from its center on x axis.
        mArrowCenterOffsetFromBoundary =
                arrowView.getArrowOffsetX() + (arrowView.getArrowWidth() >> 1);
        mListPopupWindow = initListPopupWindow(context, anchorView);
        mShortcutPopupAdapter = createAdapter(context, shortcuts);
        mListPopupWindow.setAdapter(mShortcutPopupAdapter);
        mListPopupWindow.setAnimationStyle(0);
        mListPopupWindow.setOnDismissListener(this::animateCollapse);
        mCollapseAnimationAboveAnchor = AnimationUtils.loadAnimation(mContext,
                R.anim.car_ui_shortcutspopup_above_anchor_collapse_anim);
        mCollapseAnimationBelowAnchor = AnimationUtils.loadAnimation(mContext,
                R.anim.car_ui_shortcutspopup_below_anchor_collapse_anim);
    }

    private void animateCollapse() {
        if (mListPopupWindow.getListView() == null) {
            return;
        }
        Animation animation =
                mIsAboveAnchor ? mCollapseAnimationAboveAnchor : mCollapseAnimationBelowAnchor;
        mListPopupWindow.getListView().setAnimation(animation);
        mListPopupWindow.getListView().animate();
    }

    private ListPopupWindow initListPopupWindow(Context context, View anchorView) {
        ListPopupWindow listPopupWindow = new ListPopupWindow(context);
        listPopupWindow.setAnchorView(anchorView);
        listPopupWindow.setBackgroundDrawable(null);
        listPopupWindow.setWidth(
                (int) context.getResources().getDimension(R.dimen.car_ui_shortcuts_popup_width));
        listPopupWindow.setModal(true);
        return listPopupWindow;
    }

    private ShortcutPopupAdapter createAdapter(Context context, List<ShortcutItem> shortcuts) {
        //create adapter list from shortcutList
        List<Map<String, ?>> adapterList = new ArrayList<>();
        for (ShortcutItem shortcut : shortcuts) {
            HashMap<String, Object> map = new HashMap<>();
            map.put(CAR_UI_CONTENT_LEFT_DRAWABLE, shortcut.data().mLeftDrawable);
            map.put(CAR_UI_CONTENT_NAME, shortcut.data().mShortcutName);
            adapterList.add(map);
        }
        String[] from = new String[]{CAR_UI_CONTENT_LEFT_DRAWABLE, CAR_UI_CONTENT_NAME};
        int[] to = new int[]{R.id.car_ui_content_left_drawable, R.id.car_ui_content_name};
        return new ShortcutPopupAdapter(context, this,
                adapterList,
                R.layout.car_ui_shortcut_popup_item, from, to);
    }

    /**
     * Aligns the Popup Horizontally and Vertically with the Anchor.
     * Check {@link CarUiShortcutsPopup#mayBeReversePopupJustBeforeDrawingPopupWindow()} for
     * aligning either at the top or below the anchor.
     */
    public void show() {
        //Calculate the offset, so the popup aligns with the arrow.
        int offset = measureHorizontalOffset();
        mListPopupWindow.setHorizontalOffset(offset);
        //Calculate if the popup will be drawn left of the anchor or right.
        mShortcutPopupAdapter.setArrowGravityRight(offset < -mArrowCenterOffsetFromBoundary);

        mListPopupWindow.setEpicenterBounds(
                new Rect(offset, 0, (int) mContext.getResources().getDimension(
                        R.dimen.car_ui_shortcuts_popup_width) + offset, 0));
        mListPopupWindow.show();
        if (mListPopupWindow.getListView() == null) {
            return;
        }
        mListPopupWindow.getListView().setDivider(
                AppCompatResources.getDrawable(mContext, R.drawable.car_ui_divider));

        mayBeReversePopupJustBeforeDrawingPopupWindow();
    }

    /**
     * Returns {@code true} if the popup is showing on screen.
     */
    public boolean isShowing() {
        return mListPopupWindow.isShowing();
    }

    /**
     * Dismisses the Popup if its showing.
     */
    public void dismiss() {
        if (isShowing()) {
            mListPopupWindow.dismiss();
        }
    }

    /**
     * {@link ListPopupWindow} uses {@link android.widget.PopupWindow}, which measures and draws
     * the Popup above or below the anchor based on whether there is enough roam to draw and few
     * other factors.
     * We determine the position of the popup window just before drawing and reverse arrow and the
     * list
     * if needed.
     */
    private void mayBeReversePopupJustBeforeDrawingPopupWindow() {
        if (mListPopupWindow.getListView() == null) {
            return;
        }
        View view = mListPopupWindow.getListView();
        final ViewTreeObserver.OnPreDrawListener preDrawListener =
                new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        view.getViewTreeObserver().removeOnPreDrawListener(
                                this);
                        int[] popupWindowLoc = new int[2];
                        int[] anchorLoc = new int[2];
                        view.getLocationOnScreen(popupWindowLoc);
                        mAnchorView.getLocationOnScreen(anchorLoc);
                        if (popupWindowLoc[1] <= anchorLoc[1]) {
                            mIsAboveAnchor = true;
                            mShortcutPopupAdapter.setReverse(true);
                        } else {
                            mIsAboveAnchor = false;
                            mShortcutPopupAdapter.setReverse(false);
                        }
                        mShortcutPopupAdapter.notifyDataSetChanged();
                        return true;
                    }
                };
        view.getViewTreeObserver().addOnPreDrawListener(preDrawListener);
    }

    /**
     * Returns the offset of the PopupWindow required to align to the Arrow.
     */
    private int measureHorizontalOffset() {
        int[] anchorLoc = new int[2];
        mAnchorView.getLocationOnScreen(anchorLoc);
        Rect displayFrame = getWindowVisibleDisplayFrame(mAnchorView);
        int anchorCenterX = (mAnchorView.getWidth() >> 1) - mArrowCenterOffsetFromBoundary;
        int popupStartLoc = anchorLoc[0] + anchorCenterX;
        //PopupWindow will fit the display frame horizontally when drawn to the right of the anchor.
        if (popupStartLoc + mListPopupWindow.getWidth() <= displayFrame.right) {
            return anchorCenterX;
        }
        //If not PopupWindow will be flipped to draw to the left of the anchor
        return -1 * (mListPopupWindow.getWidth()) + (mArrowCenterOffsetFromBoundary << 1)
                + anchorCenterX;
    }

    private Rect getWindowVisibleDisplayFrame(View view) {
        Rect displayFrame = new Rect();
        View appRootView = view.getRootView();
        appRootView.getWindowVisibleDisplayFrame(displayFrame);
        return displayFrame;
    }

    @Override
    public void onClick(int position) {
        if (position < 0 || position >= mShortcuts.size()) {
            throw new ArrayIndexOutOfBoundsException(
                    "position: " + position + " is not within size " + mShortcuts.size());
        }
        if (mShortcuts.get(position).onClick()) {
            mListPopupWindow.dismiss();
        }
    }

    @Override
    public boolean isEnabled(int position) {
        if (position < 0 || position >= mShortcuts.size()) {
            Log.e(TAG, "OnItemClick Out of Bounds " + position);
            return false;
        }
        return mShortcuts.get(position).isEnabled();
    }

    /**
     * Builder to create shortcuts list for {@link CarUiShortcutsPopup}
     */
    public static class Builder {
        private final List<ShortcutItem> mShortcutList = new ArrayList<>();

        /**
         * add shortcut item to {@link Builder#mShortcutList}
         *
         * @param shortcut - check {@link ShortcutItem}
         */
        public Builder addShortcut(ShortcutItem shortcut) {
            mShortcutList.add(shortcut);
            return this;
        }

        /**
         * @param context    - view's context
         * @param anchorView - Popup will be aligned relative to this view
         * @return {@link CarUiShortcutsPopup}
         */
        public CarUiShortcutsPopup build(Context context, View anchorView) {
            return new CarUiShortcutsPopup(context, anchorView, mShortcutList);
        }
    }

    /**
     * Defines each item in {@link CarUiShortcutsPopup#mListPopupWindow}
     */
    public interface ShortcutItem {
        /**
         * Defines
         * {@link ItemData#mLeftDrawable} and
         * {@link ItemData#mShortcutName}
         */
        ItemData data();

        /**
         * Defines onClick UserAction
         */
        boolean onClick();

        /**
         * Sets the Enabled State
         */
        boolean isEnabled();
    }

    /**
     * Data class for Item in {@link CarUiShortcutsPopup#mListPopupWindow}
     */
    public static class ItemData {
        private final int mLeftDrawable;
        private final String mShortcutName;

        public ItemData(int leftDrawable, String shortcutName) {
            mLeftDrawable = leftDrawable;
            mShortcutName = shortcutName;
        }
    }
}
