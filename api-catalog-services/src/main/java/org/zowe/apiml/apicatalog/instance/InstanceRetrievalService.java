/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.instance;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.converters.jackson.EurekaJsonJacksonCodec;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicHeader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.zowe.apiml.apicatalog.discovery.DiscoveryConfigProperties;
import org.zowe.apiml.constants.EurekaMetadataDefinition;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.instance.InstanceInitializationException;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;
import org.zowe.apiml.product.registry.ApplicationWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import static org.zowe.apiml.product.constants.CoreService.GATEWAY;

/**
 * Service for instance retrieval from Eureka
 */
@Slf4j
@Service
public class InstanceRetrievalService {

    private final DiscoveryConfigProperties discoveryConfigProperties;
    private final CloseableHttpClient httpClient;

    private static final String APPS_ENDPOINT = "apps/";
    private static final String DELTA_ENDPOINT = "delta";
    private static final String UNKNOWN = "unknown";

    @InjectApimlLogger
    private final ApimlLogger apimlLog = ApimlLogger.empty();

    private ObjectMapper mapper = new ObjectMapper();

    @Autowired
    public InstanceRetrievalService(DiscoveryConfigProperties discoveryConfigProperties,
                                    CloseableHttpClient httpClient) {
        this.discoveryConfigProperties = discoveryConfigProperties;
        this.httpClient = httpClient;
    }

    private InstanceInfo getInstanceInfo(String serviceId, AtomicBoolean instanceFound, Predicate<InstanceInfo> selector) {
        List<EurekaServiceInstanceRequest> eurekaServiceInstanceRequests = constructServiceInfoQueryRequest(serviceId, false);
        // iterate over list of discovery services, return at first success
        for (EurekaServiceInstanceRequest eurekaServiceInstanceRequest : eurekaServiceInstanceRequests) {
            // call Eureka REST endpoint to fetch single or all Instances
            try {
                String responseBody = queryDiscoveryForInstances(eurekaServiceInstanceRequest);
                if (responseBody != null) {
                    instanceFound.set(true);
                    return extractSingleInstanceFromApplication(serviceId, responseBody, selector);
                }
            } catch (Exception e) {
                log.debug("Error obtaining instance information from {}, error message: {}",
                    eurekaServiceInstanceRequest.getEurekaRequestUrl(), e.getMessage());
            }
        }
        return null;
    }

    /**
     * Retrieves {@link InstanceInfo} of particular service
     *
     * @param serviceId the service to search for
     * @return service instance
     */
    public InstanceInfo getInstanceInfo(@NotBlank(message = "Service Id must be supplied") String serviceId) {
        if (serviceId.equalsIgnoreCase(UNKNOWN)) {
            return null;
        }

        // identification if there was no instance or any error happened during fetching
        AtomicBoolean instanceFound = new AtomicBoolean(false);
        InstanceInfo instanceInfo = getInstanceInfo(serviceId, instanceFound,
            ii -> EurekaMetadataDefinition.RegistrationType.of(ii.getMetadata()).isPrimary()
        );
        if (instanceInfo == null) {
            // maybe the input is apimlId, try to find the matching Gateway (multi-tenancy use case)
            instanceInfo = getInstanceInfo(GATEWAY.getServiceId(), instanceFound,
                ii -> EurekaMetadataDefinition.RegistrationType.of(ii.getMetadata()).isAdditional()
            );
        }

        if (!instanceFound.get()) {
            String msg = "An error occurred when trying to get instance info for:  " + serviceId;
            throw new InstanceInitializationException(msg);
        }

        return instanceInfo;
    }

    /**
     * Retrieve instances from the discovery service
     *
     * @param delta filter the registry information to the just updated infos
     * @return the Applications object that wraps all the registry information
     */
    public Applications getAllInstancesFromDiscovery(boolean delta) {

        List<EurekaServiceInstanceRequest> requestInfoList = constructServiceInfoQueryRequest(null, delta);
        for (EurekaServiceInstanceRequest requestInfo : requestInfoList) {
            try {
                String responseBody = queryDiscoveryForInstances(requestInfo);
                return extractApplications(responseBody);
            } catch (Exception e) {
                log.debug("Not able to contact discovery service: {}", requestInfo.getEurekaRequestUrl(), e);
            }
        }
        //  call Eureka REST endpoint to fetch single or all Instances
        return null;
    }

    /**
     * Parse information from the response and extract the Applications object which contains all the registry information returned by eureka server
     *
     * @param responseBody the http response body
     * @return Applications object that wraps all the registry information
     */
    private Applications extractApplications(String responseBody) {
        Applications applications = null;
        ObjectMapper mapper = new EurekaJsonJacksonCodec().getObjectMapper(Applications.class);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        try {
            applications = mapper.readValue(responseBody, Applications.class);
        } catch (IOException e) {
            apimlLog.log("org.zowe.apiml.apicatalog.serviceRetrievalParsingFailed", e.getMessage());
        }

        return applications;
    }

