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

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;

public class PaymentTypeDescriptor implements BaseProperties {

    private static String[] checkAccounts(String[] Uris) throws IOException {
        if (Uris.length == 0) {
            throw new IOException("Empty \"" + PAYEE_ACCOUNT_TYPES_JSON + "\" not allowed");
        }
        return Uris;
    }

    PaymentTypeDescriptor(JSONObjectReader rd) throws IOException {
        String argument = rd.getString(PAYMENT_TYPE_JSON);
        for (PAYMENT_TYPES paymentType : PAYMENT_TYPES.values()) {
            if (paymentType.argument.equals(argument)) {
                if (paymentType == PAYMENT_TYPES.CREDIT_CARD) {
                    aquirerEncryptionKeyUrl = rd.getString(ACQUIRER_ENCRYPTION_KEY_URL_JSON);
                } else {
                    payeeAccountTypeUris = checkAccounts(rd.getStringArray(PAYEE_ACCOUNT_TYPES_JSON));
                }
                this.paymentType = paymentType;
                return;
            }
        }
        throw new IOException("No such \"" + PAYMENT_TYPE_JSON + "\" :" + argument);
    }
    
    private PAYMENT_TYPES paymentType;
    
    private String aquirerEncryptionKeyUrl;
    
    private String[] payeeAccountTypeUris;

    public PAYMENT_TYPES getPaymentType() {
        return paymentType;
    }

    public String getAquirerEncryptionKeyUrl() {
        return aquirerEncryptionKeyUrl;
    }

    public String[] getPayeeAccountTypeUris() {
        return payeeAccountTypeUris;
    }

    public enum PAYMENT_TYPES {
       
        CREDIT_CARD        ("CreditCard"),
        ACCOUNT_TO_ACCOUNT ("Account2Account");
        
        private String argument;
        
        PAYMENT_TYPES(String argument) {
            this.argument = argument;
        }
   }
    
   public static JSONObjectWriter createCreditCardPaymentType(String aquirerEncryptionKeyUrl) throws IOException {
        return new JSONObjectWriter()
            .setString(PAYMENT_TYPE_JSON, PAYMENT_TYPES.CREDIT_CARD.argument)
            .setString(ACQUIRER_ENCRYPTION_KEY_URL_JSON, aquirerEncryptionKeyUrl);
    }

    public static JSONObjectWriter createAccount2AccountPaymentType(String[] payeeAccountTypeUris) throws IOException {
        return new JSONObjectWriter()
            .setString(PAYMENT_TYPE_JSON, PAYMENT_TYPES.ACCOUNT_TO_ACCOUNT.argument)
            .setStringArray(PAYEE_ACCOUNT_TYPES_JSON, checkAccounts(payeeAccountTypeUris));
    }
}
