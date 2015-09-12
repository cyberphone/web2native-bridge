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
import org.webpki.w2nb.webpayment.common.FinalizeResponse;
import org.webpki.w2nb.webpayment.common.ReserveOrDebitResponse;
import org.webpki.w2nb.webpayment.common.FinalizeRequest;
import org.webpki.w2nb.webpayment.common.PaymentRequest;
import org.webpki.webutil.ServletUtil;

public class AcquirementServlet extends HttpServlet implements BaseProperties {

    private static final long serialVersionUID = 1L;
    
    static Logger logger = Logger.getLogger(AcquirementServlet.class.getCanonicalName());
    
    static int referenceId = 194006;
    
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        JSONObjectWriter acquirerResponse = null;
         try {
            String contentType = request.getContentType();
            if (!contentType.equals(JSON_CONTENT_TYPE)) {
                throw new IOException("Content-Type must be \"" + JSON_CONTENT_TYPE + "\" , found: " + contentType);
            }
            JSONObjectReader payeeRequest = JSONParser.parse(ServletUtil.getData(request));
            logger.info("Received:\n" + payeeRequest);

            // Decode the finalize request message
            FinalizeRequest payeeFinalizationRequest = new FinalizeRequest(payeeRequest);

            // Get the embedded authorization from the payer's payment provider (bank)
            ReserveOrDebitResponse genericAuthorizationResponse =
                payeeFinalizationRequest.getGenericAuthorizationResponse();

            // Verify that the provider's signature belongs to a valid payment provider trust network
            genericAuthorizationResponse.getSignatureDecoder().verify(AcquirerService.paymentRoot);

            // Get the the account data we sent encrypted through the merchant 
            logger.info("Protected Account Data:\n" +
                genericAuthorizationResponse.getProtectedAccountData(AcquirerService.decryptionKeys));

            // The original request contains some required data like currency
            PaymentRequest paymentRequest = genericAuthorizationResponse.getPaymentRequest();

            // Verify that the merchant's signature belongs to a valid merchant trust network
            paymentRequest.getSignatureDecoder().verify(AcquirerService.merchantRoot);

            // Verify that the merchant is one of our customers.  Simplistic "database": a single customer
            String merchantDn = paymentRequest.getSignatureDecoder().getCertificatePath()[0].getSubjectX500Principal().getName();
            if (!merchantDn.equals(AcquirerService.merchantDN)) {
                throw new IOException ("Unknown merchant: " + merchantDn);
            }

            //////////////////////////////////////////////////////////////////////////////
            // Now we have all data needed for talking to the payment network but since //
            // we don't have such a connection we simply return success...              //
            //////////////////////////////////////////////////////////////////////////////
            acquirerResponse = FinalizeResponse.encode(payeeFinalizationRequest,
                                                       "#" + (referenceId++),
                                                       AcquirerService.acquirerKey);
            logger.info("Returned to caller:\n" + acquirerResponse);

        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
 
        response.setContentType(JSON_CONTENT_TYPE);
        response.setHeader("Pragma", "No-Cache");
        response.setDateHeader("EXPIRES", 0);
        response.getOutputStream().write(acquirerResponse.serializeJSONObject(JSONOutputFormats.NORMALIZED));
      }
  }
