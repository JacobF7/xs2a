/*
 * Copyright 2018-2018 adorsys GmbH & Co KG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.adorsys.psd2.xs2a.service.validator.tpp;

import de.adorsys.psd2.xs2a.core.tpp.TppInfo;
import de.adorsys.psd2.xs2a.core.tpp.TppRole;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class TppRoleValidationService {
    private static Map<TppRole, Set<String>> tppRoleAccess;

    static {
        tppRoleAccess = TppRoleAccess.builder()
                            .linkTppRolePatterns(TppRole.AISP,
                                "/api/v1/accounts",
                                "/v1/accounts",
                                "/api/v1/consents",
                                "/v1/consents")
                            .linkTppRolePatterns(TppRole.PISP,
                                "/api/v1/bulk-payments",
                                "/v1/bulk-payments",
                                "/api/v1/payments",
                                "/v1/payments",
                                "/api/v1/periodic-payments",
                                "/v1/periodic-payments")
                            .linkTppRolePatterns(TppRole.PIISP,
                                "/api/v1/funds-confirmations",
                                "/v1/funds-confirmations")
                            .build();
    }

    public boolean hasAccess(TppInfo tppInfo, HttpServletRequest request) {
        List<TppRole> xs2aTppRoles = tppInfo.getTppRoles();
        if (CollectionUtils.isEmpty(xs2aTppRoles)) {
            return false;
        }

        for (TppRole role : xs2aTppRoles) {
            TppRole tppRole = TppRole.valueOf(role.name());
            if (tppRoleAccess.containsKey(tppRole) && matches(request, tppRoleAccess.get(tppRole))) {
                return true;
            }
        }

        return false;
    }

    private boolean matches(HttpServletRequest request, Set<String> paths) {
        for (String path : paths) {
            String targetPath = request.getRequestURI();
            if (targetPath.startsWith(path)) {
                return true;
            }
        }
        return false;
    }
}
