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

console.log("Extension loaded");

var web2native_bridge = 'org.webpki.w2nb';

var ports = [];

var BASE64URL =
['A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P',
 'Q','R','S','T','U','V','W','X','Y','Z','a','b','c','d','e','f',
 'g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v',
 'w','x','y','z','0','1','2','3','4','5','6','7','8','9','-','_'];

function json2B64(jsonObject) {
    var string = JSON.stringify(jsonObject);
    var binArray = [];
    for (var n = 0; n < string.length; n++) {
        binArray.push(string.charCodeAt(n) & 0xFF);
    }
    var encoded = new String();
    var i = 0;
    var modulo3 = binArray.length % 3;
    while (i < binArray.length - modulo3) {
        encoded += BASE64URL[(binArray[i] >>> 2) & 0x3F];
        encoded += BASE64URL[((binArray[i++] << 4) & 0x30) | ((binArray[i] >>> 4) & 0x0F)];
        encoded += BASE64URL[((binArray[i++] << 2) & 0x3C) | ((binArray[i] >>> 6) & 0x03)];
        encoded += BASE64URL[binArray[i++] & 0x3F];
    }
    if (modulo3 == 1) {
        encoded += BASE64URL[(binArray[i] >>> 2) & 0x3F];
        encoded += BASE64URL[(binArray[i] << 4) & 0x30];
    }
    else if (modulo3 == 2) {
        encoded += BASE64URL[(binArray[i] >>> 2) & 0x3F];
        encoded += BASE64URL[((binArray[i++] << 4) & 0x30) | ((binArray[i] >>> 4) & 0x0F)];
        encoded += BASE64URL[(binArray[i] << 2) & 0x3C];
    }
    return encoded;
}

// Test that the native proxy is alive and kicking
chrome.runtime.onStartup.addListener(function() {
    chrome.runtime.sendNativeMessage(web2native_bridge, {proxyVersion:"1.00"}, function(response) {
        if (response) {
            console.log("Extension successfully verified");
        } else {
            alert('Web2Native Bridge library issue: ' + chrome.runtime.lastError.message);
        }
    });
});

function getPort(sender) {
    var port = ports[sender.tab.id];
    if (!port) {
        alert('Missing port for: ' + sender.tab.id);
    }
    return port;
}

function getDisconnectPort(sender) {
    var port = getPort(sender);
    if (port) {
        delete ports[sender.tab.id];
    }
    return port;
}

// When message is received from page send it to native
chrome.runtime.onMessage.addListener(function(request, sender, sendResponse) {
    if(sender.id !== chrome.runtime.id || !sender.tab) {
        alert('Internal error');
        return;
    }
    if (request.src === 'openreq') {
        var port = chrome.runtime.connectNative(web2native_bridge);
        ports[sender.tab.id] = port;
        port.onMessage.addListener(function(message) {
            // DEBUG
            chrome.tabs.sendMessage(sender.tab.id, {message:message,tabid:sender.tab.id});
        });
        port.onDisconnect.addListener(function() {
            // DEBUG
            getDisconnectPort(sender);
            chrome.tabs.sendMessage(sender.tab.id, {disconnect:true,tabid:sender.tab.id});
        });
        // DEBUG
        port.postMessage({url:request.origin,
                          application:request.application,
                          windowB64:json2B64(request.window),
                          argumentsB64:json2B64(request.arguments)});
        sendResponse({success:sender.tab.id});
    } else if (request.src === 'webdis') {
        // DEBUG
        getDisconnectPort(sender).disconnect();
    } else if (request.src === 'webmsg') {
        // DEBUG
        getPort(sender).postMessage(request.message);
    } else {
        sendResponse({err:{}});
    }
});
