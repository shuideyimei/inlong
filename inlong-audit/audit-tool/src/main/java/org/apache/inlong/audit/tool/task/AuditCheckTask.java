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
import org.apache.inlong.audit.tool.DTO.AuditData;
import org.apache.inlong.audit.tool.VO.AuditMetricVo;
import org.apache.inlong.audit.tool.basemetric.BaseMetricReporter;
import org.apache.inlong.audit.tool.config.AppConfig;
import org.apache.inlong.audit.tool.evaluator.AlertEvaluator;
import org.apache.inlong.audit.tool.manager.ManagerClient;
import org.apache.inlong.audit.tool.reporter.OpenTelemetryReporter;
import org.apache.inlong.audit.tool.reporter.PrometheusReporter;
import org.apache.inlong.audit.tool.service.AuditMetricService;

import lombok.Getter;
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
            LOGGER.info("未读取到执行间隔时间相关配置,默认设置为1");
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
        //获取接口提供的auditIds
        List<String> auditIds = managerClient.fetchAuditIds();
        List<String> icbergAuditIds = new ArrayList<>();
        List<String> hiveAuditIds = new ArrayList<>();

        //将auditIds进行归类，分别归类为icberg和dataproxy的auditId
        for (String auditId : auditIds) {
            int auditIdInt = Integer.parseInt(auditId);

            if (auditIdInt == AuditIdEnum.SORT_HIVE_INPUT.getValue() ||
                    auditIdInt == AuditIdEnum.SORT_HIVE_OUTPUT.getValue()) {
                hiveAuditIds.add(auditId);
            } else if (auditIdInt == AuditIdEnum.SORT_ICEBERG_INPUT.getValue() ||
                    auditIdInt == AuditIdEnum.SORT_ICEBERG_OUTPUT.getValue() ||
                    auditIdInt == AuditIdEnum.ICEBERG_AO_INPUT.getValue() ||
                    auditIdInt == AuditIdEnum.ICEBERG_AO_OUTPUT.getValue()) {
                icbergAuditIds.add(auditId);
            }
        }

        //通过auditId去数据库查找相关数据
        List<AuditMetricVo> dataproxyAuditMetrics = auditMetricService.getDataproxyAuditMetrics();
        List<AuditMetricVo> icebergAuditMetrics = auditMetricService.getIcebergAuditMetrics(icbergAuditIds);
        List<AuditMetricVo> hiveAuditMetrics = auditMetricService.getHiveAuditMetrics(hiveAuditIds);

        // 获取告警策略
        List<AuditAlertRule> alertRules = managerClient.fetchAlertRules();

        for (AuditAlertRule alertRule : alertRules) {
            //达到阈值条件时，将告警信息输出到控制台，并且上报到prometheus
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