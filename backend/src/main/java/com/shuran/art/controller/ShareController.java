package com.shuran.art.controller;

import com.shuran.art.dto.Result;
import com.shuran.art.dto.ShareRequest;
import com.shuran.art.service.ShareService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/share")
@RequiredArgsConstructor
public class ShareController {

    private final ShareService shareService;

    @PostMapping("/record")
    public Result<Map<String, Object>> recordShare(
            HttpServletRequest request,
            @RequestBody ShareRequest shareRequest) {
        Long visitorId = (Long) request.getAttribute("userId");
        Map<String, Object> result = shareService.recordShare(visitorId, shareRequest);
        return Result.success(result);
    }
}
