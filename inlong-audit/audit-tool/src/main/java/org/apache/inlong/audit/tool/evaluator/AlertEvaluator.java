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

import org.apache.inlong.audit.tool.DTO.AuditAlertCondition;
import org.apache.inlong.audit.tool.DTO.AuditAlertRule;
import org.apache.inlong.audit.tool.VO.AuditMetricVo;
import org.apache.inlong.audit.tool.manager.ManagerClient;
import org.apache.inlong.audit.tool.reporter.PrometheusReporter;
import org.apache.inlong.audit.tool.config.ConfigConstants;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class AlertEvaluator {


    private static final Logger LOGGER = LoggerFactory.getLogger(AlertEvaluator.class);


    private final PrometheusReporter prometheusReporter;
    @Getter
    private final ManagerClient managerClient;

    public AlertEvaluator(PrometheusReporter prometheusReporter, ManagerClient managerClient) {
        this.prometheusReporter = prometheusReporter;
        this.managerClient = managerClient;
    }

    public void evaluateAndReport(List<AuditMetricVo> dataproxyAuditMetrics, List<AuditMetricVo> storageAuditMetrics,
                                  AuditAlertRule alertRule) {
        if (dataproxyAuditMetrics == null || storageAuditMetrics == null || alertRule == null) {
            return;
        }

        AuditAlertCondition condition = alertRule.getCondition();
        if (condition == null) {
            LOGGER.warn("Alert condition is null");
            return;
        }

        Double thresholdObj = condition.getValue();
        String op = condition.getOperator();
        if (thresholdObj == null || op == null) {
            LOGGER.warn("Invalid threshold or operator: threshold={}, operator={}", thresholdObj, op);
            return;
        }

        double threshold = thresholdObj;

        String storageName = managerClient.fetchStorageType();
        if (storageName == null) {
            LOGGER.warn("Storage name is null");
            return;
        }

        // 构建 storageAuditMetrics 的索引 map 提高查找效率
        Map<String, AuditMetricVo> storageMap = storageAuditMetrics.stream()
                .filter(Objects::nonNull)
                .filter(st -> st.getInlongGroupId() != null && st.getInlongStreamId() != null)
                .collect(Collectors.toMap(
                        st -> st.getInlongGroupId() + "#" + st.getInlongStreamId(),
                        st -> st,
                        (existing, replacement) -> existing
                ));

        for (AuditMetricVo dp : dataproxyAuditMetrics) {
            if (dp == null || dp.getInlongGroupId() == null || dp.getInlongStreamId() == null) {
                continue;
            }

            String key = dp.getInlongGroupId() + "#" + dp.getInlongStreamId();
            AuditMetricVo st = storageMap.get(key);
            if (st == null) {
                continue;
            }

            long diff = Math.abs(dp.getCount() - st.getCount());
            boolean hit = evaluateCondition(diff, op, threshold);

            if (hit) {
                LOGGER.info("[ALERT] groupId={}, streamId={} | dataproxy={}, {}={} | diff={} {} threshold={}",
                        dp.getInlongGroupId(), dp.getInlongStreamId(),
                        dp.getCount(), storageName, st.getCount(), diff, op, threshold);

                switch (storageName) {
                    case ConfigConstants.STORAGE_ICEBERG:
                        prometheusReporter.getAuditMetric().updateDataproxyWithIcbergAlert(diff);
                        break;
                    case ConfigConstants.STORAGE_HIVE:
                        prometheusReporter.getAuditMetric().updateDataproxyWithHiveAlert(diff);
                        break;
                    default:
                        LOGGER.warn("[ALERT] Unknown storage name: {}", storageName);
                        break;
                }
            }
        }
    }

    private boolean evaluateCondition(long diff, String op, double threshold) {
        switch (op) {
            case ">":
                return diff > threshold;
            case ">=":
                return diff >= threshold;
            case "<":
                return diff < threshold;
            case "<=":
                return diff <= threshold;
            case "==":
                return diff == threshold;
            case "!=":
                return diff != threshold;
            default:
                LOGGER.warn("Unsupported operator: {}", op);
                return false;
        }
    }
}
