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

package org.apache.inlong.audit.tool.manager;

import org.apache.inlong.audit.tool.DTO.AuditAlertRule;
import org.apache.inlong.audit.tool.DTO.AuditData;
import org.apache.inlong.audit.tool.DTO.AuditInfo;
import org.apache.inlong.audit.tool.DTO.AuditRequest;
import org.apache.inlong.audit.tool.VO.AuditVO;
import org.apache.inlong.audit.tool.config.AppConfig;
import org.apache.inlong.audit.tool.response.Response;
import org.apache.inlong.audit.tool.util.CommonBeanUtils;
import org.apache.inlong.audit.tool.util.HttpUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manager Client layer implementation
 */
public class ManagerClient {

    private final AppConfig appConfig;
    private static final Logger LOGGER = LoggerFactory.getLogger(ManagerClient.class);
    private final RestTemplate restTemplate = new RestTemplate();

    public ManagerClient(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    /**
     * Fetches the list of audit alert rules from the manager API.
     * Constructs the request URL, sends an HTTP GET request, and processes the response.
     *
     * @return A list of {@link AuditAlertRule} objects if successful, or null if the request fails.
     */
    public List<AuditAlertRule> fetchAlertRules() {
        // Get the manager URL from app configuration
        String managerUrl = appConfig.getManagerUrl();
        String path = "/audit/alert/rule/list";
        List<AuditAlertRule> auditAlertRuleList = new ArrayList<>();

        // Ensure there is only one forward slash between the base URL and path
        String url = (managerUrl.endsWith("/") ? managerUrl.substring(0, managerUrl.length() - 1) : managerUrl) + path;

        LOGGER.info("begin query audit alertRule list request={}");

        // Send HTTP GET request to the manager API to retrieve audit alert rules
        Response<List<AuditAlertRule>> result = HttpUtils.request(
                restTemplate,
                url,
                HttpMethod.GET,
                null,
                null,
                new ParameterizedTypeReference<Response<List<AuditAlertRule>>>() {
                });

        LOGGER.info("success to query audit info for url ={}", url);

        if (result.isSuccess()) {
            // Copy and return the list of audit alert rules
            auditAlertRuleList = result.getData();
            return auditAlertRuleList;
        } else {
            LOGGER.error("fetchAlertPolicies fail: {}", result.getErrMsg());
            return auditAlertRuleList;
        }
    }

    /**
     * Fetches audit data by retrieving audit alert rules and querying detailed audit information for each rule.
     *
     * @return List of {@link String} containing detailed audit information
     * @see #fetchAlertRules()
     */
    public List<String> fetchAuditIds() {
        // Retrieve all audit alert rules from the manager API
        List<AuditAlertRule> auditAlertRules = fetchAlertRules();
        List<String> auditIds = new ArrayList<>();

        // Process each audit alert rule to fetch corresponding auditIds
        for (AuditAlertRule auditAlertRule : auditAlertRules) {
            // Convert comma-separated audit IDs to list and trim whitespace
            List<String> auditIdList = Arrays.stream(auditAlertRule.getAuditId().split(","))
                    .map(String::trim)
                    .collect(Collectors.toList());
            auditIds.addAll(auditIdList);
        }

        // Return empty list if no successful responses were received
        return auditIds;
    }
}