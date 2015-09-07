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
package org.webpki.w2nb.webpayment.common;

import java.io.IOException;

import java.util.Date;
import java.util.GregorianCalendar;

import org.webpki.crypto.AsymSignatureAlgorithms;
import org.webpki.crypto.SignerInterface;

import org.webpki.json.JSONAlgorithmPreferences;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONSignatureDecoder;
import org.webpki.json.JSONX509Signer;

public class GenericAuthorizationRequest implements BaseProperties {

    public static final String SOFTWARE_ID      = "WebPKI.org - Wallet";
    public static final String SOFTWARE_VERSION = "1.00";

    public static JSONObjectWriter encode(PaymentRequest paymentRequest,
                                          String domainName,
                                          String accountType,
                                          String accountId,
                                          Date dateTime,
                                          JSONX509Signer signer) throws IOException {
        return Messages.createBaseMessage(Messages.PAYER_GENERIC_AUTH_REQ)
            .setObject(PAYMENT_REQUEST_JSON, paymentRequest.root)
            .setString(DOMAIN_NAME_JSON, domainName)
            .setString(ACCOUNT_TYPE_JSON, accountType)
            .setString(ACCOUNT_ID_JSON, accountId)
            .setDateTime(TIME_STAMP_JSON, dateTime, false)
            .setObject(SOFTWARE_JSON, Software.encode(SOFTWARE_ID, SOFTWARE_VERSION))
            .setSignature (signer);
    }

    public static JSONObjectWriter encode(PaymentRequest paymentRequest,
                                          String domainName,
                                          String accountType,
                                          String accountId,
                                          AsymSignatureAlgorithms signatureAlgorithm,
                                          SignerInterface signer) throws IOException {
        return encode(paymentRequest,
                      domainName,
                      accountType,
                      accountId,
                      new Date(),
                      new JSONX509Signer(signer).setSignatureAlgorithm(signatureAlgorithm)
                                                .setSignatureCertificateAttributes(true)
                                                .setAlgorithmPreferences(JSONAlgorithmPreferences.JOSE));
    }

    public static String formatCardNumber(String accountId) {
        StringBuffer s = new StringBuffer();
        int q = 0;
        for (char c : accountId.toCharArray()) {
            if (q != 0 && q % 4 == 0) {
                s.append(' ');
            }
            s.append(c);
            q++;
        }
        return s.toString();
    }
    
    PaymentRequest paymentRequest;
    
    String domainName;
    
    String accountType;
    
    String accountId;
    
    GregorianCalendar dateTime;
    
    Software software;
    
    JSONSignatureDecoder signatureDecoder;
    
    JSONObjectReader root;
    
    public GenericAuthorizationRequest(JSONObjectReader rd) throws IOException {
        root = Messages.parseBaseMessage(Messages.PAYER_GENERIC_AUTH_REQ, rd);
        paymentRequest = new PaymentRequest(rd.getObject(PAYMENT_REQUEST_JSON));
        domainName = rd.getString(DOMAIN_NAME_JSON);
        accountType = rd.getString(ACCOUNT_TYPE_JSON);
        accountId = rd.getString(ACCOUNT_ID_JSON);
        dateTime = rd.getDateTime(TIME_STAMP_JSON);
        software = new Software(rd);
        signatureDecoder = rd.getSignature(JSONAlgorithmPreferences.JOSE);
        rd.checkForUnread();
    }

    public PaymentRequest getPaymentRequest() {
        return paymentRequest;
    }

    public String getDomainName() {
        return domainName;
    }

    public String getAccountType() {
        return accountType;
    }

    public String getAccountId() {
        return accountId;
    }

    public GregorianCalendar getDateTime() {
        return dateTime;
    }

    public Software getSoftware() {
        return software;
    }

    public JSONSignatureDecoder getSignatureDecoder() {
        return signatureDecoder;
    }

    public JSONObjectReader getRoot() {
        return root;
    }
}
