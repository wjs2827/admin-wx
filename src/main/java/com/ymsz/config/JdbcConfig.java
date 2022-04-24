package com.ymsz.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.support.http.StatViewServlet;
import com.alibaba.druid.support.http.WebStatFilter;
import com.ymsz.controller.UserController;
import com.ymsz.observer.Observer;
import com.ymsz.observer.impl.DishFavoriteNumObserver;
import com.ymsz.utils.Base64Util;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author ymsz
 * @description  jdbc配置
 * @date 2021/11/6
 * @email jinshan.wang.it@foxmail.com
 */
@Configuration
@Slf4j
public class JdbcConfig {


    @Value("${spring.datasource.username}")
    private String druidUsername;

    @Value("${spring.datasource.password}")
    private String druidPassword;

    /**
     * 配置druid数据源
     * 配置redis端口ip信息方式
     *
     * @return 第一种方式：注解@ConfigurationProperties(prefix = "spring.datasource")//指定数据源的前缀,在application.properties文件中指定
     * #配置jdbc数据源
     * spring.datasource.username=*********
     * return new DruidDataSource();自动注解配置
     */
    @Bean(name = "dataSource")
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource dataSource() {
        return new DruidDataSource();
    }

     /**
     * 配置事务管理器
     */
    @Bean(name = "transactionManager")
    public DataSourceTransactionManager transactionManager() {
        return new DataSourceTransactionManager(dataSource());
    }

    /**
     * 创建SqlSessionFactory 并加载mapper接口对应的xml
     * @return
     * @throws Exception
     */
    @Bean
    public SqlSessionFactory sqlSessionFactoryBean() throws Exception {
        log.info("###############创建SqlSessionFactory#############################");
        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(dataSource());
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        sqlSessionFactoryBean.setMapperLocations(resolver.getResources("classpath:/com/ymsz/mapper/*.xml"));
        return sqlSessionFactoryBean.getObject();
    }

    /**
     * druid监控
     * 监控访问地址 http://localhost/druid/index.html
     * @return
     */
    @Bean
    public ServletRegistrationBean druidServlet() {
        log.info("##################druid监控加载开始#############################");
        ServletRegistrationBean reg = new ServletRegistrationBean();
        reg.setServlet(new StatViewServlet());
        reg.addUrlMappings("/druid/*");
        reg.addInitParameter("loginUsername", druidUsername);
        reg.addInitParameter("loginPassword", druidPassword);
        return reg;
    }

    /**
     * druid监控过滤
     * @return
     */
    @Bean
    public FilterRegistrationBean filterRegistrationBean() {
        log.info("##################druid监控过滤加载开始#############################");
        FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean();
        filterRegistrationBean.setFilter(new WebStatFilter());
        filterRegistrationBean.addUrlPatterns("/*");
        filterRegistrationBean.addInitParameter("exclusions", "*.js,*.gif,*.jpg,*.png,*.css,*.ico,/druid/*");
        return filterRegistrationBean;
    }

    /**
     * 对于数据比较敏感的场景，
     * 读锁：在读取数据时是不能出现多次读取不一致的情况的，这点有点像可重复读和幻读，
     * 写锁：写数据时，又不能同时读取数据
     * @return
     */
    @Bean
    public ReadWriteLock getReadWriteLock() {
        return new ReentrantReadWriteLock();
    }

    /**
     * 观察者
     * @param controller
     * @return
     */
    @Bean
    public Observer observer(UserController controller) {
        DishFavoriteNumObserver observer = new DishFavoriteNumObserver();
        controller.observerRegister(observer, 0);
        return observer;
    }

    @Bean
    public String fzby(){
        log.info("##################fzby#############################");
        String fozuStr = "ICAgICAgICAgICAgICAgICAgIF9vb09vb18KICAgICAgICAgICAgICAgICAgbzg4ODg4ODhvCiAgICAgICAgICAgICAgICAgIDg4IiAuICI4OAogICAgICAgICAgICAgICAgICAofCAtXy0gfCkKICAgICAgICAgICAgICAgICAgT1wgID0gIC9PCiAgICAgICAgICAgICAgIF9fX18vYC0tLSdcX19fXwogICAgICAgICAgICAgLicgIFxcfCAgICAgfC8vICBgLgogICAgICAgICAgICAvICBcXHx8fCAgOiAgfHx8Ly8gIFwKICAgICAgICAgICAvICBffHx8fHwgLTotIHx8fHx8LSAgXAogICAgICAgICAgIHwgICB8IFxcXCAgLSAgLy8vIHwgICB8CiAgICAgICAgICAgfCBcX3wgICcnXC0tLS8nJyAgfCAgIHwKICAgICAgICAgICBcICAuLVxfXyAgYC1gICBfX18vLS4gLwogICAgICAgICBfX19gLiAuJyAgLy0tLi0tXCAgYC4gLiBfXwogICAgICAuIiIgJzwgIGAuX19fXF88fD5fL19fXy4nICA+JyIiLgogICAgIHwgfCA6ICBgLSBcYC47YFwgXyAvYDsuYC8gLSBgIDogfCB8CiAgICAgXCAgXCBgLS4gICBcXyBfX1wgL19fIF8vICAgLi1gIC8gIC8KPT09PT09YC0uX19fX2AtLl9fX1xfX19fXy9fX18uLWBfX19fLi0nPT09PT09CiAgICAgICAgICAgICAgICAgICBgPS0tLT0nCl5eXl5eXl5eXl5eXl5eXl5eXl5eXl5eXl5eXl5eXl5eXl5eXl5eXl5eXl5eXgogICAgICAgICAgICAgICAgIOS9m+elluS/neS9kSAgICAgICDmsLjml6BCVUc=";
        byte[] decode = null;
        try {
            decode = Base64Util.decodeBase64(fozuStr);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            System.out.println("\n" + new String(decode, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return fozuStr;
    }

}
