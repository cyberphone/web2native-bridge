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

public class SVGPath extends SVGObject {

    SVGValue x;
    SVGValue y;
    SVGPathValue path;
    
    static class Coordinate {
        boolean absolute;
        double x;
        double y;
        
        Coordinate (boolean absolute, double x, double y) {
            this.absolute = absolute;
            this.x = x;
            this.y = y;
        }
    }

    static class SubCommand {
        char command;
        Vector<Coordinate> coordinates = new Vector<Coordinate>();
        
        SubCommand (char command) {
            this.command = command;
        }
        
        SubCommand addCoordinate(boolean absolute, double x, double y) {
            coordinates.add(new Coordinate(absolute, x, y));
            return this;
        }
    }

    class SVGPathValue extends SVGValue {
        
        Vector<SubCommand> commands = new Vector<SubCommand>();
        
        @Override
        public String getStringRepresentation() {
            StringBuffer result = new StringBuffer();
            for (SubCommand subCommand : commands) {
                if (result.length() > 0) {
                    result.append(' ');
                }
                result.append(subCommand.command);
                for (Coordinate coordinate : subCommand.coordinates) {
                    double xValue = coordinate.x;
                    double yValue = coordinate.y;
                    if (coordinate.absolute) {
                        xValue += x.getDouble();
                        yValue += y.getDouble();
                    }
                    result.append(' ')
                          .append(niceDouble(xValue))
                          .append(',')
                          .append(niceDouble(yValue));
                }
            }
            return result.toString();
        }
        
        void addSubCommand(SubCommand subCommand) {
            commands.add(subCommand);
        }
    }
    
    public SVGPath(SVGValue x,
                   SVGValue y,
                   Double strokeWidth,
                   String strokeColor,
                   String fillColor) {
        processColor(strokeWidth, strokeColor, fillColor);
        _addAttribute(SVGAttributes.D, path = new SVGPathValue());
        this.x = x;
        this.y = y;
    }

    public SVGPath(SVGAnchor anchor,
                   Double strokeWidth,
                   String strokeColor,
                   String fillColor) {
        this(anchor.x,
             anchor.y,
             strokeWidth,
             strokeColor,
             fillColor);
        if (anchor.alignment != SVGAnchor.ALIGNMENT.TOP_LEFT) {
            throw new RuntimeException("Only TOP_LEFT is allowed for SVGPath");
        }
    }
    
    public SVGPath moveAbsolute(double x, double y) {
        path.addSubCommand(new SubCommand('M').addCoordinate(true, x, y));
        return this;
    }

    public SVGPath lineToRelative(double x, double y) {
        path.addSubCommand(new SubCommand('l').addCoordinate(false, x, y));
        return this;
    }

    @Override
    String getTag() {
        return "path";
    }

    @Override
    boolean hasBody() {
        return false;
    }
    
    public SVGPath setFilter(String filter) {
        addString(SVGAttributes.FILTER, new SVGStringValue(filter));
        return this;
    }

    @Override
    double getMaxX() {
        x = new SVGAddOffset(x, SVGDocument.marginX);
        return x.getDouble();
    }

    @Override
    double getMaxY() {
        y = new SVGAddOffset(y, SVGDocument.marginY);
        return y.getDouble();
    }

    public SVGPath setDashMode(double written, double empty) {
        addDashes(written, empty);
        return this;
    }
}
