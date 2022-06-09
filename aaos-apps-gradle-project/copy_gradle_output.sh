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
cd ../../../../../out/aaos-apps-gradle-build/ || exit


# APKs
cp car-dialer-app/outputs/apk/production/release/car-dialer-app-production-release-unsigned.apk $1/CarDialerApp.apk
cp car-media-app/outputs/apk/release/car-media-app-release-unsigned.apk $1/CarMediaApp.apk
cp car-messenger-app/outputs/apk/release/car-messenger-app-release-unsigned.apk $1/CarMessengerApp.apk
cp PaintBooth/outputs/apk/release/PaintBooth-release-unsigned.apk $1/PaintBooth.apk
cp test-rotary-playground/outputs/apk/release/test-rotary-playground-release-unsigned.apk $1/RotaryPlayground.apk
# ??? TestMediaApp
# TODO: create gradle project for RotaryIME

# AARs
cp car-ui-lib/outputs/aar/car-ui-lib-release.aar $1/car-ui-lib.aar
cp car-uxr-client-lib/outputs/aar/car-uxr-client-lib-release.aar $1/car-uxr-client-lib.aar
cp car-assist-lib/outputs/aar/car-assist-lib-release.aar $1/car-assist-lib.aar
cp car-apps-common/outputs/aar/car-apps-common-release.aar $1/car-apps-common.aar
cp car-media-common/outputs/aar/car-media-common-release.aar $1/car-media-common.aar
cp car-telephony-common/outputs/aar/car-telephony-common-release.aar $1/car-telephony-common-aar

# ??? car-ui-lib-oem-apis-jar
# ??? car-ui-lib-testing-support-aar
# ??? car-messaging-models-aar

# TODO CarDialerUnitTests, CarDialerAppForTesting, CarMessengerUnitTests, CarRotaryLibUnitTests, CarTelephonyLibTests, CarUILibUnitTests, car-apps-common-unit-tests
