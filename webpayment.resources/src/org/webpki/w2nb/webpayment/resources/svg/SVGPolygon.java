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

public class SVGPolygon extends SVGObject {

    double[] coordinates;
    
    public SVGPolygon(SVGValue x,
                      SVGValue y,
                      double[] coordinates,
                      SVGValue strokeWidth,
                      SVGValue strokeColor,
                      SVGValue fillColor) {
        if (strokeWidth == null ^ strokeColor == null) {
            throw new RuntimeException("You must either specify color+stroke or nulls");
        }
        if (strokeWidth == null) {
            addString(SVGAttributes.STROKE_COLOR, new SVGStringValue("none"));
        } else {
            addDouble(SVGAttributes.STROKE_WIDTH, strokeWidth);
        }
        if (strokeColor != null) {
            addString(SVGAttributes.STROKE_COLOR, strokeColor);
        }
        addString(SVGAttributes.FILL_COLOR, fillColor == null ? new SVGStringValue("none") : fillColor);
        addPoints(SVGAttributes.POINTS, new SVGPointsValue(x, y, coordinates));
        this.coordinates = coordinates;
      }

    @Override
    SVGValue getAnchorX() {
        return new SVGAddOffset(getAttribute(SVGAttributes.POINTS), coordinates[0]);
    }
    
    @Override
    SVGValue getAnchorY() {
        return new SVGAddOffset(getAttribute(SVGAttributes.POINTS), coordinates[1]);
    }

    @Override
    String getTag() {
        return "polygon";
    }

    @Override
    boolean hasBody() {
        return false;
    }
}
