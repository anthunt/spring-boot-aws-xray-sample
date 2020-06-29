package com.anthunt.aws.spring.boot.xray.config;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.zaxxer.hikari.HikariDataSource;

@Configuration
public class AWSXRayHikariConfig {

	@Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    //DataSourceProperties will convert `spring.datasource.url` property to hikari's jdbcUrl property
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "hikariDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.hikari")//set hikari specific properties
    public HikariDataSource hikariDataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean(name = "dataSource")
    @Primary
    public DataSource dataSource(HikariDataSource hikariDataSource) {
        //wrap the spring ds into xray tracing ds
        return new AWSXRayTracingDataSource(hikariDataSource);
    }
    
}
