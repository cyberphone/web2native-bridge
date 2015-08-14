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
package org.webpki.w2nb.webpayment.resources.svg;

public class SVGText extends SVGObject {
    //   <text text-anchor="middle" font-family="Sans-serif" font-size="12" y="254" x="277.5" fill="#000000">HTTP</text>
    
    public enum TEXT_ANCHOR {START, MIDDLE, END};
    
    String text;

    public SVGText(SVGValue x,
                   SVGValue y,
                   String fontFamily,
                   double fontSize,
                   TEXT_ANCHOR textAnchor,
                   String fillColor,
                   String text) {
        addDouble(SVGAttributes.X, x);
        addDouble(SVGAttributes.Y, y);
        addString(SVGAttributes.FONT_FAMILY, new SVGStringValue(fontFamily));
        addDouble(SVGAttributes.FONT_SIZE, new SVGDoubleValue(fontSize));
        addString(SVGAttributes.TEXT_ANCHOR, new SVGStringValue(textAnchor.toString().toLowerCase()));
        addString(SVGAttributes.FILL_COLOR, new SVGStringValue(fillColor));
        this.text = text;
    }
    
    public SVGText(SVGValue x,
                   SVGValue y,
                   String fontFamily,
                   double fontSize,
                   TEXT_ANCHOR textAnchor,
                   String text) {
        this(x, y, fontFamily, fontSize, textAnchor, "#000000", text);
    }

    @Override
    String getTag() {
        return "text";
    }

    @Override
    boolean hasBody() {
        return true;
    }
    
    @Override
    String getBody() {
        return text;
    }
    public SVGText setDy(String dy) {
        addString(SVGAttributes.DY, new SVGStringValue(dy));
        return this;
    }

    @Override
    double getMaxX() {
         return getAttribute(SVGAttributes.X).getDouble();
    }

    @Override
    double getMaxY() {
        return getAttribute(SVGAttributes.Y).getDouble();
    }
}
