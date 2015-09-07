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

public class PayerIndirectModeAuthorizationRequest implements BaseProperties {
    
    String authorityUrl;

    public PayerIndirectModeAuthorizationRequest(JSONObjectReader rd) throws IOException {
        EncryptedData.parse(Messages.parseBaseMessage(Messages.PAYER_INDIRECT_AUTH_REQ, rd).getObject(PROVIDER_AUTHORITY_URL_JSON));
        authorityUrl = rd.getString(PROVIDER_AUTHORITY_URL_JSON);
        rd.checkForUnread();
    }
    
    public String getAuthorityUrl() {
        return authorityUrl;
    }

    public static JSONObjectWriter encode(JSONObjectWriter unencryptedRequest,
                                          String authorityUrl,
                                          String cardType,
                                          String dataEncryptionAlgorithm,
                                          PublicKey keyEncryptionKey,
                                          String keyEncryptionAlgorithm) throws IOException, GeneralSecurityException {
        JSONObjectWriter encryptedRequest = Messages.createBaseMessage(Messages.PAYER_INDIRECT_AUTH_REQ)
            .setString(PROVIDER_AUTHORITY_URL_JSON, authorityUrl)
            .setString(ACCOUNT_TYPE_JSON, cardType);
        encryptedRequest.setObject(AUTHORIZATION_DATA_JSON,
                                   EncryptedData.encode(unencryptedRequest,
                                                        dataEncryptionAlgorithm,
                                                        keyEncryptionKey,
                                                        keyEncryptionAlgorithm));
        return encryptedRequest;
    }
}
