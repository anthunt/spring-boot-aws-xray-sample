package com.anthunt.aws.spring.boot.xray.config;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import com.amazonaws.xray.sql.TracingDataSource;

public class AWSXRayTracingDataSource extends TracingDataSource {

	public AWSXRayTracingDataSource(DataSource dataSource) {
		super(dataSource);
	}

	@Override
	public Connection getConnection() throws SQLException {
		return AWSXRayTracingConnection.decorate(delegate.getConnection());
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		return AWSXRayTracingConnection.decorate(delegate.getConnection(username, password));
	}

}
