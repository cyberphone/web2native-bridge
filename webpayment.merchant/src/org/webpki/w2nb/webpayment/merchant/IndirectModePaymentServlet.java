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
import org.webpki.w2nb.webpayment.common.AccountTypes;
import org.webpki.w2nb.webpayment.common.Authority;
import org.webpki.w2nb.webpayment.common.GenericAuthorizationResponse;
import org.webpki.w2nb.webpayment.common.Messages;
import org.webpki.w2nb.webpayment.common.PayeeFinalizeRequest;
import org.webpki.w2nb.webpayment.common.PayeeIndirectModeAuthorizationRequest;
import org.webpki.w2nb.webpayment.common.PayerIndirectModeAuthorizationRequest;

public class IndirectModePaymentServlet extends PaymentCoreServlet {

    private static final long serialVersionUID = 1L;
    
    static final int TIMEOUT_FOR_REQUEST = 5000;

    static String portFilter(String url) throws IOException {
        // Our JBoss installation has some port mapping issues...
        if (MerchantService.bankPortMapping == null) {
            return url;
        }
        URL url2 = new URL(url);
        return new URL(url2.getProtocol(),
                       url2.getHost(),
                       MerchantService.bankPortMapping,
                       url2.getFile()).toExternalForm(); 
    }

    @Override
    protected GenericAuthorizationResponse processInput(HttpSession session,
                                                        JSONObjectReader input,
                                                        byte[] requestHash,
                                                        String clientIpAddress)
    throws IOException, GeneralSecurityException {

        // Decode the user's indirect mode authorization request
        PayerIndirectModeAuthorizationRequest request = new PayerIndirectModeAuthorizationRequest(input);

        // Lookup indicated authority
        String providerAuthorityUrl = request.getAuthorityUrl();
        HTTPSWrapper wrap = new HTTPSWrapper();
        wrap.setTimeout(TIMEOUT_FOR_REQUEST);
        wrap.setHeader("Content-Type", MerchantService.jsonMediaType);
        wrap.setRequireSuccess(true);
        wrap.makeGetRequest(portFilter(providerAuthorityUrl));
        if (!wrap.getContentType().equals(JSON_CONTENT_TYPE)) {
            throw new IOException("Content-Type must be \"" + JSON_CONTENT_TYPE + "\" , found: " + wrap.getContentType());
        }
        Authority providerAuthority = new Authority(JSONParser.parse(wrap.getData()), providerAuthorityUrl);

        // Attest the user's encrypted authorization to show "intent"
        JSONObjectWriter providerRequest =
            PayeeIndirectModeAuthorizationRequest.encode(input.getObject(AUTHORIZATION_DATA_JSON),
                                                         requestHash,
                                                         request.getAccountType(),
                                                         (String)session.getAttribute(UserPaymentServlet.REQUEST_REFID_SESSION_ATTR),
                                                         MerchantService.acquirerAuthorityUrl,
                                                         clientIpAddress,
                                                         MerchantService.merchantKey);
        String transactionUrl = providerAuthority.getTransactionUrl();

        logger.info("About to send to \"" + transactionUrl + "\":\n" + providerRequest);

        // Call the payment provider (which is the only party that can deal with
        // encrypted user authorizations)
        byte[] bankRequest = providerRequest.serializeJSONObject(JSONOutputFormats.NORMALIZED);
        wrap.setTimeout(TIMEOUT_FOR_REQUEST);
        wrap.setHeader("Content-Type", MerchantService.jsonMediaType);
        wrap.setRequireSuccess(true);
        wrap.makePostRequest(portFilter(transactionUrl), bankRequest);
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

        GenericAuthorizationResponse bankResponse = new GenericAuthorizationResponse(resultMessage);

        // Lookup indicated authority
         wrap = new HTTPSWrapper();
        wrap.setTimeout(TIMEOUT_FOR_REQUEST);
        wrap.setHeader("Content-Type", MerchantService.jsonMediaType);
        wrap.setRequireSuccess(true);
        wrap.makeGetRequest(portFilter(MerchantService.acquirerAuthorityUrl));
        if (!wrap.getContentType().equals(JSON_CONTENT_TYPE)) {
            throw new IOException("Content-Type must be \"" + JSON_CONTENT_TYPE + "\" , found: " + wrap.getContentType());
        }
        Authority acquirerAuthority = new Authority(JSONParser.parse(wrap.getData()), MerchantService.acquirerAuthorityUrl);

        JSONObjectWriter finalizationMessage =
                PayeeFinalizeRequest.encode(bankResponse,
                                            bankResponse.getPaymentRequest().getAmount(),
                                            MerchantService.merchantKey);

        // Call the payment provider (which is the only party that can deal with
        // encrypted user authorizations)
        wrap.setTimeout(TIMEOUT_FOR_REQUEST);
        wrap.setHeader("Content-Type", MerchantService.jsonMediaType);
        wrap.setRequireSuccess(true);
        wrap.makePostRequest(portFilter(acquirerAuthority.getTransactionUrl()),
                                        finalizationMessage.serializeJSONObject(JSONOutputFormats.NORMALIZED));
        if (!wrap.getContentType().equals(JSON_CONTENT_TYPE)) {
            throw new IOException("Content-Type must be \"" + JSON_CONTENT_TYPE + "\" , found: " + wrap.getContentType());
        }

        // The result should be a provider-signed authorization
        return new GenericAuthorizationResponse(resultMessage);
    }
}
