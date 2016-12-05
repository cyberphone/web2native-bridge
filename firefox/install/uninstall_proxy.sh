#!/bin/sh
# Copyright 2013 The Chromium Authors. All rights reserved.
# Use of this source code is governed by a BSD-style license that can be
# found in the LICENSE file.

# Adapted for the Web2Native Bridge for Firefox by Anders Rundgren

set -e

DIR="$( cd "$( dirname "$0" )" && pwd )"
if [ "$(uname -s)" = "Darwin" ]; then
  if [ "$(whoami)" = "root" ]; then
    TARGET_DIR="/Library/Mozilla/NativeMessagingHosts"
  else
    TARGET_DIR="$HOME/Library/Application Support/Mozilla/NativeMessagingHosts"
  fi
else
# Only local user support at the moment
  TARGET_DIR="$HOME/.mozilla/native-messaging-hosts"
fi

HOST_NAME=org.webpki.w2nb.moz
MANIFEST=$HOST_NAME.json

rm "$TARGET_DIR/$MANIFEST"
echo "Native messaging host $TARGET_DIR/$MANIFEST has been uninstalled."

