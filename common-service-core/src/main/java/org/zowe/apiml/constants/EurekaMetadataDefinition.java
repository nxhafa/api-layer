/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.constants;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.Optional;

public final class EurekaMetadataDefinition {

    private EurekaMetadataDefinition() {
        throw new IllegalStateException("Eureka metadata constant class");
    }

    public static final String CURRENT_VERSION = "2.2.0";

    //v2
    public static final String VERSION = "version";
    public static final String ROUTES = "apiml.routes";
    public static final String ROUTES_GATEWAY_URL = "gatewayUrl";
    public static final String ROUTES_SERVICE_URL = "serviceUrl";

    public static final String CATALOG_ID = "apiml.catalog.tile.id";
    public static final String CATALOG_VERSION = "apiml.catalog.tile.version";
    public static final String CATALOG_TITLE = "apiml.catalog.tile.title";
    public static final String CATALOG_DESCRIPTION = "apiml.catalog.tile.description";

    public static final String SERVICE_TITLE = "apiml.service.title";
    public static final String SERVICE_DESCRIPTION = "apiml.service.description";
    public static final String SERVICE_EXTERNAL_URL = "apiml.service.externalUrl";
    public static final String SERVICE_SUPPORTING_CLIENT_CERT_FORWARDING = "apiml.service.supportClientCertForwarding";
    public static final String ENABLE_URL_ENCODED_CHARACTERS = "apiml.enableUrlEncodedCharacters";
    public static final String APPLY_RATE_LIMITER_FILTER = "apiml.gateway.applyRateLimiterFilter";
    public static final String APIML_ID = "apiml.service.apimlId";
    public static final String REGISTRATION_TYPE = "apiml.registrationType";

    public static final String API_INFO = "apiml.apiInfo";
    public static final String API_INFO_API_ID = "apiId";
    public static final String API_INFO_GATEWAY_URL = "gatewayUrl";
    public static final String API_INFO_VERSION = "version";
    public static final String API_INFO_SWAGGER_URL = "swaggerUrl";
    public static final String API_INFO_GRAPHQL_URL = "graphqlUrl";
    public static final String API_INFO_DOCUMENTATION_URL = "documentationUrl";
    public static final String API_INFO_IS_DEFAULT = "defaultApi";

    public static final String CODE_SNIPPET = "codeSnippet";
    public static final String CODE_SNIPPET_ENDPOINT = "endpoint";
    public static final String CODE_SNIPPET_CODE_BLOCK = "codeBlock";
    public static final String CODE_SNIPPET_LANGUAGE = "language";

    public static final String AUTHENTICATION_SCHEME = "apiml.authentication.scheme";
    public static final String AUTHENTICATION_APPLID = "apiml.authentication.applid";
    public static final String AUTHENTICATION_SSO = "apiml.authentication.sso";
    public static final String AUTHENTICATION_HEADERS = "apiml.authentication.headers";
    //v1
    public static final String ROUTES_V1 = "routed-services";
    public static final String ROUTES_GATEWAY_URL_V1 = "gateway-url";
    public static final String ROUTES_SERVICE_URL_V1 = "service-url";

    public static final String CATALOG_ID_V1 = "mfaas.discovery.catalogUiTile.id";
    public static final String CATALOG_VERSION_V1 = "mfaas.discovery.catalogUiTile.version";
    public static final String CATALOG_TITLE_V1 = "mfaas.discovery.catalogUiTile.title";
    public static final String CATALOG_DESCRIPTION_V1 = "mfaas.discovery.catalogUiTile.description";

    public static final String SERVICE_TITLE_V1 = "mfaas.discovery.service.title";
    public static final String SERVICE_DESCRIPTION_V1 = "mfaas.discovery.service.description";
    public static final String ENABLE_APIDOC_V1 = "mfaas.discovery.enableApiDoc";

    public static final String API_VERSION_PROPERTIES_BASE_PACKAGE_V1 = "mfaas.api-info.apiVersionProperties.v1.basePackage";
    public static final String API_VERSION_PROPERTIES_TITLE_V1 = "mfaas.api-info.apiVersionProperties.v1.title";
    public static final String API_VERSION_PROPERTIES_VERSION_V1 = "mfaas.api-info.apiVersionProperties.v1.version";
    public static final String API_VERSION_PROPERTIES_DESCRIPTION_V1 = "mfaas.api-info.apiVersionProperties.v1.description";

    public interface BasicRegistrationType {

        default boolean isAdditional() {
            return false;
        }

        default boolean isPrimary() {
            return false;
        }

    }

    @RequiredArgsConstructor
    public enum RegistrationType implements BasicRegistrationType {
            PRIMARY("primary"),
            ADDITIONAL("additional")
        ;

        @Getter
        private final String value;

        public static RegistrationType of(String value) {
            for (RegistrationType registrationType : RegistrationType.values()) {
                if (StringUtils.equals(value, registrationType.getValue())) {
                    return registrationType;
                }
            }
            if (value == null) {
                return PRIMARY;
            }
            return null;
        }

        public static BasicRegistrationType of(Map<String, String> metadata) {
            return Optional.ofNullable(metadata)
                .map(m -> of(m.get(REGISTRATION_TYPE)))
                .map(BasicRegistrationType.class::cast)
                .orElse(new BasicRegistrationType() {});
        }

        public boolean isAdditional() {
            return this == ADDITIONAL;
        }

        public boolean isPrimary() {
            return this == PRIMARY;
        }

    }

}
