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
                                          String cardType,
                                          String cardNumber,
                                          Date dateTime,
                                          JSONX509Signer signer) throws IOException {
        return Messages.createBaseMessage(Messages.PAYER_GENERIC_AUTH_REQ)
            .setObject(PAYMENT_REQUEST_JSON, paymentRequest.root)
            .setString(DOMAIN_NAME_JSON, domainName)
            .setString(CARD_TYPE_JSON, cardType)
            .setString(CARD_NUMBER_JSON, cardNumber)
            .setDateTime(DATE_TIME_JSON, dateTime, false)
            .setObject(SOFTWARE_JSON, Software.encode(SOFTWARE_ID, SOFTWARE_VERSION))
            .setSignature (signer);
    }

    public static JSONObjectWriter encode(PaymentRequest paymentRequest,
                                          String domainName,
                                          String cardType,
                                          String cardNumber,
                                          AsymSignatureAlgorithms signatureAlgorithm,
                                          SignerInterface signer) throws IOException {
        return encode(paymentRequest,
                      domainName,
                      cardType,
                      cardNumber,
                      new Date(),
                      new JSONX509Signer(signer).setSignatureAlgorithm(signatureAlgorithm)
                                                .setSignatureCertificateAttributes(true)
                                                .setAlgorithmPreferences(JSONAlgorithmPreferences.JOSE));
    }

    public static String formatCardNumber(String cardNumber) {
        StringBuffer s = new StringBuffer();
        int q = 0;
        for (char c : cardNumber.toCharArray()) {
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
    
    String cardType;
    
    String cardNumber;
    
    GregorianCalendar dateTime;
    
    Software software;
    
    JSONSignatureDecoder signatureDecoder;
    
    JSONObjectReader root;
    
    public GenericAuthorizationRequest(JSONObjectReader rd) throws IOException {
        root = Messages.parseBaseMessage(Messages.PAYER_GENERIC_AUTH_REQ, rd);
        paymentRequest = new PaymentRequest(rd.getObject(PAYMENT_REQUEST_JSON));
        domainName = rd.getString(DOMAIN_NAME_JSON);
        cardType = rd.getString(CARD_TYPE_JSON);
        cardNumber = rd.getString(CARD_NUMBER_JSON);
        dateTime = rd.getDateTime(DATE_TIME_JSON);
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

    public String getCardType() {
        return cardType;
    }

    public String getCardNumber() {
        return cardNumber;
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
