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

// DEBUG

// Forward the message from inject.js to extension.js
window.addEventListener("message", function(event) {
    // We only accept messages from ourselves
    if (event.source !== window || !event.data.src)
        return;

    // and forward to extension
    if (event.data.src === 'openreq') {
        // DEBUG
        chrome.runtime.sendMessage(event.data, function(response) {
            window.postMessage({res:response,src:'openres'}, '*');
            // DEBUG
        });
    } else if (event.data.src === 'webdis') {
        // DEBUG
        chrome.runtime.sendMessage(event.data);
    } else if (event.data.src === 'webmsg') {
        // DEBUG
        chrome.runtime.sendMessage(event.data);
    } else if (event.data.src === 'natmsg' || event.data.src === 'natdis') {
        // DEBUG
    } else {
        // DEBUG
    }
});

// post messages from extension to injected page
chrome.runtime.onMessage.addListener(function(request, sender, sendResponse) {
    // DEBUG
    window.postMessage({req:request,src:request.message ? 'natmsg' : 'natdis'}, '*');
});

// Inject inject.js to the DOM of every page.  A horrible idea but it wasn't mine :-)
var s = document.createElement('script');
s.src = chrome.extension.getURL('inject.js');

// remove script tag after script itself has loaded
s.onload = function() {this.parentNode.removeChild(this);};
(document.head || document.documentElement).appendChild(s);