package com.ep.auth.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

@Aspect
@Component
public class ExecutionTimeAspect {
    private static final Logger log = LoggerFactory.getLogger(ExecutionTimeAspect.class);

    @Around("execution(public * com.ep.auth..*(..))"
            + " && !within(com.ep.auth.config..*)"
            + " && !within(com.ep.auth.domain..*)"
            + " && !within(com.ep.auth.dto..*)")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            return joinPoint.proceed();
        } finally {
            stopWatch.stop();
            log.info("method={} elapsedMs={}", joinPoint.getSignature().toShortString(), stopWatch.getTotalTimeMillis());
        }
    }
}
