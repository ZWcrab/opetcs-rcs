package com.ruoyi.web.service;

import com.ruoyi.web.model.entity.Nav2Point;

import java.util.List;

/**
 * 导航点位服务接口
 *
 * @author ruoyi
 * @date 2025-12-27
 */
public interface Nav2PointService {
    /**
     * 保存导航点位
     *
     * @param nav2Point 导航点位信息
     * @return 影响行数
     */
    int saveNav2Point(Nav2Point nav2Point);

    /**
     * 查询所有导航点位
     *
     * @return 导航点位列表
     */
    List<Nav2Point> getAllNav2Points();
    
    /**
     * 删除导航点位
     *
     * @param id 导航点位ID
     * @return 影响行数
     */
    int deleteNav2Point(Long id);
}