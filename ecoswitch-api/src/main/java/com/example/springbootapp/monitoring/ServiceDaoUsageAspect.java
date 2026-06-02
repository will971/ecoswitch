package com.example.springbootapp.monitoring;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ServiceDaoUsageAspect {

	private final UsageMonitor usageMonitor;

	public ServiceDaoUsageAspect(UsageMonitor usageMonitor) {
		this.usageMonitor = usageMonitor;
	}

	@Around("within(@org.springframework.stereotype.Service *) || within(@org.springframework.stereotype.Repository *)")
	public Object monitor(ProceedingJoinPoint joinPoint) throws Throwable {
		long startNanos = System.nanoTime();
		try {
			return joinPoint.proceed();
		} finally {
			long elapsed = System.nanoTime() - startNanos;
			String component = joinPoint.getSignature().getDeclaringTypeName();
			String methodName = joinPoint.getSignature().getName();
			usageMonitor.record(component, methodName, elapsed);
		}
	}
}
