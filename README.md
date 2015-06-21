# Web2Native Bridge - Emulator
This repository contains all code for building and testing an emulation of
the Web2Native Bridge concept
(https://cyberphone.github.io/openkeystore/resources/docs/web2native-bridge.pdf)
on the Chrome (desktop) browser.

The emulator code exploits Chrome's native messaging (https://developer.chrome.com/extensions/nativeMessaging)
featured in a single universal Chrome extension.

The applications callable by Web2Native Bridge emulator must be written in Java and stored in a specific
directory.  This ensures that you will not be subjected to unpleasant
surprises (an improperly designed native message extension could enable access to any application!)
if somebody succeeds making you navigate to a malicious page.
