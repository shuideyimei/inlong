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

import org.apache.inlong.audit.tool.DTO.AuditAlertCondition;
import org.apache.inlong.audit.tool.DTO.AuditAlertRule;
import org.apache.inlong.audit.tool.VO.AuditMetricVo;
import org.apache.inlong.audit.tool.evaluator.AlertEvaluator;
import org.apache.inlong.audit.tool.manager.AuditAlertRuleManager;
import org.apache.inlong.audit.tool.metric.AuditMetric;
import org.apache.inlong.audit.tool.reporter.PrometheusReporter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AlertEvaluatorTest {

    private AlertEvaluator alertEvaluator;
    private PrometheusReporter prometheusReporter;
    private AuditAlertRuleManager auditAlertRuleManager;
    private AuditMetric auditMetric;

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    void setUp() {
        prometheusReporter = mock(PrometheusReporter.class);
        auditAlertRuleManager = mock(AuditAlertRuleManager.class);
        auditMetric = mock(AuditMetric.class);

        when(prometheusReporter.getAuditMetric()).thenReturn(auditMetric);

        alertEvaluator = new AlertEvaluator(prometheusReporter, auditAlertRuleManager);

        // Redirect System.out to a ByteArrayOutputStream.
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    void restoreStreams() {
        System.setOut(originalOut);
        outContent.reset();
    }

    @Test
    void testPrintAndReportDataproxyCompareWithStorage_NullMetrics() {
        AuditAlertRule alertRule = createAlertRule(">", 10.0);

        // Test with null dataProxyMetrics
        assertDoesNotThrow(
                () -> alertEvaluator.printAndReportDataproxyCompareWithStorage(null, new ArrayList<>(), alertRule));

        // Test with null storageMetrics
        assertDoesNotThrow(
                () -> alertEvaluator.printAndReportDataproxyCompareWithStorage(new ArrayList<>(), null, alertRule));
    }

    @Test
    void testPrintAndReportDataproxyCompareWithStorage_EmptyMetrics() {
        AuditAlertRule alertRule = createAlertRule(">", 10.0);
        List<AuditMetricVo> dataProxyMetrics = new ArrayList<>();
        List<AuditMetricVo> storageMetrics = new ArrayList<>();

        alertEvaluator.printAndReportDataproxyCompareWithStorage(dataProxyMetrics, storageMetrics, alertRule);

        assertEquals("", outContent.toString());
    }

    @Test
    void testPrintAndReportDataproxyCompareWithStorage_DifferentGroupOrStream() {
        AuditAlertRule alertRule = createAlertRule(">", 10.0);

        List<AuditMetricVo> dataProxyMetrics = new ArrayList<>();
        AuditMetricVo dp = new AuditMetricVo();
        dp.setInlongGroupId("group1");
        dp.setInlongStreamId("stream1");
        dp.setCount(100);
        dataProxyMetrics.add(dp);

        List<AuditMetricVo> storageMetrics = new ArrayList<>();
        AuditMetricVo st = new AuditMetricVo();
        st.setInlongGroupId("group2"); // Different group
        st.setInlongStreamId("stream1");
        st.setCount(50);
        storageMetrics.add(st);

        alertEvaluator.printAndReportDataproxyCompareWithStorage(dataProxyMetrics, storageMetrics, alertRule);

        assertEquals("", outContent.toString());
    }

    @Test
    void testPrintAndReportDataproxyCompareWithStorage_GreaterThanOperator_Triggered() {
        AuditAlertRule alertRule = createAlertRule(">", 10.0);

        List<AuditMetricVo> dataProxyMetrics = new ArrayList<>();
        AuditMetricVo dp = new AuditMetricVo();
        dp.setInlongGroupId("group1");
        dp.setInlongStreamId("stream1");
        dp.setCount(100);
        dataProxyMetrics.add(dp);

        List<AuditMetricVo> storageMetrics = new ArrayList<>();
        AuditMetricVo st = new AuditMetricVo();
        st.setInlongGroupId("group1");
        st.setInlongStreamId("stream1");
        st.setCount(50); // Difference is 50, which is > 10
        storageMetrics.add(st);

        alertEvaluator.printAndReportDataproxyCompareWithStorage(dataProxyMetrics, storageMetrics, alertRule);

        String output = outContent.toString();
        assertTrue(output.contains("[ALERT]"));
        assertTrue(output.contains("groupId=group1"));
        assertTrue(output.contains("streamId=stream1"));
        assertTrue(output.contains("sourceCount=100"));
        assertTrue(output.contains("sinkCount=50"));
        assertTrue(output.contains("diff=50"));
        assertTrue(output.contains(">"));
        assertTrue(output.contains("threshold=10"));

        verify(auditMetric).updateSourcAndSinkAuditDiffMetric(50L);
    }

    @Test
    void testPrintAndReportDataproxyCompareWithStorage_GreaterThanOperator_NotTriggered() {
        AuditAlertRule alertRule = createAlertRule(">", 10.0);

        List<AuditMetricVo> dataProxyMetrics = new ArrayList<>();
        AuditMetricVo dp = new AuditMetricVo();
        dp.setInlongGroupId("group1");
        dp.setInlongStreamId("stream1");
        dp.setCount(100);
        dataProxyMetrics.add(dp);

        List<AuditMetricVo> storageMetrics = new ArrayList<>();
        AuditMetricVo st = new AuditMetricVo();
        st.setInlongGroupId("group1");
        st.setInlongStreamId("stream1");
        st.setCount(95); // Difference is 5, which is not > 10
        storageMetrics.add(st);

        alertEvaluator.printAndReportDataproxyCompareWithStorage(dataProxyMetrics, storageMetrics, alertRule);

        assertEquals("", outContent.toString());
        verify(auditMetric, never()).updateSourcAndSinkAuditDiffMetric(anyLong());
    }

    @Test
    void testPrintAndReportDataproxyCompareWithStorage_GreaterEqualOperator() {
        AuditAlertRule alertRule = createAlertRule(">=", 10.0);

        List<AuditMetricVo> dataProxyMetrics = new ArrayList<>();
        AuditMetricVo dp = new AuditMetricVo();
        dp.setInlongGroupId("group1");
        dp.setInlongStreamId("stream1");
        dp.setCount(100);
        dataProxyMetrics.add(dp);

        List<AuditMetricVo> storageMetrics = new ArrayList<>();
        AuditMetricVo st = new AuditMetricVo();
        st.setInlongGroupId("group1");
        st.setInlongStreamId("stream1");
        st.setCount(90); // Difference is 10, which is >= 10
        storageMetrics.add(st);

        alertEvaluator.printAndReportDataproxyCompareWithStorage(dataProxyMetrics, storageMetrics, alertRule);

        String output = outContent.toString();
        assertTrue(output.contains("[ALERT]"));
        assertTrue(output.contains("diff=10"));
        assertTrue(output.contains(">="));
        assertTrue(output.contains("threshold=10"));

        verify(auditMetric).updateSourcAndSinkAuditDiffMetric(10L);
    }

    @Test
    void testPrintAndReportDataproxyCompareWithStorage_LessThanOperator() {
        AuditAlertRule alertRule = createAlertRule("<", 20.0);

        List<AuditMetricVo> dataProxyMetrics = new ArrayList<>();
        AuditMetricVo dp = new AuditMetricVo();
        dp.setInlongGroupId("group1");
        dp.setInlongStreamId("stream1");
        dp.setCount(100);
        dataProxyMetrics.add(dp);

        List<AuditMetricVo> storageMetrics = new ArrayList<>();
        AuditMetricVo st = new AuditMetricVo();
        st.setInlongGroupId("group1");
        st.setInlongStreamId("stream1");
        st.setCount(95); // Difference is 5, which is < 20
        storageMetrics.add(st);

        alertEvaluator.printAndReportDataproxyCompareWithStorage(dataProxyMetrics, storageMetrics, alertRule);

        String output = outContent.toString();
        assertTrue(output.contains("[ALERT]"));
        assertTrue(output.contains("diff=5"));
        assertTrue(output.contains("<"));
        assertTrue(output.contains("threshold=20"));

        verify(auditMetric).updateSourcAndSinkAuditDiffMetric(5L);
    }

    @Test
    void testPrintAndReportDataproxyCompareWithStorage_LessEqualOperator() {
        AuditAlertRule alertRule = createAlertRule("<=", 5.0);

        List<AuditMetricVo> dataProxyMetrics = new ArrayList<>();
        AuditMetricVo dp = new AuditMetricVo();
        dp.setInlongGroupId("group1");
        dp.setInlongStreamId("stream1");
        dp.setCount(100);
        dataProxyMetrics.add(dp);

        List<AuditMetricVo> storageMetrics = new ArrayList<>();
        AuditMetricVo st = new AuditMetricVo();
        st.setInlongGroupId("group1");
        st.setInlongStreamId("stream1");
        st.setCount(95); // Difference is 5, which is <= 5
        storageMetrics.add(st);

        alertEvaluator.printAndReportDataproxyCompareWithStorage(dataProxyMetrics, storageMetrics, alertRule);

        String output = outContent.toString();
        assertTrue(output.contains("[ALERT]"));
        assertTrue(output.contains("diff=5"));
        assertTrue(output.contains("<="));
        assertTrue(output.contains("threshold=5"));

        verify(auditMetric).updateSourcAndSinkAuditDiffMetric(5L);
    }

    @Test
    void testPrintAndReportDataproxyCompareWithStorage_EqualsOperator() {
        AuditAlertRule alertRule = createAlertRule("==", 5.0);

        List<AuditMetricVo> dataProxyMetrics = new ArrayList<>();
        AuditMetricVo dp = new AuditMetricVo();
        dp.setInlongGroupId("group1");
        dp.setInlongStreamId("stream1");
        dp.setCount(100);
        dataProxyMetrics.add(dp);

        List<AuditMetricVo> storageMetrics = new ArrayList<>();
        AuditMetricVo st = new AuditMetricVo();
        st.setInlongGroupId("group1");
        st.setInlongStreamId("stream1");
        st.setCount(95); // Difference is 5, which is == 5
        storageMetrics.add(st);

        alertEvaluator.printAndReportDataproxyCompareWithStorage(dataProxyMetrics, storageMetrics, alertRule);

        String output = outContent.toString();
        assertTrue(output.contains("[ALERT]"));
        assertTrue(output.contains("diff=5"));
        assertTrue(output.contains("=="));
        assertTrue(output.contains("threshold=5"));

        verify(auditMetric).updateSourcAndSinkAuditDiffMetric(5L);
    }

    @Test
    void testPrintAndReportDataproxyCompareWithStorage_NotEqualsOperator() {
        AuditAlertRule alertRule = createAlertRule("!=", 5.0);

        List<AuditMetricVo> dataProxyMetrics = new ArrayList<>();
        AuditMetricVo dp = new AuditMetricVo();
        dp.setInlongGroupId("group1");
        dp.setInlongStreamId("stream1");
        dp.setCount(100);
        dataProxyMetrics.add(dp);

        List<AuditMetricVo> storageMetrics = new ArrayList<>();
        AuditMetricVo st = new AuditMetricVo();
        st.setInlongGroupId("group1");
        st.setInlongStreamId("stream1");
        st.setCount(95); // Difference is 5, which is == 5, so != 5 is false
        storageMetrics.add(st);

        alertEvaluator.printAndReportDataproxyCompareWithStorage(dataProxyMetrics, storageMetrics, alertRule);

        assertEquals("", outContent.toString());
        verify(auditMetric, never()).updateSourcAndSinkAuditDiffMetric(anyLong());
    }

    @Test
    void testPrintAndReportDataproxyCompareWithStorage_InvalidOperator() {
        AuditAlertRule alertRule = createAlertRule("invalid", 5.0);

        List<AuditMetricVo> dataProxyMetrics = new ArrayList<>();
        AuditMetricVo dp = new AuditMetricVo();
        dp.setInlongGroupId("group1");
        dp.setInlongStreamId("stream1");
        dp.setCount(100);
        dataProxyMetrics.add(dp);

        List<AuditMetricVo> storageMetrics = new ArrayList<>();
        AuditMetricVo st = new AuditMetricVo();
        st.setInlongGroupId("group1");
        st.setInlongStreamId("stream1");
        st.setCount(95); // Difference is 5
        storageMetrics.add(st);

        alertEvaluator.printAndReportDataproxyCompareWithStorage(dataProxyMetrics, storageMetrics, alertRule);

        assertEquals("", outContent.toString());
        verify(auditMetric, never()).updateSourcAndSinkAuditDiffMetric(anyLong());
    }

    @Test
    void testPrintAndReportDataproxyCompareWithStorage_BoundaryCondition_EqualsThreshold() {
        // 测试边界条件：差值正好等于阈值
        AuditAlertRule alertRule = createAlertRule(">=", 10.0);

        List<AuditMetricVo> dataProxyMetrics = new ArrayList<>();
        AuditMetricVo dp = new AuditMetricVo();
        dp.setInlongGroupId("group1");
        dp.setInlongStreamId("stream1");
        dp.setCount(100);
        dataProxyMetrics.add(dp);

        List<AuditMetricVo> storageMetrics = new ArrayList<>();
        AuditMetricVo st = new AuditMetricVo();
        st.setInlongGroupId("group1");
        st.setInlongStreamId("stream1");
        st.setCount(90); // Difference is exactly 10
        storageMetrics.add(st);

        alertEvaluator.printAndReportDataproxyCompareWithStorage(dataProxyMetrics, storageMetrics, alertRule);

        String output = outContent.toString();
        assertTrue(output.contains("[ALERT]"));
        assertTrue(output.contains("diff=10"));
        assertTrue(output.contains(">="));
        assertTrue(output.contains("threshold=10"));

        verify(auditMetric).updateSourcAndSinkAuditDiffMetric(10L);
    }

    @Test
    void testPrintAndReportDataproxyCompareWithStorage_NullGroupId() {
        AuditAlertRule alertRule = createAlertRule(">", 10.0);

        List<AuditMetricVo> dataProxyMetrics = new ArrayList<>();
        AuditMetricVo dp = new AuditMetricVo();
        dp.setInlongGroupId(null);
        dp.setInlongStreamId("stream1");
        dp.setCount(100);
        dataProxyMetrics.add(dp);

        List<AuditMetricVo> storageMetrics = new ArrayList<>();
        AuditMetricVo st = new AuditMetricVo();
        st.setInlongGroupId(null);
        st.setInlongStreamId("stream1");
        st.setCount(50);
        storageMetrics.add(st);

        assertDoesNotThrow(() -> alertEvaluator.printAndReportDataproxyCompareWithStorage(dataProxyMetrics,
                storageMetrics, alertRule));
    }

    @Test
    void testGetAuditAlertRuleManager() {
        assertSame(auditAlertRuleManager, alertEvaluator.getAuditAlertRuleManager());
    }

    private AuditAlertRule createAlertRule(String operator, double value) {
        AuditAlertRule alertRule = new AuditAlertRule();
        AuditAlertCondition condition = new AuditAlertCondition();
        condition.setOperator(operator);
        condition.setValue(value);
        condition.setType("data_loss");
        alertRule.setCondition(condition);
        return alertRule;
    }
}
