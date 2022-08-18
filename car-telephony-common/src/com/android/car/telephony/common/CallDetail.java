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

package com.android.car.telephony.common;

import android.net.Uri;
import android.os.Bundle;
import android.telecom.Call;
import android.telecom.DisconnectCause;
import android.telecom.GatewayInfo;
import android.telecom.PhoneAccountHandle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Represents details of {@link Call.Details}.
 */
public class CallDetail {
    private static final String EXTRA_SCO_STATE = "com.android.bluetooth.hfpclient.SCO_STATE";

    public static final int STATE_AUDIO_ERROR = -1;
    public static final int STATE_AUDIO_DISCONNECTED = 0;
    public static final int STATE_AUDIO_CONNECTING = 1;
    public static final int STATE_AUDIO_CONNECTED = 2;

    private final Call.Details mCallDetails;
    private final String mNumber;
    private final CharSequence mDisconnectCause;
    private final Uri mGatewayInfoOriginalAddress;
    private final long mConnectTimeMillis;
    private final boolean mIsConference;
    private final PhoneAccountHandle mPhoneAccountHandle;
    private final int mScoState;
    private final String mCallerDisplayName;
    private final boolean mIsSelfManaged;

    private CallDetail(@Nullable Call.Details callDetails) {
        mCallDetails = callDetails;
        mNumber = getNumber(callDetails);
        mDisconnectCause = getDisconnectCause(callDetails);
        mGatewayInfoOriginalAddress = getGatewayInfoOriginalAddress(callDetails);
        mConnectTimeMillis = getConnectTimeMillis(callDetails);
        mIsConference = isConferenceCall(callDetails);
        mPhoneAccountHandle = getPhoneAccountHandle(callDetails);
        mScoState = getScoState(callDetails);
        mCallerDisplayName = getCallerDisplayName(callDetails);
        mIsSelfManaged = isSelfManagedCall(callDetails);
    }

    /** Returns if the call supports the given capability. */
    public boolean can(int capability) {
        return mCallDetails != null && mCallDetails.can(capability);
    }

    /**
     * Creates an instance of {@link CallDetail} from a {@link Call.Details}.
     */
    public static CallDetail fromTelecomCallDetail(@Nullable Call.Details callDetail) {
        return new CallDetail(callDetail);
    }

    /**
     * Returns the phone number. Returns empty string if phone number is not available.
     */
    @NonNull
    public String getNumber() {
        return mNumber;
    }

    /**
     * Returns a descriptive reason of disconnect cause.
     */
    @Nullable
    public CharSequence getDisconnectCause() {
        return mDisconnectCause;
    }

    /**
     * Returns the address that the user is trying to connect to via the gateway.
     */
    @Nullable
    public Uri getGatewayInfoOriginalAddress() {
        return mGatewayInfoOriginalAddress;
    }

    /**
     * Returns the timestamp when the call is connected. Returns 0 if detail is not available.
     */
    public long getConnectTimeMillis() {
        return mConnectTimeMillis;
    }

    /**
     * Returns whether the call is a conference.
     */
    public boolean isConference() {
        return mIsConference;
    }

    /** Returns if the call is a self managed call. */
    public boolean isSelfManaged() {
        return mIsSelfManaged;
    }

    /**
     * Returns the SCO state of the call.
     */
    public int getScoState() {
        return mScoState;
    }

    /**
     * Returns the caller display name.
     */
    public String getCallerDisplayName() {
        return mCallerDisplayName;
    }

    /** Returns the {@link PhoneAccountHandle} for this call. */
    @Nullable
    public PhoneAccountHandle getPhoneAccountHandle() {
        return mPhoneAccountHandle;
    }

    private static String getNumber(Call.Details callDetail) {
        String number = "";
        if (callDetail == null) {
            return number;
        }

        GatewayInfo gatewayInfo = callDetail.getGatewayInfo();
        if (gatewayInfo != null) {
            number = gatewayInfo.getOriginalAddress().getSchemeSpecificPart();
        } else if (callDetail.getHandle() != null) {
            number = callDetail.getHandle().getSchemeSpecificPart();
        }
        return number;
    }

    @Nullable
    private static CharSequence getDisconnectCause(Call.Details callDetail) {
        DisconnectCause cause = callDetail == null ? null : callDetail.getDisconnectCause();
        return cause == null ? null : cause.getLabel();
    }

    @Nullable
    private static Uri getGatewayInfoOriginalAddress(Call.Details callDetail) {
        return callDetail != null && callDetail.getGatewayInfo() != null
                ? callDetail.getGatewayInfo().getOriginalAddress()
                : null;
    }

    private static long getConnectTimeMillis(Call.Details callDetail) {
        return callDetail == null ? 0 : callDetail.getConnectTimeMillis();
    }

    private static boolean isConferenceCall(Call.Details callDetail) {
        return callDetail != null && callDetail.hasProperty(Call.Details.PROPERTY_CONFERENCE);
    }

    private static boolean isSelfManagedCall(Call.Details callDetail) {
        return callDetail != null && callDetail.hasProperty(Call.Details.PROPERTY_SELF_MANAGED);
    }

    @Nullable
    private static PhoneAccountHandle getPhoneAccountHandle(Call.Details callDetail) {
        return callDetail == null ? null : callDetail.getAccountHandle();
    }

    @Nullable
    private static String getCallerDisplayName(Call.Details callDetail) {
        if (callDetail != null) {
            String callerDisplayName = callDetail.getCallerDisplayName();
            if (!TextUtils.isEmpty(callerDisplayName)) {
                return callerDisplayName;
            }
            return callDetail.getContactDisplayName();
        }

        return null;
    }

    private static int getScoState(Call.Details callDetail) {
        int state = STATE_AUDIO_ERROR;
        if (callDetail != null) {
            Bundle extras = callDetail.getExtras();
            if (extras != null && extras.containsKey(EXTRA_SCO_STATE)) {
                state = extras.getInt(EXTRA_SCO_STATE);
            }
        }
        return state;
    }
}
