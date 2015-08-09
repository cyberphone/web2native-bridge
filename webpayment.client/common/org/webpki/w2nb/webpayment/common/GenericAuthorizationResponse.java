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

import org.webpki.json.JSONAlgorithmPreferences;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONSignatureDecoder;
import org.webpki.json.JSONX509Signer;

public class GenericAuthorizationResponse implements BaseProperties {
    
    public static final String SOFTWARE_ID      = "WebPKI.org - Bank";
    public static final String SOFTWARE_VERSION = "1.00";
    
    public static JSONObjectWriter encode(PaymentRequest paymentRequest,
                                          String cardType,
                                          String cardNumber,
                                          String referenceId,
                                          JSONX509Signer signer) throws IOException {
        StringBuffer cardReference = new StringBuffer();
        int q = cardNumber.length() - 4;
        for (char c : cardNumber.toCharArray()) {
            cardReference.append((--q < 0) ? c : '*');
        }
        return Messages.createBaseMessage(Messages.PROVIDER_GENERIC_AUTH_RES)
            .setObject(PAYMENT_REQUEST_JSON, paymentRequest.root)
            .setString(CARD_TYPE_JSON, cardType)
            .setString(CARD_REFERENCE_JSON, cardReference.toString())
            .setString(REFERENCE_ID_JSON, referenceId)
            .setDateTime(DATE_TIME_JSON, new Date(), true)
            .setString(SOFTWARE_ID_JSON, SOFTWARE_ID)
            .setString(SOFTWARE_VERSION_JSON, SOFTWARE_VERSION)
            .setSignature (signer);
    }

    PaymentRequest paymentRequest;
    
    String cardType;
    
    String cardReference;
    
    String referenceId;
    
    GregorianCalendar dateTime;
    
    String softwareId;
    
    String softwareVersion;
    
    JSONSignatureDecoder signatureDecoder;
    
    JSONObjectReader root;
    
    public GenericAuthorizationResponse(JSONObjectReader rd) throws IOException {
        root = Messages.parseBaseMessage(Messages.PROVIDER_GENERIC_AUTH_RES, rd);
        paymentRequest = new PaymentRequest(rd.getObject(PAYMENT_REQUEST_JSON));
        cardType = rd.getString(CARD_TYPE_JSON);
        cardReference = rd.getString(CARD_REFERENCE_JSON);
        referenceId = rd.getString(REFERENCE_ID_JSON);
        dateTime = rd.getDateTime(DATE_TIME_JSON);
        softwareId = rd.getString(SOFTWARE_ID_JSON);
        softwareVersion = rd.getString(SOFTWARE_VERSION_JSON);
        signatureDecoder = rd.getSignature(JSONAlgorithmPreferences.JOSE);
        rd.checkForUnread();
    }

    public PaymentRequest getPaymentRequest() {
        return paymentRequest;
    }

    public String getCardType() {
        return cardType;
    }

    public String getCardReference() {
        return cardReference;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public GregorianCalendar getDateTime() {
        return dateTime;
    }

    public String getSoftwareId() {
        return softwareId;
    }

    public String getSoftwareVersion() {
        return softwareVersion;
    }

    public JSONSignatureDecoder getSignatureDecoder() {
        return signatureDecoder;
    }

    public JSONObjectReader getRoot() {
        return root;
    }
}
