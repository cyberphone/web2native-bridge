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

import org.webpki.json.JSONAlgorithmPreferences;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONOutputFormats;
import org.webpki.json.JSONParser;

import org.webpki.util.ArrayUtil;

import org.webpki.w2nb.webpayment.common.DecryptionKeyHolder;
import org.webpki.w2nb.webpayment.common.PayerPullAuthorizationRequest;
import org.webpki.w2nb.webpayment.common.GenericAuthorizationRequest;
import org.webpki.w2nb.webpayment.common.KeyStoreEnumerator;

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
            System.out.println("Inserted key:\n" + 
                new String(new JSONObjectWriter().setPublicKey(key.getPublicKey(),
                                                               JSONAlgorithmPreferences.JOSE).serializeJSONObject(JSONOutputFormats.PRETTY_PRINT), "UTF-8"));
        }

        PayerPullAuthorizationRequest ear =
                new PayerPullAuthorizationRequest(JSONParser.parse(ArrayUtil.readFile(args[args.length - 1])));
        
        GenericAuthorizationRequest gar = ear.getDecryptedAuthorizationRequest(decryptionKeys);

        System.out.println("Decrypted authorization request:\n" +
                new String(gar.getRoot().serializeJSONObject(JSONOutputFormats.PRETTY_PRINT)));
    }
}
