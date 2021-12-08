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

import static android.view.accessibility.AccessibilityEvent.TYPE_VIEW_FOCUSED;
import static android.view.accessibility.AccessibilityNodeInfo.FOCUS_INPUT;

import static com.android.car.ui.RotaryScrollWebView.TAG;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.util.Objects;

/**
 * A manager for Rotary scrolling and {@link AccessibilityEvent}s related to Rotary scrolling.
 * <p>
 * The primary function of this class is to enhance Rotary scrolling for {@link WebView}s in the
 * following ways:
 * <ul>
 *  <li>It converts {@link MotionEvent}s intended to cause scrolling from source
 *      {@link InputDevice#SOURCE_ROTARY_ENCODER} into the same event with
 *      {@link InputDevice#SOURCE_MOUSE} since WebViews can handle those natively.
 *  <li>It detects if child {@link AccessibilityNodeInfo}s with focus go off-screen and clears
 *      focus from them so as to avoid confusing the user if a center button click was sent to an
 *      element that they can no longer see.
 *  <li>It populates AccessibilityEvents of {@link AccessibilityEvent#TYPE_VIEW_FOCUSED} with the
 *      node ID set with {@link R.id#car_ui_web_view_last_focused_element}, so that
 *      {@link com.android.car.rotary.RotaryService} can track the actually focused child node.
 * </ul>
 * <p>
 * This class also listens for focus and touch events to overlay or remove the blue focus
 * highlight over the scrollbar thumb based on whether any children have focus outside of touch
 * mode. It does so by using a {@link android.graphics.PorterDuffColorFilter} which keeps the rest
 * of the scrollbar thumb drawable unmodified. We do this to let the user know the WebView will
 * scroll when the user rotates the Rotary knob.
 * <p>
 * NOTE: This class cannot enable Rotary scrolling for WebViews alone. This is because WebViews
 * do not call {@link View.AccessibilityDelegate#getAccessibilityNodeProvider} and also do
 * not call {@link View.AccessibilityDelegate#onInitializeAccessibilityNodeInfo} for the host node.
 * <p>
 * TIP: To use this class with a non-{@link RotaryScrollWebView}, you must also use a
 * {@link RotaryScrollNodeProvider}. Simply override {@link WebView#getAccessibilityNodeProvider}
 * to return a RotaryScrollNodeProvider in addition to using this class.
 * <p>
 * This class is generic, and does not rely on any specific WebView calls. It could be updated
 * to work with non-WebViews as well.
 */
