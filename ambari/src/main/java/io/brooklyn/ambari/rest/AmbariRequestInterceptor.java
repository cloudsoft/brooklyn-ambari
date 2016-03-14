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

import org.apache.brooklyn.util.http.HttpTool;
import org.apache.http.auth.UsernamePasswordCredentials;

import com.google.common.base.Preconditions;
import com.google.common.net.HttpHeaders;

import retrofit.RequestInterceptor;

/**
 * Adds the required Ambari headers for every single request made to the API.
 */
public class AmbariRequestInterceptor implements RequestInterceptor {

    private final String basicAuth;

    public AmbariRequestInterceptor(UsernamePasswordCredentials usernamePasswordCredentials) {
        Preconditions.checkNotNull(usernamePasswordCredentials, "Credentials must not be null");
        this.basicAuth = HttpTool.toBasicAuthorizationValue(usernamePasswordCredentials);
    }

    @Override
    public void intercept(RequestFacade requestFacade) {
        requestFacade.addHeader(HttpHeaders.AUTHORIZATION, basicAuth);
        requestFacade.addHeader("X-Requested-By", "Brooklyn Ambari");
        requestFacade.addHeader("Content-Type", "text/plain");
    }
}
