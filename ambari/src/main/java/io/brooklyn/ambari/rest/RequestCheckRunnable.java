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

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.brooklyn.util.core.http.HttpTool;
import org.apache.brooklyn.util.repeat.Repeater;
import org.apache.brooklyn.util.time.Duration;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.HttpClient;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.jayway.jsonpath.JsonPath;

import io.brooklyn.ambari.rest.domain.Request;

public class RequestCheckRunnable implements Runnable {

    private static final List<String> VALID_STATES = ImmutableList.of("IN_PROGRESS", "COMPLETED", "PENDING");

    private final Builder builder;

    protected RequestCheckRunnable(Builder builder) {
        this.builder = builder;
    }

    public static Builder check(Request request) {
        return new Builder(request);
    }

    @Override
    public void run() {
        boolean done = Repeater.create(String.format("Request %s status check", builder.request.toString()))
                .every(Duration.ONE_SECOND)
                .until(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        final String json = HttpTool.httpGet(builder.httpClient, URI.create(builder.request.getHref()), builder.headers).getContentAsString();
                        final String status = JsonPath.read(json, "$.Requests.request_status");
                        if (!VALID_STATES.contains(status)) {
                            throw new RuntimeException(
                                    "Request fails with state " + status +
                                            ". Check here for details " + builder.request.getHref());
                        }
                        return StringUtils.equals(status, "COMPLETED");
                    }
                })
                .limitTimeTo(builder.timeout)
                .rethrowExceptionImmediately()
                .run();

        if (!done) {
            throw new RuntimeException(builder.errorMessage);
        }
        ;
    }

    public static class Builder {

        private final Request request;
        private Map<String, String> headers;
        private Duration timeout;
        private String errorMessage;
        private HttpClient httpClient;

        public Builder(Request request) {
            this.request = request;
        }

        public Builder headers(Map<String, String> headers) {
            this.headers = headers;
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder client(HttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        public RequestCheckRunnable build() {
            Preconditions.checkNotNull(this.request);

            if (this.headers == null) {
                this.headers = ImmutableMap.of();
            }
            if (this.timeout == null) {
                this.timeout = Duration.FIVE_MINUTES;
            }
            if (StringUtils.isEmpty(this.errorMessage)) {
                this.errorMessage = String.format("The request did not finish with the status \"COMPLETED\" or within %s", this.timeout.toString());
            }
            if (this.httpClient == null) {
                this.httpClient = HttpTool.httpClientBuilder().build();
            }

            return new RequestCheckRunnable(this);
        }
    }
}
