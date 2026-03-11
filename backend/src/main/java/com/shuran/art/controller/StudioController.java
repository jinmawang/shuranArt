package com.shuran.art.controller;

import com.shuran.art.dto.Result;
import com.shuran.art.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/studio")
@RequiredArgsConstructor
public class StudioController {

    private final AdminService adminService;

    @GetMapping("/config")
    public Result<Map<String, String>> getStudioConfig() {
        return Result.success(adminService.getStudioConfig());
    }
}
