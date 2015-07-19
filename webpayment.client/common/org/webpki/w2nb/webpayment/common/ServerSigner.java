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
import java.io.InputStream;

import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import java.security.interfaces.RSAPublicKey;

import java.util.Enumeration;
import java.util.Vector;

import org.webpki.crypto.AsymSignatureAlgorithms;
import org.webpki.crypto.KeyStoreReader;
import org.webpki.crypto.SignatureWrapper;
import org.webpki.crypto.SignerInterface;

import org.webpki.json.JSONAlgorithmPreferences;
import org.webpki.json.JSONX509Signer;

public class ServerSigner {

    Vector<X509Certificate> certificatePath = new Vector<X509Certificate>();
    PrivateKey privateKey;
    AsymSignatureAlgorithms signatureAlgorithm;

    public ServerSigner(InputStream is, String password) throws IOException {
        try {
            KeyStore ks = KeyStoreReader.loadKeyStore(is, password);
            Enumeration<String> aliases = ks.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if (ks.isKeyEntry(alias)) {
                    privateKey = (PrivateKey) ks.getKey(alias, password.toCharArray());
                    for (Certificate cert : ks.getCertificateChain(alias)) {
                        certificatePath.add((X509Certificate) cert);
                    }
                    break;
                }
            }
            signatureAlgorithm = certificatePath.firstElement().getPublicKey() instanceof RSAPublicKey ?
                    AsymSignatureAlgorithms.RSA_SHA256 : AsymSignatureAlgorithms.ECDSA_SHA256;
        } catch (Exception e) {
            throw new IOException(e);
        }
        if (privateKey == null) {
            throw new IOException("No private key!");
        }
    }

    public JSONX509Signer getJSONX509Signer() throws IOException {
        return new JSONX509Signer(new SignerInterface () {
            @Override
            public X509Certificate[] getCertificatePath() throws IOException {
                return certificatePath.toArray(new X509Certificate[0]);
            }
            @Override
            public byte[] signData(byte[] data, AsymSignatureAlgorithms algorithm) throws IOException {
                try {
                    return new SignatureWrapper (algorithm, privateKey)
                        .update (data)
                        .sign ();
                } catch (GeneralSecurityException e) {
                    throw new IOException (e);
                }
            }
        }).setSignatureCertificateAttributes(true)
          .setSignatureAlgorithm(signatureAlgorithm)
          .setAlgorithmPreferences(JSONAlgorithmPreferences.JOSE);
    }
}
