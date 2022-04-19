package org.jetlinks.community.influxdb2.service;

import com.influxdb.annotations.Column;
import com.influxdb.annotations.Measurement;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.reactive.InfluxDBClientReactive;
import com.influxdb.client.reactive.QueryReactiveApi;
import com.influxdb.client.reactive.WriteReactiveApi;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import io.reactivex.Maybe;
import org.jetbrains.annotations.NotNull;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Function;


/**
 * InfluxDB操作
 *
 * @author Tensai
 */
@Service
public class Influxdb2Service {

    private final WriteReactiveApi writeApi;

    private final QueryReactiveApi queryApi;

    public Influxdb2Service(InfluxDBClientReactive fluxClient) {
        this.writeApi = fluxClient.getWriteReactiveApi();
        this.queryApi = fluxClient.getQueryReactiveApi();
    }


    /**
     * 写入 <br />
     * 推荐等级 ==> 1
     *
     * @param measurementList publisher<实体对象>
     * @param <M>             实体类型 {@link Measurement} {@link Column}
     */
    public <M> void save(@Nonnull final List<M> measurementList) {
        writeApi.writeMeasurements(WritePrecision.NS, Flux.fromIterable(measurementList));
    }

    /**
     * 查询<br />
     * 推荐等级 ==> 1 <br />
     * Publisher<实体>
     *
     * @param flux  flux语法查询
     * @param clazz Class对象
     * @param <T>   目标类型
     * @return Publisher<实体>
     */
    public <T> Flux<T> query(@NotNull com.influxdb.query.dsl.Flux flux, Class<T> clazz) {
        return reactor.adapter.rxjava.RxJava2Adapter
            .flowableToFlux(queryApi.query(flux.toString(), clazz));
    }

    /**
     * 写入 <br />
     * 推荐等级 ==> 2 <br />
     *
     * @param pointFlux point的Publisher
     */
    public void save(Publisher<Point> pointFlux) {
        writeApi.writePoints(pointFlux);
    }

    /**
     * 查询 <br />
     * 推荐等级 ==> 2 <br />
     * Publisher<实体>
     *
     * @param flux        flux语法查询
     * @param mapFunction mapFunction
     * @param <R>         实体类型 {@link Measurement} {@link Column}
     * @return Publisher<实体>
     */
    public <R> Flux<R> query(@NotNull com.influxdb.query.dsl.Flux flux,
                             Function<FluxRecord, R> mapFunction) {
        return query(flux.toString(), mapFunction);
    }

    /**
     * 查询<br />
     * 推荐等级 ==> 3 <br />
     * Publisher<实体>
     *
     * @param query flux语法查询
     * @param clazz Class对象
     * @param <T>   目标类型
     * @return Publisher<实体>
     */
    public <T> Flux<T> query(String query, Class<T> clazz) {
        return reactor.adapter.rxjava.RxJava2Adapter
            .flowableToFlux(queryApi.query(query, clazz));
    }

    /**
     * Flux 语法
     * <pre>
     * from(bucket: "iotsharp-bucket")
     *   |> range(start: v.timeRangeStart, stop: v.timeRangeStop)
     *   |> filter(fn: (r) => r["_measurement"] == "TelemetryData")
     *   |> filter(fn: (r) => r["DeviceId"] == "id")
     *   |> group(columns: [""_field""])
     *   |> yield()
     * </pre>
     * <p>
     * Flux-dsl 用法
     * https://github.com/influxdata/influxdb-client-java/tree/master/flux-dsl
     *
     * @param query    flux查询
     * @param function 转换函数
     * @param <R>      目标类型
     * @return Publisher<实体>
     */
    public <R> Flux<R> query(String query, Function<FluxRecord, R> function) {
        return reactor.adapter.rxjava.RxJava2Adapter
            .flowableToFlux(queryApi.query(query))
            .map(function);
    }

    public <R> Flux<R> count(String query, Function<FluxRecord, R> function) {
        return reactor.adapter.rxjava.RxJava2Adapter
            .flowableToFlux(queryApi.query(query))
            .map(function);
    }

    /**
     * MayBe方式写入数据，不推荐
     *
     * @param bucket    bucket
     * @param org       org
     * @param precision precision
     * @param record    record记录（可以单个可以多个）
     */
    @Deprecated
    public void save(@Nonnull final String bucket,
                     @Nonnull final String org,
                     @Nonnull final WritePrecision precision,
                     @Nonnull final Maybe<String> record) {
        writeApi.writeRecord(bucket, org, precision, record);
    }

    /**
     * MayBe 线性写入， 不推荐
     *
     * @param record 记录
     */
    @Deprecated
    public void save(Maybe<String> record) {
        writeApi.writeRecord(WritePrecision.NS, record);
    }

    /**
     * 找到最完全的定义
     *
     * @param lineProtocol 线性协议表达式
     */
    @Deprecated
    public void save(String lineProtocol) {
        Maybe<String> record = Maybe.just(lineProtocol);
        save(record);
    }

    /**
     * 响应式 自定义 bucket org 写入， 强烈推荐
     *
     * @param bucket bucket
     * @param org    org
     * @param points points
     */
    public void save(@Nonnull final String bucket,
                     @Nonnull final String org,
                     @Nonnull final Publisher<Point> points) {
        writeApi.writePoints(bucket, org, points);
    }

    /**
     * 响应式 自定义 bucket org 写入， 强烈推荐
     *
     * @param bucket       bucket
     * @param org          org
     * @param precision    时序等级{秒，毫秒...}
     * @param measurements measurements
     * @param <M>          实体
     */
    public <M> void save(@Nonnull final String bucket,
                         @Nonnull final String org,
                         @Nonnull final WritePrecision precision,
                         @Nonnull final Publisher<M> measurements) {
        writeApi.writeMeasurements(bucket, org, precision, measurements);
    }


    /**
     * 查询
     * Publisher<实体>
     *
     * @param flux flux语法查询
     * @return Publisher<实体>
     */
    public Flux<FluxRecord> query(com.influxdb.query.dsl.Flux flux) {
        return query(flux, Function.identity());
    }


}
