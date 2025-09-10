/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.audit.manager;

import org.apache.inlong.audit.tool.DTO.AuditAlertRule;
import org.apache.inlong.audit.tool.DTO.AuditData;
import org.apache.inlong.audit.tool.DTO.AuditInfo;
import org.apache.inlong.audit.tool.DTO.AuditVO;
import org.apache.inlong.audit.tool.DTO.AlertPolicy;
import org.apache.inlong.audit.tool.config.AppConfig;
import org.apache.inlong.audit.tool.manager.ManagerClient;
import org.apache.inlong.audit.tool.response.Response;

import org.apache.inlong.audit.tool.util.HttpUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


class ManagerClientTest {

    @Mock
    private AppConfig appConfig;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private HttpUtils httpUtils;

    @InjectMocks
    private ManagerClient managerClient;

    private final String testManagerUrl = "http://localhot:8080";


    @Test
    void testFetchAlertPolicies_Success() throws Exception {
        // Mock data
        AuditAlertRule rule1 = new AuditAlertRule();
        rule1.setId(1);
        rule1.setInlongGroupId("group1");
        rule1.setInlongStreamId("stream1");
        rule1.setAuditId("1,2,3");

        AuditAlertRule rule2 = new AuditAlertRule();
        rule2.setId(2);
        rule2.setInlongGroupId("group2");
        rule2.setInlongStreamId("stream2");
        rule2.setAuditId("4,5");

        List<AuditAlertRule> mockRules = Arrays.asList(rule1, rule2);

        // Mock response
        Response<List<AuditAlertRule>> mockResponse = new Response<>();
        mockResponse.setSuccess(true);
        mockResponse.setData(mockRules);

        // Mock HTTP call
        when(HttpUtils.request(
                any(RestTemplate.class),
                eq(testManagerUrl + "/audit/alert/rule/list"),
                eq(HttpMethod.GET),
                isNull(),
                isNull(),
                any(ParameterizedTypeReference.class)))
                .thenReturn(mockResponse);

        // Execute
        List<AlertPolicy> result = managerClient.fetchAlertPolicies();

        // Verify
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void testFetchAlertPolicies_Failure() throws Exception {
        // Mock response
        Response<List<AuditAlertRule>> mockResponse = new Response<>();
        mockResponse.setSuccess(false);
        mockResponse.setErrMsg("Failed to fetch alert rules");

        // Mock HTTP call
        when(HttpUtils.request(
                any(RestTemplate.class),
                anyString(),
                any(HttpMethod.class),
                isNull(),
                isNull(),
                any(ParameterizedTypeReference.class)))
                .thenReturn(mockResponse);

        // Execute
        List<AlertPolicy> result = managerClient.fetchAlertPolicies();

        // Verify
        assertNull(result);
    }

    @Test
    void testFetchAlertRules_Success() {
        // Mock data
        AuditAlertRule rule = new AuditAlertRule();
        rule.setId(1);
        rule.setInlongGroupId("group1");

        Response<List<AuditAlertRule>> mockResponse = new Response<>();
        mockResponse.setSuccess(true);
        mockResponse.setData(Collections.singletonList(rule));

        // Mock HTTP call
        when(HttpUtils.request(
                any(RestTemplate.class),
                eq(testManagerUrl + "/audit/alert/rule/list"),
                eq(HttpMethod.GET),
                isNull(),
                isNull(),
                any(ParameterizedTypeReference.class)))
                .thenReturn(mockResponse);

        // Execute
        List<AuditAlertRule> result = managerClient.fetchAlertRules();

        // Verify
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("group1", result.get(0).getInlongGroupId());
    }

    @Test
    void testFetchAlertRules_Failure() {
        // Mock response
        Response<List<AuditAlertRule>> mockResponse = new Response<>();
        mockResponse.setSuccess(false);

        // Mock HTTP call
        when(HttpUtils.request(
                any(RestTemplate.class),
                anyString(),
                any(HttpMethod.class),
                isNull(),
                isNull(),
                any(ParameterizedTypeReference.class)))
                .thenReturn(mockResponse);

        // Execute
        List<AuditAlertRule> result = managerClient.fetchAlertRules();

        // Verify
        assertNull(result);
    }

    @Test
    void testFetchAuditData_Success() throws Exception {
        // Mock alert rules
        AuditAlertRule rule = new AuditAlertRule();
        rule.setInlongGroupId("group1");
        rule.setInlongStreamId("stream1");
        rule.setAuditId("1,2,3");

        Response<List<AuditAlertRule>> alertRulesResponse = new Response<>();
        alertRulesResponse.setSuccess(true);
        alertRulesResponse.setData(Collections.singletonList(rule));

        // Mock audit data response
        AuditVO auditVO = new AuditVO();
        AuditInfo auditInfo = new AuditInfo();
        auditInfo.setCount(100);
        auditVO.setAuditSet(Collections.singletonList(auditInfo));

        Response<List<AuditVO>> auditDataResponse = new Response<>();
        auditDataResponse.setSuccess(true);
        auditDataResponse.setData(Collections.singletonList(auditVO));

        // Mock HTTP calls
        when(HttpUtils.request(
                any(RestTemplate.class),
                eq(testManagerUrl + "/audit/alert/rule/list"),
                eq(HttpMethod.GET),
                isNull(),
                isNull(),
                any(ParameterizedTypeReference.class)))
                .thenReturn(alertRulesResponse);

        when(HttpUtils.request(
                any(RestTemplate.class),
                eq(testManagerUrl + "/audit/listAll"),
                eq(HttpMethod.POST),
                anyString(),
                isNull(),
                any(ParameterizedTypeReference.class)))
                .thenReturn(auditDataResponse);

        // Execute
        List<AuditData> result = managerClient.fetchAuditData();

        // Verify
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("1", result.get(0).getAuditId());
    }

    @Test
    void testFetchAuditData_NoAlertRules() throws Exception {
        // Mock empty alert rules
        Response<List<AuditAlertRule>> alertRulesResponse = new Response<>();
        alertRulesResponse.setSuccess(true);
        alertRulesResponse.setData(Collections.emptyList());

        // Mock HTTP call
        when(HttpUtils.request(
                any(RestTemplate.class),
                eq(testManagerUrl + "/audit/alert/rule/list"),
                eq(HttpMethod.GET),
                isNull(),
                isNull(),
                any(ParameterizedTypeReference.class)))
                .thenReturn(alertRulesResponse);

        // Execute
        List<AuditData> result = managerClient.fetchAuditData();

        // Verify
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testFetchAuditData_AuditRequestFailure() throws Exception {
        // Mock alert rules
        AuditAlertRule rule = new AuditAlertRule();
        rule.setInlongGroupId("group1");
        rule.setInlongStreamId("stream1");
        rule.setAuditId("1,2,3");

        Response<List<AuditAlertRule>> alertRulesResponse = new Response<>();
        alertRulesResponse.setSuccess(true);
        alertRulesResponse.setData(Collections.singletonList(rule));

        // Mock failed audit data response
        Response<List<AuditVO>> auditDataResponse = new Response<>();
        auditDataResponse.setSuccess(false);

        // Mock HTTP calls
        when(HttpUtils.request(
                any(RestTemplate.class),
                eq(testManagerUrl + "/audit/alert/rule/list"),
                eq(HttpMethod.GET),
                isNull(),
                isNull(),
                any(ParameterizedTypeReference.class)))
                .thenReturn(alertRulesResponse);

        when(HttpUtils.request(
                any(RestTemplate.class),
                eq(testManagerUrl + "/audit/listAll"),
                eq(HttpMethod.POST),
                anyString(),
                isNull(),
                any(ParameterizedTypeReference.class)))
                .thenReturn(auditDataResponse);

        // Execute
        List<AuditData> result = managerClient.fetchAuditData();

        // Verify
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}