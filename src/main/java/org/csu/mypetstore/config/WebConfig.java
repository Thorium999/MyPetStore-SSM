package org.csu.mypetstore.config;

import org.csu.mypetstore.interceptor.CsrfInterceptor;
import org.csu.mypetstore.interceptor.LoginInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 注册拦截器。顺序有意义：先校验 CSRF（所有写操作），再判断是否登录。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final CsrfInterceptor csrfInterceptor;
    private final LoginInterceptor loginInterceptor;

    public WebConfig(CsrfInterceptor csrfInterceptor, LoginInterceptor loginInterceptor) {
        this.csrfInterceptor = csrfInterceptor;
        this.loginInterceptor = loginInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(csrfInterceptor)
                .addPathPatterns("/**");

        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/cart", "/cart/**", "/checkout", "/orders", "/orders/**");
    }
}
