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

import java.security.cert.X509Certificate;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Vector;

import org.webpki.json.JSONAlgorithmPreferences;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;

import org.webpki.util.ArrayUtil;

public class ReserveOrDebitRequest implements BaseProperties {
    
    public static final String JOSE_SHA_256_ALG_ID = "S256";              // Well, not really JOSE but "similar" :-)

    public ReserveOrDebitRequest(JSONObjectReader rd) throws IOException {
        encryptedData = EncryptedData.parse(Messages.parseBaseMessage(Messages.RESERVE_FUNDS_REQUEST, rd).getObject(AUTHORIZATION_DATA_JSON));
        if (!rd.getObject(REQUEST_HASH_JSON).getString(ALGORITHM_JSON).equals(JOSE_SHA_256_ALG_ID)) {
            throw new IOException("Expected algorithm: " + JOSE_SHA_256_ALG_ID);
        }
        requestHash = rd.getObject(REQUEST_HASH_JSON).getBinary(VALUE_JSON);
        accountType = AccountTypes.fromType(rd.getString(ACCOUNT_TYPE_JSON));
        referenceId = rd.getString(REFERENCE_ID_JSON);
        acquirerAuthorityUrl = rd.getString(ACQUIRER_AUTHORITY_URL_JSON);
        clientIpAddress = rd.getString(CLIENT_IP_ADDRESS_JSON);
        dateTime = rd.getDateTime(TIME_STAMP_JSON);
        software = new Software(rd);
        outerCertificatePath = rd.getSignature(JSONAlgorithmPreferences.JOSE).getCertificatePath();
        rd.checkForUnread();
    }

    byte[] requestHash;
    
    AccountTypes accountType;
    
    String referenceId;
    
    GregorianCalendar dateTime;

    Software software;
    
    X509Certificate[] outerCertificatePath;
    
    EncryptedData encryptedData;

    String acquirerAuthorityUrl;
    public String getAcquirerAuthorityUrl() {
        return acquirerAuthorityUrl;
    }
 
    String clientIpAddress;
    public String getClientIpAddress() {
        return clientIpAddress;
    }

    public static JSONObjectWriter encode(JSONObjectReader encryptedRequest,
                                          byte[] requestHash,
                                          AccountTypes accountType,
                                          String referenceId,
                                          String acquirerAuthorityUrl,
                                          String clientIpAddress,
                                          ServerSigner signer)
        throws IOException, GeneralSecurityException {
        return Messages.createBaseMessage(Messages.RESERVE_FUNDS_REQUEST)
            .setObject(AUTHORIZATION_DATA_JSON, encryptedRequest)
            .setObject(REQUEST_HASH_JSON, new JSONObjectWriter()
                                              .setString(ALGORITHM_JSON, JOSE_SHA_256_ALG_ID)
                                              .setBinary(VALUE_JSON, requestHash))
            .setString(ACCOUNT_TYPE_JSON, accountType.getType())
            .setString(REFERENCE_ID_JSON, referenceId)
            .setString(ACQUIRER_AUTHORITY_URL_JSON, acquirerAuthorityUrl)
            .setString(CLIENT_IP_ADDRESS_JSON, clientIpAddress)
            .setDateTime(TIME_STAMP_JSON, new Date(), true)
            .setObject(SOFTWARE_JSON, Software.encode (PaymentRequest.SOFTWARE_ID,
                                                       PaymentRequest.SOFTWARE_VERSION))
            .setSignature(signer);
    }

    static void assertTrue(boolean assertion) throws IOException {
        if (!assertion) {
            throw new IOException("Outer and inner certificate paths differ");
        }
    }

    public static void compareCertificatePaths(X509Certificate[] outer, PaymentRequest paymentRequest) throws IOException {
        X509Certificate[] inner = paymentRequest.signatureDecoder.getCertificatePath();
        assertTrue(inner.length == outer.length);
        for (int q = 0; q < inner.length; q++) {
            assertTrue(outer[q].equals(inner[q]));
        }
    }

    public AuthorizationData getDecryptedAuthorizationRequest(Vector<DecryptionKeyHolder> decryptionKeys)
    throws IOException, GeneralSecurityException {
        AuthorizationData genericAuthorizationRequest =
            new AuthorizationData(encryptedData.getDecryptedData(decryptionKeys));
        compareCertificatePaths(outerCertificatePath, genericAuthorizationRequest.paymentRequest);
        if (!ArrayUtil.compare(requestHash, genericAuthorizationRequest.paymentRequest.getRequestHash())) {
            throw new IOException("Non-matching \"" + REQUEST_HASH_JSON + "\" value");
        }
        if (!referenceId.equals(genericAuthorizationRequest.paymentRequest.getReferenceId())) {
            throw new IOException("Non-matching \"" + REFERENCE_ID_JSON + "\" value");
        }
        return genericAuthorizationRequest;
    }
}