package org.apache.inlong.audit.tool.basemetric;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Gauge;

public class AuditMetric {
    private final Gauge dataproxyWithIcbergAlert;
    private final Gauge dataproxyWithHiveAlert;

    public AuditMetric(CollectorRegistry registry) {
        this.dataproxyWithIcbergAlert = Gauge.build()
                .name("inlong_dataproxy_icberg_diff")
                .help("dataproxy和icberg中触发告警时的count差值")
                .register(registry);

        this.dataproxyWithHiveAlert = Gauge.build()
                .name("nlong_dataproxy_hive_diff")
                .help("dataproxy和hive中触发告警时的count差值")
                .register(registry);
    }

    public void updateDataproxyWithIcbergAlert(long diff) {
        dataproxyWithIcbergAlert.set(diff);
    }

    public void updateDataproxyWithHiveAlert(long diff) {
        dataproxyWithHiveAlert.set(diff);
    }
}
