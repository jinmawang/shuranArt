package com.shuran.art.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shuran.art.dto.Result;
import com.shuran.art.entity.Banner;
import com.shuran.art.mapper.BannerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/banner")
@RequiredArgsConstructor
public class BannerController {

    private final BannerMapper bannerMapper;

    @GetMapping("/list")
    public Result<List<Banner>> getBanners() {
        List<Banner> banners = bannerMapper.selectList(
            new LambdaQueryWrapper<Banner>()
                .eq(Banner::getStatus, 1)
                .orderByAsc(Banner::getSortOrder)
        );
        return Result.success(banners);
    }

    @GetMapping("/detail")
    public Result<Banner> getBanner(@RequestParam Long id) {
        return Result.success(bannerMapper.selectById(id));
    }
}
