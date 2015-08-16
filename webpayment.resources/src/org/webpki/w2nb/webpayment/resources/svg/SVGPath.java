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
    
    double maxX;
    double maxY;
    
    double minX;
    double minY;

    double currX;
    double currY;
    
    class Coordinate {
        boolean absolute;
        double xValue;
        double yValue;
        
        Coordinate (boolean absolute, boolean actual, double xValue, double yValue) {
            this.absolute = absolute;
            this.xValue = xValue;
            this.yValue = yValue;
            if (actual) {
                if (absolute) {
                    currX = xValue;
                    currY = yValue;
                } else {
                    currX += xValue;
                    currY += yValue;
                    if (currX > maxX) {
                        maxX = currX;
                    } else if (currX < minX) {
                        minX = currX;
                    }
                    if (currY > maxY) {
                        maxY = currY;
                    } else if (currY < minY) {
                        minY = currY;
                    }
                }
            }
        }
    }

    class SubCommand {
        char command;
        Vector<Coordinate> coordinates = new Vector<Coordinate>();
        
        SubCommand (char command) {
            this.command = command;
        }
        
        SubCommand addCoordinate(boolean absolute, boolean actual, double x, double y) {
            coordinates.add(new Coordinate(absolute, actual, x, y));
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
                    double xValue = coordinate.xValue;
                    double yValue = coordinate.yValue;
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
        path.addSubCommand(new SubCommand('M').addCoordinate(true, true, x, y));
        return this;
    }

    public SVGPath lineToRelative(double x, double y) {
        path.addSubCommand(new SubCommand('l').addCoordinate(false, true, x, y));
        return this;
    }

    public SVGPath cubicBezier(double c1x, double c1y, double c2x,double c2y, double x, double y) {
        path.addSubCommand(new SubCommand('c').addCoordinate(false, false, c1x, c1y)
                                              .addCoordinate(false, false, c2x, c2y)
                                              .addCoordinate(false, true, x, y));
        return this;
    }

    public SVGPath endPath() {
        path.addSubCommand(new SubCommand('z'));
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
        if (x.getDouble() + minX < 0) {
            throw new RuntimeException("SVGPath X negative!");
        }
        x = new SVGAddOffset(x, SVGDocument.marginX);
        return x.getDouble() + maxX;
    }

    @Override
    double getMaxY() {
        if (y.getDouble() + minY < 0) {
            throw new RuntimeException("SVGPath Y negative!");
        }
        y = new SVGAddOffset(y, SVGDocument.marginY);
        return y.getDouble() + maxY;
    }

    public SVGPath setDashMode(double written, double empty) {
        addDashes(written, empty);
        return this;
    }

    public SVGPath setRoundLineCap() {
        _setRoundLineCap();
        return this;
    }
}
