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
import org.apache.inlong.audit.tool.basemetric.util.AuditSQLUtil;
import org.apache.inlong.audit.tool.config.AppConfig;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class AuditMetricServiceTest {
    @Test
    public void testGetDataproxyAuditMetrics(){
        //Query service initialization
        AppConfig appConfig = new AppConfig();
        AuditSQLUtil.initialize(appConfig.getProperties());
        AuditMetricService auditMetricService=new AuditMetricService();
        //Search for relevant data
        List<AuditMetricVo> dataproxyAuditMetrics = auditMetricService.getDataproxyAuditMetrics();

        for(AuditMetricVo auditMetricVo:dataproxyAuditMetrics){
            System.out.println(auditMetricVo.getInlongGroupId()+" "+auditMetricVo.getInlongStreamId()+" "+auditMetricVo.getCount());
        }
    }

    @Test
    public void testGetIcebergAuditMetrics(){
        //Query service initialization
        AppConfig appConfig = new AppConfig();
        AuditSQLUtil.initialize(appConfig.getProperties());
        AuditMetricService auditMetricService=new AuditMetricService();

        //Search for relevant data
        List<String> icebergAuditIds=new ArrayList<>();
        icebergAuditIds.add("1073741838");

        List<AuditMetricVo> dataproxyAuditMetrics = auditMetricService.getIcebergAuditMetrics(icebergAuditIds);
        for(AuditMetricVo auditMetricVo:dataproxyAuditMetrics){
            System.out.println(auditMetricVo.getInlongGroupId()+" "+auditMetricVo.getInlongStreamId()+" "+auditMetricVo.getCount());
        }
    }

    @Test
    public void testGetHiveAuditMetrics(){

        AppConfig appConfig = new AppConfig();
        AuditSQLUtil.initialize(appConfig.getProperties());
        AuditMetricService auditMetricService=new AuditMetricService();


        List<String> icebergAuditIds=new ArrayList<>();
        icebergAuditIds.add("1073741838");

        List<AuditMetricVo> dataproxyAuditMetrics = auditMetricService.getHiveAuditMetrics(icebergAuditIds);
        for(AuditMetricVo auditMetricVo:dataproxyAuditMetrics){
            System.out.println(auditMetricVo.getInlongGroupId()+" "+auditMetricVo.getInlongStreamId()+" "+auditMetricVo.getCount());
        }
    }
}
