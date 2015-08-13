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

import java.util.LinkedHashMap;
import java.util.Vector;

public abstract class SVGObject {
    
    private LinkedHashMap<SVGAttributes,SVGValue> _attributes = new LinkedHashMap<SVGAttributes,SVGValue>();
    
    Vector<SVGObject> dependencyElements = new Vector<SVGObject>();
    
    abstract String getTag();
    
    abstract boolean hasBody();
    
    String getBody() {
        throw new RuntimeException("Unexptected call to getBody() by " + this.getClass().getCanonicalName());
    }
    
    LinkedHashMap<SVGAttributes,SVGValue> getAttributes() {
        return _attributes;
    }
    
    SVGValue getAttribute(SVGAttributes attribute) {
        return _attributes.get(attribute);
    }
    
    private void _addAttribute(SVGAttributes svgAttribute, SVGValue svgValue) {
        if (_attributes.put(svgAttribute, svgValue) != null) {
            throw new RuntimeException("Trying to assign attribute multiple times: " + svgAttribute);
        }
    }
    
    void addDouble(SVGAttributes svgAttribute, SVGValue svgValue) {
        svgValue.getDouble(); // For type checking
        _addAttribute(svgAttribute, svgValue);
    }


    public void addString(SVGAttributes svgAttribute, SVGValue svgValue) {
        svgValue.getString(); // For type checking
        _addAttribute(svgAttribute, svgValue);
    }

    public void addPoints(SVGAttributes svgAttribute, SVGPointsValue svgValue) {
        _addAttribute(svgAttribute, svgValue);
    }

    public LinkedHashMap<SVGAttributes,SVGValue> getSVGAttributes() {
         return _attributes;
    }
}