@TargetApi(Build.VERSION_CODES.R)
class RotaryScrollManager extends View.AccessibilityDelegate
        implements View.OnGenericMotionListener, View.OnFocusChangeListener, View.OnTouchListener {

    /** Whether the scroll bar will fade when the view is not scrolling. */
    private final boolean mIsScrollbarFadingEnabledByDefault;

    @VisibleForTesting
    RotaryScrollManager(boolean isScrollbarFadingEnabledByDefault) {
        mIsScrollbarFadingEnabledByDefault = isScrollbarFadingEnabledByDefault;
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        // Only handle losing focus here, because WebViews are often focusable in touch mode.
        if (v == null || hasFocus) {
            return;
        }
        Drawable drawable = v.getVerticalScrollbarThumbDrawable();
        if (drawable != null) {
            drawable.clearColorFilter();
        }
        v.setScrollbarFadingEnabled(mIsScrollbarFadingEnabledByDefault);
    }

    @Override
    public boolean performAccessibilityAction(View host, int action, Bundle args) {
        boolean result = super.performAccessibilityAction(host, action, args);
        if (!result || action != AccessibilityNodeInfo.ACTION_FOCUS || host == null) {
            return result;
        }
        // This call only happens when RotaryService moves focus into the WebView from
        // outside the WebView. When this happens, we can safely adjust the scrollbar UI based
        // on any state we saved previously.
        host.setScrollbarFadingEnabled(false);
        if (findFocus(host) == null) {
            setScrollbarThumbHighlight(host);
        }
        return true;
    }

    @Override
    public boolean onGenericMotion(View view, MotionEvent event) {
        boolean shouldProcessRotaryScroll =
                event != null
                        && event.getAction() == MotionEvent.ACTION_SCROLL
                        && event.getSource() == InputDevice.SOURCE_ROTARY_ENCODER;
        if (!shouldProcessRotaryScroll) {
            return false;
        }

        // Convert SOURCE_ROTARY_ENCODER scroll events into SOURCE_MOUSE scroll events that
        // WebView knows how to handle.
        event.setSource(InputDevice.SOURCE_MOUSE);
        boolean didScroll = view.onGenericMotionEvent(event);
        if (!didScroll) {
            return false;
        }
        // Always show the scrollbar when Rotary scrolling.
        view.setScrollbarFadingEnabled(false);
        // If the focused child just went off-screen without anything else to focus, we highlight
        // the scrollbar for the likely occurrence that no other children take focus. If another
        // child does immediately take focus, the AccessibilityNodeProvider will remove the
        // highlight again when that happens. Otherwise, if the lastFocusedId is null, we either
        // just came out of touch mode or no child currently has focus anyway. We try to add the
        // highlight in case this call is following a touch event. (isInTouchMode is not reliable
        // because the WebView returns that it is in Touch Mode as long as no child has focus.)
        if (maybeClearFocusedChild(view, event) || findFocus(view) == null) {
            setScrollbarThumbHighlight(view);
        }
        return true;
    }

    @Override
    public void onInitializeAccessibilityEvent(View view, AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(view, event);
        if (view == null || event == null) {
            return;
        }
        if (isJumpScrollEvent(event, view.getHeight()) && getLastFocusedNodeId(view) != null) {
            // The last focused node cannot be on-screen anymore after scrolling the whole height
            // of the view, so reset focus to the container if a child had focus before the jump.
            // If a child did have focus before the jump, then it must be in rotary mode,
            // so highlight the scrollbar in case nothing is focused after the jump. If something
            // takes focus after jump scrolling, it should do so after the scroll event completes
            // so the user cannot interact with off-screen nodes. If this happens, the
            // AccessibilityNodeProvider clears the scrollbar highlight anyway.
            setLastFocusedNodeId(view, null);
            setScrollbarThumbHighlight(view);
        } else if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_FOCUSED) {
            // Try to let RotaryService know which specific child just took focus so that
            // RotaryService updates its internal state. WebViews do not already do this because
            // they do not send TYPE_VIEW_FOCUSED events for their children at all. See
            // setLastFocusedNodeId() for where to send events when child nodes take focus.
            if (getLastFocusedNodeId(view) != null) {
                event.setSource(/* root= */ view, getLastFocusedNodeId(view));
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View view, MotionEvent event) {
        // Note: WebView does not use performClick().
        view.setScrollbarFadingEnabled(mIsScrollbarFadingEnabledByDefault);
        setLastFocusedNodeId(view, null);
        // Never highlight the thumb in touch mode.
        clearScrollbarThumbHighlight(view);
        return false;
    }

    static Integer getLastFocusedNodeId(@NonNull View view) {
        return (Integer) view.getTag(R.id.car_ui_web_view_last_focused_element);
    }

    static void setLastFocusedNodeId(@NonNull View view, @Nullable Integer id) {
        Integer oldId = getLastFocusedNodeId(view);
        if (!Objects.equals(oldId, id)) {
            view.setTag(R.id.car_ui_web_view_last_focused_element, id);
            // We always set this when something was focused, so send an event to communicate it.
            view.sendAccessibilityEvent(TYPE_VIEW_FOCUSED);
        }
    }

    static void clearScrollbarThumbHighlight(@NonNull View view) {
        Drawable drawable = view.getVerticalScrollbarThumbDrawable();
        if (drawable != null) {
            drawable.clearColorFilter();
        }
    }

    /** Attaches a default {@link RotaryScrollManager} to the {@code webView}. */
    @SuppressLint("ClickableViewAccessibility")
    static void attach(@NonNull WebView webView) {
        // Note: WebView does not use performClick().
        attach(webView, new RotaryScrollManager(webView.isScrollbarFadingEnabled()));
    }

    /**
     * Attaches the {@code manager} to the {@code webView}.
     * <p>
     * This method sets the RotaryScrollManager instance as all the following on the
     * {@code webView}:
     * <ul>
     *   <li>{@link View.AccessibilityDelegate}
     *   <li>{@link View.OnFocusChangeListener}
     *   <li>{@link View.OnGenericMotionListener}
     *   <li>{@link View.OnTouchListener}
     * </ul>
     * <p>
     * All of these overwrite any other previously set listeners. To set those listeners while
     * using this class, extend the RotaryScrollManager and override as necessary.
     * <p>
     * This method assumes the user would like to scroll through the entire web content, and thus
     * also sets {@link android.webkit.WebSettings#setNeedInitialFocus} to false to avoid jumping
     * to the first interactive element.
     */
    @SuppressLint("ClickableViewAccessibility")
    static void attach(@NonNull WebView webView, @NonNull RotaryScrollManager manager) {
        // Note: WebView does not use performClick().
        // Prevent the first Rotary input from snapping to the first focusable node.
        webView.getSettings().setNeedInitialFocus(false);
        webView.setAccessibilityDelegate(manager);
        webView.setOnFocusChangeListener(manager);
        webView.setOnGenericMotionListener(manager);
        webView.setOnTouchListener(manager);
    }

    /**
     * Clears focus from child node if it is about to go off-screen.
     * <p>
     * This method checks if the current node that has focus is about to go off-screen once the
     * {@code view} processes the scroll action specified by the {@code event}. If it is about
     * to go off-screen, we need to clear focus from it. Doing so prevents the user from clicking on
     * off-screen views, and also lets us manage the scrollbar's focus highlight color.
     * WebViews do not have any options to set to automatically manage this, regardless of the input
     * mode.
     *
     * @param event the {@link MotionEvent} from {@link com.android.car.rotary.RotaryService} that
     *              caused the scrolling action
     */
    private boolean maybeClearFocusedChild(@NonNull View view, @NonNull MotionEvent event) {
        Integer nodeId = getLastFocusedNodeId(view);
        if (nodeId == null) {
            return false;
        }
        AccessibilityNodeInfo focusedNode = findFocus(view);
        if (focusedNode == null || !focusedNode.isFocused()) {
            return false;
        }
        Rect bounds = getBoundsInScreen(focusedNode);
        float verticalScrollFactor =
                ViewConfiguration.get(view.getContext()).getScaledVerticalScrollFactor();
        float verticalScrollOffset =
                verticalScrollFactor * event.getAxisValue(MotionEvent.AXIS_VSCROLL);
        bounds.offset(/* dx= */ 0, (int) Math.round(verticalScrollOffset));
        boolean intersects = Rect.intersects(bounds,
                getBoundsInScreen(view.createAccessibilityNodeInfo()));
        if (intersects) {
            return false;
        }
        boolean result = false;
        AccessibilityNodeProvider provider = view.getAccessibilityNodeProvider();
        // We already checked the type in findFocus().
        if (provider != null) {
            result = provider.performAction(nodeId,
                    AccessibilityNodeInfo.ACTION_CLEAR_FOCUS, /* arguments= */ null);
        }
        if (result && Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Cleared focus from child with ID " + nodeId + " that moved off-screen.");
        }
        return result;
    }

    /**
     * Sets a color filter on the drawable of the vertical scrollbar thumb to make it the same
     * color as the system default focus highlight.
     */
    private static void setScrollbarThumbHighlight(@NonNull View view) {
        Drawable drawable = view.getVerticalScrollbarThumbDrawable();
        if (drawable != null) {
            int color = view.getContext().getColor(R.color.car_ui_rotary_focus_stroke_color);
            drawable.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC));
        }
    }

    private static boolean isJumpScrollEvent(@NonNull AccessibilityEvent event, int viewHeight) {
        return event.getEventType() == AccessibilityEvent.TYPE_VIEW_SCROLLED
                && Math.abs(event.getScrollDeltaY()) > viewHeight;
    }

    /** Returns the currently focused or lastly focused node inside the given {@code webView}. */
    private static AccessibilityNodeInfo findFocus(@NonNull View webView) {
        AccessibilityNodeProvider provider = webView.getAccessibilityNodeProvider();
        if (webView instanceof WebView && provider != null
                && !(provider instanceof RotaryScrollNodeProvider)) {
            throw new IllegalStateException(
                    "WebViews must use a RotaryScrollNodeProvider with RotaryScrollManager.");
        }
        // Note: the provider of the WebView will be null if the WebView has not loaded the page
        // yet. Return null in this case.
        return provider == null ? null : provider.findFocus(FOCUS_INPUT);
    }

    private static Rect getBoundsInScreen(@NonNull AccessibilityNodeInfo node) {
        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);
        return bounds;
    }
}
