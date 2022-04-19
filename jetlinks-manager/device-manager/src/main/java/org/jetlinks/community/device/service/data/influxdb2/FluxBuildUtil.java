package org.jetlinks.community.device.service.data.influxdb2;

import com.influxdb.query.dsl.functions.restriction.Restrictions;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Tensai
 */
public class FluxBuildUtil {

    /**
     * 查询参数转换日志过滤
     *
     * @param param 查询参数
     * @return 日志过滤
     */
    public static List<Restrictions> queryParamToFilterLog(QueryParamEntity param) {
        List<Restrictions> restrictionsList = new ArrayList<>();
        param.getTerms().stream().filter(it -> it.getColumn().startsWith("type")).forEach(term -> {
            String[] values = term.getValue().toString().split(",");
            if ("IN".equals(term.getTermType())) {
                for (String value : values) {
                    restrictionsList.add(Restrictions.column(term.getColumn()).equal(value));
                }
            }
        });
        return restrictionsList;
    }

    /**
     * 查询参数转换时间范围
     *
     * @param param 查询参数
     * @return 时间范围
     */
    public static List<Instant> queryParamToRangeTime(QueryParamEntity param) {
        List<Instant> instantList = new ArrayList<>();
        param.getTerms().stream().filter(it -> it.getColumn().startsWith("timestamp")).forEach(term -> {
            String[] values = term.getValue().toString().split(",");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime localDateTime0 = LocalDateTime.parse(values[0], formatter);
            LocalDateTime localDateTime1 = LocalDateTime.parse(values[1], formatter);
            instantList.add(localDateTime0.toInstant(ZoneOffset.ofHours(8)));
            instantList.add(localDateTime1.toInstant(ZoneOffset.ofHours(8)));
        });
        return instantList;
    }
}
