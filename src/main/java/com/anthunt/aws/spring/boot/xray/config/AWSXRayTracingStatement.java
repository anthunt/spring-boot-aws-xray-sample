package com.anthunt.aws.spring.boot.xray.config;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Namespace;
import com.amazonaws.xray.entities.Subsegment;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AWSXRayTracingStatement {

    /**
     * Call {@code statement = AWSXRayTracingStatement.decorateStatement(statement)} to decorate your {@link Statement}
     * in order to have the queries recorded with an X-Ray Subsegment. Do not use the method on {@link PreparedStatement}
     * and {@link CallableStatement}. Use another two specific decorating method instead.
     *
     * @param statement the statement to decorate
     * @return a {@link Statement} that traces all SQL queries in X-Ray
     */
    public static Statement decorateStatement(Statement statement) {
        return (Statement) Proxy.newProxyInstance(AWSXRayTracingStatement.class.getClassLoader(),
                new Class[] { Statement.class },
                new TracingStatementHandler(statement));
    }

    /**
     * Call {@code preparedStatement = AWSXRayTracingStatement.decoratePreparedStatement(preparedStatement, sql)}
     * to decorate your {@link PreparedStatement} in order to have the queries recorded with an X-Ray Subsegment.
     *
     * @param statement the {@link PreparedStatement} to decorate
     * @param sql the sql query to execute
     * @return a {@link PreparedStatement} that traces all SQL queries in X-Ray
     */
    public static PreparedStatement decoratePreparedStatement(PreparedStatement statement, String sql) {
        return (PreparedStatement) Proxy.newProxyInstance(AWSXRayTracingStatement.class.getClassLoader(),
                new Class[] { PreparedStatement.class },
                new TracingStatementHandler(statement));
    }

    /**
     * Call {@code callableStatement = AWSXRayTracingStatement.decorateCallableStatement(callableStatement, sql)}
     * to decorate your {@link CallableStatement}in order to have the queries recorded with an X-Ray Subsegment.
     *
     * @param statement the {@link CallableStatement} to decorate
     * @param sql the sql query to execute
     * @return a {@link CallableStatement} that traces all SQL queries in X-Ray
     */
    public static CallableStatement decorateCallableStatement(CallableStatement statement, String sql) {
        return (CallableStatement) Proxy.newProxyInstance(AWSXRayTracingStatement.class.getClassLoader(),
                new Class[] { CallableStatement.class },
                new TracingStatementHandler(statement));
    }

    private static class TracingStatementHandler implements InvocationHandler {

        private static final String DEFAULT_DATABASE_NAME = "database";
        private static final String EXECUTE = "execute";
        private static final String EXECUTE_QUERY = "executeQuery";
        private static final String EXECUTE_UPDATE = "executeUpdate";
        private static final String EXECUTE_BATCH = "executeBatch";
        private static final String URL = "url";
        private static final String USER = "user";
        private static final String QUERY = "query";
        private static final String METHOD = "method";
        private static final String DRIVER_VERSION = "driver_version";
        private static final String DATABASE_TYPE = "database_type";
        private static final String DATABASE_VERSION = "database_version";

        private final Statement delegate;

        TracingStatementHandler(Statement statement) {
            this.delegate = statement;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (!isExecution(method)) {
                // don't trace non execution methods
                return method.invoke(delegate, args);
            }

            Subsegment subsegment = createSubsegment(proxy, method, args);
            if (subsegment == null) {
                // don't trace if failed to create subsegment
                return method.invoke(delegate, args);
            }

            log.debug("Invoking statement execution with X-Ray tracing.");
            try {
                // execute the query "wrapped" in a XRay Subsegment
                return method.invoke(delegate, args);
            } catch (Throwable t) {
                if (t instanceof InvocationTargetException) {
                    // the reflection may wrap the actual error with an InvocationTargetException.
                    // we want to use the root cause to make the instrumentation seamless
                    InvocationTargetException ite = (InvocationTargetException) t;
                    if (ite.getTargetException() != null) {
                        subsegment.addException(ite.getTargetException());
                        throw ite.getTargetException();
                    }
                    if (ite.getCause() != null) {
                        subsegment.addException(ite.getCause());
                        throw ite.getCause();
                    }
                    subsegment.addException(ite);
                    throw ite;
                }

                subsegment.addException(t);
                throw t;
            } finally {
                AWSXRay.endSubsegment();
            }
        }

        private boolean isExecution(Method method) {
            return EXECUTE.equals(method.getName())
                    || EXECUTE_QUERY.equals(method.getName())
                    || EXECUTE_UPDATE.equals(method.getName())
                    || EXECUTE_BATCH.equals(method.getName());
        }

        private Subsegment createSubsegment(Object proxy, Method method, Object[] args) {
            try {
                Connection connection = delegate.getConnection();
                DatabaseMetaData dbmetadata = connection.getMetaData();
                String subsegmentName = "ExecuteQuery";
                try {
                    URI normalizedURI = new URI(new URI(dbmetadata.getURL()).getSchemeSpecificPart());
                    subsegmentName += "-" + connection.getCatalog() + "@" + Optional.ofNullable(normalizedURI.getHost()).orElseGet(()->"Memory");
                } catch (URISyntaxException e) {
                    log.warn("Unable to parse database URI. Falling back to default '" + DEFAULT_DATABASE_NAME + "' for subsegment name.", e);
                }
                
                Subsegment subsegment = AWSXRay.beginSubsegment(subsegmentName);
                if (subsegment == null) {
                    return null;
                }

                subsegment.setNamespace(Namespace.REMOTE.toString());
                Map<String, Object> sqlParams = new HashMap<>();
                sqlParams.put(URL, dbmetadata.getURL());
                sqlParams.put(USER, dbmetadata.getUserName());
                sqlParams.put(DRIVER_VERSION, dbmetadata.getDriverVersion());
                sqlParams.put(DATABASE_TYPE, dbmetadata.getDatabaseProductName());
                sqlParams.put(DATABASE_VERSION, dbmetadata.getDatabaseProductVersion());
                subsegment.putAllSql(sqlParams);
                
                Map<String, Map<String, Object>> metadata = new HashMap<>();
                Map<String, Object> queryInfo = new HashMap<>();
                queryInfo.put(QUERY, delegate.toString());
                queryInfo.put(METHOD, method.getName());
                metadata.put("queryInfo", queryInfo);
                subsegment.setMetadata(metadata);
                
                return subsegment;
            } catch (Exception exception) {
                log.warn("Failed to create X-Ray subsegment for the statement execution.", exception);
                return null;
            }
        }
        
    }
        
}
