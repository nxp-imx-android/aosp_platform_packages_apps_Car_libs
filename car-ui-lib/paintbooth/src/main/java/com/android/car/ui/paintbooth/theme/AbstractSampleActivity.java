/*
 * Copyright (C) 2019 The Android Open Source Project.
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

package com.android.car.ui.paintbooth.theme;

import android.app.Activity;
import android.app.UiModeManager;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.android.car.ui.core.CarUi;
import com.android.car.ui.paintbooth.R;
import com.android.car.ui.toolbar.MenuItem;
import com.android.car.ui.toolbar.MenuItem.OnClickListener;
import com.android.car.ui.toolbar.NavButtonMode;
import com.android.car.ui.toolbar.ToolbarController;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the menu for the theme playground app
 */
public abstract class AbstractSampleActivity extends Activity {

    private final List<MenuItem> mMenuItems = new ArrayList<>();
    private UiModeManager mUiModeManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ToolbarController toolbar = CarUi.requireToolbar(this);
        toolbar.setTitle(getTitle());
        toolbar.setNavButtonMode(NavButtonMode.BACK);
        toolbar.setLogo(R.drawable.ic_launcher);
        buildOverflowMenu(toolbar);
        mUiModeManager = this.getSystemService(UiModeManager.class);
    }

    private void buildOverflowMenu(ToolbarController toolbar) {
        mMenuItems.add(createMenuItem(R.string.text_elements,
                i -> startSampleActivity(TextSamples.class)));
        mMenuItems.add(createMenuItem(R.string.button_elements,
                i -> startSampleActivity(ButtonSamples.class)));
        mMenuItems.add(createMenuItem(R.string.progress_bar_elements,
                i -> startSampleActivity(ProgressBarSamples.class)));
        mMenuItems.add(createMenuItem(R.string.panel_elements,
                i -> startSampleActivity(ColorSamples.class)));
        mMenuItems.add(createMenuItem(R.string.palette_elements,
                i -> startSampleActivity(MaterialUColorPalette.class)));
        mMenuItems.add(createMenuItem(R.string.dialog_elements,
                i -> startSampleActivity(DialogSamples.class)));
        mMenuItems.add(createMenuItem(R.string.toggle_theme,
                i -> toggleDayNight()));
        mMenuItems.add(createMenuItem(R.string.widgets,
                i -> startSampleActivity(WidgetsSamples.class)));
        mMenuItems.add(createMenuItem(R.string.recycler_view,
                i -> startSampleActivity(RecyclerViewSamples.class)));
        mMenuItems.add(createMenuItem(R.string.car_ui_recycler_view,
                i -> startSampleActivity(CarUiRecyclerViewSamples.class)));
        mMenuItems.add(createMenuItem(R.string.default_themes,
                i -> startSampleActivity(DefaultThemeSamples.class)));
        mMenuItems.add(createMenuItem(R.string.multiple_intent,
                i -> startSampleActivity(MultipleIntentSamples.class)));

        toolbar.setMenuItems(mMenuItems);
    }

    private MenuItem createMenuItem(int title, OnClickListener listener) {
        return MenuItem.builder(this)
                .setTitle(title)
                .setOnClickListener(listener)
                .setDisplayBehavior(MenuItem.DisplayBehavior.NEVER)
                .build();
    }

    /**
     * Launch the given sample activity
     */
    private boolean startSampleActivity(Class<?> cls) {
        Intent dialogIntent = new Intent(this, cls);
        startActivity(dialogIntent);
        return true;
    }

    private boolean toggleDayNight() {
        mUiModeManager.setNightMode(
                (mUiModeManager.getNightMode() == UiModeManager.MODE_NIGHT_YES)
                        ? UiModeManager.MODE_NIGHT_NO : UiModeManager.MODE_NIGHT_YES);
        return true;
    }

    void setupBackgroundColorControls(int backgroundLayoutId) {
        Button colorSetButton = findViewById(R.id.set_background_color);
        ((EditText) findViewById(R.id.background_input_color)).setText(
                R.string.default_background_color,
                TextView.BufferType.EDITABLE);
        colorSetButton.setOnClickListener(v -> {
            String value = ((EditText) findViewById(R.id.background_input_color)).getText()
                    .toString();
            try {
                int color = Color.parseColor(value);
                View dialogLayout = findViewById(backgroundLayoutId);
                dialogLayout.setBackgroundColor(color);
            } catch (Exception e) {
                Toast.makeText(this, "not a color", Toast.LENGTH_LONG).show();
            }
        });
        Button colorResetButton = findViewById(R.id.reset);
        colorResetButton.setOnClickListener(v -> {
            try {
                View dialogLayout = findViewById(backgroundLayoutId);
                dialogLayout.setBackgroundColor(
                        getResources().getColor(android.R.color.black));
            } catch (Exception e) {
                Toast.makeText(this, "Something went Wrong. Try again later.",
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}
