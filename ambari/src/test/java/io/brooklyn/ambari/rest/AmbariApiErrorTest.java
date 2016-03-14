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

import static org.testng.Assert.assertEquals;

import java.text.MessageFormat;
import java.util.List;

import org.apache.brooklyn.util.http.HttpToolResponse;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;

public class AmbariApiErrorTest {

    @Test
    public void testNameFormat() throws Exception {
        assertEquals(MessageFormat.format(AmbariApiException.ERROR_MESSAGE, "fdsfs", "Dsfsd", "dsfgdhsh"), "Error from the Ambari REST API - HTTP/fdsfs [Dsfsd]:\n" +
                "dsfgdhsh");
    }
    
    @Test
    public void testExceptionMessage() throws Exception {
        String content = "mycontent";
        HttpToolResponse httpToolResponse = new HttpToolResponse(456, ImmutableMap.<String, List<String>>of(), content.getBytes(), 0, 1, 2);
        
//        assertEquals(new AmbariApiException(httpToolResponse).getMessage(), "Unacceptable Response Code from Ambari Rest API mycontent\n" +
//                "Message from Server 456");
    }
}