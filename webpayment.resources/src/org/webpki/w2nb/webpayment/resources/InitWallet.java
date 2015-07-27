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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.security.PublicKey;

import java.security.interfaces.RSAPublicKey;

import java.util.EnumSet;

import org.webpki.crypto.AsymSignatureAlgorithms;
import org.webpki.crypto.CertificateUtil;
import org.webpki.crypto.CustomCryptoProvider;
import org.webpki.crypto.KeyAlgorithms;

import org.webpki.json.JSONAlgorithmPreferences;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONOutputFormats;

import org.webpki.keygen2.KeyGen2URIs;

import org.webpki.sks.AppUsage;
import org.webpki.sks.BiometricProtection;
import org.webpki.sks.DeleteProtection;
import org.webpki.sks.EnumeratedKey;
import org.webpki.sks.ExportProtection;
import org.webpki.sks.Grouping;
import org.webpki.sks.InputMethod;
import org.webpki.sks.PassphraseFormat;
import org.webpki.sks.PatternRestriction;
import org.webpki.sks.SecureKeyStore;

import org.webpki.sks.test.Device;
import org.webpki.sks.test.GenKey;
import org.webpki.sks.test.KeySpecifier;
import org.webpki.sks.test.PINPol;
import org.webpki.sks.test.ProvSess;
import org.webpki.sks.test.SKSReferenceImplementation;

import org.webpki.util.ArrayUtil;

import org.webpki.w2nb.webpayment.common.CredentialProperties;
import org.webpki.w2nb.webpayment.common.BaseProperties;
import org.webpki.w2nb.webpayment.common.KeyStoreEnumerator;

public class InitWallet {

    public static void main(String[] args) throws Exception {
        if (args.length != 9) {
            System.out.println("\nUsage: " +
                               InitWallet.class.getCanonicalName() +
                               "sksFile clientCertFile certFilePassword cardPin cardType/@ cardNumber" +
                               " authUrl image/image@ keyEncryptionKey/keyEncryptionKey@");
            System.exit(-3);
        }
        CustomCryptoProvider.forcedLoad(true);
        
        // Read importedKey/certificate to be imported
        KeyStoreEnumerator importedKey = new KeyStoreEnumerator(new FileInputStream(args[1]), args[2]);
        boolean rsa_flag = importedKey.getPublicKey() instanceof RSAPublicKey;
        String[] endorsed_algs = rsa_flag ?
                new String[] {AsymSignatureAlgorithms.RSA_SHA256.getURI()} 
                                          : 
                new String[] {AsymSignatureAlgorithms.ECDSA_SHA256.getURI()};

        // Setup keystore (SKS)
        SKSReferenceImplementation sks = null;
        try {
            sks = (SKSReferenceImplementation) new ObjectInputStream(new FileInputStream(args[0])).readObject();
            System.out.println("SKS found, restoring it");
        } catch (Exception e) {
            sks = new SKSReferenceImplementation();
            System.out.println("SKS not found, creating it");
        }
        Device device = new Device(sks);

        // Check for duplicates
        EnumeratedKey ek = new EnumeratedKey();
        while ((ek = sks.enumerateKeys(ek.getKeyHandle())) != null) {
            if (sks.getKeyAttributes(ek.getKeyHandle()).getCertificatePath()[0].equals(importedKey.getCertificatePath()[0])) {
                throw new IOException("Duplicate entry - importedKey #" + ek.getKeyHandle());
            }
        }

        // Start process by creating a session
        ProvSess sess = new ProvSess(device, 0);
        sess.setInputMethod(InputMethod.ANY);
        PINPol pin_policy = sess.createPINPolicy("PIN", 
                                                 PassphraseFormat.STRING,
                                                 EnumSet.noneOf(PatternRestriction.class),
                                                 Grouping.NONE,
                                                 1 /* min_length */,
                                                 50 /* max_length */,
                                                 (short) 3 /* retry_limit */,
                                                 null /* puk_policy */);

        GenKey surrogateKey = sess.createKey("Key",
                                             SecureKeyStore.ALGORITHM_KEY_ATTEST_1,
                                             null /* server_seed */,
                                             pin_policy, 
                                             args[3] /* PIN value */,
                                             BiometricProtection.NONE /* biometric_protection */,
                                             ExportProtection.NON_EXPORTABLE /* export_policy */,
                                             DeleteProtection.NONE /* delete_policy */,
                                             false /* enable_pin_caching */,
                                             AppUsage.SIGNATURE,
                                             "" /* friendly_name */, 
                                             new KeySpecifier(KeyAlgorithms.NIST_P_256), endorsed_algs);

        surrogateKey.setCertificatePath(importedKey.getCertificatePath());
        surrogateKey.setPrivateKey(importedKey.getPrivateKey());
        JSONObjectWriter ow = null;
        if (!args[4].equals("@")) {
            ow = new JSONObjectWriter()
                 .setString(CredentialProperties.CARD_TYPE_JSON, args[4])
                 .setString(CredentialProperties.CARD_NUMBER_JSON, args[5])
                 .setString(CredentialProperties.AUTH_URL_JSON, args[6])
                 .setString(CredentialProperties.SIGNATURE_ALGORITHM_JSON,
                         rsa_flag ?
                    AsymSignatureAlgorithms.RSA_SHA256.getJOSEName()
                                  :
                    AsymSignatureAlgorithms.ECDSA_SHA256.getJOSEName());
            if (!args[8].contains("@")) {
                PublicKey publicKey = CertificateUtil.getCertificateFromBlob(ArrayUtil.readFile(args[8])).getPublicKey();
                ow.setString(CredentialProperties.CONTENT_ENCRYPTION_ALGORITHM_JSON, BaseProperties.JOSE_A256CBC_HS512_ALG_ID)
                  .setString(CredentialProperties.KEY_ENCRYPTION_ALGORITHM_JSON,
                             publicKey instanceof RSAPublicKey ?
                                     BaseProperties.JOSE_RSA_OAEP_256_ALG_ID 
                                                               : 
                                     BaseProperties.JOSE_ECDH_ES_ALG_ID)
                  .setObject(CredentialProperties.KEY_ENCRYPTION_KEY_JSON)
                  .setPublicKey(publicKey, JSONAlgorithmPreferences.JOSE);
            }
            surrogateKey.addExtension(BaseProperties.W2NB_PAY_DEMO_CONTEXT_URI,
                    SecureKeyStore.SUB_TYPE_EXTENSION,
                    "",
                    ow.serializeJSONObject(JSONOutputFormats.NORMALIZED));
        }
        if (!args[7].endsWith("@")) {
            surrogateKey.addExtension(KeyGen2URIs.LOGOTYPES.CARD,
                                      SecureKeyStore.SUB_TYPE_LOGOTYPE,
                                      "image/png",
                                      ArrayUtil.readFile(args[7]));
        }
        sess.closeSession();
        
        // Serialize the updated SKS
        ObjectOutputStream oos = new ObjectOutputStream (new FileOutputStream(args[0]));
        oos.writeObject (sks);
        oos.close ();

        // Report
        System.out.println("Imported Subject: " +
                importedKey.getCertificatePath()[0].getSubjectX500Principal().getName() +
                "\nID=#" + surrogateKey.key_handle + ", " + (rsa_flag ? "RSA" : "EC") +
                (ow == null ? ", Not a card" : ", Card=\n" + ow));
    }
}
