package org.apache.inlong.audit.tool.service;

import org.apache.ibatis.session.SqlSession;
import org.apache.inlong.audit.tool.VO.AuditMetricVo;
import org.apache.inlong.audit.tool.basemetric.BaseMetricReporter;
import org.apache.inlong.audit.tool.basemetric.mapper.AuditMapper;
import org.apache.inlong.audit.tool.basemetric.util.AuditSQLUtil;
import org.apache.inlong.audit.tool.basemetric.vo.AuditDataVo;
import org.apache.inlong.audit.tool.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class AuditMetricService {
    private Integer intervalTimeMinute;

    private static final String DATAPROXY_AUDITID="5";
    private static final DateTimeFormatter LOGTS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Logger LOGGER = LoggerFactory.getLogger(AuditMetricService.class);

    public AuditMetricService(){
        //从配置文件中获取时间间隔，如果未能找到相关配置，默认取60s，即1分钟
        Properties properties=new Properties();
        try {
            properties.load(getClass().getClassLoader().getResourceAsStream("application.properties"));
            this.intervalTimeMinute=Integer.parseInt(properties.getProperty("audit.data.time.interval.minute", "1"));
        }catch (IOException e){
            this.intervalTimeMinute=1;
            LOGGER.error("No time interval related configuration found, default is set to 1 minute");
        }

        //初始化数据库查询工具，获取数据库连接信息
        AuditSQLUtil.initialize(properties);
    }

    /**
     * 查询Datapoxy相关数据并返回
     * @return
     */
    public List<AuditMetricVo> getDataproxyAuditMetrics(){
        //获取logts字段，当前系统时间减去 intervalTimeMinute 分钟
        String logts=getLogTs();
        SqlSession sqlSession = null;
        try {
            sqlSession = AuditSQLUtil.getSqlSession();
            AuditMapper auditMapper = sqlSession.getMapper(AuditMapper.class);
            List<AuditMetricVo> auditMetricVos = auditMapper.queryDataproxyAuditMetric(logts,DATAPROXY_AUDITID);
            return auditMetricVos;
        }catch (Exception e){
            LOGGER.error("查询数据库过程出现异常!"+e.getMessage());
        } finally {
            sqlSession.close();
        }
        return null;
    }

    /**
     * 查询Iceberg相关数据并返回
     * @param auditIds
     * @return
     */
    public List<AuditMetricVo> getIcebergAuditMetrics(List<String> auditIds){
        String logts=getLogTs();
        SqlSession sqlSession = null;
        try {
            sqlSession = AuditSQLUtil.getSqlSession();
            AuditMapper auditMapper = sqlSession.getMapper(AuditMapper.class);
            //遍历每个auditId，搜出在数据库的数据，并且统一放入到auditMetricVos中
            List<AuditMetricVo> auditMetricVos=new ArrayList<>();
            for(String auditId:auditIds){
                List<AuditMetricVo> tempList = auditMapper.queryDataproxyAuditMetric(logts, auditId);
                if(tempList!=null&&tempList.size()>0){
                    auditMetricVos.addAll(tempList);
                }
            }
            return auditMetricVos;
        }catch (Exception e){
            LOGGER.error("查询数据库过程出现异常!"+e.getMessage());
        } finally {
            sqlSession.close();
        }
        return null;
    }

    public List<AuditMetricVo> getHiveAuditMetrics(List<String> auditIds){
        String logts=getLogTs();
        SqlSession sqlSession = null;
        try {
            sqlSession = AuditSQLUtil.getSqlSession();
            AuditMapper auditMapper = sqlSession.getMapper(AuditMapper.class);
            //遍历每个auditId，搜出在数据库的数据，并且统一放入到auditMetricVos中
            List<AuditMetricVo> auditMetricVos=new ArrayList<>();
            for(String auditId:auditIds){
                List<AuditMetricVo> tempList = auditMapper.queryDataproxyAuditMetric(logts, auditId);
                if(tempList!=null&&tempList.size()>0){
                    auditMetricVos.addAll(tempList);
                }
            }
            return auditMetricVos;
        }catch (Exception e){
            LOGGER.error("查询数据库过程出现异常!"+e.getMessage());
        } finally {
            sqlSession.close();
        }
        return null;
    }

    //获取用于查询的logts字段的值
    private String getLogTs(){
        return LocalDateTime.now()        // 当前时间
                .withSecond(0) // 秒置 00
                .minusMinutes(intervalTimeMinute) // 减分钟
                .format(LOGTS_FMT);
    }
}
