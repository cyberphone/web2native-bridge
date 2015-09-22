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

import org.webpki.w2nb.webpayment.common.PayerAccountTypes;
import org.webpki.w2nb.webpayment.common.Authority;
import org.webpki.w2nb.webpayment.common.BaseProperties;
import org.webpki.w2nb.webpayment.common.AuthorizationData;
import org.webpki.w2nb.webpayment.common.Expires;
import org.webpki.w2nb.webpayment.common.FinalizeResponse;
import org.webpki.w2nb.webpayment.common.AccountDescriptor;
import org.webpki.w2nb.webpayment.common.RequestHash;
import org.webpki.w2nb.webpayment.common.ReserveOrDebitResponse;
import org.webpki.w2nb.webpayment.common.ReserveOrDebitRequest;
import org.webpki.w2nb.webpayment.common.FinalizeRequest;
import org.webpki.w2nb.webpayment.common.PayerAuthorization;

public class BackendPaymentServlet extends HttpServlet implements BaseProperties {

    private static final long serialVersionUID = 1L;
    
    static final int TIMEOUT_FOR_REQUEST = 5000;

    static String portFilter(String url) throws IOException {
        // Our JBoss installation has some port mapping issues...
        if (MerchantService.serverPortMapping == null) {
            return url;
        }
        URL url2 = new URL(url);
        return new URL(url2.getProtocol(),
                       url2.getHost(),
                       MerchantService.serverPortMapping,
                       url2.getFile()).toExternalForm(); 
    }
    
    static Logger logger = Logger.getLogger(BackendPaymentServlet.class.getCanonicalName());
    
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        try {
            HttpSession session = request.getSession(false);
            if (session == null) {
                ErrorServlet.sessionTimeout(response);
                return;
             }
            byte[] requestHash = (byte[]) session.getAttribute(UserPaymentServlet.REQUEST_HASH_SESSION_ATTR);

            request.setCharacterEncoding("UTF-8");
            JSONObjectReader userAuthorization = JSONParser.parse(request.getParameter(UserPaymentServlet.AUTHDATA_FORM_ATTR));
            logger.info("Received from wallet:\n" + userAuthorization);

            // Do we have web debug mode?
            DebugData debugData = null;
            boolean debug = UserPaymentServlet.getOption(session, HomeServlet.DEBUG_SESSION_ATTR);
            if (debug) {
                debugData = (DebugData) session.getAttribute(UserPaymentServlet.DEBUG_DATA_SESSION_ATTR);
                debugData.WalletInitialized = request.getParameter(UserPaymentServlet.INITMSG_FORM_ATTR).getBytes("UTF-8");
                debugData.walletResponse = userAuthorization.serializeJSONObject(JSONOutputFormats.NORMALIZED);
            }

            // Decode the user's authorization.  The encrypted data is only parsed for correctness
            PayerAuthorization payerAuthorization = new PayerAuthorization(userAuthorization);

            // Lookup indicated authority (Payment Provider)
            String providerAuthorityUrl = payerAuthorization.getAuthorityUrl();

            // Ugly patch allowing the wallet to work with a local system as well
            if (request.getServerName().equals("localhost")) {
                URL orig = new URL(providerAuthorityUrl);
                providerAuthorityUrl = new URL(request.isSecure() ? "https": "http",
                                               "localhost", request.getServerPort(), orig.getFile()).toExternalForm();
            }

            // In a production setup you would cache authority objects since they are long-lived
            HTTPSWrapper wrap = new HTTPSWrapper();
            wrap.setTimeout(TIMEOUT_FOR_REQUEST);
            wrap.setRequireSuccess(true);
            wrap.makeGetRequest(portFilter(providerAuthorityUrl));
            if (!wrap.getRawContentType().equals(JSON_CONTENT_TYPE)) {
                throw new IOException("Content-Type must be \"" + JSON_CONTENT_TYPE + "\" , found: " + wrap.getContentType());
            }
            Authority providerAuthority = new Authority(JSONParser.parse(wrap.getData()), providerAuthorityUrl);

            // Verify that the claimed authority belongs to a known payment provider network
            providerAuthority.getSignatureDecoder().verify(MerchantService.paymentRoot);

            // Direct debit is only applicable to account2account operations
            boolean directDebit = !UserPaymentServlet.getOption(session, HomeServlet.RESERVE_MODE_SESSION_ATTR) &&
                                  !payerAuthorization.getAccountType().isAcquirerBased();

            if (debug) {
                debugData.providerAuthority = wrap.getData();
                debugData.directDebit = directDebit;
            }

            AccountDescriptor[] accounts = {new AccountDescriptor("http://ultragiro.fr", "35964640"),
                                            new AccountDescriptor("http://mybank.com", 
                                                                  "J-399.962",
                                                                  new String[]{"enterprise"})};

            // Attest the user's encrypted authorization to show "intent"
            JSONObjectWriter providerRequest =
                ReserveOrDebitRequest.encode(directDebit, 
                                             userAuthorization.getObject(AUTHORIZATION_DATA_JSON),
                                             requestHash,
                                             payerAuthorization.getAccountType(),
                                             (String)session.getAttribute(UserPaymentServlet.REQUEST_REFID_SESSION_ATTR),
                                             payerAuthorization.getAccountType().isAcquirerBased() ? 
                                                              MerchantService.acquirerAuthorityUrl : null,
                                             payerAuthorization.getAccountType().isAcquirerBased() ? 
                                                              null : accounts,
                                             request.getRemoteAddr(),
                                             directDebit ? null : Expires.inMinutes(30),
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
            if (!wrap.getRawContentType().equals(JSON_CONTENT_TYPE)) {
                throw new IOException("Content-Type must be \"" + JSON_CONTENT_TYPE + "\" , found: " + wrap.getContentType());
            }

            JSONObjectReader resultMessage = JSONParser.parse(wrap.getData());
            logger.info("Returned from payment provider:\n" + resultMessage);

            if (debug) {
                debugData.reserveOrDebitRequest = bankRequest;
                debugData.reserveOrDebitResponse = wrap.getData();
            }

            ReserveOrDebitResponse bankResponse = new ReserveOrDebitResponse(resultMessage);

            if (!bankResponse.success()) {
                if (debug) {
                    debugData.softError = true;
                }
                HTML.paymentError(response, debug, bankResponse.getErrorReturn());
                return;
            }

            // No error return, then we can verify the response fully
            bankResponse.getSignatureDecoder().verify(MerchantService.paymentRoot);

            if (!ArrayUtil.compare(bankResponse.getPaymentRequest().getRequestHash(), requestHash)) {
                throw new IOException("Non-matching \"" + REQUEST_HASH_JSON + "\"");
            }
            
            if (!bankResponse.isDirectDebit()) {
                processFinalize (bankResponse, transactionUrl, debugData);
            }

            logger.info("Successful authorization of request: " + bankResponse.getPaymentRequest().getReferenceId());
            PayerAccountTypes accountType = PayerAccountTypes.fromType(bankResponse.getAccountType());
            HTML.resultPage(response,
                            debug,
                            bankResponse.getPaymentRequest(),
                            accountType,
                            accountType.isAcquirerBased() ? // = Card
                                AuthorizationData.formatCardNumber(bankResponse.getAccountReference())
                                                          :
                                bankResponse.getAccountReference());  // Currently "unmoderated" account

        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            ErrorServlet.fail(response, e.getMessage());
        }
    }

