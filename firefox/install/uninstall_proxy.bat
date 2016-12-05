:: Copyright 2014 The Chromium Authors. All rights reserved.
:: Use of this source code is governed by a BSD-style license that can be
:: found in the LICENSE file.

:: Adapted for the Web2Native Bridge emulator fo Firefox by A.Rundgren

:: Deletes the entries created by install_proxy.bat
REG DELETE "HKCU\Software\Mozilla\NativeMessagingHosts\org.webpki.w2nb.moz" /f
