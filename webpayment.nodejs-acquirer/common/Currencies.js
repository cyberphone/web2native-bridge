/*
 *  Copyright 2006-2016 WebPKI.org (http://webpki.org).
 *
 *  Licensed under the Apache License, Version 2.0 (the 'License'),
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an 'AS IS' BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
 
'use strict';

// Currencies used by the Web2Native Bridge PoC

const CURRENCIES = [
  'USD', '$\u200a',      true,  new RegExp(/^-?([1-9][0-9]*|0)[\.][0-9]{2}$/), 
  'EUR', '\u2009\u20ac', false, new RegExp(/^-?([1-9][0-9]*|0)[\.][0-9]{2}$/),
  'GBP', '\u00a3\u200a', true,  new RegExp(/^-?([1-9][0-9]*|0)[\.][0-9]{2}$/)
];

function Currencies(currency) {
  for (var i = 0; i < CURRENCIES.length; i += 4) {
      if (CURRENCIES[i++] == currency) {
          this.currency = currency;
          this.symbol = CURRENCIES[i++];
          this.symbolFirst = CURRENCIES[i++];
          this.syntax = CURRENCIES[i];
          return;
      }
  }
  throw new TypeError('Unknown currency: ' + currency);
}

Currencies.prototype.checkAmountSyntax = function(amountString) {
  if (this.syntax.test(amountString)) {
    return amountString;
  }
  throw new TypeError('Incorrect decimals or other syntax error: ' + amountString);
};

Currencies.prototype.convertAmountToDisplayString = function(amountString) {
  checkAmountSyntax(amountString);
  return this.symbolFirst ? this.symbol + amountString : amountString + this.symbol;
};

Currencies.prototype.toString = function() {
  return this.currency;
};

  
module.exports = Currencies;
