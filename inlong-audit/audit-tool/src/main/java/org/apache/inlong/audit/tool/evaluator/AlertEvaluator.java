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

package org.apache.inlong.audit.tool.evaluator;

import org.apache.inlong.audit.tool.DTO.AlertPolicy;
import org.apache.inlong.audit.tool.DTO.AuditAlertRule;
import org.apache.inlong.audit.tool.DTO.AuditData;
import org.apache.inlong.audit.tool.DTO.MetricData;
import  org.apache.inlong.audit.tool.DTO.AuditAlertCondition;
import org.apache.inlong.audit.tool.VO.AuditMetricVo;
import org.apache.inlong.audit.tool.manager.ManagerClient;
import org.apache.inlong.audit.tool.reporter.OpenTelemetryReporter;
import org.apache.inlong.audit.tool.reporter.PrometheusReporter;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AlertEvaluator {

    private final PrometheusReporter prometheusReporter;
    private final OpenTelemetryReporter openTelemetryReporter;
    @Getter
    private final ManagerClient managerClient;
    @Getter
    private AuditData auditData;
    @Getter
    private AlertPolicy alertpolicy;

    public AlertEvaluator(PrometheusReporter prometheusReporter, OpenTelemetryReporter openTelemetryReporter,
            ManagerClient managerClient) {
        this.prometheusReporter = prometheusReporter;
        this.openTelemetryReporter = openTelemetryReporter;
        this.managerClient = managerClient;
    }

    public List<String> getEnabledPlatforms(AuditAlertRule alertRule) {
        List<String> enabledPlatforms = new ArrayList<>();
        //先写死成promethus
        List<String> targets = Collections.singletonList("promethus");
        if (targets != null) {
            for (String target : targets) {
                switch (target.toLowerCase()) {
                    case "prometheus":
                        enabledPlatforms.add("prometheus");
                        break;
                    case "opentelemetry":
                        enabledPlatforms.add("opentelemetry");
                        break;
                    default:
                        System.out.println("Invalid platform");
                        break;
                }
            }
        }
        return enabledPlatforms;
    }

    public boolean shouldTriggerAlert(List<AuditMetricVo> dataProxyMetrics, List<AuditMetricVo> storageMetrics, AuditAlertRule alertRule) {
        return checkDataProxyWithStorage(dataProxyMetrics, storageMetrics, alertRule);
    }

    private boolean checkDataProxyWithStorage(List<AuditMetricVo> dataProxyMetrics, List<AuditMetricVo> storageMetrics, 
                                         AuditAlertRule alertRule) {
        if (dataProxyMetrics != null && storageMetrics != null) {
            AuditAlertCondition condition = alertRule.getCondition();
            double threshold = condition.getValue();
            
            for (AuditMetricVo dataProxyMetric : dataProxyMetrics) {
                for (AuditMetricVo storageMetric : storageMetrics) {
                    if (dataProxyMetric.getInlongGroupId().equals(storageMetric.getInlongGroupId()) &&
                        dataProxyMetric.getInlongStreamId().equals(storageMetric.getInlongStreamId())) {
                        long countDifference = Math.abs(dataProxyMetric.getCount() - storageMetric.getCount());
                        switch (condition.getOperator()) {
                            case ">":
                                return countDifference > threshold;
                            case ">=":
                                return countDifference >= threshold;
                            case "<":
                                return countDifference < threshold;
                            case "<=":
                                return countDifference <= threshold;
                            case "==":
                                return countDifference == threshold;
                            case "!=":
                                return countDifference != threshold;
                            default:
                                return false;
                        }
                    }
                }
            }
        }
        return false;
    }


    public void triggerAlert(AuditMetricVo dataProxyMetric, AuditMetricVo storageMetric, 
                             AuditAlertRule alertRule) {
        List<String> enabledPlatforms = getEnabledPlatforms(alertRule);
        
        // 根据实际数据构造 MetricData
        MetricData metricData = getMetricData(dataProxyMetric, storageMetric, alertRule);

        for (String platform : enabledPlatforms) {
            switch (platform.toLowerCase()) {
                case "prometheus":
                    prometheusReporter.report(metricData);
                    break;
                case "opentelemetry":
                    openTelemetryReporter.report(metricData);
                    break;
                default:
                    System.out.println("Invalid platform: " + platform);
                    break;
            }
        }
    }

    private static MetricData getMetricData(AuditMetricVo dataProxyMetric, AuditMetricVo storageMetric, AuditAlertRule alertRule) {
        long countDifference = Math.abs(dataProxyMetric.getCount() - storageMetric.getCount());
        MetricData metricData = new MetricData(
            dataProxyMetric.getInlongGroupId(),
            dataProxyMetric.getInlongStreamId(),
            0.0, // dataLossRate
            countDifference, // dataLossCount
            Math.min(dataProxyMetric.getCount(), storageMetric.getCount()), // auditCount
            dataProxyMetric.getCount(), // expectedCount
            storageMetric.getCount() // receivedCount
        );

        if (metricData.getAlertInfo() == null) {
            metricData.setAlertInfo(new MetricData.AlertInfo(alertRule.getAlertName()));
        }
        return metricData;
    }

}
