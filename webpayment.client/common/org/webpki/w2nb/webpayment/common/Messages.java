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

import org.webpki.json.JSONDecoderCache;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;

public enum Messages {

    WALLET_IS_READY           ("WalletIsReady"),              // Wallet to payee web page message
    INVOKE_WALLET             ("InvokeWallet"),               // Payee payment request + other data
    PAYER_GENERIC_AUTH_REQ    ("PayerGenericAuthReq"),        // Used as is for "push" and wrapped in
    PAYER_PULL_AUTH_REQ       ("PayerPullAuthReq"),           // this one for "pull"
    PAYEE_PULL_AUTH_REQ       ("PayeePullAuthReq"),           // Payee "pull"
    PROVIDER_GENERIC_AUTH_RES ("ProviderGenericAuthRes");     // Provider response for "push" and "pull"

    String qualifier;

    Messages(String qualifier) {
        this.qualifier = qualifier;
    }

    @Override
    public String toString() {
        return qualifier;
    }

    public static JSONObjectWriter createBaseMessage(Messages message) throws IOException {
        return new JSONObjectWriter()
           .setString(JSONDecoderCache.CONTEXT_JSON, BaseProperties.W2NB_PAY_DEMO_CONTEXT_URI)
           .setString(JSONDecoderCache.QUALIFIER_JSON, message.toString());
    }

    public static JSONObjectReader parseBaseMessage(Messages expected_message,
                                                    JSONObjectReader request_object) throws IOException {
        if (!request_object.getString(JSONDecoderCache.CONTEXT_JSON).equals(BaseProperties.W2NB_PAY_DEMO_CONTEXT_URI)) {
            throw new IOException("Unknown context: " + request_object.getString(JSONDecoderCache.CONTEXT_JSON));
        }
        if (!request_object.getString(JSONDecoderCache.QUALIFIER_JSON).equals(expected_message.toString())) {
            throw new IOException("Unexpected qualifier: " + request_object.getString(JSONDecoderCache.QUALIFIER_JSON));
        }
        return request_object;
    }
}
