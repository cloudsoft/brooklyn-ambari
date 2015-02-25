/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.brooklyn.ambari.rest;

import java.text.MessageFormat;

import brooklyn.util.http.HttpToolResponse;


public class AmbariApiException extends RuntimeException {

    static final String ERROR_MESSAGE =
            "Unacceptable Response Code from Ambari Rest API {0}\n" +
                    "Message from Server {1}";

    static final MessageFormat messageFormat = new MessageFormat(ERROR_MESSAGE);

    private final HttpToolResponse httpToolResponse;

    public AmbariApiException(HttpToolResponse httpToolResponse) {
        super(messageFormat.format(ERROR_MESSAGE, httpToolResponse.getContentAsString(), Integer.toString(httpToolResponse.getResponseCode())));
        this.httpToolResponse = httpToolResponse;
    }
}
