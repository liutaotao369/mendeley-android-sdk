package com.mendeley.api.request.procedure;

import android.net.Uri;
import android.util.Log;

import com.mendeley.api.AuthTokenManager;
import com.mendeley.api.ClientCredentials;
import com.mendeley.api.exceptions.HttpResponseException;
import com.mendeley.api.exceptions.MendeleyException;
import com.mendeley.api.model.RequestResponse;
import com.mendeley.api.request.GetNetworkRequest;
import com.mendeley.api.request.NetworkUtils;
import com.mendeley.api.request.ProgressPublisherInputStream;
import com.mendeley.api.request.params.Page;
import com.mendeley.api.util.DateUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base class for all synchronous network calls.
 */
public abstract class HttpUrlConnectionRequest<ResultType> extends Request<ResultType> {

    private final String url;
    private final String contentType;
    private RequestProgressListener progressListener;

    public HttpUrlConnectionRequest(String url, String contentType, AuthTokenManager authTokenManager, ClientCredentials clientCredentials) {
        super(authTokenManager, clientCredentials);
        this.url = url;
        this.contentType = contentType;
    }

    @Override
    public final RequestResponse<ResultType> doRun() throws MendeleyException {
        return doRun(Uri.parse(url), 0, true);
    }

    private RequestResponse<ResultType> doRun(Uri uri, int currentRetry, boolean addOauthToken) throws MendeleyException {

        InputStream is = null;
        HttpURLConnection con = null;

        try {
            con = createConnection(uri);

            // the redirection in implemented by us
            con.setInstanceFollowRedirects(false);

            if (addOauthToken) {
                con.addRequestProperty("Authorization", "Bearer " + authTokenManager.getAccessToken());
            }

            if (contentType != null) {
                con.addRequestProperty("Content-type", contentType);
            }

            final Map<String, String> requestHeaders = new HashMap<String, String>();
            appendHeaders(requestHeaders);
            for (String key: requestHeaders.keySet()) {
                con.addRequestProperty(key, requestHeaders.get(key));
            }
            con.connect();

            onConnected(con);

            final int responseCode = con.getResponseCode();

            // Implementation of HTTP redirection.
            if (responseCode >= 300 && responseCode < 400) {
                return followRedirection(con);
            }

            if (responseCode < 200 && responseCode >= 300) {
                throw HttpResponseException.create(con);
            }


            // wrapping the input stream of the connection in one ProgressPublisherInputStream
            // to publish progress as the file is being read
            is = new MyProgressPublisherInputStream(con.getInputStream(), con.getContentLength());

            final Map<String, List<String>> responseHeaders = con.getHeaderFields();
            return new RequestResponse<>(manageResponse(is), getServerDate(responseHeaders), getNextPage(responseHeaders));

        } catch (MendeleyException me) {
            throw me;
        } catch (ParseException pe) {
            throw new MendeleyException("Could not parse web API headers for " + url, pe);
        } catch (IOException ioe) {
            // If the issue is due to IOException, retry up to MAX_HTTP_RETRIES times
            if (currentRetry <  MAX_HTTP_RETRIES) {
                Log.w(TAG, "Problem connecting to " + url + ": " + ioe.getMessage() + ". Retrying (" + (currentRetry + 1) + "/" + MAX_HTTP_RETRIES + ")");
                return doRun(uri, currentRetry + 1, addOauthToken);
            } else {
                throw new MendeleyException("IO error in GET request " + url + ": " + ioe.toString(), ioe);
            }
        } catch (Exception e) {
            throw new MendeleyException("Error in GET request " + url + ": " + e.toString(), e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ignored) {
                }
            }
            if (con != null) {
                con.disconnect();
            }
        }
    }

    /*
     * We implement the redirection by hand because:
     * - we don't want to send the auth token in the query string, but as a HTTP header
     * - if redirected to Amazon or other server, we don't want to forward the Mendeley auth jeader
     * - ... but the redirection that HttpUrlConnection does forwards the header
     */
    private RequestResponse<ResultType> followRedirection(HttpURLConnection con) throws MendeleyException {
        final Uri redirectionUri = Uri.parse(con.getHeaderField("location"));
        final boolean addOauthToken = redirectionUri.getHost().equals(Uri.parse(NetworkUtils.API_URL).getHost());
        return doRun(redirectionUri, 0, addOauthToken);
    }

    /**
     * Sets a listener to be notified of progress
     * @param progressListener
     */
    public final void setProgressListener(GetNetworkRequest.RequestProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    protected void appendHeaders(Map<String, String> headers) {
    }

    protected abstract HttpURLConnection createConnection(Uri uri) throws IOException;

    protected abstract void onConnected(HttpURLConnection con) throws Exception;

    protected abstract ResultType manageResponse(InputStream is) throws Exception;


    private Date getServerDate(Map<String, List<String>> headersMap ) throws IOException, ParseException {
        final List<String> dateHeaders = headersMap.get("Date");
        if (dateHeaders != null) {
            final String dateHeader = headersMap.get("Date").get(0);
            if (dateHeader != null) {
                return DateUtils.parseDateInHeader(dateHeader);
            }
        }
        return null;
    }

    private Page getNextPage(Map<String, List<String>> responseHeaders) {
        final List<String> links = responseHeaders.get("Link");
        if (links != null) {
            for (String link : links) {
                try {
                    String linkString = link.substring(link.indexOf("<") + 1, link.indexOf(">"));
                    if (link.contains("next")) {
                        return new Page(linkString);
                    }
                } catch (IndexOutOfBoundsException ignored) {
                }
            }
        }
        return null;
    }

    /**
     * Implementation of {@link ProgressPublisherInputStream} that pipes the progress to the
     * {@link RequestProgressListener}
     */
    private class MyProgressPublisherInputStream extends ProgressPublisherInputStream {
        public MyProgressPublisherInputStream(InputStream inputStream, int contentLength) {
            super(inputStream, contentLength);
        }

        @Override
        protected void onProgress(int progress) {
            if (progressListener != null) {
                progressListener.onProgress(progress);
            }
        }
    }

    /**
     * To be implemented by classes that want to listen the progress of the download
     */
    public interface  RequestProgressListener {
        /**
         * @param progress in [0-100]
         */
        void onProgress(int progress);
    }
}
