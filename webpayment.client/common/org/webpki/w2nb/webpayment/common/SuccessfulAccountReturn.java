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

import org.webpki.json.JSONObjectWriter;

// For transferring status for which is related to the payer's account

public class SuccessfulAccountReturn extends ReturnStatus implements BaseProperties {

    @Override
    public boolean success() {
        return true;
    }

    @Override
    JSONObjectWriter write(JSONObjectWriter wr) throws IOException, GeneralSecurityException {
        return optionalDescription == null ? wr : wr.setString(DESCRIPTION_JSON, optionalDescription);
    }

    private String optionalDescription;

    public SuccessfulAccountReturn(String optionalDescription) {
        this.optionalDescription = optionalDescription;
    }
}
