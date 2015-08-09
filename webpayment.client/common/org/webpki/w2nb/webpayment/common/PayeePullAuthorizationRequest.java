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
import org.webpki.json.JSONParser;
import org.webpki.util.ArrayUtil;

public class PayeePullAuthorizationRequest extends EncryptedAuthorizationRequest {

    byte[] requestHash;
    
    String referenceId;
    
    String clientIpAddress;

    GregorianCalendar dateTime;

    Software software;
    
    X509Certificate[] outerCertificatePath;

    public PayeePullAuthorizationRequest(JSONObjectReader rd) throws IOException {
        super(Messages.parseBaseMessage(Messages.PAYEE_PULL_AUTH_REQ, rd).getObject(AUTH_DATA_JSON));
        if (!rd.getObject(REQUEST_HASH_JSON).getString(ALGORITHM_JSON).equals(JOSE_SHA_256_ALG_ID)) {
            throw new IOException("Expected algorithm: " + JOSE_SHA_256_ALG_ID);
        }
        requestHash = rd.getObject(REQUEST_HASH_JSON).getBinary(VALUE_JSON);
        clientIpAddress = rd.getString(CLIENT_IP_ADDRESS_JSON);
        referenceId = rd.getString(REFERENCE_ID_JSON);
        dateTime = rd.getDateTime(DATE_TIME_JSON);
        software = new Software(rd);
        outerCertificatePath = rd.getSignature(JSONAlgorithmPreferences.JOSE).getCertificatePath();
        rd.checkForUnread();
    }

    public String getClientIpAddress() {
        return clientIpAddress;
    }

    public static JSONObjectWriter encode(JSONObjectReader encryptedRequest,
                                          byte[] requestHash,
                                          String clientIpAddress,
                                          String referenceId,
                                          ServerSigner signer)
        throws IOException, GeneralSecurityException {
        return Messages.createBaseMessage(Messages.PAYEE_PULL_AUTH_REQ)
            .setObject(AUTH_DATA_JSON, encryptedRequest)
            .setObject(REQUEST_HASH_JSON, new JSONObjectWriter()
                                              .setString(ALGORITHM_JSON, JOSE_SHA_256_ALG_ID)
                                              .setBinary(VALUE_JSON, requestHash))
            .setString(CLIENT_IP_ADDRESS_JSON, clientIpAddress)
            .setString(REFERENCE_ID_JSON, referenceId)
            .setDateTime(DATE_TIME_JSON, new Date(), true)
            .setObject(SOFTWARE_JSON, Software.encode (PaymentRequest.SOFTWARE_ID,
                                                       PaymentRequest.SOFTWARE_VERSION))
            .setSignature(signer);
    }

    void assertTrue(boolean assertion) throws IOException {
        if (!assertion) {
            throw new IOException("Outer and inner certificate paths differ");
        }
    }

    public GenericAuthorizationRequest getDecryptedAuthorizationRequest(Vector<DecryptionKeyHolder> decryptionKeys)
    throws IOException, GeneralSecurityException {
        boolean notFound = true;
        for (DecryptionKeyHolder dkh : decryptionKeys) {
            if (dkh.publicKey.equals(publicKey)) {
                if (dkh.keyEncryptionAlgorithm.equals(keyEncryptionAlgorithm)) {
                    GenericAuthorizationRequest genericAuthorizationRequest =
                        new GenericAuthorizationRequest(JSONParser.parse(
                            CryptoSupport.contentEncryption(false,
                                                            contentEncryptionAlgorithm,
                                                            isRsaKey(keyEncryptionAlgorithm) ?
                                     CryptoSupport.rsaDecryptKey(keyEncryptionAlgorithm,
                                                                 encryptedKeyData,
                                                                 dkh.privateKey)
                                        :
                                     CryptoSupport.serverKeyAgreement(keyEncryptionAlgorithm,
                                                                      ephemeralPublicKey,
                                                                      dkh.privateKey),
                                                            encryptedData,
                                                            iv,
                                                            tag)));
                    X509Certificate[] certificatePath = genericAuthorizationRequest.paymentRequest.signatureDecoder.getCertificatePath();
                    assertTrue(certificatePath.length == outerCertificatePath.length);
                    for (int q = 0; q < certificatePath.length; q++) {
                        assertTrue(certificatePath[q].equals(outerCertificatePath[q]));
                    }
                    if (!ArrayUtil.compare(requestHash, genericAuthorizationRequest.paymentRequest.getRequestHash())) {
                        throw new IOException("Non-matching \"" + REQUEST_HASH_JSON + "\" value");
                    }
                    if (!referenceId.equals(genericAuthorizationRequest.paymentRequest.getReferenceId())) {
                        throw new IOException("Non-matching \"" + REFERENCE_ID_JSON + "\" value");
                    }
                    return genericAuthorizationRequest;
                }
                notFound = false;
            }
        }
        throw new IOException(notFound ? "No matching key found" : "No matching key+algorithm found");
    }
}
