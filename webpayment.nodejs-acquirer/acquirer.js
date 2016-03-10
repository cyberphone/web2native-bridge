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

const Https = require("https");
const Url = require("url");
const Fs = require("fs");

const ByteArray = require('webpki.org').ByteArray;
const Jcs = require('webpki.org').Jcs;
const JsonUtil = require('webpki.org').JsonUtil;
const Logging = require('webpki.org').Logging;

const logger = new Logging.Logger(__filename);
logger.info('Initializing...');

const homePage = Fs.readFileSync(__dirname + '/index.html');

function transact(jsonObject) {
  // Just some demo/test for now...
  var reader = new JsonUtil.ObjectReader(jsonObject);
  reader.getDateTime('now');
  reader.getString('escapeMe');
  reader.scanItem('signature');
  reader.checkForUnread();
  var verifier = new Jcs.Verifier();
  verifier.decodeSignature(jsonObject);
  return {t:8};
}

function trust(jsonObject) {
  if (jsonObject !== undefined) {
    throw new TypeError('not implemented');
  }
  return {};
}

var jsonProcessors = {
   '/transact' : transact,
   '/trust' : trust
};

const port = 8888;

const APPLICATION_JSON = 'application/json';

const options = {
  key: Fs.readFileSync('config/tlskeys/localhost.key.pem'),
  cert: Fs.readFileSync('config/tlskeys/localhost.cert.pem')
};

function serverError(response, message) {
  if (message === undefined || typeof message != 'string') {
    message = 'Unrecoverable error message';
  }
  response.writeHead(500, {'Content-Type': 'text/plain',
                           'Connection' : 'close',
                           'Content-Length': message.length});
  response.write(message);
  response.end();
}

Https.createServer(options, (request, response) => {
  if (request.method == 'GET') {
    response.writeHead(200, {'Content-Type': 'text/html'});
    response.write(homePage);
    response.end();
    return;
  }
  if (request.method != 'POST') {
    serverError(response, '"POST" method expected');
    return;
  }
  if (request.headers['content-type'] != APPLICATION_JSON) {
    serverError(response, 'Content type must be: ' + APPLICATION_JSON);
    return;
  }
  var pathname = Url.parse(request.url).pathname;
  if (pathname in jsonProcessors) {
    var input = new Buffer(0);
    request.on('data', (chunk) => {
      input = Buffer.concat([input, chunk]);
    });
    request.on('end', () => {
      try {
        var jsonIn = input.toString();
        logger.info('Received message [' + request.url + ']:\n' + jsonIn);
        var jsonOut = JSON.stringify(jsonProcessors[pathname](JSON.parse(jsonIn)));
        console.log('Sent message [' + request.url + ']:\n' + jsonOut);
        var output = ByteArray.stringToUtf8(jsonOut);
        response.writeHead(200, {'Content-Type': APPLICATION_JSON,
                                 'Content-Length': output.length});
        response.write(new Buffer(output));
        response.end();
      } catch (e) {
        logger.error(e.stack)
        serverError(response, e.message);
      }
    });
  } else {
    response.writeHead(404, {'Connection': 'close',
                             'Content-Type': 'text/plain',
                             'Content-Length': pathname.length});
    response.write(pathname);
    response.end();
  }
}).listen(parseInt(port, 10));

logger.info('Acquirer server running at http://localhost:' + port + ', ^C to shutdown');
