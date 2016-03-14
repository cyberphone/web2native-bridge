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

const Config = require('./config/config');
const ServerCertificateSigner = require('./common/servercertificatesigner');
const BaseProperties = require('./common/baseproperties');
const Authority = require('./common/authorityobject');
const Expires = require('./common/expires');

const ByteArray = require('webpki.org').ByteArray;
const Jcs = require('webpki.org').Jcs;
const JsonUtil = require('webpki.org').JsonUtil;
const Logging = require('webpki.org').Logging;

const logger = new Logging.Logger(__filename);
logger.info('Initializing...');

function transact(jsonObject) {
  // Just some demo/test for now...
  var reader = new JsonUtil.ObjectReader(jsonObject);
  reader.getDateTime('now');
  reader.getString('escapeMe');
  reader.scanItem('signature');
  reader.checkForUnread();
  var verifier = new Jcs.Verifier();
  verifier.decodeSignature(jsonObject);
  return serverCertificateSigner.sign({t:8});
}

function trust(jsonObject) {
  if (jsonObject !== undefined) {
    throw new TypeError('not implemented');
  }
  return {};
}


/////////////////////////////////
// Initiate static data
/////////////////////////////////

function readFile(path) {
  return Fs.readFileSync(path);
}

const homePage = readFile(__dirname + '/index.html');

const jsonPostProcessors = {
   '/transact' : transact,
   '/trust' : trust
};

const jsonGetProcessors = {
   '/authority' : function() {
     return authorityData;
   }
};


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

Https.createServer(options, (request, response) => {
  var pathname = Url.parse(request.url).pathname;
  if (request.method == 'GET') {
    if (pathname in jsonGetProcessors) {
      returnJsonData(request, response, jsonGetProcessors[pathname]());
    } else {
      response.writeHead(200, {'Content-Type': 'text/html'});
      response.write(homePage);
      response.end();
    }
    return;
  }
  if (request.method != 'POST') {
    serverError(response, '"POST" method expected');
    return;
  }
  if (request.headers['content-type'] != BaseProperties.JSON_CONTENT_TYPE) {
    serverError(response, 'Content type must be: ' + BaseProperties.JSON_CONTENT_TYPE);
    return;
  }
  if (pathname in jsonPostProcessors) {
    var input = new Buffer(0);
    request.on('data', (chunk) => {
      input = Buffer.concat([input, chunk]);
    });
    request.on('end', () => {
      try {
        var jsonIn = input.toString();
        logger.info('Received message [' + request.url + ']:\n' + jsonIn);
        returnJsonData(request, response, jsonPostProcessors[pathname](JSON.parse(jsonIn)));
      } catch (e) {
        logger.error(e.stack)
        serverError(response, e.message);
      }
    });
  } else {
    response.writeHead(404, {'Connection'    : 'close',
                             'Content-Type'  : 'text/plain',
                             'Content-Length': pathname.length});
    response.write(pathname);
    response.end();
  }
}).listen(parseInt(
   Config.host.lastIndexOf(':') < 7 ? 443 : Config.host.substring(Config.host.lastIndexOf(':') + 1), 10));

logger.info('Acquirer server running at ' + Config.host + ', ^C to shutdown');
