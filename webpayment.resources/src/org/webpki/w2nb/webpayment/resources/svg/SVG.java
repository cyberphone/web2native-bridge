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

import java.util.Vector;

import org.webpki.util.ArrayUtil;

public class SVG {
    
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Arguments outout-file class");
            System.exit(3);
        }
        try {
            Vector<SVGObject> svgObjects = new Vector<SVGObject>(); 
            SVGDocument doc = (SVGDocument) Class.forName(args[1]).newInstance();
            doc.svgObjects = svgObjects;
            doc.generate();
            StringBuffer svgText = new StringBuffer();
            for (SVGObject svgObject : svgObjects) {
                svgText.append("<").append(svgObject.getTag());
                for (SVGAttributes svgAttribute : svgObject.getSVGAttributes().keySet()) {
                    svgText.append(" ")
                           .append(svgAttribute.toString())
                           .append("=\"")
                           .append(svgObject.getSVGAttributes().get(svgAttribute).getStringRepresentation())
                           .append("\"");
                }
                if (svgObject.hasBody()) {
                    svgText.append("</").append(svgObject.getTag());
                } else {
                    svgText.append("/");
                }
                svgText.append(">\n");
            }
            svgText.append("</svg>");
            svgText = new StringBuffer("<svg width=\"")
                .append(doc.getWidth())
                .append("\" height=\"")
                .append(doc.getHeight())
                .append("\" xmlns=\"http://www.w3.org/2000/svg\" xmlns:svg=\"http://www.w3.org/2000/svg\">\n")
                .append(svgText);
            ArrayUtil.writeFile(args[0], svgText.toString().getBytes("UTF-8"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
