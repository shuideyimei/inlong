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

import lombok.Getter;

import java.util.List;

public class AlertEvaluator {

    private final PrometheusReporter prometheusReporter;
    @Getter
    private final ManagerClient managerClient;
    public AlertEvaluator(PrometheusReporter prometheusReporter,
            ManagerClient managerClient) {
        this.prometheusReporter = prometheusReporter;
        this.managerClient = managerClient;
    }
    public void printAndReportDataproxyCompareWithStorage(List<AuditMetricVo> dataProxyMetrics,
            List<AuditMetricVo> storageMetrics,
            AuditAlertRule alertRule,
            String storageName) {
        if (dataProxyMetrics == null || storageMetrics == null) {
            return;
        }

        AuditAlertCondition condition = alertRule.getCondition();
        double threshold = condition.getValue();
        String op = condition.getOperator();

        for (AuditMetricVo dp : dataProxyMetrics) {
            for (AuditMetricVo st : storageMetrics) {
                if (!dp.getInlongGroupId().equals(st.getInlongGroupId()) ||
                        !dp.getInlongStreamId().equals(st.getInlongStreamId())) {
                    continue;
                }

                long diff = Math.abs(dp.getCount() - st.getCount());
                boolean hit = false;

                switch (op) {
                    case ">":
                        hit = diff > threshold;
                        break;
                    case ">=":
                        hit = diff >= threshold;
                        break;
                    case "<":
                        hit = diff < threshold;
                        break;
                    case "<=":
                        hit = diff <= threshold;
                        break;
                    case "==":
                        hit = diff == threshold;
                        break;
                    case "!=":
                        hit = diff != threshold;
                        break;
                    default:
                        hit = false;
                }

                if (hit) {
                    System.out.printf(
                            "[ALERT] groupId=%s, streamId=%s | dataproxy=%d, %s=%d | diff=%d  %s threshold=%.0f%n",
                            dp.getInlongGroupId(), dp.getInlongStreamId(),
                            dp.getCount(), storageName, st.getCount(), diff, op, threshold);
                    switch (storageName) {
                        case "iceberg":
                            prometheusReporter.getAuditMetric().updateDataproxyWithIcbergAlert(diff);
                            break;
                        case "hive":
                            prometheusReporter.getAuditMetric().updateDataproxyWithHiveAlert(diff);
                            break;
                        default:
                            System.out.println("[ALERT] Unknown storage name: " + storageName);
                            break;
                    }
                }
            }
        }
    }

}