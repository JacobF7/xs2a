# Release notes v. 1.12

## Extract CMS web-endpoints to different maven modules
A SPI Developer now can decide what endpoints on CMS side he needs.
There are three modules with different endpoints to serve different possible purposes of CMS.
* consent-xs2a-web is used to provide endpoints for xs2a-service. 
This is normally needed if CMS is deployed as a separate service.
* consent-psu-web is used to provide endpoints for presentation and work with consents in some PSU application, 
i.e. online-banking system. This module can be used in any setup of CMS (embedded and standalone).
* consent-aspsp-web is used to provide other endpoints available for banking systems.
This module can be used in any setup of CMS (embedded and standalone).

## Return full SCA Object from SPI during request to send TAN
By invoking `requestAuthorisationCode` method SPI developer now obliged
to return full SCA Authentification Object for choosen SCA Method.
This is done to avoid unnecessary additional call to list all available methods second time.
`SpiAuthorizationCodeResult` now adjusted accordingly.

## Improved nullability checks in SpiResponse
Due to importance of having not-nullable results in SPI API nullability checks by SpiResponse object construction were added.
Please note, that this also implies, if you need to return VoidResponse.
Please use SpiResponse.voidResponse() static method for that.
