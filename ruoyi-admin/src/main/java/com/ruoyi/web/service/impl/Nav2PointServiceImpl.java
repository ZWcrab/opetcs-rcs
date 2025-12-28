package com.ruoyi.web.service.impl;

import com.ruoyi.web.mapper.Nav2PointMapper;
import com.ruoyi.web.model.entity.Nav2Point;
import com.ruoyi.web.service.Nav2PointService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 导航点位服务实现
 *
 * @author ruoyi
 * @date 2025-12-27
 */
@Service
public class Nav2PointServiceImpl implements Nav2PointService {

    @Autowired
    private Nav2PointMapper nav2PointMapper;

    @Override
    public int saveNav2Point(Nav2Point nav2Point) {
        return nav2PointMapper.insertNav2Point(nav2Point);
    }

    @Override
    public List<Nav2Point> getAllNav2Points() {
        return nav2PointMapper.selectAllNav2Points();
    }
}