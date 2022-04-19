package org.jetlinks.community.modbus.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.Comment;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.api.crud.entity.RecordCreationEntity;
import org.hswebframework.web.crud.generator.Generators;
import org.hswebframework.web.validator.CreateGroup;

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
@Table(name = "device_collect_config")
public class DeviceCollectConfig extends GenericEntity<String> implements RecordCreationEntity {

    @Comment("产品ID")
    @Column(name = "product_id")
    @NotBlank(message = "产品ID不能为空", groups = CreateGroup.class)
    @Schema(description = "产品ID")
    private String productId;

    @Comment("设备ID")
    @Column(name = "device_id")
    @Schema(description = "设备ID")
    private String deviceId;

    @Comment("执行频率")
    @Column(name = "interval")
    @Schema(description = "执行频率")
    private Long interval;

    @Comment("任务开关")
    @Column(name = "task_switch")
    @Schema(description = "任务开关")
    private Boolean taskSwitch;

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


}
