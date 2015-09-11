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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONOutputFormats;
import org.webpki.json.JSONParser;
import org.webpki.net.HTTPSWrapper;
import org.webpki.util.ArrayUtil;
import org.webpki.w2nb.webpayment.common.AccountTypes;
import org.webpki.w2nb.webpayment.common.Authority;
import org.webpki.w2nb.webpayment.common.BaseProperties;
import org.webpki.w2nb.webpayment.common.AuthorizationData;
import org.webpki.w2nb.webpayment.common.GenericAuthorizationResponse;
import org.webpki.w2nb.webpayment.common.PayeeFinalizeRequest;
import org.webpki.w2nb.webpayment.common.ReserveFundsRequest;
import org.webpki.w2nb.webpayment.common.PayerAuthorization;

public class BackendPaymentServlet extends HttpServlet implements BaseProperties {

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
    
    static Logger logger = Logger.getLogger(BackendPaymentServlet.class.getCanonicalName());
    
    static int transaction_id = 164006;
    
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        try {
            HttpSession session = request.getSession(false);
            if (session == null) {
                throw new IOException("Session timed out!");
            }
            byte[] requestHash = (byte[]) session.getAttribute(UserPaymentServlet.REQUEST_HASH_SESSION_ATTR);

            request.setCharacterEncoding("UTF-8");
            JSONObjectReader input = JSONParser.parse(request.getParameter(UserPaymentServlet.AUTHDATA_FORM_ATTR));
            logger.info("Received from wallet:\n" + input);

            // Do we have web debug mode?
            boolean debug = UserPaymentServlet.getOption(session, HomeServlet.DEBUG_SESSION_ATTR);

            if (debug) {
                DebugData debugData = (DebugData) session.getAttribute(UserPaymentServlet.DEBUG_DATA_SESSION_ATTR);
                debugData.initMessage = request.getParameter(UserPaymentServlet.INITMSG_FORM_ATTR).getBytes("UTF-8");
                debugData.walletResponse = input.serializeJSONObject(JSONOutputFormats.NORMALIZED);
            }

            // Decode the user's authorization.  The encrypted data is only parsed for correctness
            PayerAuthorization payerAuthorization = new PayerAuthorization(input);

            // Lookup indicated authority (Payment Provider)
            String providerAuthorityUrl = payerAuthorization.getAuthorityUrl();
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
                ReserveFundsRequest.encode(input.getObject(AUTHORIZATION_DATA_JSON),
                                           requestHash,
                                           payerAuthorization.getAccountType(),
                                           (String)session.getAttribute(UserPaymentServlet.REQUEST_REFID_SESSION_ATTR),
                                           MerchantService.acquirerAuthorityUrl,
                                           request.getRemoteAddr(),
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

            if (debug) {
                DebugData debugData = (DebugData)session.getAttribute(UserPaymentServlet.DEBUG_DATA_SESSION_ATTR); 
                debugData.bankReserveFundsRequest = bankRequest;
                debugData.bankReserveFundsResponse = wrap.getData();
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
            GenericAuthorizationResponse authorization =  new GenericAuthorizationResponse(resultMessage);

            
            if (!ArrayUtil.compare(authorization.getPaymentRequest().getRequestHash(), requestHash)) {
                throw new IOException("Non-matching \"" + REQUEST_HASH_JSON + "\"");
            }

            logger.info("Successful authorization of request: " + authorization.getPaymentRequest().getReferenceId());
            AccountTypes accountType = AccountTypes.fromType(authorization.getAccountType());
            HTML.resultPage(response,
                            UserPaymentServlet.getOption(session, HomeServlet.DEBUG_SESSION_ATTR),
                            null,
                            authorization.getPaymentRequest(),
                            accountType,
                            accountType.isAcquirerBased() ? // = Card
                                AuthorizationData.formatCardNumber(authorization.getAccountReference())
                                                          :
                                authorization.getAccountReference());  // Currently "unmoderated" account

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
