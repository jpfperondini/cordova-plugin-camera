#!/usr/bin/env bash
cordova platform remove android
cordova plugin remove cordova-plugin-camera
cordova plugin add ../../cordova-plugin-camera
cordova platform add android
cordova build android
