package com.anthunt.aws.spring.boot.xray.config;

import java.io.FileNotFoundException;
import java.net.URL;
import java.util.Optional;

import javax.servlet.Filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ResourceUtils;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.AWSXRayRecorderBuilder;
import com.amazonaws.xray.javax.servlet.AWSXRayServletFilter;
import com.amazonaws.xray.slf4j.SLF4JSegmentListener;
import com.amazonaws.xray.strategy.sampling.CentralizedSamplingStrategy;
import com.anthunt.aws.spring.boot.xray.exception.ApplicationNameNullException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class AWSXRayConfig {
	
	@Value("${spring.application.name}")
	private String AWSXRAY_SEGMENT_NAME;
	
	private static final String SAMPLING_RULE_JSON = "classpath:xray/sampling-rules.json";
	
	static {
		
		URL ruleFile = null;
		try {
			ruleFile = ResourceUtils.getURL(SAMPLING_RULE_JSON);
		} catch (FileNotFoundException e) {
			log.error("sampling rule cannot load for aws xray - {}", e.getMessage());
		}
		log.debug("sampling rule load from {} for aws xray", ruleFile.getPath());
		
		AWSXRayRecorderBuilder builder = AWSXRayRecorderBuilder.standard()
			.withDefaultPlugins()
			.withSamplingStrategy(new CentralizedSamplingStrategy(ruleFile))
			.withSegmentListener(new SLF4JSegmentListener());

		AWSXRay.setGlobalRecorder(builder.build());
		log.debug("aws xray recorder was setted globally.");
	}
	
	@Bean
	public Filter TracingFilter() {
		log.debug("The segment name for aws xray tracking has been set to {}.", AWSXRAY_SEGMENT_NAME);
		return new AWSXRayServletFilter(
				Optional.ofNullable(AWSXRAY_SEGMENT_NAME)
						.orElseThrow(()->new ApplicationNameNullException())
		);
	}
	
}