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
package org.webpki.w2nb.webpayment.bank;

import java.io.IOException;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.webpki.json.JSONDecoderCache;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONOutputFormats;
import org.webpki.json.JSONParser;

import org.webpki.w2nb.webpayment.common.BaseProperties;
import org.webpki.w2nb.webpayment.common.PullAuthorizationRequest;
import org.webpki.w2nb.webpayment.common.GenericAuthorizationRequest;
import org.webpki.w2nb.webpayment.common.GenericAuthorizationResponse;
import org.webpki.w2nb.webpayment.common.Messages;
import org.webpki.w2nb.webpayment.common.PaymentRequest;

import org.webpki.webutil.ServletUtil;

public class PaymentProviderServlet extends HttpServlet implements BaseProperties
  {
    private static final long serialVersionUID = 1L;
    
    static Logger logger = Logger.getLogger (PaymentProviderServlet.class.getCanonicalName());
    
    static int transaction_id = 164006;
    
    public void doPost (HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        JSONObjectWriter authorizationResponse = null;
        String clientIpAddress = null;
        try {
            String contentType = request.getContentType();
            int i = contentType.indexOf(';');
            if (i > 0) {
                contentType = contentType.substring(0, i).trim();
            }
            if (!contentType.equals(JSON_CONTENT_TYPE)) {
                throw new IOException("Content-Type MUST be \"" + JSON_CONTENT_TYPE + "\"");
            }
            GenericAuthorizationRequest genericAuthorizationRequest = null;
            JSONObjectReader authorizationRequest = JSONParser.parse (ServletUtil.getData (request));
            logger.info("Received:\n" + authorizationRequest);

            ////////////////////////////////////////////////////////////////////////////
            // We rationalize here by using a single end-point for both push and pull //
            ////////////////////////////////////////////////////////////////////////////

            // A minor test is though needed for dispatch the proper decoder...
            if (authorizationRequest.getString(JSONDecoderCache.QUALIFIER_JSON).equals(Messages.PAYEE_PULL_AUTH_REQ.toString())) {
                throw new IOException("Not yet...");
            } else {
                genericAuthorizationRequest = new GenericAuthorizationRequest(authorizationRequest);
                clientIpAddress = request.getRemoteAddr();
            }

            // Client IP could be used for risk-based authentication, here it is only logged
            logger.info("Client address: " + clientIpAddress);

            // Verify that the outer signature (the user's) matches the bank's client root
            genericAuthorizationRequest.getSignatureDecoder().verify(PaymentService.clientRoot);

            // Get the embedded (counter-signed) payment request
            PaymentRequest paymentRequest = genericAuthorizationRequest.getPaymentRequest();

            // Verify that the merchant's signature belongs to the merchant trust network
            paymentRequest.getSignatureDecoder().verify(PaymentService.merchantRoot);

            // Return the authorized request
            authorizationResponse = GenericAuthorizationResponse.encode(paymentRequest,
                                                                        genericAuthorizationRequest.getCardType(),
                                                                        genericAuthorizationRequest.getCardNumber(),
                                                                        PaymentService.bankKey);

            logger.info("Returned to caller:\n" + authorizationResponse);
            
        } catch (Exception e) {
            authorizationResponse = Messages.createBaseMessage (Messages.PROVIDER_GENERIC_AUTH_RES);
            authorizationResponse.setString (ERROR_JSON, e.getMessage ());
            logger.log (Level.SEVERE, e.getMessage ());
        }

        response.setContentType (JSON_CONTENT_TYPE + "; charset=utf-8");
        response.setHeader ("Pragma", "No-Cache");
        response.setDateHeader ("EXPIRES", 0);
        response.getOutputStream ().write (authorizationResponse.serializeJSONObject (JSONOutputFormats.NORMALIZED));
      }
  }
