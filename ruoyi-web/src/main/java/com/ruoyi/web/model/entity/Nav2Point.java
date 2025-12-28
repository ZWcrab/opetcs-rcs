package com.ruoyi.web.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 导航点位实体类
 *
 * @author ruoyi
 * @date 2025-12-27
 */
@Data
@TableName("tb_nav2_point")
public class Nav2Point implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * X坐标
     */
    private Double xPos;

    /**
     * Y坐标
     */
    private Double yPos;

    /**
     * 点位名称
     */
    private String name;
}