#!/bin/sh
# Copyright 2013 The Chromium Authors. All rights reserved.
# Use of this source code is governed by a BSD-style license that can be
# found in the LICENSE file.

# Adapted for the Web2Native Bridge for Firefox by Anders Rundgren

set -e

DIR="$( cd "$( dirname "$0" )" && pwd )"
if [ "$(uname -s)" = "Darwin" ]; then
  CPP=clang
  CPP_OPTION=-lc++
  if [ "$(whoami)" = "root" ]; then
    TARGET_DIR="/Library/Mozilla/NativeMessagingHosts"
  else
    TARGET_DIR="$HOME/Library/Application Support/Mozilla/NativeMessagingHosts"
  fi
else
# Only local user support at the moment
  CPP=g++
  CPP_OPTION=
  TARGET_DIR="$HOME/.mozilla/native-messaging-hosts"
fi

HOST_NAME=org.webpki.w2nb.moz
EXECUTABLE=w2nb-proxy
HOST_PATH=$DIR/../../proxy/install/$EXECUTABLE
MANIFEST=$HOST_NAME.json

echo "Compiling proxy source to $HOST_PATH"
$CPP $CPP_OPTION -w -o $HOST_PATH $DIR/../../proxy/src.cpp/$EXECUTABLE.cpp

# Create directory to store native messaging host.
mkdir -p "$TARGET_DIR"

# Copy native messaging host manifest.
cp "$DIR/../../proxy/install/$MANIFEST" "$TARGET_DIR"

# Update host path in the manifest.
sed -i -e "s%$EXECUTABLE.exe%$HOST_PATH%" "$TARGET_DIR/$MANIFEST"

# Set permissions for the manifest so that all users can read it.
chmod o+r "$TARGET_DIR/$MANIFEST"

echo "Native messaging host $TARGET_DIR/$MANIFEST has been installed"
