/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.db.es;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestClient;
import sirius.db.DB;
import sirius.db.mixing.OptimisticLockException;
import sirius.kernel.async.ExecutionPoint;
import sirius.kernel.async.Operation;
import sirius.kernel.commons.Watch;
import sirius.kernel.di.std.Part;
import sirius.kernel.health.Exceptions;
import sirius.kernel.health.Microtiming;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Internal fluent builder used to create, execute and handle requests via the given REST client.
 */
class RequestBuilder {
    private String method;
    private RestClient restClient;
    private Map<String, String> params;
    private JSONObject data;
    private String rawData;
    private HttpEntity responseEntity;
    private JSONObject responseObject;
    private Function<ResponseException, HttpEntity> customExceptionHandler;

    @Part
    private static Elastic elastic;

    protected RequestBuilder(String method, RestClient restClient) {
        this.method = method;
        this.restClient = restClient;
    }

    protected RequestBuilder withParam(String param, Object value) {
        if (value != null) {
            if (params == null) {
                params = new HashMap<>();
            }

            params.put(param, String.valueOf(value));
        }

        return this;
    }

    protected RequestBuilder data(JSONObject data) {
        this.data = data;
        return this;
    }

    protected RequestBuilder rawData(String data) {
        this.rawData = data;
        return this;
    }

    protected RequestBuilder withCustomErrorHandler(Function<ResponseException, HttpEntity> errorHandler) {
        this.customExceptionHandler = errorHandler;
        return this;
    }

    protected RequestBuilder routing(Object routing) {
        return withParam("routing", routing);
    }

    protected RequestBuilder version(Object version) {
        return withParam("version", version);
    }

    protected RequestBuilder tryExecute(String uri) throws OptimisticLockException {
        Watch w = Watch.start();
        try (Operation op = new Operation(() -> "Elastic: " + method + " " + uri, Duration.ofSeconds(30))) {
            if (Elastic.LOG.isFINE()) {
                Elastic.LOG.FINE(method + " " + uri + ": " + buildContent().orElse("-"));
            }

            NStringEntity requestContent =
                    buildContent().map(content -> new NStringEntity(content, ContentType.APPLICATION_JSON))
                                  .orElse(null);
            Response response = restClient.performRequest(method, uri, determineParams(), requestContent);
            responseEntity = response.getEntity();
            return this;
        } catch (ResponseException e) {
            return handleResponseException(e);
        } catch (IOException e) {
            throw Exceptions.handle()
                            .to(Elastic.LOG)
                            .error(e)
                            .withSystemErrorMessage(
                                    "An IO exception ocurred when performing a request against elasticsearch: %s")
                            .handle();
        } finally {
            elastic.callDuration.addValue(w.elapsedMillis());
            if (Microtiming.isEnabled()) {
                w.submitMicroTiming("elastic", method + ": " + uri);
            }
            if (w.elapsedMillis() > Elastic.getLogQueryThresholdMillis()) {
                elastic.numSlowQueries.inc();
                DB.SLOW_DB_LOG.INFO("A slow Elasticsearch query was executed (%s): %s\n%s\n%s",
                                    w.duration(),
                                    method + ": " + uri,
                                    buildContent().orElse("no content"),
                                    ExecutionPoint.snapshot().toString());
            }
        }
    }

    private RequestBuilder handleResponseException(ResponseException e) throws OptimisticLockException {
        if (customExceptionHandler != null) {
            HttpEntity result = customExceptionHandler.apply(e);
            if (result != null) {
                responseEntity = result;
                return this;
            }
        }

        JSONObject error = extractErrorJSON(e);
        if (e.getResponse().getStatusLine().getStatusCode() == 409) {
            throw new OptimisticLockException(error.getString("reason"), e);
        }

        throw Exceptions.handle()
                        .to(Elastic.LOG)
                        .error(e)
                        .withSystemErrorMessage("Elasticsearch (%s) reported an error: %s (%s)",
                                                e.getResponse().getHost(),
                                                error == null ? "unknown" : error.getString("reason"),
                                                error == null ? "-" : error.getString("type"))
                        .handle();
    }

    private Map<String, String> determineParams() {
        return params == null ? Collections.emptyMap() : params;
    }

    private Optional<String> buildContent() {
        if (data != null) {
            return Optional.of(data.toJSONString());
        }
        if (rawData != null) {
            return Optional.of(rawData);
        }

        return Optional.empty();
    }

    protected RequestBuilder execute(String uri) {
        try {
            return tryExecute(uri);
        } catch (OptimisticLockException e) {
            throw Exceptions.handle()
                            .to(Elastic.LOG)
                            .error(e)
                            .withSystemErrorMessage("An unexpected optimitic locking error ocurred: %s")
                            .handle();
        }
    }

    protected JSONObject extractErrorJSON(ResponseException e) {
        try {
            JSONObject response = JSON.parseObject(EntityUtils.toString(e.getResponse().getEntity()));
            return response.getJSONObject("error");
        } catch (IOException ex) {
            Exceptions.handle(Elastic.LOG, ex);
            throw Exceptions.handle()
                            .to(Elastic.LOG)
                            .error(e)
                            .withSystemErrorMessage("Elasticsearch (%s) reported an error which cannot be unpacked: %s",
                                                    e.getResponse().getHost())
                            .handle();
        }
    }

    protected JSONObject response() {
        try {
            if (responseObject == null) {
                if (responseEntity == null) {
                    throw new IllegalStateException("No response is available before making a request.");
                }

                responseObject = JSON.parseObject(EntityUtils.toString(responseEntity));
            }
            return responseObject;
        } catch (IOException e) {
            throw Exceptions.handle()
                            .to(Elastic.LOG)
                            .error(e)
                            .withSystemErrorMessage(
                                    "An IO exception ocurred when performing a request against elasticsearch: %s")
                            .handle();
        }
    }

    protected RequestBuilder toggle(String param, boolean toggle) {
        return withParam(param, String.valueOf(toggle));
    }

    protected RequestBuilder enable(String param, boolean flag) {
        if (!flag) {
            return this;
        }

        return withParam(param, "true");
    }

    protected RequestBuilder disable(String param, boolean flag) {
        if (flag) {
            return this;
        }

        return withParam(param, "false");
    }
}
