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

import de.adorsys.psd2.consent.api.service.TppStopListService;
import de.adorsys.psd2.consent.domain.TppStopListEntity;
import de.adorsys.psd2.consent.repository.TppStopListRepository;
import de.adorsys.psd2.xs2a.core.tpp.TppUniqueParamsHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TppStopListServiceInternal implements TppStopListService {
    private final TppStopListRepository tppStopListRepository;

    @Override
    public boolean checkIfTppBlocked(TppUniqueParamsHolder tppUniqueParams) {
        Optional<TppStopListEntity> stopListEntityOptional = tppStopListRepository.findByTppAuthorisationNumberAndNationalAuthorityId(tppUniqueParams.getAuthorisationNumber(), tppUniqueParams.getAuthorityId());

        return stopListEntityOptional
                   .map(TppStopListEntity::isBlocked)
                   .orElse(false);
    }
}