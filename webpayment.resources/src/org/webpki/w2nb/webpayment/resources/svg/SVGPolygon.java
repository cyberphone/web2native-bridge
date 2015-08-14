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
    SVGValue x;
    SVGValue y;
    
    class SVGPointsValue extends SVGValue {
 
        public SVGPointsValue() {
        }

        @Override
        public String getStringRepresentation() {
            int i = 0;
            StringBuffer result = new StringBuffer();
            while (i < coordinates.length) {
                if (i > 0) {
                    result.append(' ');
                }
                result.append(niceDouble(x.getDouble() + coordinates[i++]))
                      .append(',')
                      .append(niceDouble(y.getDouble() + coordinates[i++]));
            }
            return result.toString();
        }
    };
    
   
    public SVGPolygon(SVGValue x,
                      SVGValue y,
                      double[] coordinates,
                      Double strokeWidth,
                      String strokeColor,
                      String fillColor) {
        if ((coordinates.length & 1) != 0) {
            throw new RuntimeException("Wrong number of points");
        }
        if (strokeWidth == null ^ strokeColor == null) {
            throw new RuntimeException("You must either specify color+stroke or nulls");
        }
        if (strokeWidth == null) {
            addString(SVGAttributes.STROKE_COLOR, new SVGStringValue("none"));
        } else {
            addDouble(SVGAttributes.STROKE_WIDTH, new SVGDoubleValue(strokeWidth));
        }
        if (strokeColor != null) {
            addString(SVGAttributes.STROKE_COLOR, new SVGStringValue(strokeColor));
        }
        addString(SVGAttributes.FILL_COLOR, fillColor == null ? new SVGStringValue("none") : new SVGStringValue(fillColor));
        addPoints(SVGAttributes.POINTS, new SVGPointsValue());
        this.coordinates = coordinates;
        this.x = x;
        this.y = y;
      }

    @Override
    SVGValue getAnchorX() {
        return new SVGAddOffset(x, coordinates[0]);
    }
    
    @Override
    SVGValue getAnchorY() {
        return new SVGAddOffset(y, coordinates[1]);
    }

    @Override
    String getTag() {
        return "polygon";
    }

    @Override
    boolean hasBody() {
        return false;
    }

    private double maxCoordinate(int start) {
        double max = -10000;
        while (start < coordinates.length) {
            if (coordinates[start] > max) {
                max = coordinates[start];
            }
            start += 2;
        }
        return max;
    }

    @Override
    double getMaxX() {
        x = new SVGAddOffset(x, SVGDocument.marginX);
        return x.getDouble() + maxCoordinate(0);
    }

    @Override
    double getMaxY() {
        y = new SVGAddOffset(y, SVGDocument.marginY);
        return y.getDouble() + maxCoordinate(1);
    }
    
}
