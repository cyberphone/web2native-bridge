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

public class SVGPointsValue extends SVGValue {
    
    SVGValue x;
    SVGValue y;
    double[] points;

    public SVGPointsValue(SVGValue x, SVGValue y, double[] points) {
        this.x = x;
        this.y = y;
        this.points = points;
        if ((points.length & 1) != 0) {
            throw new RuntimeException("Wrong number of points");
        }
    }

    @Override
    public String getStringRepresentation() {
        int i = 0;
        StringBuffer result = new StringBuffer();
        while (i < points.length) {
            if (i > 0) {
                result.append(' ');
            }
            result.append(niceDouble(x.getDouble() + points[i++]))
                  .append(',')
                  .append(niceDouble(y.getDouble() + points[i++]));
        }
        return result.toString();
    }
};

