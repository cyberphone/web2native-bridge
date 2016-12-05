#!/bin/sh
# Copyright 2013 The Chromium Authors. All rights reserved.
# Use of this source code is governed by a BSD-style license that can be
# found in the LICENSE file.

# Adapted for the Web2Native Bridge by Anders Rundgren

set -e

DIR="$( cd "$( dirname "$0" )" && pwd )"
if [ "$(uname -s)" = "Darwin" ]; then
  CPP=clang
  CPP_OPTION=-lc++
  if [ "$(whoami)" = "root" ]; then
    TARGET_DIR="/Library/Google/Chrome/NativeMessagingHosts"
  else
    TARGET_DIR="$HOME/Library/Application Support/Google/Chrome/NativeMessagingHosts"
  fi
else
  CPP=g++
  CPP_OPTION=
  if [ "$(whoami)" = "root" ]; then
    TARGET_DIR="/etc/opt/chrome/native-messaging-hosts"
  else
    if [ -d "$HOME/.config/google-chrome" ]; then
      CHROME_VARIANT=google-chrome
      if [ -d "$HOME/.config/chromium" ]; then
        echo "You have both Chrome and Chromium installed. Please patch the script!"
        exit 1
      fi
    else 
      if [ -d "$HOME/.config/chromium" ]; then
        CHROME_VARIANT=chromium
      else
        echo "Can't find any Chrome variant!"
        exit 1
      fi
    fi
    TARGET_DIR="$HOME/.config/$CHROME_VARIANT/NativeMessagingHosts"
  fi
fi

HOST_NAME=org.webpki.w2nb
EXECUTABLE=w2nb-proxy
HOST_PATH=$DIR/$EXECUTABLE
MANIFEST=$HOST_NAME.json

echo "Compiling proxy source to $HOST_PATH"
$CPP $CPP_OPTION -w -o $HOST_PATH $DIR/../src.cpp/$EXECUTABLE.cpp

# Create directory to store native messaging host.
mkdir -p "$TARGET_DIR"

# Copy native messaging host manifest.
cp "$DIR/$MANIFEST" "$TARGET_DIR"

# Update host path in the manifest.
sed -i -e "s%$EXECUTABLE.exe%$HOST_PATH%" "$TARGET_DIR/$MANIFEST"

# Set permissions for the manifest so that all users can read it.
chmod o+r "$TARGET_DIR/$MANIFEST"

echo "Native messaging host $TARGET_DIR/$MANIFEST has been installed"
