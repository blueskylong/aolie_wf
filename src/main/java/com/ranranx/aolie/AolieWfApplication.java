package com.ranranx.aolie;

import com.ranranx.aolie.config.AppDispatcherServletConfiguration;
import com.ranranx.aolie.config.ApplicationConfiguration;
import org.flowable.ui.modeler.conf.DatabaseConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;

/**
 * @author xxl
 * @version V0.0.1
 * @date 2021/3/4 0004 12:18
 **/
//启用全局异常拦截器
@Import(value = {
        // 引入修改的配置
        ApplicationConfiguration.class,
        AppDispatcherServletConfiguration.class,
        // 引入 DatabaseConfiguration 表更新转换
        DatabaseConfiguration.class})
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@EnableAspectJAutoProxy
public class AolieWfApplication {

    public static void main(String[] args) {
        SpringApplication.run(AolieWfApplication.class, args);
    }


}
