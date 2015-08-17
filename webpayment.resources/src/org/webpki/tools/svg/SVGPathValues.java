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
package org.webpki.tools.svg;

import java.util.Vector;

public class SVGPathValues extends SVGValue {

    double maxX =-10000;
    double maxY =-10000;
    
    double minX = 10000;
    double minY = 10000;

    double currX;
    double currY;
    
    SVGPath parent;
    
    public SVGPathValues() {
        
    }
    
    public SVGPathValues(SVGPathValues masterPath) {
        minX = masterPath.minX;
        minY = masterPath.minY;
        maxX = masterPath.maxX;
        maxY = masterPath.maxY;
    }
    
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
                    xValue += parent.x.getDouble();
                    yValue += parent.y.getDouble();
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
    
    public SVGPathValues moveAbsolute(double x, double y) {
        addSubCommand(new SubCommand('M').addCoordinate(true, true, x, y));
        return this;
    }

    public SVGPathValues lineToRelative(double x, double y) {
        addSubCommand(new SubCommand('l').addCoordinate(false, true, x, y));
        return this;
    }

    public SVGPathValues cubicBezier(double c1x, double c1y, double c2x,double c2y, double x, double y) {
        addSubCommand(new SubCommand('c').addCoordinate(false, false, c1x, c1y)
                                         .addCoordinate(false, false, c2x, c2y)
                                         .addCoordinate(false, true, x, y));
        return this;
    }

    public SVGPathValues endPath() {
        addSubCommand(new SubCommand('z'));
        return this;
    }
}
