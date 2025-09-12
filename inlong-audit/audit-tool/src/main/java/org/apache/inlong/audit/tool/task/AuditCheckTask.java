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

package org.apache.inlong.audit.tool.task;

import org.apache.inlong.audit.tool.DTO.AuditAlertRule;
import org.apache.inlong.audit.tool.VO.AuditMetricVo;
import org.apache.inlong.audit.tool.config.AppConfig;
import org.apache.inlong.audit.tool.evaluator.AlertEvaluator;
import org.apache.inlong.audit.tool.manager.ManagerClient;
import org.apache.inlong.audit.tool.service.AuditMetricService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * AuditCheckTask class: Periodically fetches audit data and evaluates alert policies.
 */
public class AuditCheckTask {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final AlertEvaluator alertEvaluator;
    private final ManagerClient managerClient;
    private static final Logger LOGGER = LoggerFactory.getLogger(ManagerClient.class);
    private final AuditMetricService auditMetricService;
    private Integer executionIntervalTime;

public AuditCheckTask(
        ManagerClient managerClient, AlertEvaluator alertEvaluator, AppConfig appConfig) {
    this.managerClient = managerClient;
    this.alertEvaluator = alertEvaluator;
    this.auditMetricService = new AuditMetricService();
    try {
        if (appConfig != null && appConfig.getProperties() != null) {
            String intervalStr = appConfig.getProperties().getProperty("audit.data.time.interval.minute");
            if (intervalStr != null && !intervalStr.trim().isEmpty()) {
                this.executionIntervalTime = Integer.valueOf(intervalStr.trim());
            } else {
                LOGGER.warn("Configuration property 'audit.data.time.interval.minute' is missing or empty, using default value: 1");
                this.executionIntervalTime = 1;
            }
        } else {
            LOGGER.warn("AppConfig or its properties is null, using default execution interval time: 1");
            this.executionIntervalTime = 1;
        }
    } catch (NumberFormatException e) {
        LOGGER.error("Failed to parse execution interval time from configuration, using default value: 1", e);
        this.executionIntervalTime = 1;
    }
}


    /**
     * Initiate the audit inspection task
     */
    public void start() {
        scheduler.scheduleAtFixedRate(this::checkAuditData, 0, executionIntervalTime, TimeUnit.MINUTES);
    }

    /**
     * Check audit data and trigger alert evaluation.
     */
    private void checkAuditData() {
        String auditId;
        List<AuditMetricVo> dataproxyAuditMetrics;
        List<AuditMetricVo> storageAuditMetrics;
        List<AuditAlertRule> alertRules;

        try {
            // Obtain auditIds provided by the interface
            auditId = managerClient.fetchAuditId();
            if (auditId == null) {
                System.err.println("Failed to fetch auditId from managerClient");
                return;
            }

            // Search for relevant data in the database using auditId
            dataproxyAuditMetrics = auditMetricService.getDataproxyAuditMetrics();
            if (dataproxyAuditMetrics == null) {
                dataproxyAuditMetrics = new ArrayList<>();
            }

            // Get storage type mappings from configuration
            storageAuditMetrics = auditMetricService.getStorageAuditMetrics(auditId);
            if (storageAuditMetrics == null) {
                storageAuditMetrics = new ArrayList<>();
            }

            // Obtain alarm strategy
            alertRules = managerClient.fetchAlertRules();
            if (alertRules == null) {
                alertRules = new ArrayList<>();
            }

            for (AuditAlertRule alertRule : alertRules) {
                // When the threshold condition is reached, output the alarm information to the console and report it to
                alertEvaluator.evaluateAndReport(dataproxyAuditMetrics, storageAuditMetrics, alertRule);
            }
        } catch (Exception e) {
            System.err.println("Error occurred during audit data checking: " + e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * Stop the audit inspection task
     */
    public void stop() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

}