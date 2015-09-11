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
import java.security.GeneralSecurityException;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Vector;

import org.webpki.json.JSONAlgorithmPreferences;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONSignatureDecoder;
import org.webpki.json.JSONX509Signer;

public class GenericAuthorizationResponse implements BaseProperties {
    
    public static final String SOFTWARE_ID      = "WebPKI.org - Bank";
    public static final String SOFTWARE_VERSION = "1.00";
    
    public static JSONObjectWriter encode(PaymentRequest paymentRequest,
                                          String accountType,
                                          String accountId,
                                          JSONObjectWriter encryptedCardData,
                                          String referenceId,
                                          JSONX509Signer signer) throws IOException {
        StringBuffer accountReference = new StringBuffer();
        int q = accountId.length() - 4;
        for (char c : accountId.toCharArray()) {
            accountReference.append((--q < 0) ? c : '*');
        }
        JSONObjectWriter rd = Messages.createBaseMessage(Messages.RESERVE_FUNDS_RESPONSE)
            .setObject(PAYMENT_REQUEST_JSON, paymentRequest.root)
            .setString(ACCOUNT_TYPE_JSON, accountType)
            .setString(ACCOUNT_REFERENCE_JSON, accountReference.toString());
        if (encryptedCardData != null) {
            rd.setObject(PROTECTED_ACCOUNT_DATA_JSON, encryptedCardData);
        }
        return rd.setString(REFERENCE_ID_JSON, referenceId)
                 .setDateTime(TIME_STAMP_JSON, new Date(), true)
                 .setObject(SOFTWARE_JSON, Software.encode(SOFTWARE_ID, SOFTWARE_VERSION))
                 .setSignature (signer);
    }
    
    EncryptedData encryptedData;
    
    JSONObjectReader root;
    
    public GenericAuthorizationResponse(JSONObjectReader rd) throws IOException {
        root = Messages.parseBaseMessage(Messages.RESERVE_FUNDS_RESPONSE, rd);
        paymentRequest = new PaymentRequest(rd.getObject(PAYMENT_REQUEST_JSON));
        accountType = rd.getString(ACCOUNT_TYPE_JSON);
        accountReference = rd.getString(ACCOUNT_REFERENCE_JSON);
        if (rd.hasProperty(PROTECTED_ACCOUNT_DATA_JSON)) {
            encryptedData = EncryptedData.parse(rd.getObject(PROTECTED_ACCOUNT_DATA_JSON));
        }
        referenceId = rd.getString(REFERENCE_ID_JSON);
        timeStamp = rd.getDateTime(TIME_STAMP_JSON);
        software = new Software(rd);
        signatureDecoder = rd.getSignature(JSONAlgorithmPreferences.JOSE);
        rd.checkForUnread();
    }

    public boolean isAccount2Account() {
        return encryptedData == null;
    }
    
    public ProtectedAccountData getProtectedAccountData(Vector<DecryptionKeyHolder> decryptionKeys) throws IOException, GeneralSecurityException {
        return new ProtectedAccountData(encryptedData.getDecryptedData(decryptionKeys));
    }

    PaymentRequest paymentRequest;
    public PaymentRequest getPaymentRequest() {
        return paymentRequest;
    }

    String accountType;
    public String getAccountType() {
        return accountType;
    }

    String accountReference;
    public String getAccountReference() {
        return accountReference;
    }

    String referenceId;
    public String getReferenceId() {
        return referenceId;
    }

    GregorianCalendar timeStamp;
    public GregorianCalendar getTimeStamp() {
        return timeStamp;
    }

    Software software;
    public Software getSoftware() {
        return software;
    }

    JSONSignatureDecoder signatureDecoder;
    public JSONSignatureDecoder getSignatureDecoder() {
        return signatureDecoder;
    }
}
