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
            console.debug('rec: ' + JSON.stringify(message));
            chrome.tabs.sendMessage(sender.tab.id, {message:message,tabid:sender.tab.id});
        });
        port.onDisconnect.addListener(function() {
            console.debug('native disconnect');
            getDisconnectPort(sender);
            chrome.tabs.sendMessage(sender.tab.id, {disconnect:true,tabid:sender.tab.id});
        });
        console.debug('connect: ' + JSON.stringify(request));
        port.postMessage({url:request.origin,application:request.application});
        sendResponse({success:sender.tab.id});
    } else if (request.src === 'webdis') {
        console.debug('web disconnect');
        getDisconnectPort(sender).disconnect();
    } else if (request.src === 'webmsg') {
        console.debug('web message');
        getPort(sender).postMessage(request.message);
    } else {
        sendResponse({err:{}});
    }
});
