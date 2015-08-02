/*
 *  Copyright 2006-2015 WebPKI.org (http://webpki.org).
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
 
 "use strict";

console.debug('Injected page loaded');

var _promise;

var ports = [];

var org = org || {};
org.webpki = org.webpki || {};

org.webpki.port = function(tabid) {
   this.tabid = tabid;
   ports[tabid] = this;
};

org.webpki.port.prototype.addMessageListener = function(callback) {
   this.messageCallback = callback;
};

org.webpki.port.prototype.addDisconnectListener = function(callback) {
   this.disconnectCallback = callback;
};

org.webpki.port.prototype.disconnect = function() {
    var msg = {};
    msg.src = 'webdis';
    msg.tabid = this.tabid;
    window.postMessage(msg, '*');
};

org.webpki.port.prototype.postMessage = function(message) {
    var msg = {};
    msg.src = 'webmsg';
    msg.tabid = this.tabid;
    msg.message = message;
    window.postMessage(msg, '*');
};

// Forward the message from extension.js to inject.js
window.addEventListener("message", function(event) {
    // We only accept messages from ourselves
    if (event.source !== window || !event.data.src) 
        return;

    // and forward to extension
    if (event.data.src === "openres") {
        console.debug('got open return');
        if (event.data.res.success) {
            _promise.resolve(new org.webpki.port(event.data.res.success));
        } else if (event.data.res.err) {
            _promise.reject(event.data.res.err);
        } else {
            _promise.reject("Internal error");
        }
        delete document._promise;
    } else if (event.data.src === "natmsg") {
        console.debug('inject req:' + JSON.stringify(event.data.req));
        if (ports[event.data.req.tabid].messageCallback) {
            ports[event.data.req.tabid].messageCallback(event.data.req.message);
        } else {
            console.debug('missing listener: ' + JSON.stringify(event.data.req));
        }
    } else if (event.data.src === "natdis") {
        console.debug('inject disconnect:' + JSON.stringify(event.data.req));
        if (ports[event.data.req.tabid].disconnectCallback) {
            ports[event.data.req.tabid].disconnectCallback();
        } else {
            console.debug('missing listener: ' + JSON.stringify(event.data.req));
        }
    } else if (event.data.src !== "webdis") {
        console.debug('page-other: ' + event.data.src);
    }
});

navigator.nativeConnect = function(applicationName, optionalArguments) {
    return new Promise(function(resolve, reject) {
        var msg = {};
        msg.src = 'openreq';
        msg.origin = location.href;
        msg.application = applicationName;
        msg.window = {screenWidth: window.screen.width,
                      screenHeight: window.screen.height,
                      windowX: window.screenX,
                      windowY: window.screenY,
                      windowOuterWidth: window.outerWidth,
                      windowOuterHeight: window.outerHeight,
                      windowInnerWidth: window.innerWidth,
                      windowInnerHeight: window.innerHeight};
        msg.arguments = optionalArguments ? optionalArguments : {};
        window.postMessage(msg, '*');
        _promise = {resolve: resolve, reject: reject};
    });
};

