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

package de.adorsys.psd2.xs2a.service;

import de.adorsys.psd2.xs2a.core.event.EventType;
import de.adorsys.psd2.xs2a.core.sca.ScaStatus;
import de.adorsys.psd2.xs2a.domain.MessageErrorCode;
import de.adorsys.psd2.xs2a.domain.ResponseObject;
import de.adorsys.psd2.xs2a.exception.MessageError;
import de.adorsys.psd2.xs2a.service.authorization.pis.PisScaAuthorisationService;
import de.adorsys.psd2.xs2a.service.event.Xs2aEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentAuthorisationService {
    private final Xs2aEventService xs2aEventService;
    private final PisScaAuthorisationService pisScaAuthorisationService;

    /**
     * Gets SCA status of payment initiation authorisation
     *
     * @param paymentId       ASPSP identifier of the payment, associated with the authorisation
     * @param authorisationId authorisation identifier
     * @return Response containing SCA status of authorisation or corresponding error
     */
    public ResponseObject<ScaStatus> getPaymentInitiationAuthorisationScaStatus(String paymentId, String authorisationId) {
        xs2aEventService.recordPisTppRequest(paymentId, EventType.GET_PAYMENT_AUTHORISATION_SCA_STATUS);

        Optional<ScaStatus> scaStatus = pisScaAuthorisationService.getAuthorisationScaStatus(paymentId, authorisationId);

        if (!scaStatus.isPresent()) {
            return ResponseObject.<ScaStatus>builder()
                       .fail(new MessageError(MessageErrorCode.RESOURCE_UNKNOWN_403))
                       .build();
        }

        return ResponseObject.<ScaStatus>builder()
                   .body(scaStatus.get())
                   .build();
    }
}