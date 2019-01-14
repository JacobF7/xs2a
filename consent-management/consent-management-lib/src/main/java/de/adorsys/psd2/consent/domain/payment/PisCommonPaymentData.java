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

package de.adorsys.psd2.consent.domain.payment;

import de.adorsys.psd2.consent.domain.InstanceDependableEntity;
import de.adorsys.psd2.consent.domain.PsuData;
import de.adorsys.psd2.consent.domain.TppInfoEntity;
import de.adorsys.psd2.xs2a.core.pis.TransactionStatus;
import de.adorsys.psd2.xs2a.core.profile.PaymentType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.persistence.*;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity(name = "pis_common_payment")
@ApiModel(description = "pis common payment entity", value = "PisCommonPaymentData")
public class PisCommonPaymentData extends InstanceDependableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pis_common_payment_generator")
    @SequenceGenerator(name = "pis_common_payment_generator", sequenceName = "pis_common_payment_id_seq")
    private Long id;

    @Column(name = "payment_id", nullable = false)
    private String paymentId;

    @Column(name = "payment_type", nullable = false)
    @Enumerated(value = EnumType.STRING)
    @ApiModelProperty(value = "Payment type: BULK, SINGLE or PERIODIC.", required = true, example = "SINGLE")
    private PaymentType paymentType;

    @Column(name = "payment_product", nullable = false)
    @ApiModelProperty(value = "Payment product", required = true, example = "sepa-credit-transfers")
    private String paymentProduct;

    @Column(name = "transaction_status", nullable = false)
    @Enumerated(value = EnumType.STRING)
    @ApiModelProperty(name = "transactionStatus", example = "ACCP", required = true)
    private TransactionStatus transactionStatus;

    @Lob
    @Column(name = "payment", nullable = false)
    @ApiModelProperty(value = "All data about the payment", required = true)
    private byte[] payment;

    @OneToMany(cascade = CascadeType.ALL)
    @ApiModelProperty(value = "List of PSU", required = true)
    private List<PsuData> psuData = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "tpp_info_id", nullable = false)
    @ApiModelProperty(value = "Information about TPP", required = true)
    private TppInfoEntity tppInfo;

    @OneToMany(mappedBy = "paymentData",
        cascade = CascadeType.ALL,
        orphanRemoval = true)
    @ApiModelProperty(value = "List of authorizations", required = true)
    private List<PisAuthorization> authorizations = new ArrayList<>();

    @OneToMany(mappedBy = "paymentData",
        cascade = CascadeType.ALL,
        orphanRemoval = true)
    @ApiModelProperty(value = "List of single payments ", required = true)
    private List<PisPaymentData> payments = new ArrayList<>();

    @Column(name = "creation_timestamp")
    @ApiModelProperty(value = "Creation timestamp of the consent.", required = true, example = "2018-12-28T00:00:00Z")
    private OffsetDateTime creationTimestamp = OffsetDateTime.now();

    @Column(name = "aspsp_account_id", length = 100)
    @ApiModelProperty(value = "Aspsp-Account-ID: Bank specific account ID", example = "DE2310010010123456789")
    private String aspspAccountId;

    public boolean isConfirmationExpired(long expirationPeriodMs) {
        if (isNotConfirmed()) {
            return creationTimestamp.plus(expirationPeriodMs, ChronoUnit.MILLIS)
                       .isBefore(OffsetDateTime.now());
        }

        return false;
    }

    public boolean isNotConfirmed() {
        return transactionStatus == TransactionStatus.RCVD;
    }
}

