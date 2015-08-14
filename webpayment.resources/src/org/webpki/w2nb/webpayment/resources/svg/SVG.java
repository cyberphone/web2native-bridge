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

import org.webpki.util.ArrayUtil;

public class SVG {
    
    static StringBuffer svgText = new StringBuffer();
    
    static SVGDocument doc;
    
    static void writeSVGObject(SVGObject svgObject) {
        doc.findLargestSize(svgObject);
        svgText.append("<").append(svgObject.getTag());
        for (SVGAttributes svgAttribute : svgObject.getSVGAttributes().keySet()) {
            svgText.append(" ")
                   .append(svgAttribute.toString())
                   .append("=\"")
                   .append(svgObject.getAttribute(svgAttribute).getStringRepresentation())
                   .append("\"");
        }
        if (svgObject.hasBody()) {
            svgText.append(">").append(svgObject.getBody()).append("</").append(svgObject.getTag());
        } else {
            svgText.append("/");
        }
        svgText.append(">\n");
    }

    public static void main(String[] args) {
        if (args.length != 2 && args.length != 3) {
            System.out.println("Arguments outout-file class [filter-file]");
            System.exit(3);
        }
        try {
            doc = (SVGDocument) Class.forName(args[1]).newInstance();
            String filters = "";
            if (args.length == 3) {
                filters = new String(ArrayUtil.readFile(args[2]), "UTF-8");
            }
            doc.generate();
            for (SVGObject svgObject : SVGDocument.svgObjects) {
                for (SVGObject dependencyElement : svgObject.beforeDependencyElements) {
                    writeSVGObject(dependencyElement);
                }
                writeSVGObject(svgObject);
                for (SVGObject dependencyElement : svgObject.afterDependencyElements) {
                    writeSVGObject(dependencyElement);
                }
            }
            svgText.append("</g>\n</svg>");
            svgText = new StringBuffer("<svg width=\"")
                .append((long)(doc.currentMaxX + 10))
                .append("\" height=\"")
                .append((long)(doc.currentMaxY + 10))
                .append("\" xmlns=\"http://www.w3.org/2000/svg\" xmlns:svg=\"http://www.w3.org/2000/svg\">\n")
                .append(filters)
                .append("<g>\n")
                .append(svgText);
            ArrayUtil.writeFile(args[0], svgText.toString().getBytes("UTF-8"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
