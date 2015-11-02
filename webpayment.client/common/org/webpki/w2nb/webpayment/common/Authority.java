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

import java.security.interfaces.RSAPublicKey;

import java.util.Date;
import java.util.GregorianCalendar;

import org.webpki.crypto.AlgorithmPreferences;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONSignatureDecoder;
import org.webpki.json.JSONSignatureTypes;

public class Authority implements BaseProperties {
    
    public static JSONObjectWriter encode(String authorityUrl,
                                          String transactionUrl,
                                          PublicKey publicKey,
                                          Date expires,
                                          ServerSigner signer) throws IOException {
        return Messages.createBaseMessage(Messages.AUTHORITY)
            .setString(AUTHORITY_URL_JSON, authorityUrl)
            .setString(TRANSACTION_URL_JSON, transactionUrl)
            .setObject(ENCRYPTION_PARAMETERS_JSON, new JSONObjectWriter()
                .setString(BaseProperties.DATA_ENCRYPTION_ALGORITHM_JSON, Encryption.JOSE_A128CBC_HS256_ALG_ID)
                .setString(BaseProperties.KEY_ENCRYPTION_ALGORITHM_JSON, 
                             publicKey instanceof RSAPublicKey ?
                           Encryption.JOSE_RSA_OAEP_256_ALG_ID : Encryption.JOSE_ECDH_ES_ALG_ID)
                .setPublicKey(publicKey, AlgorithmPreferences.JOSE))
             .setDateTime(TIME_STAMP_JSON, new Date(), true)
            .setDateTime(BaseProperties.EXPIRES_JSON, expires, true)
            .setSignature(signer);
    }

    public Authority(JSONObjectReader rd, String expectedAuthorityUrl) throws IOException {
        root = Messages.parseBaseMessage(Messages.AUTHORITY, rd);
        authorityUrl = rd.getString(AUTHORITY_URL_JSON);
        if (!authorityUrl.equals(expectedAuthorityUrl)) {
            throw new IOException("\"" + AUTHORITY_URL_JSON + "\" mismatch, read=" + authorityUrl +
                                  " expected=" + expectedAuthorityUrl);
        }
        transactionUrl = rd.getString(TRANSACTION_URL_JSON);
        JSONObjectReader encryptionParameters = rd.getObject(ENCRYPTION_PARAMETERS_JSON);
        dataEncryptionAlgorithm = encryptionParameters.getString(DATA_ENCRYPTION_ALGORITHM_JSON);
        keyEncryptionAlgorithm = encryptionParameters.getString(KEY_ENCRYPTION_ALGORITHM_JSON);
        publicKey = encryptionParameters.getPublicKey(AlgorithmPreferences.JOSE);
        timeStamp = rd.getDateTime(TIME_STAMP_JSON);
        expires = rd.getDateTime(EXPIRES_JSON);
        signatureDecoder = rd.getSignature(AlgorithmPreferences.JOSE);
        signatureDecoder.verify(JSONSignatureTypes.X509_CERTIFICATE);
        rd.checkForUnread();
    }
    
    String authorityUrl;
    public String getAuthorityUrl() {
        return authorityUrl;
    }

    String transactionUrl;
    public String getTransactionUrl() {
        return transactionUrl;
    }

    String dataEncryptionAlgorithm;
    public String getDataEncryptionAlgorithm() {
        return dataEncryptionAlgorithm;
    }

    String keyEncryptionAlgorithm;
    public String getKeyEncryptionAlgorithm() {
        return keyEncryptionAlgorithm;
    }

    PublicKey publicKey;
    public PublicKey getPublicKey() {
        return publicKey;
    }

    GregorianCalendar expires;
    public GregorianCalendar getExpires() {
        return expires;
    }

    GregorianCalendar timeStamp;
     public GregorianCalendar getTimeStamp() {
        return timeStamp;
    }

    JSONSignatureDecoder signatureDecoder;
    public JSONSignatureDecoder getSignatureDecoder() {
        return signatureDecoder;
    }

    JSONObjectReader root;
    public JSONObjectReader getRoot() {
        return root;
    }
}
