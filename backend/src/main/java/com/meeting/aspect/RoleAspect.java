package com.meeting.aspect;

import com.meeting.annotation.RequireRole;
import com.meeting.common.exception.BusinessException;
import com.meeting.entity.SysUser;
import com.meeting.service.SysUserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Arrays;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RoleAspect {

    private final SysUserService userService;

    @Around("@annotation(com.meeting.annotation.RequireRole) || @within(com.meeting.annotation.RequireRole)")
    public Object checkRole(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        RequireRole annotation = method.getAnnotation(RequireRole.class);
        if (annotation == null) {
            annotation = joinPoint.getTarget().getClass().getAnnotation(RequireRole.class);
        }

        if (annotation != null) {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                throw new BusinessException("无法获取请求信息");
            }

            HttpServletRequest request = attributes.getRequest();
            Long userId = (Long) request.getAttribute("userId");

            if (userId == null) {
                throw new BusinessException(401, "请先登录");
            }

            SysUser user = userService.getById(userId);
            if (user == null) {
                throw new BusinessException("用户不存在");
            }

            Integer userType = user.getUserType();
            String[] requiredRoles = annotation.value();

            boolean hasRole = false;
            for (String role : requiredRoles) {
                int roleCode = Integer.parseInt(role);
                if (userType != null && userType == roleCode) {
                    hasRole = true;
                    break;
                }
            }

            if (!hasRole) {
                log.warn("用户 {} 尝试访问需要角色 {} 的接口，但用户类型为 {}", userId,
                    Arrays.toString(requiredRoles), userType);
                throw new BusinessException(403, annotation.message());
            }
        }

        return joinPoint.proceed();
    }
}
