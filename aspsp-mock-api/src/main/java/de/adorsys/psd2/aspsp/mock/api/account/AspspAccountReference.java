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

package de.adorsys.psd2.aspsp.mock.api.account;

import lombok.AllArgsConstructor;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.NonFinal;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceConstructor;

import java.util.Currency;

@Value
@AllArgsConstructor(onConstructor_ = {@PersistenceConstructor})
public class AspspAccountReference {
    @Id
    @Setter
    @NonFinal
    private String accountId;
    private String iban;
    private String bban;
    private String pan;
    private String maskedPan;
    private String msisdn;
    private Currency currency;

    /**
     * Creates AspspAccountReference by default
     *
     * @param aspspAccountId Bank specific account ID
     * @param currency       Currency according codes following ISO 4217
     */
    public AspspAccountReference(String aspspAccountId, Currency currency) {
        this(aspspAccountId, null, null, null, null, null, currency);
    }
}
