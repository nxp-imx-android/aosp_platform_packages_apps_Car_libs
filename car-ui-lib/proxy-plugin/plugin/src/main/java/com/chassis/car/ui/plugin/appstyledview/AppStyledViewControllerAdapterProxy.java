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

package com.chassis.car.ui.plugin.appstyledview;

import android.view.View;
import android.view.WindowManager.LayoutParams;

import androidx.annotation.NonNull;

import com.android.car.ui.appstyledview.AppStyledViewController;
import com.android.car.ui.appstyledview.AppStyledViewController.AppStyledViewNavIcon;
import com.android.car.ui.plugin.oemapis.appstyledview.AppStyledViewControllerOEMV2;

/**
 * Adapts a {@link AppStyledViewController} into a {@link AppStyledViewControllerOEMV2}.
 */
public class AppStyledViewControllerAdapterProxy implements AppStyledViewControllerOEMV2 {

    @NonNull
    private final AppStyledViewController mStaticController;
    private View mContentView;

    public AppStyledViewControllerAdapterProxy(@NonNull AppStyledViewController controllerOEMV1) {
        mStaticController = controllerOEMV1;
    }

    @Override
    public View getView() {
        return mStaticController.getAppStyledView(mContentView);
    }

    @Override
    public void setContent(View view) {
        mContentView = view;
    }

    @Override
    public void setOnBackClickListener(Runnable runnable) {
        mStaticController.setOnNavIconClickListener(runnable);
    }

    @Override
    public void setNavIcon(@AppStyledViewNavIcon int navIcon) {
        switch (navIcon) {
            case AppStyledViewControllerOEMV2.NAV_ICON_BACK:
                mStaticController.setNavIcon(AppStyledViewNavIcon.BACK);
                break;
            case AppStyledViewControllerOEMV2.NAV_ICON_CLOSE:
                mStaticController.setNavIcon(AppStyledViewNavIcon.CLOSE);
                break;
            default:
                throw new IllegalArgumentException("Unknown nav icon style: " + navIcon);
        }
    }

    @Override
    public LayoutParams getDialogWindowLayoutParam(LayoutParams params) {
        return mStaticController.getDialogWindowLayoutParam(params);
    }

    @Override
    public int getContentAreaWidth() {
        return mStaticController.getContentAreaWidth();
    }

    @Override
    public int getContentAreaHeight() {
        return mStaticController.getContentAreaHeight();
    }
}
