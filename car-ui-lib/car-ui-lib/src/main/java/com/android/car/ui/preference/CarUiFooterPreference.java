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

package com.android.car.ui.preference;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceViewHolder;

import com.android.car.ui.R;
import com.android.car.ui.utils.CarUiUtils;

/**
 * This class represents a Footer preference with optional title, summary, and
 * icon, as well as an optional link.
 * <p>
 * <p>
 * Example xml usage:
 * {@code <com.android.car.ui.preference.CarUiFooterPreference
 *     // assume namespaces (android/app) are here
 *     android:key="footer_link"
 *     android:icon="@drawable/car_ui_icon_info"
 *     android:summary="@string/footer_link_text"/>
 * <p>
 * <p>
 * The footer link contains a linkText and a clickListener (passed as Runnable)
 * Both of these fields are set in {@link #setLink(String, Runnable)}) and can
 * only be set using this function
 * <p>
 * <p>
 * Example setLink java usage:
 * {@code private void setupFooterClickListenerWebLink(CarUiFooterPreference footer) {
 *     footer.setLink("Learn More",
 *             () -> startActivity(new Intent(Intent.ACTION_VIEW).setData(
 *             Uri.parse("http://www.google.com"))));
 * }
 */
public class CarUiFooterPreference extends CarUiPreference {
    private String mLinkText;
    private @Nullable Runnable mClickListener;

    public CarUiFooterPreference(Context context, AttributeSet attrs,
                                 int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setLayoutResource(R.layout.car_ui_preference_footer);
    }

    public CarUiFooterPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, R.style.Preference_CarUi_Preference);
    }

    public CarUiFooterPreference(Context context, AttributeSet attrs) {
        this(context, attrs, /* defStyleAttr= */ 0);
    }

    public CarUiFooterPreference(Context context) {
        this(context, /* attrs= */ null);
    }

    /**
     * Sets footer link with text linkText and action clickListener whenever clicked. Passing in
     * null for both parameters removes the footer link
     *
     * @param linkText                  String which is rendered as the clickable link text
     * @param clickListener             Runnable which is called every time linkText is clicked
     * @throws IllegalArgumentException if strictly one of linkText and clickListener are null
     */
    public void setLink(@Nullable String linkText, @Nullable Runnable clickListener) {
        if ((linkText == null && clickListener != null)
                || (linkText != null && clickListener == null)) {
            throw new IllegalArgumentException("Error: Both or neither argument must be null");
        }
        mLinkText = linkText;
        mClickListener = clickListener;
        notifyChanged();
    }

    /**
     * Returns link text for preference
     */
    @Nullable
    public String getLinkText() {
        return mLinkText;
    }

    /**
     * Returns boolean corresponding to whether or not the footer link is enabled
     */
    @NonNull
    public boolean isLinkEnabled() {
        return mLinkText != null && mClickListener != null;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        TextView link = CarUiUtils.findViewByRefId(holder.itemView, R.id.car_ui_link);

        if (isLinkEnabled()) {
            link.setText(mLinkText);
            link.setOnClickListener(v -> mClickListener.run());
            link.setClickable(true);
            link.setVisibility(View.VISIBLE);
        } else { // link not enabled
            link.setText("");
            link.setOnClickListener(null);
            link.setClickable(false);
            link.setVisibility(View.GONE);
        }
    }
}
