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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.brooklyn.util.exceptions.PropagatedRuntimeException;
import org.apache.brooklyn.util.time.Duration;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.message.BasicStatusLine;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.Test;

import com.jayway.jsonpath.PathNotFoundException;

import io.brooklyn.ambari.rest.domain.Request;

public class RequestCheckRunnableTest {

    class MyCustomException extends Exception {}

    private static final String ERROR_MESSAGE = "Something went wrong, please check Ambari UI";

    @Test(expectedExceptions = NullPointerException.class)
    public void testNullRequestThrowsExOnBuild() {
        new RequestCheckRunnable.Builder(null).build();
    }

    @Test(expectedExceptions = PropagatedRuntimeException.class)
    public void testRunnablePropagatesEx() throws IOException {
        HttpClient httpClient = mock(HttpClient.class);
        when(httpClient.execute(any(HttpGet.class))).thenThrow(MyCustomException.class);

        new RequestCheckRunnable.Builder(mockRequest("http://www.example.com")).client(httpClient).build().run();
    }

    @Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = "Request fails with state FAILED. Check here for details http://www.example.com")
    public void testFailedRequestThrowsRuntimeEx() throws IOException {
        new RequestCheckRunnable.Builder(mockRequest("http://www.example.com"))
                .client(mockHttpClient("{\"Requests\":{\"request_status\": \"FAILED\"}}"))
                .errorMessage(ERROR_MESSAGE)
                .build()
                .run();
    }

    @Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = ERROR_MESSAGE)
    public void testTimeoutRequestThrowsRuntimeEx() throws IOException {
        new RequestCheckRunnable.Builder(mockRequest("http://www.example.com"))
                .client(mockHttpClient("{\"Requests\":{\"request_status\": \"IN_PROGRESS\"}}"))
                .timeout(Duration.FIVE_SECONDS)
                .errorMessage(ERROR_MESSAGE)
                .build()
                .run();
    }

    @Test(expectedExceptions = PathNotFoundException.class)
    public void testInvalidReponseThrowsParseEx() throws IOException {
        new RequestCheckRunnable.Builder(mockRequest("http://www.example.com"))
                .client(mockHttpClient("Invalid body: No Json"))
                .timeout(Duration.FIVE_SECONDS)
                .build()
                .run();
    }

    @Test
    public void testSuccessfulRequestFinishes() {
        Throwable throwable = null;

        try {
            new RequestCheckRunnable.Builder(mockRequest("http://www.example.com"))
                    .client(mockHttpClient("{\"Requests\":{\"request_status\": \"COMPLETED\"}}"))
                    .timeout(Duration.FIVE_SECONDS)
                    .build()
                    .run();
        } catch (Throwable t) {
            throwable = t;
        }

        assertNull(throwable);
    }

    private Request mockRequest(String url) {
        Request request = mock(Request.class);
        when(request.getHref()).thenReturn(url);

        return request;
    }

    private HttpClient mockHttpClient(String returnedBody) throws IOException {
        HttpEntity httpEntity = mock(HttpEntity.class);
        when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream(returnedBody.getBytes(StandardCharsets.UTF_8)));
        when(httpEntity.getContentLength()).then(new Answer<Long>() {
            @Override
            public Long answer(InvocationOnMock invocationOnMock) throws Throwable {
                ((HttpEntity) invocationOnMock.getMock()).getContent().reset();
                return 1L;
            }
        });

        HttpResponse httpResponse = mock(HttpResponse.class);
        when(httpResponse.getEntity()).thenReturn(httpEntity);
        when(httpResponse.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, null));

        HttpClient httpClient = mock(HttpClient.class);
        when(httpClient.execute(any(HttpGet.class))).thenReturn(httpResponse);

        return httpClient;
    }
}
