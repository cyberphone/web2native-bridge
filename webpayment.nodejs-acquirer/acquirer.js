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

// This is a node.js version of the "Acquirer" server used in the Web2Native Bridge
// proof-of-concept payment system.

const Https = require('https');
const Url = require('url');
const Fs = require('fs');

const Keys = require('webpki.org').Keys;
const Jcs = require('webpki.org').Jcs;

const Config = require('./config/config');
const ServerCertificateSigner = require('./common/ServerCertificateSigner');
const BaseProperties = require('./common/BaseProperties');
const Authority = require('./common/AuthorityObject');
const Expires = require('./common/Expires');
const ErrorReturn = require('./common/ErrorReturn');
const FinalizeRequest = require('./common/FinalizeRequest');
const PaymentRequest = require('./common/PaymentRequest');
const Currencies = require('./common/Currencies');
const Payee = require('./common/Payee');
const Big = require('./contributed/big/big');

const ByteArray = require('webpki.org').ByteArray;
const JsonUtil = require('webpki.org').JsonUtil;
const Logging = require('webpki.org').Logging;

const logger = new Logging.Logger(__filename);
logger.info('Initializing...');


/////////////////////////////////
// Initiate static data
/////////////////////////////////

function readFile(path) {
  return Fs.readFileSync(path);
}

const homePage = readFile(__dirname + '/index.html');

const jsonPostProcessors = {
 
  // To be used with "test/sample.json"  
  sample : function(reader) {
    // Just some demo/test for now...
    reader.getDateTime('now');
    reader.getString('escapeMe');
    reader.getSignature();
    return serverCertificateSigner.sign({t:8});
  },

  // To be used with "test/utf8.json"  
  utf8 : function(reader) {
    // Just some demo/test for now...
    reader.getInt('\u20ac\u00e5\u00f6k');
    reader.getSignature();
    return serverCertificateSigner.sign({t:8});
  },
  
  error : function(reader) {
    var errorReturn = new ErrorReturn(reader);
    errorReturn = new ErrorReturn(ErrorReturn.BLOCKED_ACCOUNT,"extra");
    var wr = new JsonUtil.ObjectWriter();
    errorReturn.write(wr);
    return wr.getRootObject();
  },

  transact : function(reader) {
    var finalizeRequest = new FinalizeRequest(reader);
    logger.info(finalizeRequest.getEmbeddedResponse().getProtectedAccountData(encryptionKeys).toString());
    logger.info(finalizeRequest.getAmount().toString());
    function x() {
      this.privateKey = encryptionKeys[0];
    }
    x.prototype.sign = function(jsonObject) {
      var signer = new Jcs.Signer(this.privateKey);
      return signer.sign(jsonObject);
    };
    return PaymentRequest.encode(Payee.init('Demo Merchant','#126740'),
                                 new Big('3450.75'),
                                 new Currencies('USD'),
                                 '68005',
                                 Expires.inMinutes(30),
                                 new x()).getRootObject();
  }

};

const jsonGetProcessors = {

  authority : function() {
    return authorityData;
  }

};

var port = Url.parse(Config.host).port;
if (port == null) {
  port = 443;
}
var applicationPath = Url.parse(Config.host).path;
if (applicationPath == '/') {
  applicationPath = '';
}

/////////////////////////////////
// Initiate cryptographic keys
/////////////////////////////////

const options = {
  key: readFile(Config.tlsKeys.keyFile),
  cert: readFile(Config.tlsKeys.certFile)
};

var keyData = readFile(Config.ownKeys.certAndKey);

const serverCertificateSigner =
  new ServerCertificateSigner(Keys.createPrivateKeyFromPem(keyData),
                              Keys.createCertificatesFromPem(keyData));

const paymentRoot = Keys.createCertificatesFromPem(readFile(Config.trustAnchors));

const encryptionKeys = [];
encryptionKeys.push(Keys.createPrivateKeyFromPem(readFile(Config.ownKeys.ecEncryptionKey)));
encryptionKeys.push(Keys.createPrivateKeyFromPem(readFile(Config.ownKeys.rsaEncryptionKey)));

const authorityData = Authority.encode(Config.host + '/authority',
                                       Config.host + '/transact',
                                       encryptionKeys[0].getPublicKey(),
                                       Expires.inDays(365),
                                       serverCertificateSigner).getRootObject();
  
/////////////////////////////////
// Core HTTP server code
/////////////////////////////////

function serverError(response, message) {
  if (message === undefined || typeof message != 'string') {
    message = 'Unrecoverable error message';
  }
  response.writeHead(500, {'Content-Type'  : 'text/plain',
                           'Connection'    : 'close',
                           'Content-Length': message.length});
  response.write(message);
  response.end();
}

function returnJsonData(request, response, jsonObject) {
  var jsonOut = JSON.stringify(jsonObject);
  console.log('Sent message [' + request.url + ']:\n' + jsonOut);
  var output = ByteArray.stringToUtf8(jsonOut);
  response.writeHead(200, {'Content-Type'  : BaseProperties.JSON_CONTENT_TYPE,
                           'Connection'    : 'close',
                           'Content-Length': output.length});
  response.write(new Buffer(output));
  response.end();
}

function noSuchFileResponse(response, request) {
    var message = 'No such file: ' + request.url;
    response.writeHead(404, {'Connection'    : 'close',
                             'Content-Type'  : 'text/plain',
                             'Content-Length': message.length});
    response.write(message);
    response.end();
 }

Https.createServer(options, (request, response) => {
  var pathname = Url.parse(request.url).pathname;
  if (pathname.startsWith(applicationPath + '/')) {
    pathname = pathname.substring(applicationPath.length + 1);
  }
  if (request.method == 'GET') {
    if (pathname in jsonGetProcessors) {
      returnJsonData(request, response, jsonGetProcessors[pathname]());
    } else if (pathname == '') {
      response.writeHead(200, {'Content-Type': 'text/html'});
      response.write(homePage);
      response.end();
    } else {
      noSuchFileResponse(response, request);
    }
    return;
  }
  request.setEncoding('utf8');
  if (request.method != 'POST') {
    serverError(response, '"POST" method expected');
    return;
  }
  if (pathname in jsonPostProcessors) {
    if (request.headers['content-type'] != BaseProperties.JSON_CONTENT_TYPE) {
      serverError(response, 'Content type must be: ' + BaseProperties.JSON_CONTENT_TYPE);
      return;
    }
    var jsonIn = '';
    request.on('data', (chunk) => {
      jsonIn += chunk;
    });
    request.on('end', () => {
      try {
        logger.info('Received message [' + request.url + ']:\n' + jsonIn);
        returnJsonData(request,
                       response,
                       jsonPostProcessors[pathname](new JsonUtil.ObjectReader(JSON.parse(jsonIn))));
      } catch (e) {
        logger.error(e.stack)
        serverError(response, e.message);
      }
    });
  } else {
    noSuchFileResponse(response, request);
  }
}).listen(port, 10);

logger.info('Acquirer server running at ' + Config.host + ', ^C to shutdown');