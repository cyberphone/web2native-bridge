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
    String WINDOW_JSON                       = "window";
    String HEIGHT_JSON                       = "height";
    String WIDTH_JSON                        = "width";
    String PAYMENT_REQUEST_JSON              = "paymentRequest";
    String INDIRECT_MODE_JSON                = "indirectMode";      // false or absent => "direct mode" payment 
    String PAYMENT_TYPE_JSON                 = "paymentType";       // CreditCard or Account2Account
    String ACQUIRER_ENCRYPTION_KEY_URL_JSON  = "acquirerEncryptionKeyUrl";  // For CreditCard payments
    String PAYEE_ACCOUNT_TYPES_JSON          = "payeeAccountTypes";         // For Account2Account payments
    String EXPIRES_JSON                      = "expires";           // Payment requests perform a "reservation"
    String AMOUNT_JSON                       = "amount";
    String ERROR_JSON                        = "error";
    String CURRENCY_JSON                     = "currency";
    String DATE_TIME_JSON                    = "dateTime";
    String TRANSACTION_ID_JSON               = "transactionId";
    String CLIENT_IP_ADDRESS_JSON            = "clientIpAddress";
    String REFERENCE_ID_JSON                 = "referenceId";
    String PAYEE_JSON                        = "payee";             // Common name of payee to be used in UIs
    String AUTH_DATA_JSON                    = "authData";          // Payer authorization request in the "indirect" mode
    String AUTH_URL_JSON                     = "authUrl";           // URL to payment provider
    String ACCEPTED_CARD_TYPES_JSON          = "acceptedCardTypes"; // List of CARD_TYPE_JSON
    String CARD_TYPE_JSON                    = "cardType";          // Card type
    String CARD_NUMBER_JSON                  = "cardNumber";        // Card number (a.k.a. PAN)
    String CARD_REFERENCE_JSON               = "cardReference";     // Card number for payee (like ************5678)
    String PAYMENT_TOKEN_JSON                = "paymentToken";      // Tokenization result
    String REQUEST_HASH_JSON                 = "requestHash";
    String VALUE_JSON                        = "value";
    String DOMAIN_NAME_JSON                  = "domainName";
    String ENCRYPTED_DATA_JSON               = "encryptedData";
    String ENCRYPTED_KEY_JSON                = "encryptedKey";
    String STATIC_PROVIDER_KEY_JSON          = "staticProviderKey";
    String EPHEMERAL_SENDER_KEY_JSON         = "ephemeralSenderKey";
    String KEY_ENCRYPTION_ALGORITHM_JSON     = "keyEncryptionAlgorithm";     // For acquirer encryption key
    String CONTENT_ENCRYPTION_ALGORITHM_JSON = "contentEncryptionAlgorithm"; //    -"-
    String ALGORITHM_JSON                    = "algorithm";
    String IV_JSON                           = "iv";                // For symmetric encryption
    String TAG_JSON                          = "tag";               // Authenticated data for symmetric encryption
    String CIPHER_TEXT_JSON                  = "cipherText";
    String SOFTWARE_JSON                     = "software";
    String ID_JSON                           = "id";
    String VERSION_JSON                      = "version";
    
    String W2NB_WEB_PAY_CONTEXT_URI          = "http://xmlns.webpki.org/webpay/v1";

    String JSON_CONTENT_TYPE                 = "application/json";
}
