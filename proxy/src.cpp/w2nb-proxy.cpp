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

//////////////////////////////////////////////////////////////////////////////////////////
//                                   w2nb-proxy.cpp                                     //
//                                                                                      //
// This program emulates the proxy required on the native side by the Web2Native Bridge //
// while still only relying on Chrome's Native Messaging.                               //
//                                                                                      //
// Note: The security features provided by the Web2Native Bridge are NOT supported!     //
//                                                                                      //
// Author: Anders Rundgren                                                              //
//////////////////////////////////////////////////////////////////////////////////////////

#ifdef WIN32
#define FILE_SEPARATOR   '\\'
#define _CRT_SECURE_NO_WARNINGS
#define JAVA_PLAF " -Dswing.defaultlaf=com.sun.java.swing.plaf.windows.WindowsLookAndFeel"
#define JAVA_LOG " \"-Djava.util.logging.SimpleFormatter.format=%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$s %2$s %5$s%6$s%n\""
#else
#define FILE_SEPARATOR   '/'
#define JAVA_LOG " \"-Djava.util.logging.SimpleFormatter.format=%1\\$tY-%1\\$tm-%1\\$td %1\\$tH:%1\\$tM:%1\\$tS %4\\$s %2\\$s %5\\$s%6\\$s%n\""
#define JAVA_PLAF ""
#endif

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdarg.h>

#define PROXY_VERSION "1.00"

#define MANIFEST_SIZE 1000

static char *res;

static FILE* logFile;

static void loggedError(const char* format, ...) {
    if (logFile) {
        va_list args;
        va_start(args, format);
        vfprintf(logFile, format, args);
        va_end(args);
        fclose(logFile);
    }
    exit(EXIT_FAILURE);
}

// "Budget" JSON parser
static char* getJSONProperty(char *property, char beginChar, char endChar) {
    char buffer[100] = "\"";
    strcat(buffer, property);
    strcat(buffer, "\":");
    char *start = strstr(res, buffer);
    if (!start) {
        loggedError("Property %s missing\nJSON=\n%s", property, res);
    }
    start += strlen(buffer);
    if (*start++ != beginChar) {
        loggedError("Expected: %c\nJSON=\n%s", beginChar, res);
    }
    char *end = strchr(start, endChar);
    if (!end) {
        loggedError("Expected: %c\nJSON=\n%s", endChar, res);
    }
    int length = end - start + 1;
    char *value = new char[length--];
    strncpy(value, start, length);
    value[length] = 0;
    return value;
}

static char* getJSONString(char *property) {
    return getJSONProperty(property, '\"', '\"');
}

static char* getJSONArray(char *property) {
    return getJSONProperty(property, '[', ']');
}

static bool matching(char* accessDescriptor, char* calledBy) {
    while (*accessDescriptor) {
        if (*accessDescriptor == '*') {
            accessDescriptor++;
            while (*calledBy != *accessDescriptor) {
                if (*calledBy) {
                    calledBy++;
                } else {
                    return false;
                }
            }
        } else {
            if (*calledBy != *accessDescriptor) {
                return false;
            }
            accessDescriptor++;
            calledBy++;
        }
    }
    return !*calledBy;
}

static void checkAccess(char* appPath, char* calledBy) {
    char manifest[MANIFEST_SIZE + 1];
    int c;
    int index = 0;
    bool quote = false;

    strcat(appPath, "manifest.json");
    FILE* manifestFile = fopen(appPath, "r");
    if (!manifestFile) {
        loggedError("Error: 'manifest.json' not found!");
    }
    while ((c = fgetc(manifestFile)) != EOF) {
        if (index == MANIFEST_SIZE) {
            loggedError("Error: manifest bigger than: %d", MANIFEST_SIZE);
        }
        if (c == '\"') {
            quote = !quote;
        }
        else if (c == '\\') {
            if (!quote || fgetc(manifestFile) != '\\') {
                loggedError("Error: unexpected \\ in manifest");
            }
        }
        else if (!quote && (c == ' ' || c == '\n' || c == '\t' || c == '\r')) {
            continue;
        }
        manifest[index++] = (char)c;
    }
    manifest[index] = (char)0;
    fclose(manifestFile);
    res = manifest;
    char* callableFrom = getJSONArray("callableFrom");
    index = 0;
    int arrayLength = strlen(callableFrom) - 4;
    bool next = false;
    while (index < arrayLength) {
        if (next) {
            if (callableFrom[index++] != ',') {
                loggedError("Error: missing ',' in manifest");
            }
        }
        next = true;
        if (callableFrom[index++] != '\"') {
            loggedError("Error: missing '\"' in manifest");
        }
        char* startString = callableFrom + index;
        char* endString = strchr(startString, '\"');
        if (!endString) {
            loggedError("Error: missing '\"' in manifest");
        }
        *endString = 0;
        if (matching(startString, calledBy)) {
            return;
        }
        index += strlen(startString) + 1;
    }
    loggedError("Error: not allowed: %s", calledBy);
}

