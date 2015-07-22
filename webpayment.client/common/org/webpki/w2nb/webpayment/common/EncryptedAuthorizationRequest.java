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
import java.security.interfaces.RSAPublicKey;

import java.util.Vector;

import org.webpki.json.JSONAlgorithmPreferences;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONOutputFormats;
import org.webpki.json.JSONParser;

public class EncryptedAuthorizationRequest implements BaseProperties {

    PublicKey publicKey;

    ECPublicKey ephemeralPublicKey;  // For ECHD only

    String contentEncryptionAlgorithm;

    byte[] iv;

    byte[] tag;

    String keyEncryptionAlgorithm;

    byte[] encryptedKeyData;  // For RSA only

    byte[] encryptedData;
    
    private static boolean isRsaKey(String keyEncryptionAlgorithm) {
        return keyEncryptionAlgorithm.contains("RSA");
    }

    public EncryptedAuthorizationRequest(JSONObjectReader rd) throws IOException {
        rd = rd.getObject(ENCRYPTED_DATA_JSON);
        contentEncryptionAlgorithm = rd.getString(ALGORITHM_JSON);
        iv = rd.getBinary(IV_JSON);
        tag = rd.getBinary(TAG_JSON);
        JSONObjectReader encryptedKey = rd.getObject(ENCRYPTED_KEY_JSON);
        keyEncryptionAlgorithm = encryptedKey.getString(ALGORITHM_JSON);
        if (isRsaKey(keyEncryptionAlgorithm)) {
            encryptedKeyData = encryptedKey.getBinary(CIPHER_TEXT_JSON);
        } else {
            publicKey = encryptedKey.getPublicKey(JSONAlgorithmPreferences.JOSE);
            ephemeralPublicKey = (ECPublicKey) encryptedKey.getPublicKey(JSONAlgorithmPreferences.JOSE);
        }
        encryptedData = rd.getBinary(CIPHER_TEXT_JSON);
        rd.checkForUnread();
    }

    public GenericAuthorizationRequest getDecryptedAuthorizationRequest(Vector<DecryptionKeyHolder> decryptionKeys) throws IOException, GeneralSecurityException {
        boolean notFound = true;
        for (DecryptionKeyHolder dkh : decryptionKeys) {
            if (dkh.publicKey.equals(publicKey)) {
                if (dkh.keyEncryptionAlgorithm.equals(keyEncryptionAlgorithm)) {
                    return new GenericAuthorizationRequest(JSONParser.parse(
                        CryptoSupport.contentEncryption(false,
                                                        contentEncryptionAlgorithm,
                                                        isRsaKey(keyEncryptionAlgorithm) ?
                            CryptoSupport.rsaDecryptKey(keyEncryptionAlgorithm, encryptedKeyData, dkh.privateKey)
                                :
                            CryptoSupport.serverKeyAgreement(keyEncryptionAlgorithm, ephemeralPublicKey, dkh.privateKey),
                                                        encryptedData,
                                                        iv,
                                                        tag)));
                }
                notFound = false;
            }
        }
        throw new IOException(notFound ? "No matching key found" : "No matching key+algorithm found");
    }

    public static JSONObjectWriter encode(JSONObjectWriter unencryptedRequest,
                                          String authUrl,
                                          String contentEncryptionAlgorithm,
                                          PublicKey encryptionKey,
                                          String keyEncryptionAlgorithm) throws IOException, GeneralSecurityException {
        
        byte[] content = unencryptedRequest.serializeJSONObject(JSONOutputFormats.NORMALIZED);
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes (iv);
        byte[] tag = new byte[16];
        new SecureRandom().nextBytes (tag);
        JSONObjectWriter encryptedRequest = Messages.createBaseMessage(Messages.PAYER_PULL_AUTH_REQ)
           .setString(BaseProperties.AUTH_URL_JSON, authUrl);
        JSONObjectWriter encryptedContent = encryptedRequest.setObject(BaseProperties.AUTH_DATA_JSON)
           .setObject(ENCRYPTED_DATA_JSON)
           .setString(ALGORITHM_JSON, contentEncryptionAlgorithm)
           .setBinary(IV_JSON, iv)
           .setBinary(TAG_JSON, tag);
        JSONObjectWriter keyEncryption = encryptedContent.setObject(BaseProperties.ENCRYPTED_KEY_JSON)
            .setString(BaseProperties.ALGORITHM_JSON, keyEncryptionAlgorithm);
        byte[] aesKey = null;
        if (isRsaKey(keyEncryptionAlgorithm)) {
            aesKey = new byte[32];
            new SecureRandom().nextBytes (aesKey);
            keyEncryption.setBinary(BaseProperties.CIPHER_TEXT_JSON,
                                    CryptoSupport.rsaEncryptKey(keyEncryptionAlgorithm, aesKey, encryptionKey));
        } else {
            ECPublicKey[] epk = new ECPublicKey[1];
            aesKey = CryptoSupport.clientKeyAgreement(keyEncryptionAlgorithm, epk, encryptionKey);
            keyEncryption.setObject(BaseProperties.PAYMENT_PROVIDER_KEY_JSON)
                .setPublicKey(encryptionKey, JSONAlgorithmPreferences.JOSE);
            keyEncryption.setObject(BaseProperties.EPHEMERAL_CLIENT_KEY_JSON)
                .setPublicKey(epk[0], JSONAlgorithmPreferences.JOSE);
        }
        encryptedContent.setBinary(BaseProperties.CIPHER_TEXT_JSON,
                                   CryptoSupport.contentEncryption(true,
                                                                   contentEncryptionAlgorithm,
                                                                   aesKey,
                                                                   content,
                                                                   iv,
                                                                   tag));
        return encryptedRequest;
    }
}
