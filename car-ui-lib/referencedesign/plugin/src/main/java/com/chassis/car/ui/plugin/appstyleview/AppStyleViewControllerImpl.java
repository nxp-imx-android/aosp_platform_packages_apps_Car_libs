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

package com.chassis.car.ui.plugin.appstyleview;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;
import android.widget.ScrollView;

import com.android.car.ui.plugin.oemapis.appstyledview.AppStyledViewControllerOEMV1;
import com.android.car.ui.plugin.oemapis.appstyledview.AppStyledViewControllerOEMV2;

import com.chassis.car.ui.plugin.R;

/**
 * The OEM implementation for {@link AppStyledViewControllerOEMV1} for a AppStyledView.
 */
public class AppStyleViewControllerImpl implements AppStyledViewControllerOEMV2 {
    private final Context mPluginContext;
    private final View mAppStyleView;
    private int mWidth;
    private int mHeight;

    public AppStyleViewControllerImpl(Context pluginContext) {
        mPluginContext = pluginContext;
        LayoutInflater inflater = LayoutInflater.from(mPluginContext);
        mAppStyleView = inflater.inflate(R.layout.app_styled_view, null, false);
    }

    @Override
    public View getView() {
        return mAppStyleView;
    }

    @Override
    public void setContent(View content) {
        ScrollView scrollview = mAppStyleView.requireViewById(R.id.app_styled_content);
        scrollview.removeAllViews();
        scrollview.addView(content);
    }

    @Override
    public void setOnBackClickListener(Runnable listener) {
        ImageView navIconView = mAppStyleView.requireViewById(R.id.app_styled_view_icon_close);
        if (listener == null) {
            navIconView.setOnClickListener(null);
            navIconView.setClickable(false);
        } else {
            navIconView.setOnClickListener(v -> listener.run());
        }
    }

    @Override
    public void setNavIcon(int navIcon) {
        ImageView navIconView = mAppStyleView.requireViewById(R.id.app_styled_view_icon_close);
        navIconView.setVisibility(navIcon == AppStyledViewControllerOEMV1.NAV_ICON_DISABLED
                ? View.INVISIBLE : View.VISIBLE);
        if (navIcon == AppStyledViewControllerOEMV1.NAV_ICON_BACK) {
            navIconView.setImageResource(R.drawable.icon_back);
        } else if (navIcon == AppStyledViewControllerOEMV1.NAV_ICON_CLOSE) {
            navIconView.setImageResource(R.drawable.icon_close);
        }
    }

    @Override
    public LayoutParams getDialogWindowLayoutParam(LayoutParams params) {
        mWidth = Math.round(
                mPluginContext.getResources().getDimension(R.dimen.app_styled_dialog_width));
        params.width = mWidth;
        mHeight = Math.round(
                mPluginContext.getResources().getDimension(R.dimen.app_styled_dialog_height));
        params.height = mHeight;
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = Math.round(
                mPluginContext.getResources().getDimension(R.dimen.app_styled_dialog_position_x));
        params.y = Math.round(
                mPluginContext.getResources().getDimension(R.dimen.app_styled_dialog_position_y));
        return params;
    }

    @Override
    public int getContentAreaWidth() {
        return mWidth - mPluginContext.getResources().getDimensionPixelSize(
                R.dimen.app_styled_dialog_nav_bar_width);
    }

    @Override
    public int getContentAreaHeight() {
        return mHeight;
    }
}
