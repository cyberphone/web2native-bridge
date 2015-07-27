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

import org.webpki.json.JSONAlgorithmPreferences;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONSignatureDecoder;

public class PayeePullAuthorizationRequest extends EncryptedAuthorizationRequest {

    String clientIpAddress;
    
    JSONSignatureDecoder outerSignature;
    
    public PayeePullAuthorizationRequest(JSONObjectReader rd) throws IOException {
        super(Messages.parseBaseMessage(Messages.PAYEE_PULL_AUTH_REQ, rd).getObject(AUTH_DATA_JSON));
        clientIpAddress = rd.getString(CLIENT_IP_ADDRESS_JSON);
        outerSignature = rd.getSignature(JSONAlgorithmPreferences.JOSE);
        rd.checkForUnread();
    }

    public String getClientIpAddress() {
        return clientIpAddress;
    }

    public static JSONObjectWriter encode(JSONObjectReader encryptedRequest,
                                          String clientIpAddress,
                                          ServerSigner signer)
        throws IOException, GeneralSecurityException {
        return Messages.createBaseMessage(Messages.PAYEE_PULL_AUTH_REQ)
            .setString(CLIENT_IP_ADDRESS_JSON, clientIpAddress)
            .setObject(AUTH_DATA_JSON, encryptedRequest)
            .setSignature(signer);
    }
}