    /**
     * Query Discovery
     *
     * @param eurekaServiceInstanceRequest information used to query the discovery service
     * @return ResponseEntity<String> query response
     */
    String queryDiscoveryForInstances(EurekaServiceInstanceRequest eurekaServiceInstanceRequest) throws IOException {
        HttpGet httpGet = new HttpGet(eurekaServiceInstanceRequest.getEurekaRequestUrl());
        for (Header header : createRequestHeader(eurekaServiceInstanceRequest)) {
            httpGet.setHeader(header);
        }

        return httpClient.execute(httpGet, response -> {
            final int statusCode = response.getCode();
            final HttpEntity responseEntity = response.getEntity();

            String responseBody = "";
            if (responseEntity != null) {
                responseBody = EntityUtils.toString(responseEntity, StandardCharsets.UTF_8);
            }

            if (HttpStatus.valueOf(statusCode).is2xxSuccessful()) {
                return responseBody;
            }

            apimlLog.log("org.zowe.apiml.apicatalog.serviceRetrievalRequestFailed",
                eurekaServiceInstanceRequest.getServiceId(),
                eurekaServiceInstanceRequest.getEurekaRequestUrl(),
                statusCode,
                response.getReasonPhrase() != null ? response.getReasonPhrase() : responseBody
            );

            return null;
        });
    }

    /**
     * @param serviceId    the service to search for
     * @param responseBody the fetch attempt response body
     * @return service instance
     */
    InstanceInfo extractSingleInstanceFromApplication(String serviceId, String responseBody, Predicate<InstanceInfo> selector) {
        ApplicationWrapper application = null;
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        try {
            application = mapper.readValue(responseBody, ApplicationWrapper.class);
        } catch (IOException e) {
            log.debug("Could not extract service: {} info from discovery --{}", serviceId, e.getMessage(), e);
        }

        return Optional.ofNullable(application)
            .map(ApplicationWrapper::getApplication)
            .map(Application::getInstances)
            .orElse(Collections.emptyList())
            .stream().filter(selector)
            .findFirst()
            .orElse(null);
    }

    /**
     * Construct a tuple used to query the discovery service
     *
     * @param serviceId optional service id
     * @return request information
     */
    private List<EurekaServiceInstanceRequest> constructServiceInfoQueryRequest(String serviceId, boolean getDelta) {
        String[] discoveryServiceUrls = discoveryConfigProperties.getLocations();
        List<EurekaServiceInstanceRequest> eurekaServiceInstanceRequests = new ArrayList<>(discoveryServiceUrls.length);
        for (String discoveryUrl : discoveryServiceUrls) {
            String discoveryServiceLocatorUrl = discoveryUrl + APPS_ENDPOINT;
            if (getDelta) {
                discoveryServiceLocatorUrl += DELTA_ENDPOINT;
            } else {
                if (serviceId != null) {
                    discoveryServiceLocatorUrl += serviceId.toLowerCase();
                }
            }

            String eurekaUsername = discoveryConfigProperties.getEurekaUserName();
            String eurekaUserPassword = discoveryConfigProperties.getEurekaUserPassword();

            log.debug("Querying instance information of the service {} from the URL {} with the user {} and password {}",
                serviceId, discoveryServiceLocatorUrl, eurekaUsername,
                StringUtils.isEmpty(eurekaUserPassword) ? "NO PASSWORD" : "*******");

            EurekaServiceInstanceRequest eurekaServiceInstanceRequest = EurekaServiceInstanceRequest.builder()
                .serviceId(serviceId)
                .eurekaRequestUrl(discoveryServiceLocatorUrl)
                .username(eurekaUsername)
                .password(eurekaUserPassword)
                .build();
            eurekaServiceInstanceRequests.add(eurekaServiceInstanceRequest);
        }

        return eurekaServiceInstanceRequests;
    }

    /**
     * Create HTTP headers
     *
     * @return HTTP Headers
     */
    private List<Header> createRequestHeader(EurekaServiceInstanceRequest eurekaServiceInstanceRequest) {
        List<Header> headers = new ArrayList<>();
        if (eurekaServiceInstanceRequest != null && eurekaServiceInstanceRequest.getUsername() != null && eurekaServiceInstanceRequest.getPassword() != null) {
            String basicToken = "Basic " + Base64.getEncoder().encodeToString((eurekaServiceInstanceRequest.getUsername() + ":"
                + eurekaServiceInstanceRequest.getPassword()).getBytes());
            headers.add(new BasicHeader(HttpHeaders.AUTHORIZATION, basicToken));
        }
        headers.add(new BasicHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));
        headers.add(new BasicHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE));
        return headers;
    }
}
