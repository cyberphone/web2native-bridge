:: Copyright 2014 The Chromium Authors. All rights reserved.
:: Use of this source code is governed by a BSD-style license that can be
:: found in the LICENSE file.

:: Adapted for the Web2Native Bridge emulator by A.Rundgren

:: Change HKCU to HKLM if you want to install globally.
:: %~dp0 is the directory containing this bat script and ends with a backslash.
REG ADD "HKCU\Software\Google\Chrome\NativeMessagingHosts\org.webpki.w2nb" /ve /t REG_SZ /d "%~dp0org.webpki.w2nb.json" /f
COPY /Y "%~dp0..\build-proxy-windows\Debug\w2nb-proxy.exe" "%~dp0"
