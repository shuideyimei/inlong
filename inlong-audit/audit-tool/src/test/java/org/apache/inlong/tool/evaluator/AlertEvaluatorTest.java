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

package org.apache.inlong.tool.evaluator;
import org.apache.inlong.audit.tool.evaluator.AlertEvaluator;
import org.apache.inlong.audit.tool.dto.AuditAlertCondition;
import org.apache.inlong.audit.tool.dto.AuditAlertRule;
import org.apache.inlong.audit.tool.entity.AuditMetric;
import org.apache.inlong.audit.tool.manager.AuditAlertRuleManager;
import org.apache.inlong.audit.tool.reporter.PrometheusReporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertEvaluatorTest {

    @Mock
    private PrometheusReporter prometheusReporter;

    @Mock
    private AuditAlertRuleManager auditAlertRuleManager;

    private AlertEvaluator alertEvaluator;

    @lombok.Getter
    @Mock
    private Logger mockLogger;

    @BeforeEach
    void setUp() {
        alertEvaluator = new AlertEvaluator(prometheusReporter, auditAlertRuleManager);
    }

    @Test
    void testEvaluateAndReportAlertWithNullMetrics() {
        // Test with null source metrics
        alertEvaluator.evaluateAndReportAlert(null, Arrays.asList(new AuditMetric()), new AuditAlertRule());

        // Test with null sink metrics
        alertEvaluator.evaluateAndReportAlert(Collections.singletonList(new AuditMetric()), null, new AuditAlertRule());

        // Verify no interaction with prometheus reporter
        verifyNoInteractions(prometheusReporter);
    }

    @Test
    void testEvaluateAndReportAlertWithNonMatchingGroupAndStream() {
        // Setup
        AuditMetric sourceMetric = new AuditMetric();
        sourceMetric.setInlongGroupId("group1");
        sourceMetric.setInlongStreamId("stream1");
        sourceMetric.setCount(100L);

        AuditMetric sinkMetric = new AuditMetric();
        sinkMetric.setInlongGroupId("group2");  // Different group
        sinkMetric.setInlongStreamId("stream1");
        sinkMetric.setCount(90L);

        AuditAlertRule alertRule = new AuditAlertRule();
        alertRule.setInlongGroupId("group1");
        alertRule.setInlongStreamId("stream1");

        AuditAlertCondition condition = new AuditAlertCondition();
        condition.setOperator(">");
        condition.setValue(0.1);
        alertRule.setCondition(condition);

        // Execute
        alertEvaluator.evaluateAndReportAlert(
                Collections.singletonList(sourceMetric),
                Collections.singletonList(sinkMetric),
                alertRule
        );

        // Verify no alert was triggered
        verifyNoInteractions(prometheusReporter);
    }

    @Test
    void testEvaluateAndReportAlertWithZeroSourceCount() {
        // Setup
        AuditMetric sourceMetric = new AuditMetric();
        sourceMetric.setInlongGroupId("group1");
        sourceMetric.setInlongStreamId("stream1");
        sourceMetric.setCount(0L);  // Zero count

        AuditMetric sinkMetric = new AuditMetric();
        sinkMetric.setInlongGroupId("group1");
        sinkMetric.setInlongStreamId("stream1");
        sinkMetric.setCount(90L);

        AuditAlertRule alertRule = new AuditAlertRule();
        alertRule.setInlongGroupId("group1");
        alertRule.setInlongStreamId("stream1");

        AuditAlertCondition condition = new AuditAlertCondition();
        condition.setOperator(">");
        condition.setValue(0.1);
        alertRule.setCondition(condition);

        // Execute
        alertEvaluator.evaluateAndReportAlert(
                Collections.singletonList(sourceMetric),
                Collections.singletonList(sinkMetric),
                alertRule
        );

        // Verify no alert was triggered
        verifyNoInteractions(prometheusReporter);
    }

    @Test
    void testEvaluateAndReportAlertWithGreaterThanCondition() {
        // Setup - diff = (90-100)/100 = -0.1 which is not > 0.1
        AuditMetric sourceMetric = new AuditMetric();
        sourceMetric.setInlongGroupId("group1");
        sourceMetric.setInlongStreamId("stream1");
        sourceMetric.setCount(100L);

        AuditMetric sinkMetric = new AuditMetric();
        sinkMetric.setInlongGroupId("group1");
        sinkMetric.setInlongStreamId("stream1");
        sinkMetric.setCount(90L);

        AuditAlertRule alertRule = new AuditAlertRule();
        alertRule.setInlongGroupId("group1");
        alertRule.setInlongStreamId("stream1");

        AuditAlertCondition condition = new AuditAlertCondition();
        condition.setOperator(">");
        condition.setValue(0.1);
        alertRule.setCondition(condition);

        // Execute
        alertEvaluator.evaluateAndReportAlert(
                Collections.singletonList(sourceMetric),
                Collections.singletonList(sinkMetric),
                alertRule
        );

        // Verify no alert was triggered
        verifyNoInteractions(prometheusReporter);

        // Setup - diff = (120-100)/100 = 0.2 which is > 0.1
        AuditMetric sinkMetric2 = new AuditMetric();
        sinkMetric2.setInlongGroupId("group1");
        sinkMetric2.setInlongStreamId("stream1");
        sinkMetric2.setCount(120L);

        // Execute
        alertEvaluator.evaluateAndReportAlert(
                Collections.singletonList(sourceMetric),
                Collections.singletonList(sinkMetric2),
                alertRule
        );

        // Verify alert was triggered
        verify(prometheusReporter, times(1)).getAuditMetric();
    }

    @Test
    void testEvaluateAndReportAlertWithGreaterThanOrEqualCondition() {
        // Setup
        AuditMetric sourceMetric = new AuditMetric();
        sourceMetric.setInlongGroupId("group1");
        sourceMetric.setInlongStreamId("stream1");
        sourceMetric.setCount(100L);

        AuditMetric sinkMetric = new AuditMetric();
        sinkMetric.setInlongGroupId("group1");
        sinkMetric.setInlongStreamId("stream1");
        sinkMetric.setCount(110L);  // diff = 0.1

        AuditAlertRule alertRule = new AuditAlertRule();
        alertRule.setInlongGroupId("group1");
        alertRule.setInlongStreamId("stream1");

        AuditAlertCondition condition = new AuditAlertCondition();
        condition.setOperator(">=");
        condition.setValue(0.1);
        alertRule.setCondition(condition);

        // Execute
        alertEvaluator.evaluateAndReportAlert(
                Collections.singletonList(sourceMetric),
                Collections.singletonList(sinkMetric),
                alertRule
        );

        // Verify alert was triggered
        verify(prometheusReporter, times(1)).getAuditMetric();
    }

    @Test
    void testEvaluateAndReportAlertWithLessThanCondition() {
        // Setup
        AuditMetric sourceMetric = new AuditMetric();
        sourceMetric.setInlongGroupId("group1");
        sourceMetric.setInlongStreamId("stream1");
        sourceMetric.setCount(100L);

        AuditMetric sinkMetric = new AuditMetric();
        sinkMetric.setInlongGroupId("group1");
        sinkMetric.setInlongStreamId("stream1");
        sinkMetric.setCount(90L);  // diff = -0.1

        AuditAlertRule alertRule = new AuditAlertRule();
        alertRule.setInlongGroupId("group1");
        alertRule.setInlongStreamId("stream1");

        AuditAlertCondition condition = new AuditAlertCondition();
        condition.setOperator("<");
        condition.setValue(-0.05);  // -0.1 < -0.05
        alertRule.setCondition(condition);

        // Execute
        alertEvaluator.evaluateAndReportAlert(
                Collections.singletonList(sourceMetric),
                Collections.singletonList(sinkMetric),
                alertRule
        );

        // Verify alert was triggered
        verify(prometheusReporter, times(1)).getAuditMetric();
    }

    @Test
    void testEvaluateAndReportAlertWithLessThanOrEqualCondition() {
        // Setup
        AuditMetric sourceMetric = new AuditMetric();
        sourceMetric.setInlongGroupId("group1");
        sourceMetric.setInlongStreamId("stream1");
        sourceMetric.setCount(100L);

        AuditMetric sinkMetric = new AuditMetric();
        sinkMetric.setInlongGroupId("group1");
        sinkMetric.setInlongStreamId("stream1");
        sinkMetric.setCount(90L);  // diff = -0.1

        AuditAlertRule alertRule = new AuditAlertRule();
        alertRule.setInlongGroupId("group1");
        alertRule.setInlongStreamId("stream1");

        AuditAlertCondition condition = new AuditAlertCondition();
        condition.setOperator("<=");
        condition.setValue(-0.1);  // -0.1 <= -0.1
        alertRule.setCondition(condition);

        // Execute
        alertEvaluator.evaluateAndReportAlert(
                Arrays.asList(sourceMetric),
                Arrays.asList(sinkMetric),
                alertRule
        );

        // Verify alert was triggered
        verify(prometheusReporter, times(1)).getAuditMetric();
    }

    @Test
    void testEvaluateAndReportAlertWithEqualCondition() {
        // Setup
        AuditMetric sourceMetric = new AuditMetric();
        sourceMetric.setInlongGroupId("group1");
        sourceMetric.setInlongStreamId("stream1");
        sourceMetric.setCount(100L);

        AuditMetric sinkMetric = new AuditMetric();
        sinkMetric.setInlongGroupId("group1");
        sinkMetric.setInlongStreamId("stream1");
        sinkMetric.setCount(110L);  // diff = 0.1

        AuditAlertRule alertRule = new AuditAlertRule();
        alertRule.setInlongGroupId("group1");
        alertRule.setInlongStreamId("stream1");

        AuditAlertCondition condition = new AuditAlertCondition();
        condition.setOperator("==");
        condition.setValue(0.1);
        alertRule.setCondition(condition);

        // Execute
        alertEvaluator.evaluateAndReportAlert(
                Collections.singletonList(sourceMetric),
                Collections.singletonList(sinkMetric),
                alertRule
        );

        // Verify alert was triggered
        verify(prometheusReporter, times(1)).getAuditMetric();
    }

    @Test
    void testEvaluateAndReportAlertWithNotEqualCondition() {
        // Setup
        AuditMetric sourceMetric = new AuditMetric();
        sourceMetric.setInlongGroupId("group1");
        sourceMetric.setInlongStreamId("stream1");
        sourceMetric.setCount(100L);

        AuditMetric sinkMetric = new AuditMetric();
        sinkMetric.setInlongGroupId("group1");
        sinkMetric.setInlongStreamId("stream1");
        sinkMetric.setCount(110L);  // diff = 0.1

        AuditAlertRule alertRule = new AuditAlertRule();
        alertRule.setInlongGroupId("group1");
        alertRule.setInlongStreamId("stream1");

        AuditAlertCondition condition = new AuditAlertCondition();
        condition.setOperator("!=");
        condition.setValue(0.2);  // 0.1 != 0.2
        alertRule.setCondition(condition);

        // Execute
        alertEvaluator.evaluateAndReportAlert(
                Collections.singletonList(sourceMetric),
                Collections.singletonList(sinkMetric),
                alertRule
        );

        // Verify alert was triggered
        verify(prometheusReporter, times(1)).getAuditMetric();
    }

    @Test
    void testEvaluateAndReportAlertWithUnknownOperator() {
        // Setup
        AuditMetric sourceMetric = new AuditMetric();
        sourceMetric.setInlongGroupId("group1");
        sourceMetric.setInlongStreamId("stream1");
        sourceMetric.setCount(100L);

        AuditMetric sinkMetric = new AuditMetric();
        sinkMetric.setInlongGroupId("group1");
        sinkMetric.setInlongStreamId("stream1");
        sinkMetric.setCount(110L);

        AuditAlertRule alertRule = new AuditAlertRule();
        alertRule.setInlongGroupId("group1");
        alertRule.setInlongStreamId("stream1");

        AuditAlertCondition condition = new AuditAlertCondition();
        condition.setOperator("unknown");  // Unknown operator
        condition.setValue(0.1);
        alertRule.setCondition(condition);

        // Execute
        alertEvaluator.evaluateAndReportAlert(
                Collections.singletonList(sourceMetric),
                Collections.singletonList(sinkMetric),
                alertRule
        );

        // Verify no alert was triggered for unknown operator
        verifyNoInteractions(prometheusReporter);
    }

    @Test
    void testGetAuditAlertRuleManager() {
        // Test the getter method
        assertEquals(auditAlertRuleManager, alertEvaluator.getAuditAlertRuleManager());
    }

}