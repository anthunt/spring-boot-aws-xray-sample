package com.anthunt.aws.spring.boot.xray.dao;

import org.apache.ibatis.annotations.Mapper;

import com.amazonaws.xray.spring.aop.XRayEnabled;

@Mapper
@XRayEnabled
public interface TestMapper {

	public int count(int idx);
	
}
