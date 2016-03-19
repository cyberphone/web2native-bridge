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

// Configuration parameters fo the "Acquirer" server

var config = {};

config.host = 'https://localhost:8888';

config.tlsKeys = {
  keyFile:  __dirname + '/tlskeys/localhost.key.pem',
  certFile: __dirname + '/tlskeys/localhost.cert.pem'
};

config.ownKeys = {
  certAndKey      :  __dirname + '/ownkeys/acquirer.cert-and-key.pem',
  rsaEncryptionKey: __dirname + '/ownkeys/acquirer.rsa-enc-key.pem',
  ecEncryptionKey : __dirname + '/ownkeys/acquirer.ec-enc-key.pem'
};

config.trustAnchors = __dirname + '/trustanchors/paymentnetworks.pem';

module.exports = config;