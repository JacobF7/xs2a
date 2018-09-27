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

package de.adorsys.aspsp.xs2a.web;

import de.adorsys.aspsp.xs2a.domain.ResponseObject;
import de.adorsys.aspsp.xs2a.domain.Xs2aBookingStatus;
import de.adorsys.aspsp.xs2a.domain.account.Xs2aAccountReport;
import de.adorsys.aspsp.xs2a.domain.account.Xs2aTransactionsReport;
import de.adorsys.aspsp.xs2a.service.AccountService;
import de.adorsys.aspsp.xs2a.service.mapper.AccountModelMapper;
import de.adorsys.aspsp.xs2a.service.mapper.ResponseMapper;
import de.adorsys.psd2.api.AccountApi;
import de.adorsys.psd2.model.*;
import io.swagger.annotations.*;
import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@RestController
@AllArgsConstructor
@Api(value = "v1", description = "Provides access to the account information", tags = {"Account Information Service (AIS)"})
public class AccountController implements AccountApi {

    private final AccountService accountService;
    private final ResponseMapper responseMapper;
    private final AccountModelMapper accountModelMapper;

    @Override
    public ResponseEntity<?> getAccountList(UUID xRequestID, String consentID, Boolean withBalance, String digest, String signature, byte[] tpPSignatureCertificate, String psUIPAddress, Object psUIPPort, String psUAccept, String psUAcceptCharset, String psUAcceptEncoding, String psUAcceptLanguage, String psUUserAgent, String psUHttpMethod, UUID psUDeviceID, String psUGeoLocation) {
        return responseMapper.ok(accountService.getAccountDetailsList(consentID, Optional.ofNullable(withBalance).orElse(false)), accountModelMapper::mapToAccountList);
    }

    @Override
    public ResponseEntity<?> readAccountDetails(String accountId, UUID xRequestID, String consentID, Boolean withBalance, String digest, String signature, byte[] tpPSignatureCertificate, String psUIPAddress, Object psUIPPort, String psUAccept, String psUAcceptCharset, String psUAcceptEncoding, String psUAcceptLanguage, String psUUserAgent, String psUHttpMethod, UUID psUDeviceID, String psUGeoLocation) {
        return responseMapper.ok(accountService.getAccountDetails(consentID, accountId, Optional.ofNullable(withBalance).orElse(false)), accountModelMapper::mapToAccountDetails);
    }

    @Override
    public ResponseEntity<?> getBalances(String accountId, UUID xRequestID, String consentID, String digest, String signature, byte[] tpPSignatureCertificate, String psUIPAddress, Object psUIPPort, String psUAccept, String psUAcceptCharset, String psUAcceptEncoding, String psUAcceptLanguage, String psUUserAgent, String psUHttpMethod, UUID psUDeviceID, String psUGeoLocation) {
        return responseMapper.ok(accountService.getBalances(consentID, accountId), accountModelMapper::mapToBalance);
    }

