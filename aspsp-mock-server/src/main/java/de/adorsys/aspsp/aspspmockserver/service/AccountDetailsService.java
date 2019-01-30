/*
 * Copyright 2018-2019 adorsys GmbH & Co KG
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

package de.adorsys.aspsp.aspspmockserver.service;

import de.adorsys.aspsp.aspspmockserver.repository.AccountDetailsRepository;
import de.adorsys.psd2.aspsp.mock.api.account.AspspAccountDetails;
import de.adorsys.psd2.aspsp.mock.api.account.AspspAccountReference;
import de.adorsys.psd2.aspsp.mock.api.psu.AspspPsuData;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@AllArgsConstructor
public class AccountDetailsService {
    private final AccountDetailsRepository accountDetailsRepository;

    public List<AspspPsuData> getPsuDataByAccount(AspspAccountReference debtorAccount) {
        return accountDetailsRepository.findByIbanAndCurrency(debtorAccount.getIban(), debtorAccount.getCurrency())
                   .stream()
                   .findFirst()
                   .map(AspspAccountDetails::getPsuDataList)
                   .orElseGet(Collections::emptyList);
    }
}

