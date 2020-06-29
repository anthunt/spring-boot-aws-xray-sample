package com.anthunt.aws.spring.boot.xray;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy
@EnableFeignClients(basePackages = {"com.anthunt.aws.spring.boot.xray.service.clients"})
@MapperScan(basePackages = "com.anthunt.aws.spring.boot.xray.dao")
public class SpringBootXRayApplication {

	public static void main(String[] args) {
		
		SpringApplication.run(SpringBootXRayApplication.class, args);
		
	}

}
