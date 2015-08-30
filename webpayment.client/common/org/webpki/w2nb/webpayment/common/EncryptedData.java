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

import java.util.Vector;

import org.webpki.json.JSONAlgorithmPreferences;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONOutputFormats;
import org.webpki.json.JSONParser;

public class EncryptedData implements BaseProperties {

    private PublicKey publicKey;

    private ECPublicKey ephemeralPublicKey;  // For ECHD only

    private String contentEncryptionAlgorithm;

    private byte[] iv;

    private byte[] tag;

    private String keyEncryptionAlgorithm;

    private byte[] encryptedKeyData;  // For RSA only

    private byte[] encryptedData;
    
    private byte[] authenticatedData;
    
    static boolean isRsaKey(String keyEncryptionAlgorithm) {
        return keyEncryptionAlgorithm.contains("RSA");
    }
    
    public static EncryptedData parse(JSONObjectReader rd) throws IOException {
        return new EncryptedData(rd);
    }

    private EncryptedData(JSONObjectReader rd) throws IOException {
        rd = rd.getObject(ENCRYPTED_DATA_JSON);
        contentEncryptionAlgorithm = rd.getString(ALGORITHM_JSON);
        iv = rd.getBinary(IV_JSON);
        tag = rd.getBinary(TAG_JSON);
        JSONObjectReader encryptedKey = rd.getObject(ENCRYPTED_KEY_JSON);
        authenticatedData = encryptedKey.serializeJSONObject(JSONOutputFormats.NORMALIZED);
        keyEncryptionAlgorithm = encryptedKey.getString(ALGORITHM_JSON);
        if (isRsaKey(keyEncryptionAlgorithm)) {
            publicKey = encryptedKey.getPublicKey(JSONAlgorithmPreferences.JOSE);
            encryptedKeyData = encryptedKey.getBinary(CIPHER_TEXT_JSON);
        } else {
            publicKey = encryptedKey.getObject(STATIC_PROVIDER_KEY_JSON).getPublicKey(JSONAlgorithmPreferences.JOSE);
            ephemeralPublicKey = (ECPublicKey) encryptedKey.getObject(EPHEMERAL_KEY_JSON).getPublicKey(JSONAlgorithmPreferences.JOSE);
        }
        encryptedData = rd.getBinary(CIPHER_TEXT_JSON);
    }

    public JSONObjectReader getDecryptedData(Vector<DecryptionKeyHolder> decryptionKeys)
    throws IOException, GeneralSecurityException {
        boolean notFound = true;
        for (DecryptionKeyHolder dkh : decryptionKeys) {
            if (dkh.publicKey.equals(publicKey)) {
                notFound = false;
                if (dkh.keyEncryptionAlgorithm.equals(keyEncryptionAlgorithm)) {
                    return JSONParser.parse(
                        CryptoSupport.contentEncryption(false,
                                                        contentEncryptionAlgorithm,
                                                        isRsaKey(keyEncryptionAlgorithm) ?
                                 CryptoSupport.rsaDecryptKey(keyEncryptionAlgorithm,
                                                             encryptedKeyData,
                                                             dkh.privateKey)
                                    :
                                 CryptoSupport.serverKeyAgreement(keyEncryptionAlgorithm,
                                                                  contentEncryptionAlgorithm,
                                                                  ephemeralPublicKey,
                                                                  dkh.privateKey),
                                                        encryptedData,
                                                        iv,
                                                        tag,
                                                        authenticatedData));
                }
            }
        }
        throw new IOException(notFound ? "No matching key found" : "No matching key+algorithm found");
    }

    public static JSONObjectWriter encode(JSONObjectWriter unencryptedData,
                                          String contentEncryptionAlgorithm,
                                          PublicKey keyEncryptionKey,
                                          String keyEncryptionAlgorithm)
    throws IOException, GeneralSecurityException {
        byte[] content = unencryptedData.serializeJSONObject(JSONOutputFormats.NORMALIZED);
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes (iv);
        byte[] tag = new byte[16];
        new SecureRandom().nextBytes (tag);
        JSONObjectWriter encryptedContent = new JSONObjectWriter();
        JSONObjectWriter encryptedData = encryptedContent.setObject(ENCRYPTED_DATA_JSON);
        JSONObjectWriter encryptedKey = encryptedData.setObject(ENCRYPTED_KEY_JSON)
            .setString(ALGORITHM_JSON, keyEncryptionAlgorithm);
        byte[] contentEncryptionKey = null;
        if (EncryptedData.isRsaKey(keyEncryptionAlgorithm)) {
            encryptedKey.setPublicKey(keyEncryptionKey, JSONAlgorithmPreferences.JOSE);
            contentEncryptionKey = new byte[32];
            new SecureRandom().nextBytes (contentEncryptionKey);
            encryptedKey.setBinary(CIPHER_TEXT_JSON,
            CryptoSupport.rsaEncryptKey(keyEncryptionAlgorithm,
                                        contentEncryptionKey,
                                        keyEncryptionKey));
        } else {
            ECPublicKey[] ephemeralKey = new ECPublicKey[1];
            contentEncryptionKey = CryptoSupport.clientKeyAgreement(keyEncryptionAlgorithm,
                                                                    contentEncryptionAlgorithm,
                                                                    ephemeralKey,
                                                                    keyEncryptionKey);
            encryptedKey.setObject(STATIC_PROVIDER_KEY_JSON)
                .setPublicKey(keyEncryptionKey, JSONAlgorithmPreferences.JOSE);
            encryptedKey.setObject(EPHEMERAL_KEY_JSON)
                .setPublicKey(ephemeralKey[0], JSONAlgorithmPreferences.JOSE);
        }
        encryptedData.setString(ALGORITHM_JSON, contentEncryptionAlgorithm)
                     .setBinary(IV_JSON, iv)
                     .setBinary(TAG_JSON, tag)
                     .setBinary(CIPHER_TEXT_JSON,
            CryptoSupport.contentEncryption(true,
                                            contentEncryptionAlgorithm,
                                            contentEncryptionKey,
                                            content,
                                            iv,
                                            tag,
                                            encryptedKey.serializeJSONObject(JSONOutputFormats.NORMALIZED)));
        return encryptedContent;
    }
}