    @ApiOperation(value = "Read Transaction List", nickname = "getTransactionList", notes = "Read transaction reports or transaction lists of a given account adressed by \"account-id\", depending on the steering parameter  \"bookingStatus\" together with balances.  For a given account, additional parameters are e.g. the attributes \"dateFrom\" and \"dateTo\".  The ASPSP might add balance information, if transaction lists without balances are not supported. ", response = TransactionsResponse200Json.class, authorizations = {
        @Authorization(value = "BearerAuthOAuth")}, tags = {"Account Information Service (AIS)",})
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK", response = TransactionsResponse200Json.class),
        @ApiResponse(code = 400, message = "Bad Request", response = TppMessages400.class),
        @ApiResponse(code = 401, message = "Unauthorized", response = TppMessages401.class),
        @ApiResponse(code = 403, message = "Forbidden", response = TppMessages403.class),
        @ApiResponse(code = 404, message = "Not found", response = TppMessages404.class),
        @ApiResponse(code = 405, message = "Method Not Allowed", response = TppMessages405.class),
        @ApiResponse(code = 406, message = "Not Acceptable", response = TppMessages406.class),
        @ApiResponse(code = 408, message = "Request Timeout"),
        @ApiResponse(code = 415, message = "Unsupported Media Type"),
        @ApiResponse(code = 429, message = "Too Many Requests", response = TppMessages429.class),
        @ApiResponse(code = 500, message = "Internal Server Error"),
        @ApiResponse(code = 503, message = "Service Unavailable")})
    @RequestMapping(value = "/v1/accounts/{account-id}/transactions",
        produces = {"application/json", "application/xml"},
        method = RequestMethod.GET)
    @Override
    public ResponseEntity<?> _getTransactionList(@ApiParam(value = "This identification is denoting the addressed account.  The account-id is retrieved by using a \"Read Account List\" call.  The account-id is the \"id\" attribute of the account structure.  Its value is constant at least throughout the lifecycle of a given consent. ", required = true) @PathVariable("account-id") String accountId, @NotNull @ApiParam(value = "Permitted codes are    * \"booked\",   * \"pending\" and    * \"both\" \"booked\" shall be supported by the ASPSP. To support the \"pending\" and \"both\" feature is optional for the ASPSP,  Error code if not supported in the online banking frontend ", required = true, allowableValues = "booked, pending, both") @Valid @RequestParam(value = "bookingStatus", required = true) String bookingStatus,
                                                 @ApiParam(value = "ID of the request, unique to the call, as determined by the initiating party.", required = true) @RequestHeader(value = "X-Request-ID", required = true) UUID xRequestID,
                                                 @ApiParam(value = "This then contains the consentId of the related AIS consent, which was performed prior to this payment initiation. ", required = true) @RequestHeader(value = "Consent-ID", required = true) String consentID,
                                                 @ApiParam(value = "Conditional: Starting date (inclusive the date dateFrom) of the transaction list, mandated if no delta access is required. ") @Valid @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @RequestParam(value = "dateFrom", required = false) LocalDate dateFrom,
                                                 @ApiParam(value = "End date (inclusive the data dateTo) of the transaction list, default is now if not given. ") @Valid @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @RequestParam(value = "dateTo", required = false) LocalDate dateTo,
                                                 @ApiParam(value = "This data attribute is indicating that the AISP is in favour to get all transactions after  the transaction with identification entryReferenceFrom alternatively to the above defined period.  This is a implementation of a delta access.  If this data element is contained, the entries \"dateFrom\" and \"dateTo\" might be ignored by the ASPSP  if a delta report is supported.  Optional if supported by API provider. ") @Valid @RequestParam(value = "entryReferenceFrom", required = false) String entryReferenceFrom,
                                                 @ApiParam(value = "This data attribute is indicating that the AISP is in favour to get all transactions after the last report access for this PSU on the addressed account. This is another implementation of a delta access-report. This delta indicator might be rejected by the ASPSP if this function is not supported. Optional if supported by API provider") @Valid @RequestParam(value = "deltaList", required = false) Boolean deltaList,
                                                 @ApiParam(value = "If contained, this function reads the list of accessible payment accounts including the booking balance,  if granted by the PSU in the related consent and available by the ASPSP.  This parameter might be ignored by the ASPSP.  ") @Valid @RequestParam(value = "withBalance", required = false) Boolean withBalance,
                                                 @ApiParam(value = "Is contained if and only if the \"Signature\" element is contained in the header of the request.") @RequestHeader(value = "Digest", required = false) String digest,
                                                 @ApiParam(value = "A signature of the request by the TPP on application level. This might be mandated by ASPSP. ") @RequestHeader(value = "Signature", required = false) String signature,
                                                 @ApiParam(value = "The certificate used for signing the request, in base64 encoding.  Must be contained if a signature is contained. ") @RequestHeader(value = "TPP-Signature-Certificate", required = false) byte[] tpPSignatureCertificate,
                                                 @ApiParam(value = "The forwarded IP Address header field consists of the corresponding http request IP Address field between PSU and TPP. ") @RequestHeader(value = "PSU-IP-Address", required = false) String psUIPAddress,
                                                 @ApiParam(value = "The forwarded IP Port header field consists of the corresponding HTTP request IP Port field between PSU and TPP, if available. ") @RequestHeader(value = "PSU-IP-Port", required = false) Object psUIPPort,
                                                 @ApiParam(value = "The forwarded IP Accept header fields consist of the corresponding HTTP request Accept header fields between PSU and TPP, if available. ") @RequestHeader(value = "PSU-Accept", required = false) String psUAccept,
                                                 @ApiParam(value = "The forwarded IP Accept header fields consist of the corresponding HTTP request Accept header fields between PSU and TPP, if available. ") @RequestHeader(value = "PSU-Accept-Charset", required = false) String psUAcceptCharset,
                                                 @ApiParam(value = "The forwarded IP Accept header fields consist of the corresponding HTTP request Accept header fields between PSU and TPP, if available. ") @RequestHeader(value = "PSU-Accept-Encoding", required = false) String psUAcceptEncoding,
                                                 @ApiParam(value = "The forwarded IP Accept header fields consist of the corresponding HTTP request Accept header fields between PSU and TPP, if available. ") @RequestHeader(value = "PSU-Accept-Language", required = false) String psUAcceptLanguage,
                                                 @ApiParam(value = "The forwarded Agent header field of the HTTP request between PSU and TPP, if available. ") @RequestHeader(value = "PSU-User-Agent", required = false) String psUUserAgent,
                                                 @ApiParam(value = "HTTP method used at the PSU ? TPP interface, if available. Valid values are: * GET * POST * PUT * PATCH * DELETE ", allowableValues = "GET, POST, PUT, PATCH, DELETE") @RequestHeader(value = "PSU-Http-Method", required = false) String psUHttpMethod,
                                                 @ApiParam(value = "UUID (Universally Unique Identifier) for a device, which is used by the PSU, if available. UUID identifies either a device or a device dependant application installation. In case of an installation identification this ID need to be unaltered until removal from device. ") @RequestHeader(value = "PSU-Device-ID", required = false) UUID psUDeviceID,
                                                 @ApiParam(value = "The forwarded Geo Location of the corresponding http request between PSU and TPP if available. ") @RequestHeader(value = "PSU-Geo-Location", required = false) String psUGeoLocation) {
        return getTransactionList(accountId, bookingStatus, xRequestID, consentID, dateFrom, dateTo, entryReferenceFrom, deltaList, withBalance, digest, signature, tpPSignatureCertificate, psUIPAddress, psUIPPort, psUAccept, psUAcceptCharset, psUAcceptEncoding, psUAcceptLanguage, psUUserAgent, psUHttpMethod, psUDeviceID, psUGeoLocation);
    }

    @Override
    public ResponseEntity<?> getTransactionList(String accountId, String bookingStatus, UUID xRequestID, String consentID, LocalDate dateFrom, LocalDate dateTo, String entryReferenceFrom, Boolean deltaList, Boolean withBalance, String digest, String signature, byte[] tpPSignatureCertificate, String psUIPAddress, Object psUIPPort, String psUAccept, String psUAcceptCharset, String psUAcceptEncoding, String psUAcceptLanguage, String psUUserAgent, String psUHttpMethod, UUID psUDeviceID, String psUGeoLocation) {
        ResponseObject<Xs2aTransactionsReport> transactionsReport =
            accountService.getTransactionsReportByPeriod(accountId, Optional.ofNullable(withBalance).orElse(false), consentID, dateFrom, dateTo, Xs2aBookingStatus.forValue(bookingStatus));
        return responseMapper.ok(transactionsReport, accountModelMapper::mapToTransactionsResponse200Json);
    }

    @Override
    public ResponseEntity<?> getTransactionDetails(String accountId, String resourceId, UUID xRequestID, String consentID, String digest, String signature, byte[] tpPSignatureCertificate, String psUIPAddress, Object psUIPPort, String psUAccept, String psUAcceptCharset, String psUAcceptEncoding, String psUAcceptLanguage, String psUUserAgent, String psUHttpMethod, UUID psUDeviceID, String psUGeoLocation) {
        ResponseObject<Xs2aAccountReport> responseObject =
            accountService.getAccountReportByTransactionId(consentID, accountId, resourceId);
        return responseMapper.ok(responseObject, accountModelMapper::mapToAccountReport);
    }
}
