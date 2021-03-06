package com.nepxion.discovery.plugin.strategy.service.aop;

/**
 * <p>Title: Nepxion Discovery</p>
 * <p>Description: Nepxion Discovery</p>
 * <p>Copyright: Copyright (c) 2017-2050</p>
 * <p>Company: Nepxion</p>
 * @author Haojun Ren
 * @version 1.0
 */

import feign.RequestInterceptor;
import feign.RequestTemplate;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.nepxion.discovery.common.constant.DiscoveryConstant;
import com.nepxion.discovery.common.util.StringUtil;
import com.nepxion.discovery.plugin.framework.adapter.PluginAdapter;
import com.nepxion.discovery.plugin.strategy.service.constant.ServiceStrategyConstant;
import com.nepxion.discovery.plugin.strategy.service.context.ServiceStrategyContextHolder;

public class FeignStrategyInterceptor implements RequestInterceptor {
    private static final Logger LOG = LoggerFactory.getLogger(FeignStrategyInterceptor.class);

    @Autowired
    private ConfigurableEnvironment environment;

    @Autowired
    private PluginAdapter pluginAdapter;

    @Autowired
    private ServiceStrategyContextHolder serviceStrategyContextHolder;

    private List<String> requestHeaderList = new ArrayList<String>();

    public FeignStrategyInterceptor(String requestHeaders) {
        LOG.info("------------- Feign Intercept Information -----------");
        if (StringUtils.isNotEmpty(requestHeaders)) {
            requestHeaderList.addAll(StringUtil.splitToList(requestHeaders.toLowerCase(), DiscoveryConstant.SEPARATE));
        }
        if (!requestHeaderList.contains(DiscoveryConstant.N_D_VERSION)) {
            requestHeaderList.add(DiscoveryConstant.N_D_VERSION);
        }
        if (!requestHeaderList.contains(DiscoveryConstant.N_D_REGION)) {
            requestHeaderList.add(DiscoveryConstant.N_D_REGION);
        }
        if (!requestHeaderList.contains(DiscoveryConstant.N_D_ADDRESS)) {
            requestHeaderList.add(DiscoveryConstant.N_D_ADDRESS);
        }
        if (!requestHeaderList.contains(DiscoveryConstant.N_D_VERSION_WEIGHT)) {
            requestHeaderList.add(DiscoveryConstant.N_D_VERSION_WEIGHT);
        }
        if (!requestHeaderList.contains(DiscoveryConstant.N_D_REGION_WEIGHT)) {
            requestHeaderList.add(DiscoveryConstant.N_D_REGION_WEIGHT);
        }
        LOG.info("Feign intercepted headers are {}", StringUtils.isNotEmpty(requestHeaders) ? requestHeaders : "empty");
        LOG.info("-------------------------------------------------");
    }

    @Override
    public void apply(RequestTemplate requestTemplate) {
        applyInnerHeader(requestTemplate);
        applyOuterHeader(requestTemplate);
    }

    private void applyInnerHeader(RequestTemplate requestTemplate) {
        requestTemplate.header(DiscoveryConstant.N_D_SERVICE_ID, pluginAdapter.getServiceId());
        requestTemplate.header(DiscoveryConstant.N_D_GROUP, pluginAdapter.getGroup());
    }

    private void applyOuterHeader(RequestTemplate requestTemplate) {
        if (CollectionUtils.isEmpty(requestHeaderList)) {
            return;
        }

        ServletRequestAttributes attributes = serviceStrategyContextHolder.getRestAttributes();
        if (attributes == null) {
            return;
        }

        HttpServletRequest previousRequest = attributes.getRequest();
        Enumeration<String> headerNames = previousRequest.getHeaderNames();
        if (headerNames == null) {
            return;
        }

        Boolean interceptLogPrint = environment.getProperty(ServiceStrategyConstant.SPRING_APPLICATION_STRATEGY_INTERCEPT_LOG_PRINT, Boolean.class, Boolean.FALSE);
        if (interceptLogPrint) {
            LOG.info("------------- Feign Route Information -----------");
        }
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String header = previousRequest.getHeader(headerName);

            if (requestHeaderList.contains(headerName.toLowerCase())) {
                if (interceptLogPrint) {
                    LOG.info("{}={}", headerName, header);
                }
                requestTemplate.header(headerName, header);
            }
        }
        if (interceptLogPrint) {
            LOG.info("-------------------------------------------------");
        }
    }
}