int main(int argc, char *argv[]) {
    // Reading an initial JSON message which can be of two kinds:
    // 1. Proxy verification which consists of an object {"proxyVersion":"n.nn"}
    // 2. Java application call which consists of an object ("application":"dotted-path",
    //                                                       "url":"invocation-url",
    //                                                       "windowB64":"base64url-encoded-json-object",
    //                                                       "argumentsB64":"base64url-encoded-json-object"}

    // Chrome presumes message length in native order. Not very cool.
    // The following code therefore only runs on little-endian CPUs.
    int length = 0;
    for (int i = 0; i < 32; i += 8) {
        length += getchar() << i;
    }

    // We expect a tiny JSON object
    if (length > 10000) {
        exit(EXIT_FAILURE);
    }

    // OK. Read the JSON string
    res = new char[length + 1];
    for (int n = 0; n < length; n++) {
        res[n] = getchar();
    }
    res[length] = 0;

    // Are we doing proxy verification?
    if (strstr(res, "\"proxyVersion\":")) {
        if (strcmp(PROXY_VERSION, getJSONString("proxyVersion"))) {
            exit(EXIT_FAILURE);
        }
        char zeroObject[] = { 2,0,0,0,'{','}' };
        for (int n = 0; n < sizeof(zeroObject); n++) {
            putchar(zeroObject[n]);
        }
        fclose(stdout);
        exit(EXIT_SUCCESS);
    }

    // No, we are executing a Java target application
    char cmd[2000] = "java" JAVA_LOG JAVA_PLAF " -jar \"";
    char path[500];
    strcpy(path, argv[0]);
    int i = strlen(path);
    while (path[--i] != FILE_SEPARATOR)
        ;
    path[i] = 0;
    char fs[] = { FILE_SEPARATOR,0 };

    char *application = getJSONString("application");

    // Check that the caller isn't trying to get outside the sandbox
    i = 0;
    char c;
    while (c = application[i++]) {
        if ((c > 'Z' || c < 'A') && (c > 'z' || c < 'a') && (c > '9' || c < '0') && c != '.' && c != '_') {
            exit(EXIT_FAILURE);
        }
    }

    // Create a path to the application directory including /
    char appPath[500];
    strcpy(appPath, path);
    strcat(appPath, fs);
    strcat(appPath, "apps");
    strcat(appPath, fs);
    strcat(appPath, application);
    strcat(appPath, fs);

    // The actual JAR to call
    strcat(cmd, appPath);
    strcat(cmd, application);
    strcat(cmd, ".jar\" \"");

    // Parameters to called Java program

    // args[0] => Full path to proxy/install
    strcat(cmd, path);
    strcat(cmd, "\" ");

    // args[1] => Dotted path (=application name)
    strcat(cmd, application);
    strcat(cmd, " \"");

    // args[2] => invoking URL
    char* calledBy = getJSONString("url");
    strcat(cmd, calledBy);
    strcat(cmd, "\" ");

    // args[3] => Invoking window core data
    strcat(cmd, getJSONString("windowB64"));
    strcat(cmd, " ");

    // args[4] => Optional arguments to navigator.nativeConnect
    strcat(cmd, getJSONString("argumentsB64"));

    // args[5..n] => Chrome standard arguments
    for (int i = 1; i < argc; i++) {
        strcat(cmd, " ");
        strcat(cmd, argv[i]);
    }

    // Log file to last executed command
    char fileName[500];
    strcpy(fileName, path);
    strcat(fileName, fs);
    strcat(fileName, "logs");
    strcat(fileName, fs);
    strcat(fileName, "w2nb-proxy-init.log");
    logFile = fopen(fileName, "w");
    fprintf(logFile, "commmand: %s\n", cmd);

    // We need to read the manifest now
//	fprintf(logFile, "called by: %s\n", calledBy);
    checkAccess(appPath, calledBy);

    // This is not the recommended solution for POSIX-compliant systems but hey, this is a PoC...
    int returnCode = system(cmd);
    if (returnCode) {
        fprintf(logFile, "Error: '%s' %d\n", strerror(returnCode), returnCode);
    }
    fclose(logFile);
    return 0;
}
