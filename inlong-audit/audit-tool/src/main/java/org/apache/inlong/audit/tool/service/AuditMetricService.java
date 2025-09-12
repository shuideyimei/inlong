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

package org.apache.inlong.audit.tool.service;

import org.apache.inlong.audit.tool.VO.AuditMetricVo;
import org.apache.inlong.audit.tool.basemetric.mapper.AuditMapper;
import org.apache.inlong.audit.tool.basemetric.util.AuditSQLUtil;

import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AuditMetricService {

    private Integer intervalTimeMinute;

    private static final String DATAPROXY_AUDITID = "5";
    private static final DateTimeFormatter LOGTS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Logger LOGGER = LoggerFactory.getLogger(AuditMetricService.class);

    public AuditMetricService() {
        // Retrieve the time interval from the configuration file. If the relevant configuration cannot be found, it
        // defaults to 1 minute
        Properties properties = new Properties();
        try {
            properties.load(getClass().getClassLoader().getResourceAsStream("application.properties"));
            this.intervalTimeMinute = Integer.parseInt(properties.getProperty("audit.data.time.interval.minute", "1"));
        } catch (IOException e) {
            this.intervalTimeMinute = 1;
            LOGGER.error("No time interval related configuration found, default is set to 1 minute");
        }
    }

    /**
     * Query Datapoxy related data and return it
     * @return
     */
    public List<AuditMetricVo> getDataproxyAuditMetrics() {
        // Retrieve the logts field, subtract intervalTimeMinute from the current system time
        String logts = getLogTs();
        SqlSession sqlSession = null;
        List<AuditMetricVo> auditMetricVos = new ArrayList<>();
        try {
            sqlSession = AuditSQLUtil.getSqlSession();
            AuditMapper auditMapper = sqlSession.getMapper(AuditMapper.class);
            auditMetricVos = auditMapper.queryDataproxyAuditMetric(logts, DATAPROXY_AUDITID);
            return auditMetricVos;
        } catch (Exception e) {
            LOGGER.error("An exception occurred during the database query process!" + e.getMessage());
        } finally {
            sqlSession.close();
        }
        return auditMetricVos;
    }

    /**
     * Retrieve Iceberg related data and return it
     * @param auditIds
     * @return
     */
    public List<AuditMetricVo> getIcebergAuditMetrics(List<String> auditIds) {
        String logts = getLogTs();
        SqlSession sqlSession = null;
        List<AuditMetricVo> auditMetricVos = new ArrayList<>();
        try {
            sqlSession = AuditSQLUtil.getSqlSession();
            AuditMapper auditMapper = sqlSession.getMapper(AuditMapper.class);
            // Traverse each auditId, retrieve data from the database, and consolidate it into auditMetricVos
            for (String auditId : auditIds) {
                List<AuditMetricVo> tempList = auditMapper.queryDataproxyAuditMetric(logts, auditId);
                if (tempList != null && tempList.size() > 0) {
                    auditMetricVos.addAll(tempList);
                }
            }
            return auditMetricVos;
        } catch (Exception e) {
            LOGGER.error("An exception occurred during the database query process!" + e.getMessage());
        } finally {
            sqlSession.close();
        }
        return auditMetricVos;
    }

    public List<AuditMetricVo> getHiveAuditMetrics(List<String> auditIds) {
        String logts = getLogTs();
        SqlSession sqlSession = null;
        List<AuditMetricVo> auditMetricVos = new ArrayList<>();
        try {
            sqlSession = AuditSQLUtil.getSqlSession();
            AuditMapper auditMapper = sqlSession.getMapper(AuditMapper.class);
            // Traverse each auditId, retrieve data from the database, and consolidate it into auditMetricVos
            for (String auditId : auditIds) {
                List<AuditMetricVo> tempList = auditMapper.queryDataproxyAuditMetric(logts, auditId);
                if (tempList != null && tempList.size() > 0) {
                    auditMetricVos.addAll(tempList);
                }
            }
            return auditMetricVos;
        } catch (Exception e) {
            LOGGER.error("An exception occurred during the database query process!" + e.getMessage());
        } finally {
            sqlSession.close();
        }
        return auditMetricVos;
    }

    // Get the value of the logts field used for querying
    private String getLogTs() {
        return LocalDateTime.now()
                .withSecond(0)
                .minusMinutes(intervalTimeMinute)
                .format(LOGTS_FMT);
    }
}
