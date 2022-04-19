package org.jetlinks.community.modbus.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.Comment;
import org.hswebframework.ezorm.rdb.mapping.annotation.EnumCodec;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.api.crud.entity.RecordCreationEntity;
import org.hswebframework.web.crud.generator.Generators;
import org.hswebframework.web.validator.CreateGroup;
import org.jetlinks.modbus.protocol.tcp.model.DataType;
import org.jetlinks.modbus.protocol.tcp.model.ModbusProductModelView;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;


/**
 * @author Tensai
 */
@Getter
@Setter
@Table(name = "modbus_product_model")
public class ModbusProductModel extends GenericEntity<String> implements RecordCreationEntity {

    @Comment("产品ID")
    @Column(name = "product_id")
    @NotBlank(message = "产品ID不能为空", groups = CreateGroup.class)
    @Schema(description = "产品ID")
    private String productId;

    @Comment("指标编码")
    @Column(name = "metric_code")
    @Schema(description = "指标编码")
    private String metricCode;

    @Comment("指标名称")
    @Column(name = "metric_name")
    @Schema(description = "指标名称")
    private String metricName;

    @Comment("偏移量")
    @Column(name = "offset")
    @Schema(description = "偏移量")
    private Integer offset;

    @Comment("指标系数")
    @Column(name = "ratio")
    @Schema(description = "指标系数")
    private Double ratio;

    @Comment("字节长度")
    @Column(name = "length")
    @Schema(description = "字节长度")
    private Integer length;

    @Comment("数据类型")
    @Column(name = "dataType")
    @Schema(description = "数据类型")
    @EnumCodec
    @ColumnType(javaType = String.class)
    private DataType dataType;

    @Column(name = "creator_id")
    @Comment("创建者id")
    @Schema(description = "创建者ID(只读)")
    private String creatorId;

    @Comment("创建时间")
    @Column(name = "create_time")
    @Schema(description = "创建者时间(只读)")
    private Long createTime;

    @Override
    @GeneratedValue(generator = Generators.SNOW_FLAKE)
    @Pattern(
        regexp = "^[0-9a-zA-Z_\\-]+$",
        message = "ID只能由数字,字母,下划线和中划线组成",
        groups = CreateGroup.class)
    @Schema(description = "ID")
    public String getId() {
        return super.getId();
    }

    public ModbusProductModelView mapView() {
        return ModbusProductModelView.of(productId, metricCode, ratio, offset, length, dataType);
    }

}
