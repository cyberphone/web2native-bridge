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
package org.webpki.w2nb.webpayment.merchant;

import java.io.IOException;

import java.security.GeneralSecurityException;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONParser;

import org.webpki.util.ArrayUtil;

import org.webpki.w2nb.webpayment.common.BaseProperties;
import org.webpki.w2nb.webpayment.common.GenericAuthorizationRequest;
import org.webpki.w2nb.webpayment.common.GenericAuthorizationResponse;

public abstract class PaymentCoreServlet extends HttpServlet implements BaseProperties {

    private static final long serialVersionUID = 1L;
    
    static Logger logger = Logger.getLogger(PaymentCoreServlet.class.getCanonicalName());
    
    static int transaction_id = 164006;
    
    protected abstract GenericAuthorizationResponse processInput(JSONObjectReader input,
                                                                 byte[] requestHash,
                                                                 String clientIpAddress)
    throws IOException, GeneralSecurityException;
    
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        try {
            byte[] requestHash = (byte[]) request.getSession(false).getAttribute(CheckoutServlet.REQUEST_HASH_ATTR);
            request.setCharacterEncoding("UTF-8");
            JSONObjectReader input = JSONParser.parse(request.getParameter("authreq"));
            logger.info("Received:\n" + input);
            
            GenericAuthorizationResponse authorization = processInput(input, requestHash, request.getRemoteAddr());
            if (!ArrayUtil.compare(authorization.getPaymentRequest().getRequestHash(), requestHash)) {
                throw new IOException("Non-matching \"" + REQUEST_HASH_JSON + "\"");
            }

            logger.info("Successful authorization of request: " + authorization.getPaymentRequest().getReferenceId());
            HTML.resultPage(response,
                            null,
                            authorization.getPaymentRequest(), 
                            authorization.getCardType(),
                            GenericAuthorizationRequest.formatCardNumber(authorization.getCardReference()));   

        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            response.setContentType("text/plain; charset=utf-8");
            response.setHeader("Pragma", "No-Cache");
            response.setDateHeader("EXPIRES", 0);
            response.getOutputStream().println("Error: " + e.getMessage());
        }
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.sendRedirect("home");
    }
}
