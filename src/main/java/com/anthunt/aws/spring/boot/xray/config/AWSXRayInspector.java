package com.anthunt.aws.spring.boot.xray.config;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Subsegment;
import com.amazonaws.xray.spring.aop.AbstractXRayInterceptor;
import com.amazonaws.xray.spring.aop.XRayInterceptorUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Aspect
@Component
public class AWSXRayInspector extends AbstractXRayInterceptor {    
	  
	@Override    
	@Pointcut("@within(com.amazonaws.xray.spring.aop.XRayEnabled) && (bean(*Controller) || bean(*Service) || bean(*Client) || bean(*Mapper))")    
	public void xrayEnabledClasses() {}

	
	@Override
	protected Object processXRayTrace(ProceedingJoinPoint pjp) throws Throwable {
		try {
            Subsegment subsegment = AWSXRay.beginSubsegment(pjp.getSignature().getDeclaringTypeName() + "." + pjp.getSignature().getName());
            log.trace("Begin aws xray subsegment");
            
            Optional.ofNullable(subsegment)
            		.ifPresent(s->s.setMetadata(generateMetadata(pjp, subsegment)));
            
            Object result = XRayInterceptorUtils.conditionalProceed(pjp);            
            Optional.ofNullable(result)
            		.ifPresent(r->{
            			Map<String, Object> resultMeta = new HashMap<>();
        	            resultMeta.put(result.getClass().getCanonicalName(), result);
        	            subsegment.getMetadata().put("Result", resultMeta);
            		});
            
            return result;
        } catch (Exception e) {
            AWSXRay.getCurrentSegment().addException(e);
            throw e;
        } finally {
            log.trace("Ending aws xray subsegment");
            AWSXRay.endSubsegment();
        }
	}


	@Override
	protected Map<String, Map<String, Object>> generateMetadata(ProceedingJoinPoint pjp, Subsegment subsegment) {
		log.trace("aws xray tracing method - {}.{}", pjp.getSignature().getDeclaringTypeName(), pjp.getSignature().getName());
		
		Map<String, Map<String, Object>> metadata = super.generateMetadata(pjp, subsegment);
		metadata.get("ClassInfo").put("Class", pjp.getSignature().getDeclaringTypeName());
		
		Map<String, Object> argumentsInfo = new HashMap<>();
		
		Arrays.stream(pjp.getArgs())
			  .forEach(arg->argumentsInfo.put(arg.getClass().getSimpleName(), arg));

		metadata.put("Arguments", argumentsInfo);
		metadata.get("ClassInfo").put("Package", pjp.getSignature().getDeclaringType().getPackage().getName());
		metadata.get("ClassInfo").put("Method", pjp.getSignature().getName());
		
		return metadata;
	}
  
}