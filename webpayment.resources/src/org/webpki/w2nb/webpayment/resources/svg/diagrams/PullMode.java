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
import org.webpki.w2nb.webpayment.resources.svg.SVGHorizontalLine;
import org.webpki.w2nb.webpayment.resources.svg.SVGPolygon;
import org.webpki.w2nb.webpayment.resources.svg.SVGRect;
import org.webpki.w2nb.webpayment.resources.svg.SVGDoubleValue;
import org.webpki.w2nb.webpayment.resources.svg.SVGStringValue;
import org.webpki.w2nb.webpayment.resources.svg.SVGText;
import org.webpki.w2nb.webpayment.resources.svg.SVGValue;
import org.webpki.w2nb.webpayment.resources.svg.SVGVerticalLine;

public class PullMode extends SVGDocument {
    SVGDoubleValue linesLength = new SVGDoubleValue(500);
    SVGDoubleValue verticalLineWidth = new SVGDoubleValue(2);
    SVGStringValue verticalLineColor = new SVGStringValue("#0000FF");
    SVGVerticalLine vertLine1;
    SVGVerticalLine vertLine2;
    
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
        return 200;
    }

    @Override
    public double getHeight() {
        return linesLength.getDouble() + linesY.getDouble() + 10;
    }

    @Override
    public void generate() {
        add(vertLine1 = new SVGVerticalLine(lines1_X = new SVGDoubleValue(10), 
                                linesY = new SVGDoubleValue(30),
                                linesLength,
                                verticalLineWidth,
                                verticalLineColor));

        add(new SVGRect(boxOneX = new SVGAddOffset(lines1_X, 20),
                        boxOneY = new SVGDoubleValue(20),
                        boxOneWidth = new SVGDoubleValue(80),
                        boxOneHeight = new SVGDoubleValue(20),
                        new SVGDoubleValue(2.5),
                        new SVGStringValue("#FF0000"),
                        null).setRadiusX(3).setRadiusY(3));

        add(vertLine2 = new SVGVerticalLine(lines2_X = new SVGAddDouble(boxOneX, boxOneWidth, 20), 
                                linesY,
                                linesLength,
                                verticalLineWidth,
                                verticalLineColor));
        
        add(new SVGRect(        new SVGCenter(lines1_X, lines2_X, 10),
                                new SVGAddDouble(boxOneY, boxOneHeight, 20),
                                new SVGDoubleValue(10),
                                new SVGDoubleValue(20),
                                null,
                                null,
                                new SVGStringValue("#00FF00")));
        
        add(new SVGText(new SVGCenter(lines1_X, lines2_X),
                        lastY = new SVGDoubleValue(80),
                        new SVGStringValue("Sans-serif"),
                        new SVGDoubleValue(10),
                        SVGText.TEXT_ANCHOR.MIDDLE,
                        "Hi There!"));
/*
    public SVGHorizontalLine(SVGVerticalLine vertLine1,
                             SVGVerticalLine vertLine2,
                             SVGValue y,
                             SVGValue strokeWidth,
                             SVGValue strokeColor) {
        super(vertLine1.getAttribute(SVGAttributes.X1),
        
 */
        add(new SVGHorizontalLine(vertLine1, 
                                  vertLine2,
                                  new SVGDoubleValue(90),
                                  new SVGDoubleValue(0.8),
                                  new SVGStringValue("#000000")).setLeftArrow(new SVGHorizontalLine.Arrow(4, 3, 0.5)));

        add(new SVGHorizontalLine(vertLine1, 
                vertLine2,
                new SVGDoubleValue(92),
                new SVGDoubleValue(0.8),
                new SVGStringValue("#000000")).setRightArrow(new SVGHorizontalLine.Arrow(4, 3, 0.5)));

        add(new SVGHorizontalLine(vertLine1, 
                vertLine2,
                new SVGDoubleValue(95),
                new SVGDoubleValue(0.8),
                new SVGStringValue("#000000")).setLeftGutter(2));

        add(new SVGPolygon(        new SVGCenter(lines1_X, lines2_X),
                new SVGDoubleValue(60),
                new double[]{-5,-5,-5, 5, 5, 0},
                null,
                null,
                new SVGStringValue("#80ff80")));

        add(new SVGHorizontalLine(vertLine1, 
                                  vertLine2,
                                  new SVGDoubleValue(140),
                                  new SVGDoubleValue(0.8),
                                  new SVGStringValue("#000000"))
                    .setLeftArrow(new SVGHorizontalLine.Arrow(4, 3, 0.5))
                    .setRect(new SVGHorizontalLine.Rect(null,
                                                        null,
                                                        new SVGDoubleValue(44),
                                                        new SVGDoubleValue(16),
                                                        null,
                                                        null,
                                                        new SVGStringValue("#0c960c")
                    ).setRadiusX(4).setRadiusY(4))
                    .setText(new SVGHorizontalLine.Text(null,
                                                        null,
                                                        new SVGStringValue("Sans-serif"),
                                                        new SVGDoubleValue(12),
                                                        new SVGStringValue("#FFFFFF"),
                                                        "W2NB")));

        add(new SVGHorizontalLine(vertLine1, 
                                  vertLine2,
                                  new SVGDoubleValue(170),
                                  new SVGDoubleValue(0.8),
                                  new SVGStringValue("#000000"))
                    .setLeftArrow(new SVGHorizontalLine.Arrow(4, 3, 0.5))
                    .setRect(new SVGHorizontalLine.Rect(null,
                                      null,
                                      new SVGDoubleValue(44),
                                      new SVGDoubleValue(14),
                                      new SVGDoubleValue(0.5),
                                      new SVGStringValue("#000000"),
                                      new SVGStringValue("#FFFFFF")
  ).setRadiusX(4).setRadiusY(4))
  .setText(new SVGHorizontalLine.Text(null,
                                      null,
                                      new SVGStringValue("Sans-serif"),
                                      new SVGDoubleValue(12),
                                      new SVGStringValue("#000000"),
                                      "HTTP")));
        add(new SVGHorizontalLine(vertLine1, 
                vertLine2,
                new SVGDoubleValue(200),
                new SVGDoubleValue(0.8),
                new SVGStringValue("#000000"))
  .setLeftArrow(new SVGHorizontalLine.Arrow(4, 3, 0.5))
.setText(new SVGHorizontalLine.Text(10.0,
                    5.0,
                    new SVGStringValue("Sans-serif"),
                    new SVGDoubleValue(12),
                    new SVGStringValue("#000000"),
                    "Some text")));

        linesLength.setDouble(lastY.getDouble() + 200);
    }
}
