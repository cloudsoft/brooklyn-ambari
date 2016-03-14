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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.apache.brooklyn.util.http.HttpTool;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.testng.annotations.Test;

import com.google.common.net.HttpHeaders;

import retrofit.RequestInterceptor;

public class AmbariRequestInterceptorTest {

    UsernamePasswordCredentials usernamePasswordCredentials = new UsernamePasswordCredentials("admin", "password");

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = "Credentials must not be null")
    public void constructorThrowExIfArgIsNull() {
        new AmbariRequestInterceptor(null);
    }

    @Test
    public void addCorrectHeaderToRequest() {
        AmbariRequestInterceptor requestInterceptor = new AmbariRequestInterceptor(usernamePasswordCredentials);

        RequestInterceptor.RequestFacade requestFacade = mock(RequestInterceptor.RequestFacade.class);
        requestInterceptor.intercept(requestFacade);

        verify(requestFacade).addHeader(HttpHeaders.AUTHORIZATION, HttpTool.toBasicAuthorizationValue(usernamePasswordCredentials));
        verify(requestFacade).addHeader("X-Requested-By", "Brooklyn Ambari");
        verify(requestFacade).addHeader("Content-Type", "text/plain");
    }
}
