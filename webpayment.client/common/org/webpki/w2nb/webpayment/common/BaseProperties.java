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

public interface BaseProperties {
    String WINDOW_JSON                = "window";
    String HEIGHT_JSON                = "height";
    String WIDTH_JSON                 = "width";
    String PAYMENT_REQUEST_JSON       = "paymentRequest";
    String PULL_PAYMENT_JSON          = "pullPayment";       // false or absent => "push" payment        
    String AMOUNT_JSON                = "amount";
    String ERROR_JSON                 = "error";
    String CURRENCY_JSON              = "currency";
    String DATE_TIME_JSON             = "dateTime";
    String TRANSACTION_ID_JSON        = "transactionId";
    String CLIENT_IP_ADDRESS_JSON     = "clientIpAddress";
    String REFERENCE_ID_JSON          = "referenceId";
    String PAYEE_JSON                 = "payee";             // Common name of payee to be used in UIs
    String AUTH_DATA_JSON             = "authData";          // Payer authorization request in "pull" mode
    String AUTH_URL_JSON              = "authUrl";           // URL to payment provider
    String ACCEPTED_CARD_TYPES_JSON   = "acceptedCardTypes"; // List of CARD_TYPE_JSON
    String CARD_TYPE_JSON             = "cardType";          // Card type
    String CARD_NUMBER_JSON           = "cardNumber";        // Card number (a.k.a. PAN)
    String CARD_REFERENCE_JSON        = "cardReference";     // Card number for payee (like ************5678)
    String PAYMENT_TOKEN_JSON         = "paymentToken";      // Tokenization result
    String REQUEST_HASH_JSON          = "requestHash";
    String VALUE_JSON                 = "value";
    String DOMAIN_NAME_JSON           = "domainName";
    String ENCRYPTED_DATA_JSON        = "encryptedData";
    String ENCRYPTED_KEY_JSON         = "encryptedKey";
    String PAYMENT_PROVIDER_KEY_JSON  = "paymentProviderKey";
    String EPHEMERAL_CLIENT_KEY_JSON  = "ephemeralClientKey";
    String ALGORITHM_JSON             = "algorithm";
    String IV_JSON                    = "iv";                // For symmetric encryption
    String TAG_JSON                   = "tag";               // Authenticated data for symmetric encryption
    String CIPHER_TEXT_JSON           = "cipherText";
    String SOFTWARE_JSON              = "software";
    String ID_JSON                    = "id";
    String VERSION_JSON               = "version";
    
    String W2NB_WEB_PAY_CONTEXT_URI   = "http://xmlns.webpki.org/webpay/v1";

    String JOSE_RSA_OAEP_256_ALG_ID   = "RSA-OAEP-256";
    String JOSE_ECDH_ES_ALG_ID        = "ECDH-ES";
    String JOSE_A256CBC_HS512_ALG_ID  = "A256CBC-HS512";
    String JOSE_SHA_256_ALG_ID        = "S256";              // Well, not really JOSE but "similar" :-)

    String JSON_CONTENT_TYPE          = "application/json";
}
