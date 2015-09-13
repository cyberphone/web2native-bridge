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
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.GregorianCalendar;

import org.webpki.json.JSONAlgorithmPreferences;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;

public class FinalizeRequest implements BaseProperties {
    
    public FinalizeRequest(JSONObjectReader rd) throws IOException, GeneralSecurityException {
        root = Messages.parseBaseMessage(Messages.FINALIZE_REQUEST, rd);
        amount = rd.getBigDecimal(AMOUNT_JSON);
        embeddedResponse = new ReserveOrDebitResponse(rd.getObject(PROVIDER_AUTHORIZATION_JSON));
        timeStamp = rd.getDateTime(TIME_STAMP_JSON);
        software = new Software(rd);
        outerCertificatePath = rd.getSignature(JSONAlgorithmPreferences.JOSE).getCertificatePath();
        PaymentRequest paymentRequest = embeddedResponse.getPaymentRequest();
        ReserveOrDebitRequest.compareCertificatePaths(outerCertificatePath, paymentRequest);
        if (amount.compareTo(paymentRequest.getAmount()) > 0) {
            throw new IOException("Final amount must be less or equal to reserved amount");
        }
        rd.checkForUnread();
    }

    BigDecimal amount;
    
    GregorianCalendar timeStamp;

    Software software;
    
    X509Certificate[] outerCertificatePath;
    
    JSONObjectReader root;
    
    ReserveOrDebitResponse embeddedResponse;
    public ReserveOrDebitResponse getEmbeddedResponse() {
        return embeddedResponse;
    }

    String acquirerAuthorityUrl;
    public String getAcquirerAuthorityUrl() {
        return acquirerAuthorityUrl;
    }
 
    String clientIpAddress;
    public String getClientIpAddress() {
        return clientIpAddress;
    }

    public static JSONObjectWriter encode(ReserveOrDebitResponse providerResponse,
                                          BigDecimal amount,  // Less or equal the reserved amount
                                          ServerSigner signer)
    throws IOException, GeneralSecurityException {
        return Messages.createBaseMessage(Messages.FINALIZE_REQUEST)
            .setBigDecimal(AMOUNT_JSON, amount)
            .setObject(PROVIDER_AUTHORIZATION_JSON, providerResponse.root)
            .setDateTime(TIME_STAMP_JSON, new Date(), true)
            .setObject(SOFTWARE_JSON, Software.encode (PaymentRequest.SOFTWARE_ID,
                                                       PaymentRequest.SOFTWARE_VERSION))
            .setSignature(signer);
    }
}