    void processFinalize(ReserveOrDebitResponse bankResponse, String transactionUrl, DebugData debugData)
    throws IOException, GeneralSecurityException {
        HTTPSWrapper wrap = new HTTPSWrapper();
        String target = "provider";
        Authority acquirerAuthority = null;
        if (!bankResponse.isAccount2Account()) {
            target = "acquirer";
            // Lookup indicated acquirer authority
            wrap.setTimeout(TIMEOUT_FOR_REQUEST);
            wrap.setRequireSuccess(true);
            wrap.makeGetRequest(portFilter(MerchantService.acquirerAuthorityUrl));
            if (!wrap.getRawContentType().equals(JSON_CONTENT_TYPE)) {
                throw new IOException("Content-Type must be \"" + JSON_CONTENT_TYPE + "\" , found: " + wrap.getContentType());
            }
            acquirerAuthority = new Authority(JSONParser.parse(wrap.getData()), MerchantService.acquirerAuthorityUrl);
            transactionUrl = acquirerAuthority.getTransactionUrl();
            if (debugData != null) {
                debugData.acquirerMode = true;
                debugData.acquirerAuthority = wrap.getData();
            }
        }

        JSONObjectWriter finalizeRequest = FinalizeRequest.encode(bankResponse,
                                                                  bankResponse.getPaymentRequest().getAmount(),
                                                                  UserPaymentServlet.getReferenceId(),
                                                                  MerchantService.merchantKey);

        logger.info("About to send to " + target + ":\n" + finalizeRequest);

        // Call the payment provider or acquirer
        byte[] sentFinalize = finalizeRequest.serializeJSONObject(JSONOutputFormats.NORMALIZED);
        wrap.setTimeout(TIMEOUT_FOR_REQUEST);
        wrap.setHeader("Content-Type", MerchantService.jsonMediaType);
        wrap.setRequireSuccess(true);
        wrap.makePostRequest(portFilter(transactionUrl), sentFinalize);
        if (!wrap.getRawContentType().equals(JSON_CONTENT_TYPE)) {
            throw new IOException("Content-Type must be \"" + JSON_CONTENT_TYPE + "\" , found: " + wrap.getContentType());
        }
        byte[] finalizeRequestHash = RequestHash.getRequestHash(sentFinalize);

        JSONObjectReader response = JSONParser.parse(wrap.getData());
        logger.info("Received from " + target + ":\n" + response);
        
        if (debugData != null) {
            debugData.finalizeRequest = sentFinalize;
            debugData.finalizeResponse = wrap.getData();
        }
        
        FinalizeResponse finalizeResponse = new FinalizeResponse(response);

        // Check signature origins
        ReserveOrDebitRequest.compareCertificatePaths(bankResponse.isAccount2Account() ?
                                                bankResponse.getSignatureDecoder().getCertificatePath()
                                                                                       :
                                                acquirerAuthority.getSignatureDecoder().getCertificatePath(),                                         
                                                      finalizeResponse.getSignatureDecoder().getCertificatePath());

        if (!ArrayUtil.compare(finalizeRequestHash, finalizeResponse.getRequestHash())) {
            throw new IOException("Non-matching \"" + REQUEST_HASH_JSON + "\"");
        }
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.sendRedirect("home");
    }
}
