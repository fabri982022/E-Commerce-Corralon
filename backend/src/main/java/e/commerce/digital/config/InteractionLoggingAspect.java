package e.commerce.digital.config;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class InteractionLoggingAspect {

    @Around("execution(public * e.commerce.digital..*(..))"
            + " && (@within(org.springframework.stereotype.Service)"
            + " || @within(org.springframework.web.bind.annotation.RestController))")
    public Object logInteraction(ProceedingJoinPoint joinPoint) throws Throwable {
        String layer = joinPoint.getTarget().getClass().getPackageName().contains(".controller")
                ? "controller"
                : "service";
        String operation = joinPoint.getTarget().getClass().getSimpleName()
                + "." + joinPoint.getSignature().getName();
        long startedAt = System.nanoTime();

        log.info("{} {} started", layer, operation);
        try {
            Object result = joinPoint.proceed();
            log.info("{} {} completed durationMs={}",
                    layer, operation, elapsedMillis(startedAt));
            return result;
        } catch (Throwable ex) {
            log.error("{} {} failed durationMs={} exception={}",
                    layer, operation, elapsedMillis(startedAt), ex.getClass().getSimpleName(), ex);
            throw ex;
        }
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
