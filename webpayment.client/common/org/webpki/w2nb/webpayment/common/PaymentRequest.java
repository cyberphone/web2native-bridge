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

import java.security.cert.X509Certificate;

import java.util.Date;
import java.util.GregorianCalendar;

import org.webpki.json.JSONAlgorithmPreferences;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONX509Signer;

public class PaymentRequest implements BaseProperties {
    
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
            .setSignature(x509Signer);
    }

    String payee;

    BigDecimal amount;

    Currencies currency;

    String referenceId;
    
    GregorianCalendar dateTime;

    X509Certificate[] certificatePath;   
    
    public PaymentRequest(JSONObjectReader reader) throws IOException {
        payee = reader.getString(PAYEE_JSON);
        amount = reader.getBigDecimal(AMOUNT_JSON);
        try {
            currency = Currencies.valueOf(reader.getString(CURRENCY_JSON));
        } catch (Exception e) {
            throw new IOException(e);
        }
        referenceId = reader.getString(REFERENCE_ID_JSON);
        dateTime = reader.getDateTime(DATE_TIME_JSON);
        certificatePath = reader.getSignature(JSONAlgorithmPreferences.JOSE).getCertificatePath();
        reader.checkForUnread();
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

    public X509Certificate[] getCertificatePath() {
        return certificatePath;
    }
}
