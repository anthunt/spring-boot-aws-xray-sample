package com.anthunt.aws.spring.boot.xray.service.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(value="example", url="https://www.naver.com")
public interface TestClient {

	@GetMapping("/")
	String getNaver();
	
}
