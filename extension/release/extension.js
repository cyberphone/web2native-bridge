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
        port.postMessage({url:request.origin,application:request.application});
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
