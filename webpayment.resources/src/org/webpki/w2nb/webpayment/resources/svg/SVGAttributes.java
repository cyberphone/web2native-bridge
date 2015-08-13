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

public enum SVGAttributes {
    X             ("x"),
    Y             ("y"),
    X1            ("x1"),
    X2            ("x2"),
    Y1            ("y1"),
    Y2            ("y2"),
    WIDTH         ("width"),
    HEIGHT        ("height"),
    POINTS        ("points"),
    STROKE_WIDTH  ("stroke-width"),
    FILL_COLOR    ("fill"),
    FILL_OPACITY  ("fill-opacity"),
    STROKE_COLOR  ("stroke"),
    FONT_FAMILY   ("font-family"),
    FONT_SIZE     ("font-size"),
    TEXT_ANCHOR   ("text-anchor");

    String svgNotation;
    
    SVGAttributes(String svgNotation) {
        this.svgNotation = svgNotation;
    }
    
    @Override
    public String toString() {
        return svgNotation;
    }
}

