# spring-boot-aws-xray-sample for AWS XRay
[![Hits](https://hits.seeyoufarm.com/api/count/incr/badge.svg?url=https%3A%2F%2Fgithub.com%2Fanthunt%2Fspring-boot-aws-xray-sample&count_bg=%2379C83D&title_bg=%23555555&icon=&icon_color=%23E7E7E7&title=hits&edge_flat=false)](https://hits.seeyoufarm.com)
[![Java](https://img.shields.io/badge/language-Java-red.svg)](#)
[![License](https://img.shields.io/badge/license-Apache%202-blue.svg)](LICENSE.txt)
[![Gitpod](https://img.shields.io/badge/build-Gitpod-green.svg)](https://gitpod.io/#https://github.com/anthunt/AWS2Terraform)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/6e06125002e149cd8bca7e7a24e76ee6)](https://www.codacy.com/gh/anthunt/spring-boot-aws-xray-sample/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=anthunt/spring-boot-aws-xray-sample&amp;utm_campaign=Badge_Grade)
[![Build Status](https://travis-ci.org/anthunt/spring-boot-xray-demo.svg?branch=master)](https://travis-ci.org/anthunt/spring-boot-xray-demo)
![Maven Package](https://github.com/anthunt/spring-boot-xray-demo/workflows/Maven%20Package/badge.svg?branch=master)
![Java CI with Maven](https://github.com/anthunt/spring-boot-xray-demo/workflows/Java%20CI%20with%20Maven/badge.svg)

As the transition to MSA-oriented application services accelerates, it becomes more important to track complex invocation relationships between microservices.
AWS provides the X-Ray service as a service for tracking and managing the call relationships between many of these services.
This service integrates with AWS's API Gateway service, making it easy to monitor the call relationship to service requests and the performance of your application calls.
However, it does require a bit of coding to track execution within the application.
It is therefore open source providing sample code for easy integration of X-Ray tracing into SpringBoot applications.
This feature leverages SpringBoot's AOP capabilities to provide an easy way to apply X-Ray without affecting existing application code.

## AWS X-Ray Screenshot
![Screenshot of the AWS X-Ray Trace console](https://github.com/anthunt/spring-boot-aws-xray-sample/raw/master/awsxray-snapshot.png?raw=true)

## Features

1. Incoming Servlet request tracing with Spring AOP
2. Outgoing HttpRequest tracing with FeignClient
3. Database Query tracing with HikariCP

## Description

### config/AWSXRayConfig.java
- Configure SpringBoot Project Preferences for AWS X-Ray

### config/AWSXRayInspector.java
- Configuration of trace function for bean execution in SpringBoot with AOP
- Bean scope setting for tracking by Annotation setting

```
@Pointcut("@within(com.amazonaws.xray.spring.aop.XRayEnabled) && (bean(*Controller) || bean(*Service) || bean(*Client) || bean(*Mapper))")  
```

### config/AWSXRayFeignClientConfig.java
- Configure tracing function for FeignClient execution for outbound request tracing

### config/AWSXRayHikariConfig.java
- Set up integration with HikariCP for tracking database calls

### config/AWSXRayTracingConnection.java, AWSXRayTracingDataSource.java, AWSXRayTracingStatement.java
- Configure tracking function for query invocation by providing wrapping function for JDBC configuration objects



