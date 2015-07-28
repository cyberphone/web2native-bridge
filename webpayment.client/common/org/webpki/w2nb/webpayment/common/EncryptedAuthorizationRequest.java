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

import java.security.PublicKey;

import java.security.interfaces.ECPublicKey;

import org.webpki.json.JSONAlgorithmPreferences;
import org.webpki.json.JSONObjectReader;

public abstract class EncryptedAuthorizationRequest implements BaseProperties {

    PublicKey publicKey;

    ECPublicKey ephemeralPublicKey;  // For ECHD only

    String contentEncryptionAlgorithm;

    byte[] iv;

    byte[] tag;

    String keyEncryptionAlgorithm;

    byte[] encryptedKeyData;  // For RSA only

    byte[] encryptedData;
    
    static boolean isRsaKey(String keyEncryptionAlgorithm) {
        return keyEncryptionAlgorithm.contains("RSA");
    }

    EncryptedAuthorizationRequest(JSONObjectReader rd) throws IOException {
        rd = rd.getObject(ENCRYPTED_DATA_JSON);
        contentEncryptionAlgorithm = rd.getString(ALGORITHM_JSON);
        iv = rd.getBinary(IV_JSON);
        tag = rd.getBinary(TAG_JSON);
        JSONObjectReader encryptedKey = rd.getObject(ENCRYPTED_KEY_JSON);
        keyEncryptionAlgorithm = encryptedKey.getString(ALGORITHM_JSON);
        if (isRsaKey(keyEncryptionAlgorithm)) {
            publicKey = encryptedKey.getPublicKey(JSONAlgorithmPreferences.JOSE);
            encryptedKeyData = encryptedKey.getBinary(CIPHER_TEXT_JSON);
        } else {
            publicKey = encryptedKey.getObject(PAYMENT_PROVIDER_KEY_JSON).getPublicKey(JSONAlgorithmPreferences.JOSE);
            ephemeralPublicKey = (ECPublicKey) encryptedKey.getObject(EPHEMERAL_CLIENT_KEY_JSON).getPublicKey(JSONAlgorithmPreferences.JOSE);
        }
        encryptedData = rd.getBinary(CIPHER_TEXT_JSON);
    }
}
