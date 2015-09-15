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
package org.webpki.w2nb.webpayment.common;

import java.awt.Color;

import java.io.IOException;

public enum AccountTypes {

    SUPER_CARD   (true,  "https://supercard.com",  "SuperCard",   "supercard.png",   Color.BLUE), 
    BANK_DIRECT  (false, "https://bankdirect.net", "Bank Direct", "bankdirect.png",  Color.BLACK),
    UNUSUAL_CARD (false, "https://usualcard.com",  "UnusualCard", "unusualcard.png", Color.GRAY);

    boolean acquirerBased;  // True => card processor model, false = > 3 or 4 corner distributed model
    String type;            // A brand URI
    String commonName;      // What it is usually called
    String imageName;
    Color fontColor;
    
    AccountTypes (boolean acquirerBased, String type, String commonName, String imageName, Color fontColor) {
        this.acquirerBased = acquirerBased;
        this.type = type;
        this.commonName = commonName;
        this.imageName = imageName;
        this.fontColor = fontColor;
    }

    public boolean isAcquirerBased() {
        return acquirerBased;
    }

    public String getType() {
        return type;
    }

    public String getImageName() {
        return imageName;
    }
    public String getCommonName() {
        return commonName;
    }

    public Color getFontColor() {
        return fontColor;
    }
    public static AccountTypes fromType(String type) throws IOException {
        for (AccountTypes accountType : AccountTypes.values()) {
            if (accountType.type.equals(type)) {
                return accountType;
            }
        }
        throw new IOException("No such account type: " + type);
    }
}
