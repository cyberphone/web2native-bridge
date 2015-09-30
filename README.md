# Web2Native Bridge - Emulator

<table><tr><td><i>Note: this is a system in development, the specification may change anytime without notice</i></td></tr></table>
This repository contains all code for building and testing an emulation of
the Web2Native Bridge concept
(https://cyberphone.github.io/openkeystore/resources/docs/web2native-bridge.pdf)
using the Google **Chrome** desktop browser.  It also runs on the Open Source **Chromium** browser. 

The emulator code exploits Chrome's native messaging (https://developer.chrome.com/extensions/nativeMessaging)
featured in a single universal Chrome extension.

Applications callable by the Web2Native Bridge emulator **must** be written in Java and stored in a for the purpose
dedicated directory.  This limits unpleasant surprises
if you accidentally navigate to a malicious page since
an improperly designed native message extension could enable web access to *any* local application!

### API
The Web2Native Bridge emulator extends the **navigator** object by a *single* method<br>
**nativeConnect**('*Name of target application*' [, *optionalArgument*]) which
returns a JavaScript **Promise** to a **port** object.

The **port** object supports the following methods and events:
* **postMessage**(*message*)
* **disconnect**()
* **addMessageListener**(function(*message*))
* **addDisconnectListener**(function())

*optionalArgument* and *message* must be a valid JSON-serializable JavaScript objects.

An example which could be hosted in an ordinary (*non-privileged*) web page:
```javascript
navigator.nativeConnect('com.example.myapp').then(function(port) {

    port.addMessageListener(function(message) {
        // We got a message from the native application...
    });

    port.addDisconnectListener(function() {
        // Native application disconnected...
    });

    port.postMessage({greeting:'Native app, how are you doing?'});
    // Note: JavaScript serialization makes the above a genuine JSON object

    port.disonnect();  // Not much of a conversation going on here...

}, function(err) {
    console.debug(err);
});
```
The argument to **nativeConnect** holds the name of the specifically adapted local application to invoke.   The current scheme uses a Java-inspired dotted path pointing to a subdirectory and JAR-application having this name.

### Architecture
The Web2Native Bridge emulator always invokes a central proxy located at <code>proxy/install/w2nb-proxy</code>.<br>

The proxy in turn dispatches a call to the specific target application located at<br><code>
proxy/install/apps/</code>*dottedpath*<code>/</code>*dottedpath*<code>.jar</code>.

Common Java libraries may be stored in <code>proxy/install/libs</code>.

For debugging purposes there is also a logging system writing data in <code>proxy/install/logs</code>.

All local I/O is between the browser, proxy and the callable applications
is performed through <code>stdin</code> and <code>stdout</code>.

### Native Application Interface
Native applications (in the prototype Java applications hosted in JAR-files) are called as follows:
<table>
<tr><td>args[0]</td><td>Path to logfile</td></tr>
<tr><td>args[1]</td><td>URL of calling web page</td></tr>
<tr><td>args[2]</td><td>Coordinates of calling web page</td></tr>
<tr><td>args[3]</td><td>Custom invocation data (<i>OptionalArgument</i>)</td></tr>
<tr><td>args[4...]</td><td>Chrome's Native Messaging arguments</td></tr>
</table>
For detailed information about the format of these fields, turn to the code :-)

### Installation
Prerequisites: You need to have Java SE version 7 or 8 installed to run the Web2Native Bridge emulator. OS/X and Linux
installations also presume that **clang** respectively **g++** is available.

1. Install the Web2Native Bridge browser extension from the Chrome Web Store:
https://chrome.google.com/webstore/search/web2native
2. In Chrome, go to Settings->Extensions and check "Allow access to file URLs" for the Web2Native Bridge extension
3. Clone the **web2native-bridge** master repository or just download the ZIP via GitHub to any free directory
4. Start a terminal window and move to <code>proxy/install</code>. Then run the <code>install-proxy</code> script that suits your platform

That's it!  Now you can try the sample application (see next section) since it is installed by default.

Note: If you are using Oracle Java you must install the **Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy Files** as well.

Please don't hesitate contacting me if you run into problems during installation or execution of the emulator!

### Sample Application
The HTML file <code>sample1/demo/sample1.html</code> does approximately
the same thing as the application depicted in
http://www.cnet.com/news/google-paves-over-hole-left-by-chrome-plug-in-ban/
albeit with a few significant enhancements:
* The sample application is invoked by an ordinary web page
* The Web2Native Bridge browser extension is fully generic and can support any number of very different applications
* The Web2Native Bridge adds positioning support enabling alignment UI-wise with the web  

The native part of the sample application resides in <code>proxy/install/apps/org.webpki.w2nb.sample1/org.webpki.w2nb.sample1.jar</code>.

### "Wallet" Application
In addition to the sample application which highlights the core, the installation also makes a digital wallet
available for testing over the web which can be invoking by clicking the URL:
https://test.webpki.org/webpay-merchant

The wallet application features a virtual smart card, signed messages, and is effectively doing the
same thing as a payment terminal + card in a brick-and-mortar shop.

### Security Considerations
Since an emulator *by definition* isn't the "real thing" some limitations apply. That is, the Web2Native Bridge
emulator is *not intended for production* since it doesn't support the following security measures:
* Native application vetting infrastructure
* HTTPS information
* Site-blocking support and associated administration
 
Although not entirely comforting, Chrome's native messaging framework also lacks these qualities...

In addition, the scheme injects code in every web page visited which is a core "feature" of Chrome extensions
slowing down execution.  It is probably wise disabling the extension (using Chrome *settings*) when not using it.
