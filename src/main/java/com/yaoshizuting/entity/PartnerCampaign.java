package com.yaoshizuting.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.apache.ibatis.type.JdbcType;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("gyt_partner_campaign")
public class PartnerCampaign implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long ownerId;

    private String shareCode;

    private String storeName;

    private String city;

    private String phone;

    private String address;

    private String title;

    private String subtitle;

    private BigDecimal price;

    @TableField(jdbcType = JdbcType.VARCHAR)
    private String benefitText;

    @TableField(jdbcType = JdbcType.VARCHAR)
    private String giftText;

    @TableField(jdbcType = JdbcType.VARCHAR)
    private String shareRewardText;

    private Integer quota;

    private Integer signedCount;

    private String posterImage;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
