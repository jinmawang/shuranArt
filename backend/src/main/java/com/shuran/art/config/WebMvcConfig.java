package com.shuran.art.config;

import com.shuran.art.interceptor.AdminInterceptor;
import com.shuran.art.interceptor.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final AdminInterceptor adminInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 用户认证拦截器
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                    "/api/user/login",
                    "/api/teacher/list",
                    "/api/activity/list",
                    "/api/activity/detail",
                    "/api/lottery/prizes",
                    "/api/studio/config",
                    "/api/course/list",
                    "/api/course/*",
                    "/api/banner/list",
                    "/api/upload",
                    "/api/works/wall",
                    "/api/works/timeline/*"
                );

        // 管理员权限拦截器
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/api/admin/**");
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.stream()
            .filter(converter -> converter instanceof MappingJackson2HttpMessageConverter)
            .forEach(converter -> {
                MappingJackson2HttpMessageConverter jsonConverter = (MappingJackson2HttpMessageConverter) converter;
                List<MediaType> mediaTypes = new ArrayList<>();
                mediaTypes.add(new MediaType("application", "json", StandardCharsets.UTF_8));
                jsonConverter.setSupportedMediaTypes(mediaTypes);
            });
    }
}
