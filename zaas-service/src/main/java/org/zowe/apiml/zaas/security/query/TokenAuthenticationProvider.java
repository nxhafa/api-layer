/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.security.query;

import org.zowe.apiml.security.common.token.TokenAuthentication;
import org.zowe.apiml.zaas.security.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Authentication provider that verifies the JWT token
 */
@Component
@RequiredArgsConstructor
public class TokenAuthenticationProvider implements AuthenticationProvider {
    private final AuthenticationService authenticationService;

    /**
     * Authenticate the token
     *
     * @param authentication that was presented to the provider for validation
     * @return the authenticated token
     */
    @Override
    public Authentication authenticate(Authentication authentication) {
        TokenAuthentication tokenAuthentication = (TokenAuthentication) authentication;
        return authenticationService.validateJwtToken(tokenAuthentication);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return TokenAuthentication.class.isAssignableFrom(authentication);
    }
}
