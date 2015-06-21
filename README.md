# Web2Native Bridge - Emulator
This repository contains all code for building and testing an emulation of
the Web2Native Bridge concept (https://cyberphone.github.io/openkeystore/resources/docs/web2native-bridge.pdf)
on the Chrome (desktop) browser.  It exploits Chrome's native messaging (https://developer.chrome.com/extensions/nativeMessaging) and a single universal Chrome extension.

The called applications must be written in Java and stored in a specific directory.  This ensures that you will not be subjected to unwanted surprises if somebody succeeds making you navigate to a malicious page.
