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

package com.android.car.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.accessibility.AccessibilityNodeProvider;
import android.webkit.WebView;

import androidx.annotation.Nullable;

/**
 * A {@link WebView} that scrolls up or down from Rotary inputs, in addition to handling focus.
 *
 * <h1>Extending this class
 * <p>
 * This class simply attaches a {@link RotaryScrollManager} and overrides {@link
 * #getAccessibilityNodeProvider} to return a {@link RotaryScrollNodeProvider}. To implement your
 * own custom WebView with Rotary scrolling enabled, simply do the same with a different class
 * (instead of extending this one).
 *
 * <h2>
 * Listeners and delegates
 * <p>
 * The RotaryScrollManager sets itself as all the following for this class:
 * <ul>
 *   <li>{@link android.view.View.AccessibilityDelegate}
 *   <li>{@link android.view.View.OnFocusChangeListener}
 *   <li>{@link android.view.View.OnGenericMotionListener}
 *   <li>{@link android.view.View.OnTouchListener}
 * </ul>
 * <p>
 * If you want to set your own listeners or delegates for this class, do the following:
 * 1. Extend the RotaryScrollManager.
 * 2. Override the method for the corresponding listener or delegate.
 * 3. Call the super method to maintain consistent scrolling UI.
 * 4. Attach your custom instance with {@link RotaryScrollManager#attach(WebView)}}.
 *
 * <h1>Interactions with web focus
 * <p>
 * Managing focus on web pages is a joint effort from the following three components:
 * <ul>
 *   <li>The WebView
 *   <li>The loaded web page (DOM)
 *   <li>The {@link com.android.car.rotary.RotaryService}
 * </ul>
 * <p>
 * While this WebView implementation and the RotaryService handle focus, scrolling, and some
 * native UI updates, the visual appearance of focus on interactive DOM elements completely depends
 * on the web page implementation itself. No native component can overlay any UI, that is consistent
 * with the rest of the automotive rotary UX, on top of individual DOM elements.
 *
 * <h2>Common visual bugs
 * <p>
 * Using this class with Web pages that do not always enable tab focus highlights (e.g. "Wiz focus
 * service") when running on automotive devices can cause elements to appear as though they do not
 * have focus, even though they do have focus. This can occur in any of the following scenarios:
 * <ul>
 *   <li>Using touch to jump scroll to a foot-note, and then switching to Rotary to scroll.
 *   <li>Injecting other key events, such as D-Pad keys or Tab keys, and then switching back to
 *       Rotary.
 *   <li>Using Rotary to focus types of HTML elements for which WebView does not implement any sort
 *       of backup focus.
 * </ul>
 *
 * <h3>Background
 * <p>
 * As indicated by the name, standard web page a11y most often does not enable focus highlighting
 * until after the successful first tab key input. WebViews make their best effort to make up for
 * this on Android devices with custom a11y focus mechanics. One such example is "Spatial
 * Navigation," which attempts to make certain views accessible to D-Pad users that would otherwise
 * be non-interactive.
 * <p>
 * However, this technique is not perfect, and generic web pages running on automotive often have
 * visual bugs such as not seeing focus. These bugs are purely visual though; the Rotary Service
 * still successfully focuses the node within the Android a11y framework and dumping the hierarchy
 * shows this. Additionally, pressing the center button does interact with the focused node. The web
 * page itself usually must address focus visibility.
 *
 * <h3>Solution
 * <p>
 * Please work with the webpage owners to decide the best way to manage focus from the web page
 * itself. REFERTO(cl/366203871) for an example of enabling focus on a webpage when running on
 * automotive.
 *
 * <h1>Limitations
 * <p>
 * We have only tested this class as the only focusable child of a {@link
 * com.android.car.ui.FocusArea}. Adding more focusable views within the same layout may surface
 * new issues.
 * <p>
 * Additionally, this class currently only handles vertical scrolling. No clients require horizontal
 * scrolling yet.
 */
@TargetApi(Build.VERSION_CODES.R)
public final class RotaryScrollWebView extends WebView {

    static final String TAG = "RotaryScrollWebView";

    public RotaryScrollWebView(Context context) {
        super(context);
        init();
    }

    public RotaryScrollWebView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RotaryScrollWebView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public RotaryScrollWebView(Context context, @Nullable AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        RotaryScrollManager.attach(this);
    }

    @Override
    public AccessibilityNodeProvider getAccessibilityNodeProvider() {
        AccessibilityNodeProvider provider = super.getAccessibilityNodeProvider();
        return provider == null ? null : new RotaryScrollNodeProvider(this, provider);
    }
}
