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
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;

import java.security.interfaces.ECPublicKey;

import java.security.spec.ECGenParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;

import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.webpki.crypto.KeyAlgorithms;

// Core encryption class

public abstract class Encryption {
    
    public static byte[] contentEncryption(boolean encrypt,
                                           String algorithm,
                                           byte[] aesKey,
                                           byte[] data,
                                           byte[] iv,
                                           byte[] tag,
                                           byte[] authenticatedData) throws GeneralSecurityException, IOException {
        if (!permittedContentEncryptionAlgorithm(algorithm)) {
            throw new IOException("Unsupported content encryption algorithm: " + algorithm);
        }
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "BC");
        cipher.init(encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE,
                    new SecretKeySpec(aesKey, "AES"),
                    new IvParameterSpec(iv));
        return cipher.doFinal(data);
    }

    public static byte[] rsaEncryptKey(String keyEncryptionAlgorithm,
                                       byte[] rawKey,
                                       PublicKey publicKey) throws GeneralSecurityException, IOException {
        checkRsa(keyEncryptionAlgorithm);
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA256AndMGF1Padding", "BC");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(rawKey);
    }

    public static byte[] rsaDecryptKey(String keyEncryptionAlgorithm,
                                       byte[] encryptedKey,
                                       PrivateKey privateKey) throws GeneralSecurityException, IOException {
        checkRsa(keyEncryptionAlgorithm);
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA256AndMGF1Padding", "BC");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return cipher.doFinal(encryptedKey);
    }

    static void checkRsa(String keyEncryptionAlgorithm) throws IOException {
        if (!keyEncryptionAlgorithm.equals(BaseProperties.JOSE_RSA_OAEP_256_ALG_ID)) {
            throw new IOException("Unsupported RSA algorithm: " + keyEncryptionAlgorithm);
        }
    }

    static void addInt4(MessageDigest messageDigest, int value) {
        for (int i = 24; i >= 0; i -= 8) {
            messageDigest.update((byte)(value >>> i));
        }
    }

    public static byte[] receiverKeyAgreement(String keyEncryptionAlgorithm,
                                              String contentEncryptionAlgorithm,
                                              ECPublicKey receivedPublicKey,
                                              PrivateKey privateKey) throws GeneralSecurityException, IOException {
        if (!keyEncryptionAlgorithm.equals(BaseProperties.JOSE_ECDH_ES_ALG_ID)) {
            throw new IOException("Unsupported ECDH algorithm: " + keyEncryptionAlgorithm);
        }
        if (!contentEncryptionAlgorithm.equals(BaseProperties.JOSE_A128CBC_HS256_ALG_ID)) {
            throw new IOException("Unsupported content encryption algorithm: " + contentEncryptionAlgorithm);
        }
        KeyAgreement keyAgreement = KeyAgreement.getInstance("ECDH", "BC");
        keyAgreement.init(privateKey);
        keyAgreement.doPhase(receivedPublicKey, true);
        // NIST Concat KDF
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256", "BC");
        // Round 1 indicator
        addInt4(messageDigest, 1);
        // Z
        messageDigest.update(keyAgreement.generateSecret());
        // AlgorithmID = Content encryption algorithm
        addInt4(messageDigest, contentEncryptionAlgorithm.length());
        messageDigest.update(contentEncryptionAlgorithm.getBytes("UTF-8"));
        // PartyUInfo = Empty
        addInt4(messageDigest, 0);
        // PartyVInfo = Empty
        addInt4(messageDigest, 0);
        // SuppPubInfo = Key length in bits
        addInt4(messageDigest, 256);
        return messageDigest.digest();
    }

    public static byte[] senderKeyAgreement(String keyEncryptionAlgorithm,
                                            String contentEncryptionAlgorithm,
                                            ECPublicKey[] generatedEphemeralKey,
                                            PublicKey staticKey) throws IOException, GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC", "BC");
        ECGenParameterSpec eccgen = new ECGenParameterSpec(KeyAlgorithms.getKeyAlgorithm(staticKey).getJCEName());
        generator.initialize (eccgen, new SecureRandom());
        KeyPair keyPair = generator.generateKeyPair();
        generatedEphemeralKey[0] = (ECPublicKey) keyPair.getPublic();
        return receiverKeyAgreement(keyEncryptionAlgorithm,
                                    contentEncryptionAlgorithm,
                                    (ECPublicKey)staticKey,
                                    keyPair.getPrivate());
    }

    public static boolean permittedKeyEncryptionAlgorithm(String algorithm) {
        return algorithm.equals(BaseProperties.JOSE_ECDH_ES_ALG_ID) ||
               algorithm.equals(BaseProperties.JOSE_RSA_OAEP_256_ALG_ID);
    }

    public static boolean permittedContentEncryptionAlgorithm(String algorithm) {
        return algorithm.equals(BaseProperties.JOSE_A128CBC_HS256_ALG_ID);
    }
}
