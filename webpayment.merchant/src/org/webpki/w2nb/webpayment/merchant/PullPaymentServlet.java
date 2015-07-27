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

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONOutputFormats;
import org.webpki.json.JSONParser;

import org.webpki.net.HTTPSWrapper;

import org.webpki.w2nb.webpayment.common.BaseProperties;
import org.webpki.w2nb.webpayment.common.GenericAuthorizationResponse;
import org.webpki.w2nb.webpayment.common.PayeePullAuthorizationRequest;
import org.webpki.w2nb.webpayment.common.PayerPullAuthorizationRequest;

public class PullPaymentServlet extends PaymentCoreServlet {

    private static final long serialVersionUID = 1L;
    
    static final int TIMEOUT_FOR_REQUEST = 5000;

    // TODO verify merchant request using a hash

    @Override
    protected GenericAuthorizationResponse processInput(JSONObjectReader input,
                                                        String clientIpAddress)
    throws IOException, GeneralSecurityException {
        PayerPullAuthorizationRequest request = new PayerPullAuthorizationRequest(input);
        String authUrl = request.getAuthUrl();
        JSONObjectWriter providerRequest = PayeePullAuthorizationRequest.encode(input.getObject(AUTH_DATA_JSON),
                                                                                clientIpAddress,
                                                                                MerchantService.merchantKey);
        // Our JBoss installation has some port mapping issues...
        if (MerchantService.bankPortMapping != null) {
            URL url = new URL (authUrl);
            authUrl = new URL (url.getProtocol (),
                               url.getHost (),
                               MerchantService.bankPortMapping,
                               url.getFile ()).toExternalForm (); 
        }
        logger.info("About to send to \"" + authUrl + "\":\n" + providerRequest);
        HTTPSWrapper wrap = new HTTPSWrapper();
        wrap.setTimeout(TIMEOUT_FOR_REQUEST);
        wrap.setHeader("Content-Type", BaseProperties.JSON_CONTENT_TYPE);
        wrap.setRequireSuccess(true);
        wrap.makePostRequest(authUrl,
                             providerRequest.serializeJSONObject(JSONOutputFormats.NORMALIZED));
        JSONObjectReader resultMessage = JSONParser.parse(wrap.getData());
        logger.info("Returned from payment provider:\n" + resultMessage);
        return new GenericAuthorizationResponse(resultMessage);
    }
}
