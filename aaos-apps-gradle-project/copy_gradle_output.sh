#!/bin/bash

# Copies the aar and apks
# Usage: copy_gradle_output.sh DEST_DIR

if [ -z "${1+x}" ]
then
    echo "destination directory is required"
    exit 1
fi

if [[ -f $1 ]]
then
    echo "target $1 exists as a file!!"
    exit 1
elif [[ ! -d $1 ]]
then
    echo "creating $1 directory"
    mkdir $1
else
    echo "$1 directory already there"
fi

cd "$(dirname "$0")" || exit
# Keep in sync with ./build.gradle
OUTPUT_DIR=../../../../../out/aaos-apps-gradle-build

# APKs
cp $OUTPUT_DIR/car-dialer-app/outputs/apk/production/release/car-dialer-app-production-release-unsigned.apk $1/CarDialerApp.apk
cp $OUTPUT_DIR/car-media-app/outputs/apk/release/car-media-app-release-unsigned.apk $1/CarMediaApp.apk
cp $OUTPUT_DIR/car-messenger-app/outputs/apk/release/car-messenger-app-release-unsigned.apk $1/CarMessengerApp.apk
cp $OUTPUT_DIR/PaintBooth/outputs/apk/release/PaintBooth-release-unsigned.apk $1/PaintBooth.apk
cp $OUTPUT_DIR/test-rotary-playground/outputs/apk/release/test-rotary-playground-release-unsigned.apk $1/RotaryPlayground.apk
# ??? TestMediaApp
# TODO: create gradle project for RotaryIME

# AARs
cp $OUTPUT_DIR/car-ui-lib/outputs/aar/car-ui-lib-release.aar $1/car-ui-lib.aar
cp $OUTPUT_DIR/car-uxr-client-lib/outputs/aar/car-uxr-client-lib-release.aar $1/car-uxr-client-lib.aar
cp $OUTPUT_DIR/car-assist-lib/outputs/aar/car-assist-lib-release.aar $1/car-assist-lib.aar
cp $OUTPUT_DIR/car-apps-common/outputs/aar/car-apps-common-release.aar $1/car-apps-common.aar
cp $OUTPUT_DIR/car-media-common/outputs/aar/car-media-common-release.aar $1/car-media-common.aar
cp $OUTPUT_DIR/car-telephony-common/outputs/aar/car-telephony-common-release.aar $1/car-telephony-common-aar

# ??? car-ui-lib-oem-apis-jar
# ??? car-ui-lib-testing-support-aar
# ??? car-messaging-models-aar

# TODO CarMessengerUnitTests, CarRotaryLibUnitTests, CarUILibUnitTests, car-apps-common-unit-tests
cp $OUTPUT_DIR/car-dialer-app/outputs/apk/emulator/debug/car-dialer-app-emulator-debug.apk $1/CarDialerAppForTesting.apk
cp $OUTPUT_DIR/car-dialer-app/outputs/apk/androidTest/emulator/debug/car-dialer-app-emulator-debug-androidTest.apk $1/CarDialerUnitTests.apk
cp $OUTPUT_DIR/car-telephony-common/outputs/apk/androidTest/debug/car-telephony-common-debug-androidTest.apk $1/CarTelephonyLibTests.apk
