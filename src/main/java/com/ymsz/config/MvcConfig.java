package com.ymsz.config;

import com.ymsz.interceptor.MyInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.accept.HeaderContentNegotiationStrategy;
import org.springframework.web.accept.ParameterContentNegotiationStrategy;
import org.springframework.web.servlet.config.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author ymsz
 * @description
 * @date 2021/11/10
 * @email ymsz@foxmail.com
 */
@Configuration
public class MvcConfig  implements WebMvcConfigurer {

    @Bean
    public MyInterceptor myInterceptor(){
        return new MyInterceptor();
    }


//    @Bean
//    public HttpMessageConverter<String> responseBodyConverter() {
//        return new StringHttpMessageConverter(
//                StandardCharsets.UTF_8);
//    }

//    @Override
//    public void configureMessageConverters(
//            List<HttpMessageConverter<?>> converters) {
//        super.configureMessageConverters(converters);
//        converters.add(responseBodyConverter());
//
//        FastJsonHttpMessageConverter fastConverter = new FastJsonHttpMessageConverter();
//        FastJsonConfig fastJsonConfig = new FastJsonConfig();
//        fastJsonConfig.setSerializerFeatures(
//                SerializerFeature.PrettyFormat
//        );
//        List<MediaType> fastMediaTypes = new ArrayList<>();
//        fastMediaTypes.add(MediaType.APPLICATION_JSON_UTF8);
//        fastConverter.setSupportedMediaTypes(fastMediaTypes);
//        fastConverter.setFastJsonConfig(fastJsonConfig);
//
//        converters.add(fastConverter);
//    }

    /**
     * 自定义策略
     * @param configurer
     */
    @Override
    public void configureContentNegotiation(
            ContentNegotiationConfigurer configurer) {
        Map<String, MediaType> map = new HashMap<>();
        map.put("json", MediaType.APPLICATION_JSON);
        map.put("xml", MediaType.APPLICATION_ATOM_XML);
        map.put("gg",MediaType.parseMediaType("applicaion/x-z"));
        // 指定基于参数的解析类型
        ParameterContentNegotiationStrategy negotiationStrategy = new ParameterContentNegotiationStrategy(map);
        // 指定基于请求头的解析
        HeaderContentNegotiationStrategy headerContentNegotiationStrategy = new HeaderContentNegotiationStrategy();
        configurer.strategies(Arrays.asList(negotiationStrategy,headerContentNegotiationStrategy));
    }

    /**
     * 拦截器， 如果访问路径是addResourceHandler中的filepath 这个路径
     * 那么就 映射到访问本地的addResourceLocations 的参数的这个路径上，这样就可以让别人访问服务器的本地文件了
     * @param registry
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        //注册静态文件
        registry.addResourceHandler("/statics/**").addResourceLocations("classpath:/statics/");
        // 解决 SWAGGER 404报错
        registry.addResourceHandler("/swagger-ui.html").addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
    }

    /**
     * 注册拦截器
     * @param registry
     */
    @Override
    public  void addInterceptors(InterceptorRegistry registry){
        registry.addInterceptor(myInterceptor()).addPathPatterns("/get_user_orders");
    }
}
