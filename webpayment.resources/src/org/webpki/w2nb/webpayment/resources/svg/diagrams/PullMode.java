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
package org.webpki.w2nb.webpayment.resources.svg.diagrams;

import org.webpki.w2nb.webpayment.resources.svg.SVGAddDouble;
import org.webpki.w2nb.webpayment.resources.svg.SVGAddOffset;
import org.webpki.w2nb.webpayment.resources.svg.SVGCenter;
import org.webpki.w2nb.webpayment.resources.svg.SVGDocument;
import org.webpki.w2nb.webpayment.resources.svg.SVGRect;
import org.webpki.w2nb.webpayment.resources.svg.SVGStaticDouble;
import org.webpki.w2nb.webpayment.resources.svg.SVGStaticString;
import org.webpki.w2nb.webpayment.resources.svg.SVGValue;
import org.webpki.w2nb.webpayment.resources.svg.SVGVerticalLine;

public class PullMode extends SVGDocument {
    SVGStaticDouble linesLength = new SVGStaticDouble(500);
    SVGStaticDouble verticalLineWidth = new SVGStaticDouble(2);
    SVGStaticString verticalLineColor = new SVGStaticString("#0000FF");
    SVGValue lines1_X;
    SVGValue lines2_X;
    SVGValue linesY;
    SVGValue boxOneX;
    SVGValue boxOneY;
    SVGValue lastY;
    SVGValue boxOneWidth;
    SVGValue boxOneHeight;

    @Override
    public double getWidth() {
        return 100;
    }

    @Override
    public double getHeight() {
        return linesLength.getDouble() + linesY.getDouble() + 10;
    }

    @Override
    public void generate() {
        add(new SVGVerticalLine(lines1_X = new SVGStaticDouble(10), 
                                linesY = new SVGStaticDouble(30),
                                linesLength,
                                verticalLineWidth,
                                verticalLineColor));

        add(new SVGRect(boxOneX = new SVGAddOffset(lines1_X, 20),
                        boxOneY = new SVGStaticDouble(20),
                        boxOneWidth = new SVGStaticDouble(20),
                        boxOneHeight = new SVGStaticDouble(20),
                        new SVGStaticDouble(2),
                        new SVGStaticString("#FF0000")));

        add(new SVGVerticalLine(lines2_X = new SVGAddDouble(boxOneX, boxOneWidth, 20), 
                                linesY,
                                linesLength,
                                verticalLineWidth,
                                verticalLineColor));
        
        add(new SVGRect(        new SVGCenter(lines1_X, lines2_X, 10),
                        lastY = new SVGAddDouble(boxOneY, boxOneHeight, 20),
                                new SVGStaticDouble(10),
                                new SVGStaticDouble(20),
                                new SVGStaticDouble(2),
                                new SVGStaticString("#FF0000")));
        
        linesLength.setDouble(lastY.getDouble() + 20 + 10);
    }
}
