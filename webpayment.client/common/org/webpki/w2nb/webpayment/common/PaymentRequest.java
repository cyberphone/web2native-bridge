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

import java.math.BigDecimal;

import java.util.Date;
import java.util.GregorianCalendar;

import org.webpki.crypto.HashAlgorithms;

import org.webpki.json.JSONAlgorithmPreferences;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONOutputFormats;
import org.webpki.json.JSONSignatureDecoder;
import org.webpki.json.JSONSignatureTypes;
import org.webpki.json.JSONX509Signer;

public class PaymentRequest implements BaseProperties {
    
    public static final String SOFTWARE_ID      = "WebPKI.org - Merchant";
    public static final String SOFTWARE_VERSION = "1.00";

    public static JSONObjectWriter encode(String payee,
                                          BigDecimal amount,
                                          Currencies currency,
                                          String referenceId,
                                          JSONX509Signer x509Signer) throws IOException {
        return new JSONObjectWriter()
            .setString(PAYEE_JSON, payee)
            .setBigDecimal(AMOUNT_JSON, amount)
            .setString(CURRENCY_JSON, currency.toString())
            .setString(REFERENCE_ID_JSON, referenceId)
            .setDateTime(DATE_TIME_JSON, new Date(), true)
            .setString(SOFTWARE_ID_JSON, SOFTWARE_ID)
            .setString(SOFTWARE_VERSION_JSON, SOFTWARE_VERSION)
            .setSignature(x509Signer);
    }

    String payee;

    BigDecimal amount;

    Currencies currency;

    String referenceId;
    
    GregorianCalendar dateTime;

    String softwareId;
    
    String softwareVersion;
    
    JSONSignatureDecoder signatureDecoder;
    
    JSONObjectReader root;
    
    public PaymentRequest(JSONObjectReader rd) throws IOException {
        root = rd;
        payee = rd.getString(PAYEE_JSON);
        amount = rd.getBigDecimal(AMOUNT_JSON);
        try {
            currency = Currencies.valueOf(rd.getString(CURRENCY_JSON));
        } catch (Exception e) {
            throw new IOException(e);
        }
        referenceId = rd.getString(REFERENCE_ID_JSON);
        dateTime = rd.getDateTime(DATE_TIME_JSON);
        softwareId = rd.getString(SOFTWARE_ID_JSON);
        softwareVersion = rd.getString(SOFTWARE_VERSION_JSON);
        signatureDecoder = rd.getSignature(JSONAlgorithmPreferences.JOSE);
        signatureDecoder.verify(JSONSignatureTypes.X509_CERTIFICATE);
        rd.checkForUnread();
    }

    public String getPayee() {
        return payee;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Currencies getCurrency() {
        return currency;
    }

    public String getReferenceId() {
        return referenceId;
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

    public byte[] getRequestHash() throws IOException {
        return getRequestHash(new JSONObjectWriter(root));
    }

    public static byte[] getRequestHash(JSONObjectWriter paymentRequest) throws IOException {
        return HashAlgorithms.SHA256.digest(paymentRequest.serializeJSONObject(JSONOutputFormats.NORMALIZED));
    }
}
