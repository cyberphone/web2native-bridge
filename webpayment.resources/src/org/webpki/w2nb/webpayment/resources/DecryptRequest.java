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

// Web2Native Bridge emulator Payment Agent (a.k.a. Wallet) application

package org.webpki.w2nb.webpayment.resources;

import java.io.FileInputStream;

import java.util.Vector;

import org.webpki.crypto.CustomCryptoProvider;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONOutputFormats;
import org.webpki.json.JSONParser;

import org.webpki.util.ArrayUtil;

import org.webpki.w2nb.webpayment.common.BaseProperties;
import org.webpki.w2nb.webpayment.common.DecryptionKeyHolder;
import org.webpki.w2nb.webpayment.common.EncryptedAuthorizationRequest;
import org.webpki.w2nb.webpayment.common.GenericAuthorizationRequest;
import org.webpki.w2nb.webpayment.common.KeyStoreEnumerator;
import org.webpki.w2nb.webpayment.common.Messages;

public class DecryptRequest {

    public static void main(String[] args) throws Exception {
        if (args.length < 4 || (args.length & 1) != 0) {
            System.out.println("\nUsage: " +
                               DecryptRequest.class.getCanonicalName() +
                               "[certFile algorithm]... certFilePassword inputFile");
            System.exit(-3);
        }
        CustomCryptoProvider.forcedLoad(true);
        
        // Read Key/certificate enumeration
        Vector<DecryptionKeyHolder> decryptionKeys = new Vector<DecryptionKeyHolder>();
        for (int q = 0; q < args.length - 2; q += 2) {
            KeyStoreEnumerator key = new KeyStoreEnumerator(new FileInputStream(args[q]), args[args.length - 2]);
            decryptionKeys.add(new DecryptionKeyHolder(key.getPublicKey(), key.getPrivateKey(), args[q + 1]));
        }

        JSONObjectReader input =
                Messages.parseBaseMessage(Messages.PAYER_PULL_AUTH_REQ,
                                          JSONParser.parse(ArrayUtil.readFile(args[args.length - 1])));
        input.getString(BaseProperties.AUTH_URL_JSON);

        EncryptedAuthorizationRequest ear =
                new EncryptedAuthorizationRequest(input.getObject(BaseProperties.AUTH_DATA_JSON));
        
        input.checkForUnread();

        GenericAuthorizationRequest gar = ear.getDecryptedAuthorizationRequest(decryptionKeys);

        System.out.println("Authorization request:\n" +
                new String(gar.getRoot().serializeJSONObject(JSONOutputFormats.PRETTY_PRINT)));
    }
}
