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

import java.net.URL;

import java.security.GeneralSecurityException;

import javax.servlet.http.HttpSession;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONOutputFormats;
import org.webpki.json.JSONParser;

import org.webpki.net.HTTPSWrapper;

import org.webpki.w2nb.webpayment.common.GenericAuthorizationResponse;
import org.webpki.w2nb.webpayment.common.PayeeIndirectModeAuthorizationRequest;
import org.webpki.w2nb.webpayment.common.PayerIndirectModeAuthorizationRequest;

public class IndirectModePaymentServlet extends PaymentCoreServlet {

    private static final long serialVersionUID = 1L;
    
    static final int TIMEOUT_FOR_REQUEST = 5000;
    
    @Override
    protected GenericAuthorizationResponse processInput(HttpSession session,
                                                        JSONObjectReader input,
                                                        byte[] requestHash,
                                                        String clientIpAddress)
    throws IOException, GeneralSecurityException {

        // Decode the user's indirect mode authorization request
        PayerIndirectModeAuthorizationRequest request = new PayerIndirectModeAuthorizationRequest(input);

        // The payment provider want this for logging and in indirect mode the payee
        // is the only party that has a direct contact with the client
        String authUrl = request.getAuthUrl();

        // Attest the user's encrypted authorization 
        JSONObjectWriter providerRequest =
            PayeeIndirectModeAuthorizationRequest.encode(input.getObject(AUTH_DATA_JSON),
                                                         requestHash,
                                                         clientIpAddress,
                                                         (String)session.getAttribute(UserPaymentServlet.REQUEST_REFID_SESSION_ATTR),
                                                         MerchantService.merchantKey);
        // Our JBoss installation has some port mapping issues...
        if (MerchantService.bankPortMapping != null) {
            URL url = new URL(authUrl);
            authUrl = new URL(url.getProtocol(),
                              url.getHost(),
                              MerchantService.bankPortMapping,
                              url.getFile()).toExternalForm(); 
        }

        logger.info("About to send to \"" + authUrl + "\":\n" + providerRequest);

        // Call the payment provider (which is the only party that can deal with
        // encrypted user authorizations)
        byte[] bankRequest = providerRequest.serializeJSONObject(JSONOutputFormats.NORMALIZED);
        HTTPSWrapper wrap = new HTTPSWrapper();
        wrap.setTimeout(TIMEOUT_FOR_REQUEST);
        wrap.setHeader("Content-Type", MerchantService.jsonMediaType);
        wrap.setRequireSuccess(true);
        wrap.makePostRequest(authUrl, bankRequest);
        if (!wrap.getContentType().equals(JSON_CONTENT_TYPE)) {
            throw new IOException("Content-Type must be \"" + JSON_CONTENT_TYPE + "\" , found: " + wrap.getContentType());
        }

        JSONObjectReader resultMessage = JSONParser.parse(wrap.getData());
        logger.info("Returned from payment provider:\n" + resultMessage);

        if (UserPaymentServlet.getOption(session, HomeServlet.DEBUG_SESSION_ATTR)) {
            DebugData debugData = (DebugData)session.getAttribute(UserPaymentServlet.DEBUG_DATA_SESSION_ATTR); 
            debugData.indirectModeBankResponse = wrap.getData();
            debugData.indirectModeBankRequest = bankRequest;
        }

        // The result should be a provider-signed authorization
        return new GenericAuthorizationResponse(resultMessage);
    }
}