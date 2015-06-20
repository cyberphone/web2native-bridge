"use strict";

console.debug('Content script loaded');


// Forward the message from inject.js to extension.js
window.addEventListener("message", function(event) {
    // We only accept messages from ourselves
    if (event.source !== window || !event.data.src)
        return;

    // and forward to extension
    if (event.data.src === 'openreq') {
        console.debug('got open request');
        chrome.runtime.sendMessage(event.data, function(response) {
            window.postMessage({res:response,src:'openres'}, '*');
            console.debug('sent!');
        });
    } else if (event.data.src === 'webdis') {
        console.debug('disconnect-gotit!');
        chrome.runtime.sendMessage(event.data);
    } else if (event.data.src === 'webmsg') {
        console.debug('webmsg-gotit!');
        chrome.runtime.sendMessage(event.data);
    } else if (event.data.src === 'natmsg' || event.data.src === 'natdis') {
        console.debug('request-gotit:' + chrome.runtime.id);
    } else {
        console.debug('content-other' + event.data.src);
    }
});

// post messages from extension to injected page
chrome.runtime.onMessage.addListener(function(request, sender, sendResponse) {
    console.debug('Got incoming:' + JSON.stringify(request));
    window.postMessage({req:request,src:request.message ? 'natmsg' : 'natdis'}, '*');
});

// Inject inject.js to the DOM of every page.  A horrible idea but it wasn't mine :-)
var s = document.createElement('script');
s.src = chrome.extension.getURL('inject.js');

// remove script tag after script itself has loaded
s.onload = function() {this.parentNode.removeChild(this);};
(document.head || document.documentElement).appendChild(s);