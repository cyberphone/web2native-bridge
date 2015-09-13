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
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Vector;

import org.webpki.json.JSONAlgorithmPreferences;
import org.webpki.json.JSONDecoderCache;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONSignatureDecoder;
import org.webpki.json.JSONX509Signer;

public class ReserveOrDebitResponse implements BaseProperties {
    
    public static final String SOFTWARE_ID      = "WebPKI.org - Bank";
    public static final String SOFTWARE_VERSION = "1.00";

    private static JSONObjectWriter header(boolean directDebit) throws IOException {
        return Messages.createBaseMessage(directDebit ?
                       Messages.DIRECT_DEBIT_RESPONSE : Messages.RESERVE_FUNDS_RESPONSE);
    }

    public static JSONObjectWriter encode(boolean directDebit,
                                          ErrorReturn errorReturn) throws IOException, GeneralSecurityException {
        return errorReturn.write(header(directDebit));
    }

    public static JSONObjectWriter encode(boolean directDebit,
                                          PaymentRequest paymentRequest,
                                          String accountType,
                                          String accountId,
                                          JSONObjectWriter encryptedAccountData,
                                          PayeeAccountDescriptor payeeAccount, 
                                          String referenceId,
                                          JSONX509Signer signer) throws IOException {
        StringBuffer accountReference = new StringBuffer();
        int q = accountId.length() - 4;
        for (char c : accountId.toCharArray()) {
            accountReference.append((--q < 0) ? c : '*');
        }
        JSONObjectWriter wr = header(directDebit)
            .setObject(PAYMENT_REQUEST_JSON, paymentRequest.root)
            .setString(ACCOUNT_TYPE_JSON, accountType)
            .setString(ACCOUNT_REFERENCE_JSON, accountReference.toString());
        if (encryptedAccountData == null) {
            wr.setObject(PAYEE_ACCOUNT_JSON, payeeAccount.write());
        } else {
            if (directDebit) {
                throw new IOException("\""+ PROTECTED_ACCOUNT_DATA_JSON + "\" not applicable for directDebit");
            }
            ReserveOrDebitRequest.zeroTest(PAYEE_ACCOUNT_JSON, payeeAccount);
            wr.setObject(PROTECTED_ACCOUNT_DATA_JSON, encryptedAccountData);
        }
        return wr.setString(REFERENCE_ID_JSON, referenceId)
                 .setDateTime(TIME_STAMP_JSON, new Date(), true)
                 .setObject(SOFTWARE_JSON, Software.encode(SOFTWARE_ID, SOFTWARE_VERSION))
                 .setSignature (signer);
    }

    EncryptedData encryptedData;

    JSONObjectReader root;
    
    public ReserveOrDebitResponse(JSONObjectReader rd) throws IOException {
        directDebit = rd.getString(JSONDecoderCache.QUALIFIER_JSON).equals(Messages.DIRECT_DEBIT_RESPONSE.toString());
        root = Messages.parseBaseMessage(directDebit ?
                      Messages.DIRECT_DEBIT_RESPONSE : Messages.RESERVE_FUNDS_RESPONSE, rd);
        if (rd.hasProperty(ERROR_CODE_JSON)) {
            errorReturn = new ErrorReturn(rd.getInt(ERROR_CODE_JSON), rd.getStringConditional(DESCRIPTION_JSON));
            rd.checkForUnread();
            return;
        }
        paymentRequest = new PaymentRequest(rd.getObject(PAYMENT_REQUEST_JSON));
        accountType = rd.getString(ACCOUNT_TYPE_JSON);
        accountReference = rd.getString(ACCOUNT_REFERENCE_JSON);
        if (rd.hasProperty(PROTECTED_ACCOUNT_DATA_JSON)) {
            encryptedData = EncryptedData.parse(rd.getObject(PROTECTED_ACCOUNT_DATA_JSON));
            if (directDebit) {
                throw new IOException("\""+ PROTECTED_ACCOUNT_DATA_JSON + "\" not applicable for directDebit");
            }
        } else {
            account = new PayeeAccountDescriptor(rd.getObject(PAYEE_ACCOUNT_JSON));
        }
        referenceId = rd.getString(REFERENCE_ID_JSON);
        timeStamp = rd.getDateTime(TIME_STAMP_JSON);
        software = new Software(rd);
        signatureDecoder = rd.getSignature(JSONAlgorithmPreferences.JOSE);
        rd.checkForUnread();
    }

    ErrorReturn errorReturn;
    public boolean success() {
        return errorReturn == null;
    }

    public ErrorReturn getErrorReturn() {
        return errorReturn;
    }

    PayeeAccountDescriptor account;
    public PayeeAccountDescriptor getPayeeAccountDescriptor() {
        return account;
    }

    public boolean isAccount2Account() {
        return encryptedData == null;
    }
    
    public ProtectedAccountData getProtectedAccountData(Vector<DecryptionKeyHolder> decryptionKeys) throws IOException, GeneralSecurityException {
        return new ProtectedAccountData(encryptedData.getDecryptedData(decryptionKeys));
    }

    boolean directDebit;
    public boolean isDirectDebit() {
        return directDebit;
    }

    PaymentRequest paymentRequest;
    public PaymentRequest getPaymentRequest() {
        return paymentRequest;
    }

    String accountType;
    public String getAccountType() {
        return accountType;
    }

    String accountReference;
    public String getAccountReference() {
        return accountReference;
    }

    String referenceId;
    public String getReferenceId() {
        return referenceId;
    }

    GregorianCalendar timeStamp;
    public GregorianCalendar getTimeStamp() {
        return timeStamp;
    }

    Software software;
    public Software getSoftware() {
        return software;
    }

    JSONSignatureDecoder signatureDecoder;
    public JSONSignatureDecoder getSignatureDecoder() {
        return signatureDecoder;
    }
}
