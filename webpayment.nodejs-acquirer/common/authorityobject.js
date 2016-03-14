/*
 *  Copyright 2006-2016 WebPKI.org (http://webpki.org).
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
 
'use strict';

// JSON "Authority" object

const JsonUtil = require('webpki.org').JsonUtil;
const Keys = require('webpki.org').Keys;
const Encryption = require('webpki.org').Encryption;

const BaseProperties = require('./baseproperties');
const Messages = require('./messages');

function Authority() {
}

Authority.encode = function(authorityUrl,
                            transactionUrl,
                            publicKey,
                            expires,
                            signer) {
  return Messages.createBaseMessage(Messages.AUTHORITY)
    .setString(BaseProperties.AUTHORITY_URL_JSON, authorityUrl)
    .setString(BaseProperties.TRANSACTION_URL_JSON, transactionUrl)
    .setObject(BaseProperties.ENCRYPTION_PARAMETERS_JSON, new JsonUtil.ObjectWriter()
      .setString(BaseProperties.DATA_ENCRYPTION_ALGORITHM_JSON, Encryption.JOSE_A128CBC_HS256_ALG_ID)
      .setString(BaseProperties.KEY_ENCRYPTION_ALGORITHM_JSON, 
                         publicKey.jcs.type == 'RSA' ?
                 Encryption.JOSE_RSA_OAEP_256_ALG_ID : Encryption.JOSE_ECDH_ES_ALG_ID)
      .setPublicKey(publicKey))
    .setDateTime(BaseProperties.TIME_STAMP_JSON, new Date())
    .setDateTime(BaseProperties.EXPIRES_JSON, expires)
    .setSignature(signer);
};

/*
  create : function() {
    }
    public static JSONObjectWriter encode(String authorityUrl,
                                          String transactionUrl,
                                          PublicKey publicKey,
                                          Date expires,
                                          ServerX509Signer signer) throws IOException {
        return Messages.createBaseMessage(Messages.AUTHORITY)
            .setString(AUTHORITY_URL_JSON, authorityUrl)
            .setString(TRANSACTION_URL_JSON, transactionUrl)
            .setObject(ENCRYPTION_PARAMETERS_JSON, new JSONObjectWriter()
                .setString(BaseProperties.DATA_ENCRYPTION_ALGORITHM_JSON, Encryption.JOSE_A128CBC_HS256_ALG_ID)
                .setString(BaseProperties.KEY_ENCRYPTION_ALGORITHM_JSON, 
                             publicKey instanceof RSAPublicKey ?
                           Encryption.JOSE_RSA_OAEP_256_ALG_ID : Encryption.JOSE_ECDH_ES_ALG_ID)
                .setPublicKey(publicKey, AlgorithmPreferences.JOSE))
             .setDateTime(TIME_STAMP_JSON, new Date(), true)
            .setDateTime(BaseProperties.EXPIRES_JSON, expires, true)
            .setSignature(signer);
    }

*/

module.exports = Authority;
