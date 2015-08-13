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

public class SVGHorizontalLine extends SVGLine {
    
    public static class Arrow {
        double length;
        double height;
        double gutter;
        
        public Arrow(double length, double height, double gutter) {
            this.length = length;
            this.height = height;
            this.gutter = gutter;
        }
    }
    
    public SVGHorizontalLine(SVGValue x,
                             SVGValue y,
                             SVGValue length,
                             SVGValue strokeWidth,
                             SVGValue strokeColor) {
        super(x, y, new SVGAddDouble(x, length), y, strokeWidth, strokeColor);
    }
    
    public SVGHorizontalLine(SVGVerticalLine vertLine1,
                             SVGVerticalLine vertLine2,
                             SVGValue y,
                             SVGValue strokeWidth,
                             SVGValue strokeColor) {
        this(new SVGAddOffset(vertLine1.getAttribute(SVGAttributes.X1),
                              vertLine1.getAttribute(SVGAttributes.STROKE_WIDTH).getDouble() / 2),
             y,
             new SVGAddOffset(new SVGSubtractDouble(vertLine2.getAttribute(SVGAttributes.X1),
                                                    vertLine1.getAttribute(SVGAttributes.X1)),
                          - (vertLine1.getAttribute(SVGAttributes.STROKE_WIDTH).getDouble() / 2  +
                             vertLine2.getAttribute(SVGAttributes.STROKE_WIDTH).getDouble() / 2)),
             strokeWidth,
             strokeColor);
    }
    
    public SVGHorizontalLine setLeftGutter(double gutter) {
        getAttributes().put(SVGAttributes.X1, new SVGAddOffset(getAttribute(SVGAttributes.X1), gutter));
        return this;
    }

    public SVGHorizontalLine setRightGutter(double gutter) {
        getAttributes().put(SVGAttributes.X2, new SVGAddOffset(getAttribute(SVGAttributes.X2), -gutter));
        return this;
    }

    public SVGHorizontalLine setLeftArrow (Arrow arrow) {
        SVGValue x = new SVGAddOffset(getAttribute(SVGAttributes.X1), arrow.gutter);
        SVGValue y = getAttribute(SVGAttributes.Y1);
        setLeftGutter(arrow.gutter + arrow.length / 2);
        dependencyElements.add(new SVGPolygon(x,
                                              y,
                                              new double[]{0, 0,
                                                           arrow.length,-arrow.height / 2,
                                                           arrow.length, arrow.height / 2},
                                              null,
                                              null,
                                              getAttribute(SVGAttributes.STROKE_COLOR)));
        return this;
    }

    public SVGHorizontalLine setRightArrow (Arrow arrow) {
        SVGValue x = new SVGAddOffset(getAttribute(SVGAttributes.X2), -arrow.gutter);
        SVGValue y = getAttribute(SVGAttributes.Y2);
        setRightGutter(arrow.gutter + arrow.length / 2);
        dependencyElements.add(new SVGPolygon(x,
                                              y,
                                              new double[]{-arrow.length, -arrow.height / 2,
                                                           -arrow.length, arrow.height / 2,
                                                           0, 0},
                                              null,
                                              null,
                                              getAttribute(SVGAttributes.STROKE_COLOR)));
        return this;
    }
}
