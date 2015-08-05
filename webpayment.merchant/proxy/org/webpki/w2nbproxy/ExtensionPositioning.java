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
package org.webpki.w2nbproxy;

import java.io.IOException;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONParser;

import org.webpki.util.Base64URL;

public class ExtensionPositioning {
    
    public class TargetDimensions {
        
        public double targetTop;
        public double targetLeft;
        public double targetWidth;
        public double targetHeight;

        TargetDimensions(double targetTop,
                         double targetLeft,
                         double targetWidth,
                         double targetHeight) {
            this.targetTop = targetTop;
            this.targetLeft = targetLeft;
            this.targetWidth = targetWidth;
            this.targetHeight = targetHeight;
        }
    }
    
    JSONObjectReader positioningArguments;
    
    public static enum HORIZONTAL_ALIGNMENT {Left, Right, Center};
    public static enum VERTICAL_ALIGNMENT   {Top, Bottom, Center};
    
    public static final String HORIZONTAL_ALIGNMENT_JSON = "horizontalAlignment";
    public static final String VERTICAL_ALIGNMENT_JSON   = "verticalAlignment";
    
    public static final String TARGET_TOP_JSON           = "targetTop";
    public static final String TARGET_LEFT_JSON          = "targetLeft";
    public static final String TARGET_WIDTH_JSON         = "targetWidth";
    public static final String TARGET_HEIGHT_JSON        = "targetHeight";
    
    public HORIZONTAL_ALIGNMENT horizontalAlignment;
    public VERTICAL_ALIGNMENT verticalAlignment;
    
    public TargetDimensions targetElement;  // Optional (may be null)

    public ExtensionPositioning(String base64UrlEncodedArguments) throws IOException {
        positioningArguments = JSONParser.parse(Base64URL.decode(base64UrlEncodedArguments));
        horizontalAlignment = HORIZONTAL_ALIGNMENT.valueOf(
                positioningArguments.getString(HORIZONTAL_ALIGNMENT_JSON));
        verticalAlignment = VERTICAL_ALIGNMENT.valueOf(
                positioningArguments.getString(VERTICAL_ALIGNMENT_JSON));
        if (positioningArguments.hasProperty(TARGET_HEIGHT_JSON)) {
            targetElement = new TargetDimensions(positioningArguments.getDouble(TARGET_TOP_JSON),
                                                 positioningArguments.getDouble(TARGET_LEFT_JSON),
                                                 positioningArguments.getDouble(TARGET_WIDTH_JSON),
                                                 positioningArguments.getDouble(TARGET_HEIGHT_JSON));
        }
        positioningArguments.checkForUnread();
    }
    
    public static String encode(HORIZONTAL_ALIGNMENT horizontalAlignment,
                                VERTICAL_ALIGNMENT verticalAlignment,
                                String optionalTargetElementId) {
        return "setExtensionPosition(\"" + horizontalAlignment.toString() +
                                  "\", \"" + verticalAlignment.toString() + "\"" +
                        (optionalTargetElementId == null ? "" : ", \"" + optionalTargetElementId + "\"") + ")";
    }
    
    public static final String SET_EXTENSION_POSITION_FUNCTION_TEXT =
            "function setExtensionPosition(hAlign, vAlign, optionalId) {\n" +
            "    var result = {" + HORIZONTAL_ALIGNMENT_JSON + ":hAlign, " +
                                   VERTICAL_ALIGNMENT_JSON + ":vAlign}\n" +
            "    if (optionalId) {\n" +
            "        var element = document.getElementById(optionalId);\n" +
            "        result." + TARGET_TOP_JSON + " = element.style.top;\n" +
            "        result." + TARGET_LEFT_JSON + " = element.style.left;\n" +
            "        result." + TARGET_WIDTH_JSON + " = element.style.width;\n" +
            "        result." + TARGET_HEIGHT_JSON + " = element.style.height;\n" +
            "    }\n" +
            "    return result;\n" +
            "}\n";

    @Override
    public String toString() {
        return positioningArguments.toString();
    }
}
