package org.webpki.w2nb.webpayment.resources;

import java.io.IOException;

import java.math.BigInteger;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

import java.security.interfaces.ECPublicKey;

import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;




import org.webpki.crypto.AlgorithmPreferences;
import org.webpki.crypto.CustomCryptoProvider;
import org.webpki.crypto.KeyAlgorithms;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONParser;

import org.webpki.util.ArrayUtil;
import org.webpki.util.Base64URL;
import org.webpki.util.DebugFormatter;

import org.webpki.w2nb.webpayment.common.Encryption;

public class CryptoTesting {

    static final String aliceKey = 
        "{\"kty\":\"EC\"," +
         "\"crv\":\"P-256\"," +
           "\"x\":\"Ze2loSV3wrroKUN_4zhwGhCqo3Xhu1td4QjeQ5wIVR0\"," +
           "\"y\":\"HlLtdXARY_f55A3fnzQbPcm6hgr34Mp8p-nuzQCE0Zw\"," +
           "\"d\":\"r_kHyZ-a06rmxM3yESK84r1otSg-aQcVStkRhA-iCM8\"" +
         "}";

     
    static final String bobKey = 
      "{\"kty\":\"EC\"," +
       "\"crv\":\"P-256\"," +
         "\"x\":\"mPUKT_bAWGHIhg0TpjjqVsP1rXWQu_vwVOHHtNkdYoA\"," +
         "\"y\":\"8BQAsImGeAS46fyWw5MhYfGTT0IjBpFw2SS34Dv4Irs\"," +
         "\"d\":\"AtH35vJsQ9SGjYfOsjUxYXQKrPH3FjZHmEtSKoSN8cM\"" +
       "}";
    
    static BigInteger getCurvePoint (JSONObjectReader rd, String property, KeyAlgorithms ec) throws IOException {
        byte[] fixed_binary = rd.getBinary (property);
        if (fixed_binary.length != (ec.getPublicKeySizeInBits () + 7) / 8) {
            throw new IOException ("Public EC key parameter \"" + property + "\" is not nomalized");
        }
        return new BigInteger (1, fixed_binary);
    }
    
    static KeyPair getKeyPair (String jwk) throws Exception {
        JSONObjectReader rd = JSONParser.parse(jwk);
        KeyAlgorithms ec = KeyAlgorithms.getKeyAlgorithmFromID (rd.getString ("crv"),
                                                                AlgorithmPreferences.JOSE);
        if (!ec.isECKey ()) {
            throw new IOException ("\"crv\" is not an EC type");
        }
        ECPoint w = new ECPoint (getCurvePoint (rd, "x", ec), getCurvePoint (rd, "y", ec));
        PublicKey publicKey = KeyFactory.getInstance ("EC").generatePublic (new ECPublicKeySpec (w, ec.getECParameterSpec ()));
        PrivateKey privateKey = KeyFactory.getInstance ("EC").generatePrivate (new ECPrivateKeySpec (getCurvePoint (rd, "d", ec), ec.getECParameterSpec ()));
        return new KeyPair (publicKey, privateKey);
    }
    
    public static void main(String[] args) throws Exception {
        CustomCryptoProvider.forcedLoad(true);
        System.out.println("Content Encryption");
        byte[] k = DebugFormatter.getByteArrayFromHex("000102030405060708090a0b0c0d0e0f" +
                                                      "101112131415161718191a1b1c1d1e1f");

        byte[] p = DebugFormatter.getByteArrayFromHex("41206369706865722073797374656d20" +
                                                      "6d757374206e6f742062652072657175" +
                                                      "6972656420746f206265207365637265" +
                                                      "742c20616e64206974206d7573742062" +
                                                      "652061626c6520746f2066616c6c2069" +
                                                      "6e746f207468652068616e6473206f66" +
                                                      "2074686520656e656d7920776974686f" +
                                                      "757420696e636f6e76656e69656e6365");

        byte[] iv = DebugFormatter.getByteArrayFromHex("1af38c2dc2b96ffdd86694092341bc04");

        byte[] a = DebugFormatter.getByteArrayFromHex("546865207365636f6e64207072696e63" +
                                                      "69706c65206f66204175677573746520" +
                                                      "4b6572636b686f666673");

        byte[] e = DebugFormatter.getByteArrayFromHex("c80edfa32ddf39d5ef00c0b468834279" +
                                                      "a2e46a1b8049f792f76bfe54b903a9c9" +
                                                      "a94ac9b47ad2655c5f10f9aef71427e2" +
                                                      "fc6f9b3f399a221489f16362c7032336" +
                                                      "09d45ac69864e3321cf82935ac4096c8" +
                                                      "6e133314c54019e8ca7980dfa4b9cf1b" +
                                                      "384c486f3a54c51078158ee5d79de59f" +
                                                      "bd34d848b3d69550a67646344427ade5" +
                                                      "4b8851ffb598f7f80074b9473c82e2db");

        byte[] t = DebugFormatter.getByteArrayFromHex("652c3fa36b0a7c5b3219fab3a30bc1c4");

        byte[] tout = new byte[16];
        byte[] eout = Encryption.contentEncryption(Encryption.JOSE_A128CBC_HS256_ALG_ID,
                                                   k,
                                                   p,
                                                   iv,
                                                   a,
                                                   tout);
        if (!ArrayUtil.compare(t, tout)) {
            throw new IOException ("tout");
        }
        if (!ArrayUtil.compare(e, eout)) {
            throw new IOException ("eout");
        }
        byte[] pout = Encryption.contentDecryption(Encryption.JOSE_A128CBC_HS256_ALG_ID,
                                                   k,
                                                   e,
                                                   iv,
                                                   a,
                                                   t);
        if (!ArrayUtil.compare(p, pout)) {
            throw new IOException ("pout");
        }
        System.out.println("ECDH begin");
        KeyPair bob = getKeyPair(bobKey);
        KeyPair alice = getKeyPair(aliceKey);
        if (!Base64URL.encode(Encryption.receiverKeyAgreement(Encryption.JOSE_ECDH_ES_ALG_ID,
                Encryption.JOSE_A128CBC_HS256_ALG_ID,
                (ECPublicKey) bob.getPublic(),
                alice.getPrivate())).equals("hzHdlfQIAEehb8Hrd_mFRhKsKLEzPfshfXs9l6areCc")) {
            throw new IOException("Bad ECDH");
        }
        System.out.println("ECDH success");
    }
}
