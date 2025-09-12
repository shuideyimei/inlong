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

import org.apache.inlong.audit.tool.util.AuditIdEnum;
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
    private  Integer executionIntervalTime;

    public AuditCheckTask(
                          ManagerClient managerClient, AlertEvaluator alertEvaluator, AppConfig appConfig) {
        this.managerClient = managerClient;
        this.alertEvaluator = alertEvaluator;
        this.auditMetricService = new AuditMetricService();
        try {
            this.executionIntervalTime=Integer.valueOf(appConfig.getProperties().getProperty("audit.data.time.interval.minute"));
        }catch (Exception e){
            LOGGER.info("No configuration related to execution interval time was read, default setting is 1");
            this.executionIntervalTime=1;
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
        //Obtain auditIds provided by the interface
        List<String> auditIds = managerClient.fetchAuditIds();
        List<String> icebergAuditIds = new ArrayList<>();
        List<String> hiveAuditIds = new ArrayList<>();

        //Classify auditIds as iceberg and dataproxy auditIds respectively
        for (String auditId : auditIds) {
            int auditIdInt = Integer.parseInt(auditId);

            if (auditIdInt == AuditIdEnum.SORT_HIVE_INPUT.getValue() ||
                    auditIdInt == AuditIdEnum.SORT_HIVE_OUTPUT.getValue()) {
                hiveAuditIds.add(auditId);
            } else if (auditIdInt == AuditIdEnum.SORT_ICEBERG_INPUT.getValue() ||
                    auditIdInt == AuditIdEnum.SORT_ICEBERG_OUTPUT.getValue() ||
                    auditIdInt == AuditIdEnum.ICEBERG_AO_INPUT.getValue() ||
                    auditIdInt == AuditIdEnum.ICEBERG_AO_OUTPUT.getValue()) {
                icebergAuditIds.add(auditId);
            }
        }

        //Search for relevant data in the database using auditId
        List<AuditMetricVo> dataproxyAuditMetrics = auditMetricService.getDataproxyAuditMetrics();
        List<AuditMetricVo> icebergAuditMetrics = auditMetricService.getIcebergAuditMetrics(icebergAuditIds);
        List<AuditMetricVo> hiveAuditMetrics = auditMetricService.getHiveAuditMetrics(hiveAuditIds);

        // Obtain alarm strategy
        List<AuditAlertRule> alertRules = managerClient.fetchAlertRules();

        for (AuditAlertRule alertRule : alertRules) {
            //When the threshold condition is reached, output the alarm information to the console and report it to Prometheus
            alertEvaluator.printAndReportDataproxyCompareWithStorage(dataproxyAuditMetrics,icebergAuditMetrics,alertRule,"iceberg");
            alertEvaluator.printAndReportDataproxyCompareWithStorage(dataproxyAuditMetrics,hiveAuditMetrics,alertRule,"hive");
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