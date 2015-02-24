package org.apache.brooklyn.ambari.rest;

import static org.testng.Assert.assertEquals;

import java.util.List;

import org.testng.annotations.Test;

import brooklyn.util.http.HttpToolResponse;

import com.google.common.collect.ImmutableMap;

public class AmbariApiErrorTest {

    @Test
    public void testNameFormat() throws Exception {
        assertEquals(AmbariApiException.messageFormat.format(AmbariApiException.ERROR_MESSAGE, "fdsfs", "Dsfsd"), "Unacceptable Response Code from Ambari Rest API fdsfs\n" +
                "Message from Server Dsfsd");
    }
    
    @Test
    public void testExceptionMessage() throws Exception {
        String content = "mycontent";
        HttpToolResponse httpToolResponse = new HttpToolResponse(456, ImmutableMap.<String, List<String>>of(), content.getBytes(), 0, 1, 2);
        
        assertEquals(new AmbariApiException(httpToolResponse).getMessage(), "Unacceptable Response Code from Ambari Rest API mycontent\n" +
                "Message from Server 456");
    }
}