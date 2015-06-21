# Web2Native Bridge - Emulator
This repository contains all code for building and testing an emulation of
the Web2Native Bridge concept
(https://cyberphone.github.io/openkeystore/resources/docs/web2native-bridge.pdf)
on the Chrome (desktop) browser.

The emulator code exploits Chrome's native messaging (https://developer.chrome.com/extensions/nativeMessaging)
featured in a single universal Chrome extension.

Applications callable by the Web2Native Bridge emulator **must** be written in Java and stored in a for the purpose
dedicated directory.  This limits unpleasant surprises
(an improperly designed native message extension could enable access to *any* application!)
if you accindentally navigate to a malicious page.
## API
The Web2Native Bridge emulator extends the **navigator** object by a *single* method **nativeConnect**(*NameOfTargetApplication*) which
returns a promise holding a **port** object.

The **port** object supports the following methods and events:
* **postMessage**(*JSONObject*)
* **disconnect**()
* **addMessageListener**(function(*JSONObject*))
* **addDisconnectListener**(function())

An example which could be hosted in an ordinary (*non-privileged*) web page:
```javascript
navigator.nativeConnect('com.example.w2nb.sample').then(function(port) {

    port.addMessageListener(function(message) {
        // We got a message from the native application...
    });

    port.addDisconnectListener(function() {
        // Native application disconnected...
    });

    port.postMessage({greeting:'Native app, how are you doing?'});
    // Note: JS serialization makes the above a genuine JSON object

    port.disonnect();  // Not much of a conversation going on here...

}, function(err) {
    console.debug(err);
});
```
The argument to **nativeConnect** holds the name of the specifically adapted local application to invoke.   The current scheme uses a Java-inspired path pointing to a subdirectory and JAR-application having this name.
