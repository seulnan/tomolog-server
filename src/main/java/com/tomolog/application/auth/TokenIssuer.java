package com.tomolog.application.auth;

/**
 * Port for issuing access tokens, so the application layer can mint tokens without depending on the
 * JWT infrastructure. Implemented by the infrastructure JWT provider.
 */
public interface TokenIssuer {

  /** Issues an access token for the given user id. */
  String createAccessToken(Long userId);
}
