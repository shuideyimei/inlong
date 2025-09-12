package org.apache.inlong.evaluator;

import org.apache.inlong.audit.tool.DTO.AuditAlertCondition;
import org.apache.inlong.audit.tool.DTO.AuditAlertRule;
import org.apache.inlong.audit.tool.VO.AuditMetricVo;
import org.apache.inlong.audit.tool.evaluator.AlertEvaluator;
import org.apache.inlong.audit.tool.manager.ManagerClient;
import org.apache.inlong.audit.tool.reporter.PrometheusReporter;
import org.apache.inlong.audit.tool.basemetric.AuditMetric;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class AlertEvaluatorTest {

    @Mock
    private PrometheusReporter prometheusReporter;

    @Mock
    private ManagerClient managerClient;

    @Mock
    private AuditMetric auditMetric;

    private AlertEvaluator alertEvaluator;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // Mock the PrometheusReporter to return an AuditMetric
        when(prometheusReporter.getAuditMetric()).thenReturn(auditMetric);

        // Create the AlertEvaluator instance
        alertEvaluator = new AlertEvaluator(prometheusReporter, managerClient);
    }

    @Test
    public void testConstructor() {
        assertNotNull(alertEvaluator);
        assertEquals(managerClient, alertEvaluator.getManagerClient());
    }

    @Test
    public void testEvaluateAndReportWithNullParameters() {
        // Test with null parameters
        alertEvaluator.evaluateAndReport(null, null, null);
        // Should not throw any exception

        // Test with null dataproxy metrics
        alertEvaluator.evaluateAndReport(null, new ArrayList<>(), new AuditAlertRule());
        // Should not throw any exception

        // Test with null storage metrics
        alertEvaluator.evaluateAndReport(new ArrayList<>(), null, new AuditAlertRule());
        // Should not throw any exception

        // Test with null alert rule
        alertEvaluator.evaluateAndReport(new ArrayList<>(), new ArrayList<>(), null);
        // Should not throw any exception
    }

    @Test
    public void testEvaluateAndReportWithNullAlertCondition() {
        // Test with null alert condition
        AuditAlertRule alertRule = new AuditAlertRule();
        alertRule.setCondition(null);

        alertEvaluator.evaluateAndReport(new ArrayList<>(), new ArrayList<>(), alertRule);
        // Should not throw any exception
    }

    @Test
    public void testEvaluateAndReportWithInvalidConditionValues() {
        // Test with null threshold
        AuditAlertCondition condition = new AuditAlertCondition();
        condition.setValue(null);
        condition.setOperator(">");

        AuditAlertRule alertRule = new AuditAlertRule();
        alertRule.setCondition(condition);

        alertEvaluator.evaluateAndReport(new ArrayList<>(), new ArrayList<>(), alertRule);
        // Should not throw any exception

        // Test with null operator
        condition.setValue(10.0);
        condition.setOperator(null);

        alertEvaluator.evaluateAndReport(new ArrayList<>(), new ArrayList<>(), alertRule);
        // Should not throw any exception
    }

    @Test
    public void testEvaluateAndReportWithNullStorageName() {
        // Mock managerClient to return null storage type
        when(managerClient.fetchStorageType()).thenReturn(null);

        AuditAlertCondition condition = new AuditAlertCondition();
        condition.setValue(10.0);
        condition.setOperator(">");

        AuditAlertRule alertRule = new AuditAlertRule();
        alertRule.setCondition(condition);

        alertEvaluator.evaluateAndReport(new ArrayList<>(), new ArrayList<>(), alertRule);
        // Should not throw any exception
        verify(managerClient, times(1)).fetchStorageType();
    }

    @Test
    public void testEvaluateAndReportWithEmptyMetrics() {
        // Mock managerClient to return a valid storage type
        when(managerClient.fetchStorageType()).thenReturn("iceberg");

        AuditAlertCondition condition = new AuditAlertCondition();
        condition.setValue(10.0);
        condition.setOperator(">");

        AuditAlertRule alertRule = new AuditAlertRule();
        alertRule.setCondition(condition);

        alertEvaluator.evaluateAndReport(new ArrayList<>(), new ArrayList<>(), alertRule);
        // Should not throw any exception
        verify(managerClient, times(1)).fetchStorageType();
    }

    @Test
    public void testEvaluateAndReportWithValidDataNoAlert() {
        // Mock managerClient to return a valid storage type
        when(managerClient.fetchStorageType()).thenReturn("iceberg");

        // Create test data with no alert condition match
        AuditMetricVo dataproxyMetric = new AuditMetricVo();
        dataproxyMetric.setInlongGroupId("group1");
        dataproxyMetric.setInlongStreamId("stream1");
        dataproxyMetric.setCount(100);

        AuditMetricVo storageMetric = new AuditMetricVo();
        storageMetric.setInlongGroupId("group1");
        storageMetric.setInlongStreamId("stream1");
        storageMetric.setCount(95); // Difference of 5, which is less than threshold 10

        List<AuditMetricVo> dataproxyMetrics = new ArrayList<>();
        dataproxyMetrics.add(dataproxyMetric);

        List<AuditMetricVo> storageMetrics = new ArrayList<>();
        storageMetrics.add(storageMetric);

        AuditAlertCondition condition = new AuditAlertCondition();
        condition.setValue(10.0);
        condition.setOperator(">");

        AuditAlertRule alertRule = new AuditAlertRule();
        alertRule.setCondition(condition);

        alertEvaluator.evaluateAndReport(dataproxyMetrics, storageMetrics, alertRule);

        // No alert should be triggered, so no metrics should be updated
        verify(auditMetric, never()).updateDataproxyWithIcbergAlert(anyLong());
        verify(auditMetric, never()).updateDataproxyWithHiveAlert(anyLong());
    }

    @Test
    public void testEvaluateAndReportWithValidDataAlertTriggeredIceberg() {
        // Mock managerClient to return iceberg storage type
        when(managerClient.fetchStorageType()).thenReturn("iceberg");

        // Create test data with alert condition match
        AuditMetricVo dataproxyMetric = new AuditMetricVo();
        dataproxyMetric.setInlongGroupId("group1");
        dataproxyMetric.setInlongStreamId("stream1");
        dataproxyMetric.setCount(100);

        AuditMetricVo storageMetric = new AuditMetricVo();
        storageMetric.setInlongGroupId("group1");
        storageMetric.setInlongStreamId("stream1");
        storageMetric.setCount(80); // Difference of 20, which is greater than threshold 10

        List<AuditMetricVo> dataproxyMetrics = new ArrayList<>();
        dataproxyMetrics.add(dataproxyMetric);

        List<AuditMetricVo> storageMetrics = new ArrayList<>();
        storageMetrics.add(storageMetric);

        AuditAlertCondition condition = new AuditAlertCondition();
        condition.setValue(10.0);
        condition.setOperator(">");

        AuditAlertRule alertRule = new AuditAlertRule();
        alertRule.setCondition(condition);

        alertEvaluator.evaluateAndReport(dataproxyMetrics, storageMetrics, alertRule);

        // Alert should be triggered for iceberg
        verify(auditMetric, times(1)).updateDataproxyWithIcbergAlert(20L);
        verify(auditMetric, never()).updateDataproxyWithHiveAlert(anyLong());
    }

    @Test
    public void testEvaluateAndReportWithValidDataAlertTriggeredHive() {
        // Mock managerClient to return hive storage type
        when(managerClient.fetchStorageType()).thenReturn("hive");

        // Create test data with alert condition match
        AuditMetricVo dataproxyMetric = new AuditMetricVo();
        dataproxyMetric.setInlongGroupId("group1");
        dataproxyMetric.setInlongStreamId("stream1");
        dataproxyMetric.setCount(100);

        AuditMetricVo storageMetric = new AuditMetricVo();
        storageMetric.setInlongGroupId("group1");
        storageMetric.setInlongStreamId("stream1");
        storageMetric.setCount(80); // Difference of 20, which is greater than threshold 10

        List<AuditMetricVo> dataproxyMetrics = new ArrayList<>();
        dataproxyMetrics.add(dataproxyMetric);

        List<AuditMetricVo> storageMetrics = new ArrayList<>();
        storageMetrics.add(storageMetric);

        AuditAlertCondition condition = new AuditAlertCondition();
        condition.setValue(10.0);
        condition.setOperator(">");

        AuditAlertRule alertRule = new AuditAlertRule();
        alertRule.setCondition(condition);

        alertEvaluator.evaluateAndReport(dataproxyMetrics, storageMetrics, alertRule);

        // Alert should be triggered for hive
        verify(auditMetric, times(1)).updateDataproxyWithHiveAlert(20L);
        verify(auditMetric, never()).updateDataproxyWithIcbergAlert(anyLong());
    }

    @Test
    public void testEvaluateAndReportWithUnknownStorageType() {
        // Mock managerClient to return unknown storage type
        when(managerClient.fetchStorageType()).thenReturn("unknown");

        // Create test data with alert condition match
        AuditMetricVo dataproxyMetric = new AuditMetricVo();
        dataproxyMetric.setInlongGroupId("group1");
        dataproxyMetric.setInlongStreamId("stream1");
        dataproxyMetric.setCount(100);

        AuditMetricVo storageMetric = new AuditMetricVo();
        storageMetric.setInlongGroupId("group1");
        storageMetric.setInlongStreamId("stream1");
        storageMetric.setCount(80); // Difference of 20, which is greater than threshold 10

        List<AuditMetricVo> dataproxyMetrics = new ArrayList<>();
        dataproxyMetrics.add(dataproxyMetric);

        List<AuditMetricVo> storageMetrics = new ArrayList<>();
        storageMetrics.add(storageMetric);

        AuditAlertCondition condition = new AuditAlertCondition();
        condition.setValue(10.0);
        condition.setOperator(">");

        AuditAlertRule alertRule = new AuditAlertRule();
        alertRule.setCondition(condition);

        alertEvaluator.evaluateAndReport(dataproxyMetrics, storageMetrics, alertRule);

        // Alert should be triggered but no specific metric updated for unknown storage
        verify(auditMetric, never()).updateDataproxyWithHiveAlert(anyLong());
        verify(auditMetric, never()).updateDataproxyWithIcbergAlert(anyLong());
    }

    @Test
    public void testEvaluateConditionWithDifferentOperators() {
        // Test > operator
        assertTrue(invokeEvaluateCondition(10, ">", 5));
        assertFalse(invokeEvaluateCondition(5, ">", 10));

        // Test >= operator
        assertTrue(invokeEvaluateCondition(10, ">=", 10));
        assertTrue(invokeEvaluateCondition(10, ">=", 5));
        assertFalse(invokeEvaluateCondition(5, ">=", 10));

        // Test < operator
        assertTrue(invokeEvaluateCondition(5, "<", 10));
        assertFalse(invokeEvaluateCondition(10, "<", 5));

        // Test <= operator
        assertTrue(invokeEvaluateCondition(5, "<=", 5));
        assertTrue(invokeEvaluateCondition(5, "<=", 10));
        assertFalse(invokeEvaluateCondition(10, "<=", 5));

        // Test == operator
        assertTrue(invokeEvaluateCondition(10, "==", 10));
        assertFalse(invokeEvaluateCondition(10, "==", 5));

        // Test != operator
        assertTrue(invokeEvaluateCondition(10, "!=", 5));
        assertFalse(invokeEvaluateCondition(10, "!=", 10));

        // Test unsupported operator
        assertFalse(invokeEvaluateCondition(10, "unsupported", 5));
    }

    // Helper method to test private evaluateCondition method
    private boolean invokeEvaluateCondition(long diff, String op, double threshold) {
        // We can test this indirectly by setting up conditions that will trigger it
        // and checking if alerts are triggered appropriately

        // Mock managerClient to return iceberg storage type
        when(managerClient.fetchStorageType()).thenReturn("iceberg");

        // Create test data
        AuditMetricVo dataproxyMetric = new AuditMetricVo();
        dataproxyMetric.setInlongGroupId("group1");
        dataproxyMetric.setInlongStreamId("stream1");
        dataproxyMetric.setCount(100);

        AuditMetricVo storageMetric = new AuditMetricVo();
        storageMetric.setInlongGroupId("group1");
        storageMetric.setInlongStreamId("stream1");
        storageMetric.setCount(100 - diff); // Set count to create desired difference

        List<AuditMetricVo> dataproxyMetrics = new ArrayList<>();
        dataproxyMetrics.add(dataproxyMetric);

        List<AuditMetricVo> storageMetrics = new ArrayList<>();
        storageMetrics.add(storageMetric);

        AuditAlertCondition condition = new AuditAlertCondition();
        condition.setValue(threshold);
        condition.setOperator(op);

        AuditAlertRule alertRule = new AuditAlertRule();
        alertRule.setCondition(condition);

        alertEvaluator.evaluateAndReport(dataproxyMetrics, storageMetrics, alertRule);

        // Reset mocks to check if alert was triggered
        reset(auditMetric);

        // Run again with known values to check if alert is triggered
        alertEvaluator.evaluateAndReport(dataproxyMetrics, storageMetrics, alertRule);

        try {
            // Try to verify if updateDataproxyWithIcbergAlert was called
            verify(auditMetric, times(1)).updateDataproxyWithIcbergAlert(diff);
            return true;
        } catch (AssertionError e) {
            try {
                verify(auditMetric, never()).updateDataproxyWithIcbergAlert(anyLong());
                return false;
            } catch (AssertionError e2) {
                // If neither verification works, return false
                return false;
            }
        }
    }
}
