package com.mendeley.api.request.procedure;

import android.text.TextUtils;

import com.mendeley.api.AuthTokenManager;
import com.mendeley.api.ClientCredentials;
import com.mendeley.api.exceptions.HttpResponseException;
import com.mendeley.api.exceptions.MendeleyException;
import com.mendeley.api.exceptions.NotSignedInException;
import com.mendeley.api.model.RequestResponse;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * {@link Request} against the Mendeley Web API that is performed using a valid OAuth access token.
 *
 * @param <ResultType>
 */
public abstract class AuthorizedRequest<ResultType> extends Request<ResultType> {

    public AuthorizedRequest(AuthTokenManager authTokenManager, ClientCredentials clientCredentials) {
        super(authTokenManager, clientCredentials);
    }

    public final RequestResponse<ResultType> run() throws MendeleyException {
        if (TextUtils.isEmpty(authTokenManager.getAccessToken())) {
            // Must call startSignInProcess first - caller error!
            throw new NotSignedInException();
        }

        if (willExpireSoon()) {
            launchRefreshTokenRequest();
        }
        try {
            return doRun();
        } catch (HttpResponseException e) {
            if (e.httpReturnCode == 401 && e.getMessage().contains("Token has expired")) {
                // The refresh-token-in-advance logic did not work for some reason: force a refresh now
                launchRefreshTokenRequest();
                return doRun();
            } else {
                throw e;
            }
        }
    }

    private void launchRefreshTokenRequest() throws MendeleyException {
        new AuthTokenRefreshRequest(authTokenManager, clientCredentials).run();
    }

    protected abstract RequestResponse<ResultType> doRun() throws MendeleyException;

    // TODO: consider dropping this to reduce complexity
    /**
     * Checks if the current access token will expire soon (or isn't valid at all).
     */
    private boolean willExpireSoon() {
        if (TextUtils.isEmpty(authTokenManager.getAccessToken()) || authTokenManager.getAuthTenExpiresAt() == null) {
            return true;
        }
        Date now = new Date();
        Date expires = authTokenManager.getAuthTenExpiresAt();
        long timeToExpiryMs = expires.getTime() - now.getTime();
        long timeToExpirySec = TimeUnit.MILLISECONDS.toSeconds(timeToExpiryMs);
        return timeToExpirySec < AuthTokenManager.MIN_TOKEN_VALIDITY_SEC;
    }
}
