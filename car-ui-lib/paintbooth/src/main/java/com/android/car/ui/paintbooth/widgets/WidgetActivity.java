/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.car.ui.paintbooth.widgets;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import com.android.car.ui.core.CarUi;
import com.android.car.ui.paintbooth.R;
import com.android.car.ui.toolbar.MenuItem;
import com.android.car.ui.toolbar.NavButtonMode;
import com.android.car.ui.toolbar.ToolbarController;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity that shows different widgets from the device default theme.
 */
public class WidgetActivity extends Activity {
    private boolean mIsEnabled = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.widgets_activity);

        List<View> widgets = new ArrayList<>();
        widgets.add(requireViewById(R.id.check));
        widgets.add(requireViewById(R.id.switch_widget));
        widgets.add(requireViewById(R.id.toggle_button));
        widgets.add(requireViewById(R.id.radio_button));
        widgets.add(requireViewById(R.id.seekbar));

        ToolbarController toolbar = CarUi.requireToolbar(this);
        toolbar.setTitle(getTitle());
        toolbar.setNavButtonMode(NavButtonMode.BACK);
        List<MenuItem> menuItems = new ArrayList<>();
        menuItems.add(MenuItem.builder(this)
                .setTitle(getString(R.string.widget_enable_button_text))
                .setOnClickListener(
                        i -> {
                            mIsEnabled = !mIsEnabled;
                            for (View view : widgets) {
                                view.setEnabled(mIsEnabled);
                            }
                        })
                .build());
        toolbar.setMenuItems(menuItems);
    }
}
