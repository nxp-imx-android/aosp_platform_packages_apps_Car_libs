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

import static android.view.InputDevice.SOURCE_MOUSE;
import static android.view.MotionEvent.ACTION_SCROLL;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.Spy;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Unit tests for {@link RotaryScrollWebView}. */
// TODO(b/202420924): Migrate the remaining tests.
@RunWith(AndroidJUnit4.class)
public class RotaryScrollWebViewTest {
    private static final long WAIT_TIME_MS = 3000;

    private Context mContext;
    private Handler mMainHandler;
    private FakeNodeProvider mFakeNodeProvider;
    private FakeWebView mFakeWebView;
    @Spy
    private RotaryScrollManager mRotaryScrollManager;

    private AccessibilityEvent mSentEvent;

    @Before
    public void setUp() throws InterruptedException {
        mContext = ApplicationProvider.getApplicationContext();
        mMainHandler = new Handler(mContext.getMainLooper());
        mFakeNodeProvider = new FakeNodeProvider();

        RotaryScrollManager rotaryScrollManager =
                new RotaryScrollManager(/* isScrollbarFadingEnabledByDefault= */ true);
        mRotaryScrollManager = Mockito.spy(rotaryScrollManager);

        // WebView methods must be called on main thread.
        runOnMainThread(() -> {
            mFakeWebView = new FakeWebView(mFakeNodeProvider);
            RotaryScrollManager.attach(mFakeWebView, mRotaryScrollManager);
        });

        // WebView must attach a RotaryScrollManager before calling getNodeProvider().
        when(mRotaryScrollManager.getAccessibilityNodeProvider(any(View.class)))
                .thenReturn(getNodeProvider());
        doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            View host = (View) args[0];
            AccessibilityEvent event = (AccessibilityEvent) args[1];
            rotaryScrollManager.sendAccessibilityEventUnchecked(host, event);
            mSentEvent = event;
            return null;
        }).when(mRotaryScrollManager).sendAccessibilityEventUnchecked(
                any(View.class), any(AccessibilityEvent.class));
    }

    @Test
    public void test_getAccessibilityNodeProvider_returnsRotaryScrollNodeProvider() {
        RotaryScrollWebView[] webView = new RotaryScrollWebView[1];
        runOnMainThread(() -> {
            webView[0] = new RotaryScrollWebView(mContext);
            RotaryScrollManager fakeManager = new RotaryScrollManager(false) {
                // Make sure webView.super.getAccessibilityNodeProvider does not return null.
                @Override
                public AccessibilityNodeProvider getAccessibilityNodeProvider(@Nullable View host) {
                    return mFakeNodeProvider;
                }
            };
            RotaryScrollManager.attach(webView[0], fakeManager);
        });

        assertThat(webView[0].getAccessibilityNodeProvider()).isInstanceOf(
                RotaryScrollNodeProvider.class);
    }

    private AccessibilityNodeProvider getNodeProvider() {
        return mFakeWebView.getAccessibilityNodeProvider();
    }

    private void runOnMainThread(@NonNull Runnable runnable) {
        CountDownLatch latch = new CountDownLatch(1);
        mMainHandler.post(() -> {
            runnable.run();
            latch.countDown();
        });
        try {
            assertWithMessage("Timeout")
                    .that(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS)).isTrue();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static final class FakeWebView extends WebView {
        @NonNull
        private final ColorDrawable mScrollbarDrawable;
        @NonNull
        private final FakeNodeProvider mFakeNodeProvider;

        FakeWebView(@NonNull FakeNodeProvider fakeNodeProvider) {
            super(ApplicationProvider.getApplicationContext());
            mFakeNodeProvider = fakeNodeProvider;
            mScrollbarDrawable = new ColorDrawable();
            setBottom(100);
            setRight(20);
            setVerticalScrollbarThumbDrawable(mScrollbarDrawable);
        }

        @NonNull
        ColorDrawable getScrollbarDrawable() {
            return mScrollbarDrawable;
        }

        @Override
        public boolean canScrollVertically(int direction) {
            return false;
        }

        @NonNull
        @Override
        public AccessibilityNodeProvider getAccessibilityNodeProvider() {
            return new RotaryScrollNodeProvider(this, mFakeNodeProvider);
        }

        @Override
        public boolean onGenericMotionEvent(@NonNull MotionEvent event) {
            return event.getAction() == ACTION_SCROLL && event.getSource() == SOURCE_MOUSE;
        }

        @Override
        public boolean isShown() {
            return true;
        }
    }

    private final class FakeNodeProvider extends AccessibilityNodeProvider {
        @NonNull
        final AccessibilityNodeInfo mFakeHost;
        @NonNull
        final AccessibilityNodeInfo mFakeTarget;
        @Nullable
        AccessibilityNodeInfo mFindFocus;
        int mExtraDataId;
        @NonNull
        final List mFakeNodes;
        int mActionPerformed;
        int mNodeIdForLastAction;
        boolean mActionResult;

        FakeNodeProvider() {
            mFakeHost = new AccessibilityNodeInfo();
            mFakeHost.setContentDescription("contentDescription");
            mFakeHost.setBoundsInScreen(new Rect(3, 5, 20, 30));
            mFakeHost.setSource(mFakeWebView);

            mFakeTarget = new AccessibilityNodeInfo();
            mFakeTarget.setClassName((View.class.getName()));
            mFakeTarget.setSource(new View(ApplicationProvider.getApplicationContext()));
            mFakeTarget.setFocusable(true);

            mFindFocus = mFakeHost;
            mExtraDataId = -1;
            mFakeNodes = Arrays.asList(new AccessibilityNodeInfo[]{mFakeHost, mFakeTarget});
            mActionPerformed = -1;
            mNodeIdForLastAction = -1;
        }

        @NonNull
        AccessibilityNodeInfo getFakeHost() {
            return mFakeHost;
        }

        @NonNull
        AccessibilityNodeInfo getFakeTarget() {
            return mFakeTarget;
        }

        void setFindFocus(@Nullable AccessibilityNodeInfo node) {
            mFindFocus = node;
        }

        int getExtraDataId() {
            return mExtraDataId;
        }

        @NonNull
        List getFakeNodes() {
            return mFakeNodes;
        }

        int getActionPerformed() {
            return mActionPerformed;
        }

        int getNodeIdForLastAction() {
            return mNodeIdForLastAction;
        }

        boolean getActionResult() {
            return mActionResult;
        }

        void setActionResult(boolean actionResult) {
            mActionResult = actionResult;
        }

        @NonNull
        @Override
        public AccessibilityNodeInfo createAccessibilityNodeInfo(int virtualViewId) {
            return virtualViewId == HOST_VIEW_ID ? mFakeHost : mFakeTarget;
        }

        @Nullable
        @Override
        public AccessibilityNodeInfo findFocus(int focus) {
            return mFindFocus;
        }

        @Override
        public void addExtraDataToAccessibilityNodeInfo(int virtualViewId,
                @Nullable AccessibilityNodeInfo info,
                @Nullable String extraDataKey,
                @Nullable Bundle arguments) {
            mExtraDataId = virtualViewId;
        }

        @NonNull
        @Override
        public List findAccessibilityNodeInfosByText(@Nullable String text, int virtualViewId) {
            return mFakeNodes;
        }

        @Override
        public boolean performAction(int virtualViewId, int action, @Nullable Bundle arguments) {
            mActionPerformed = action;
            mNodeIdForLastAction = virtualViewId;
            return mActionResult;
        }
    }
}
