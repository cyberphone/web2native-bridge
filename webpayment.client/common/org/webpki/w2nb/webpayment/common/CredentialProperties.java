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

// Each payment credential is associated with a unique SKS key+certificate and
// an SKS extension using the URI BaseProperties.W2NB_PAY_DEMO_CONTEXT_URI
// holding a Base64URL encoded JSON object as described below:

public interface CredentialProperties {
    String CARD_NUMBER_JSON           = "cardNumber";           // Card number (PAN)
    String CARD_TYPE_JSON             = "cardType";             // Card type.  See CardTypes.java
    String AUTH_URL_JSON              = "authUrl";              // URL to payment provider
    String SIGNATURE_ALGORITHM_JSON   = "signatureAlgorithm";   // MUST match the key material

    // Optional: For the "pull" mode
    String CONTENT_ENCRYPTION_ALGORITHM_JSON = "contentEncryptionAlgorithm";  // JOSE algorithm ID (A256CBC-HS512)
    String KEY_ENCRYPTION_ALGORITHM_JSON     = "keyEncryptionAlgorithm";      // JOSE algorithm ID (ECDH-ES or RSA-OAEP-256)
    String KEY_ENCRYPTION_KEY_JSON           = "keyEncryptionKey";            // PublicKey in JCS format using JOSE algorithm IDs
}

// Note: Card images MUST be using the KeyGen2
// "http://xmlns.webpki.org/keygen2/logotype#card"
// URI in a separate SKS image extension attribute
