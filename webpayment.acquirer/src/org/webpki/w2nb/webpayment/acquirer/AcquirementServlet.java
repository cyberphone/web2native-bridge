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
package org.webpki.w2nb.webpayment.acquirer;

import java.io.IOException;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONOutputFormats;
import org.webpki.json.JSONParser;

import org.webpki.w2nb.webpayment.common.BaseProperties;

import org.webpki.webutil.ServletUtil;

public class AcquirementServlet extends HttpServlet implements BaseProperties {

    private static final long serialVersionUID = 1L;
    
    static Logger logger = Logger.getLogger(AcquirementServlet.class.getCanonicalName());
    
    static int referenceId = 164006;
    
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        JSONObjectWriter authorizationResponse = null;
        String clientIpAddress = null;
        try {
            String contentType = request.getContentType();
            if (!contentType.equals(JSON_CONTENT_TYPE)) {
                throw new IOException("Content-Type must be \"" + JSON_CONTENT_TYPE + "\" , found: " + contentType);
            }
            JSONObjectReader authorizationRequest = JSONParser.parse(ServletUtil.getData(request));
            logger.info("Received:\n" + authorizationRequest);

            ////////////////////////////////////////////////////////////////////////////////////////
            // We rationalize here by using a single end-point for both direct and indirect modes //
            ////////////////////////////////////////////////////////////////////////////////////////
        } catch (Exception e) {
            
        }
 
        response.setContentType(JSON_CONTENT_TYPE);
        response.setHeader("Pragma", "No-Cache");
        response.setDateHeader("EXPIRES", 0);
        response.getOutputStream().write(authorizationResponse.serializeJSONObject(JSONOutputFormats.NORMALIZED));
      }
  }
