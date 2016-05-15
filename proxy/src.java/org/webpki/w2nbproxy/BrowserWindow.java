/*
 *  Copyright 2006-2016 WebPKI.org (http://webpki.org).
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

package org.webpki.w2nbproxy;

import java.io.IOException;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONParser;

import org.webpki.util.Base64URL;

// This class represents the calling window argument provided by naigator.nativeConnect
// It must match "inject.js"

public class BrowserWindow {

    public double screenWidth;

    public double screenHeight;

    public double x;

    public double y;

    public double outerWidth;

    public double outerHeight;

    public double innerWidth;

    public double innerHeight;

    JSONObjectReader browserWindow;

    public BrowserWindow(String base64UrlEncodedParameters) throws IOException {
        browserWindow = JSONParser.parse(Base64URL.decode(base64UrlEncodedParameters));
        if (browserWindow.hasProperty("screenWidth")) {
            screenWidth = browserWindow.getDouble("screenWidth");
            screenHeight = browserWindow.getDouble("screenHeight");
            x = browserWindow.getDouble("windowX");
            y = browserWindow.getDouble("windowY");
            outerWidth = browserWindow.getDouble("windowOuterWidth");
            outerHeight = browserWindow.getDouble("windowOuterHeight");
            innerWidth = browserWindow.getDouble("windowInnerWidth");
            innerHeight = browserWindow.getDouble("windowInnerHeight");
        }
        browserWindow.checkForUnread();
    }

    @Override
    public String toString() {
        return browserWindow.toString();
    }
}
