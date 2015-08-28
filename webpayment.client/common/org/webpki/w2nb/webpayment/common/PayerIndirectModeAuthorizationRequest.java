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
import java.security.PublicKey;
import java.security.SecureRandom;

import java.security.interfaces.ECPublicKey;

import org.webpki.json.JSONAlgorithmPreferences;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONOutputFormats;

public class PayerIndirectModeAuthorizationRequest extends EncryptedAuthorizationRequest {
    
    String authUrl;

    public PayerIndirectModeAuthorizationRequest(JSONObjectReader rd) throws IOException {
        super(Messages.parseBaseMessage(Messages.PAYER_INDIRECT_AUTH_REQ, rd).getObject(AUTH_DATA_JSON));
        authUrl = rd.getString(AUTH_URL_JSON);
        rd.checkForUnread();
    }
    
    public String getAuthUrl() {
        return authUrl;
    }

    public static JSONObjectWriter encode(JSONObjectWriter unencryptedRequest,
                                          String authUrl,
                                          String contentEncryptionAlgorithm,
                                          PublicKey keyEncryptionKey,
                                          String keyEncryptionAlgorithm) throws IOException, GeneralSecurityException {
        JSONObjectWriter encryptedRequest = Messages.createBaseMessage(Messages.PAYER_INDIRECT_AUTH_REQ)
            .setString(AUTH_URL_JSON, authUrl);
        byte[] content = unencryptedRequest.serializeJSONObject(JSONOutputFormats.NORMALIZED);
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes (iv);
        byte[] tag = new byte[16];
        new SecureRandom().nextBytes (tag);
        JSONObjectWriter encryptedContent = encryptedRequest.setObject(AUTH_DATA_JSON)
            .setObject(ENCRYPTED_DATA_JSON)
                .setString(ALGORITHM_JSON, contentEncryptionAlgorithm)
                .setBinary(IV_JSON, iv)
                .setBinary(TAG_JSON, tag);
        JSONObjectWriter keyEncryption = encryptedContent.setObject(ENCRYPTED_KEY_JSON)
            .setString(ALGORITHM_JSON, keyEncryptionAlgorithm);
        byte[] contentEncryptionKey = null;
        if (isRsaKey(keyEncryptionAlgorithm)) {
            keyEncryption.setPublicKey(keyEncryptionKey, JSONAlgorithmPreferences.JOSE);
            contentEncryptionKey = new byte[32];
            new SecureRandom().nextBytes (contentEncryptionKey);
            keyEncryption.setBinary(CIPHER_TEXT_JSON,
                                    CryptoSupport.rsaEncryptKey(keyEncryptionAlgorithm,
                                                                contentEncryptionKey,
                                                                keyEncryptionKey));
        } else {
            ECPublicKey[] ephemeralKey = new ECPublicKey[1];
            contentEncryptionKey = CryptoSupport.clientKeyAgreement(keyEncryptionAlgorithm,
                                                                    ephemeralKey,
                                                                    keyEncryptionKey);
            keyEncryption.setObject(PAYMENT_PROVIDER_KEY_JSON)
                .setPublicKey(keyEncryptionKey, JSONAlgorithmPreferences.JOSE);
            keyEncryption.setObject(EPHEMERAL_CLIENT_KEY_JSON)
                .setPublicKey(ephemeralKey[0], JSONAlgorithmPreferences.JOSE);
        }
        encryptedContent.setBinary(CIPHER_TEXT_JSON,
                                   CryptoSupport.contentEncryption(true,
                                                                   contentEncryptionAlgorithm,
                                                                   contentEncryptionKey,
                                                                   content,
                                                                   iv,
                                                                   tag));
        return encryptedRequest;
    }
}
