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

package de.adorsys.psd2.consent.service;

import de.adorsys.psd2.consent.api.service.EventService;
import de.adorsys.psd2.xs2a.core.event.Event;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Primary
@Service
@RequiredArgsConstructor
public class EventServiceInternalEncrypted implements EventService {
    private final EncryptionDecryptionService encryptionDecryptionService;
    private final EventService eventService;

    @Override
    public boolean recordEvent(@NotNull Event event) {
        String encryptedConsentId = event.getConsentId();
        String decryptedConsentId = Optional.ofNullable(encryptedConsentId)
                                        .flatMap(encryptionDecryptionService::decryptConsentId)
                                        .orElse(null);

        String encryptedPaymentId = event.getPaymentId();
        String decryptedPaymentId = Optional.ofNullable(encryptedPaymentId)
                                        .flatMap(encryptionDecryptionService::decryptPaymentId)
                                        .orElse(null);

        Event decryptedEvent = new Event(event.getTimestamp(), decryptedConsentId, decryptedPaymentId,
                                         event.getPayload(), event.getEventOrigin(), event.getEventType());

        return eventService.recordEvent(decryptedEvent);
    }
}
