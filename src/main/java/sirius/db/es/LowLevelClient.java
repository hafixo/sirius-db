/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.db.es;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpEntity;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestClient;
import sirius.db.mixing.EntityDescriptor;
import sirius.db.mixing.OptimisticLockException;
import sirius.kernel.commons.Explain;
import sirius.kernel.di.std.Part;
import sirius.kernel.health.Exceptions;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Provides a low-level client against Elasticsearch.
 * <p>
 * This is mainly used to build and execute HTTP requests and process their response.
 */
public class LowLevelClient {

    private static final String API_REINDEX = "/_reindex?pretty";
    private static final String API_ALIAS = "/_alias";
    private static final String API_ALIASES = "/_aliases";
    private static final String API_SEARCH = "/_search";
    private static final String API_DELETE_BY_QUERY = "/_delete_by_query";

    private static final String PARAM_INDEX = "index";
    private static final String PARAM_TYPE = "type";

    private RestClient restClient;

    @Part
    private static Elastic elastic;

    /**
     * Creates a new client based on the given REST client which handle load balancing and connection management.
     *
     * @param restClient the underlying REST client to use
     */
    public LowLevelClient(RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * Returns the underlying REST client.
     *
     * @return the underlying REST client
     */
    public RestClient getRestClient() {
        return restClient;
    }

    /**
     * Builds a GET request.
     *
     * @return a request builder used to execute the request
     */
    public RequestBuilder performGet() {
        return new RequestBuilder("GET", getRestClient());
    }

    /**
     * Builds a POST request.
     *
     * @return a request builder used to execute the request
     */
    public RequestBuilder performPost() {
        return new RequestBuilder("POST", getRestClient());
    }

    /**
     * Builds a PUT request.
     *
     * @return a request builder used to execute the request
     */
    public RequestBuilder performPut() {
        return new RequestBuilder("PUT", getRestClient());
    }

    /**
     * Builds a DELETE request.
     *
     * @return a request builder used to execute the request
     */
    public RequestBuilder performDelete() {
        return new RequestBuilder("DELETE", getRestClient());
    }

    /**
     * Tells Elasticsearch to create or update the given document with the given data.
     *
     * @param index       the target index
     * @param id          the ID to use
     * @param routing     the routing to use
     * @param primaryTerm the primaryTerm to use for optimistic locking during the update
     * @param seqNo       the seqNo to use for optimistic locking during the update
     * @param data        the actual payload to store
     * @return the response of the call
     * @throws OptimisticLockException in case of an optimistic locking error (wrong version provided)
     */
    public JSONObject index(String index,
                            String id,
                            @Nullable String routing,
                            @Nullable Long primaryTerm,
                            @Nullable Long seqNo,
                            JSONObject data) throws OptimisticLockException {
        return performPut().routing(routing)
                           .primaryTerm(primaryTerm)
                           .seqNo(seqNo)
                           .data(data)
                           .tryExecute(index + "/_doc/" + id)
                           .response();
    }

    /**
     * Performs a lookup for the given document.
     *
     * @param index      the index to search in
     * @param id         the ID to search by
     * @param routing    the routing value to use
     * @param withSource <tt>true</tt> to also load the <tt>_source</tt> of the document, <tt>false</tt> otherwise
     * @return the response of the call
     */
    public JSONObject get(String index, String id, @Nullable String routing, boolean withSource) {
        return performGet().withCustomErrorHandler(this::handleNotFoundAsResponse)
                           .routing(routing)
                           .disable("_source", withSource)
                           .execute(index + "/_doc/" + id)
                           .response();
    }

    /**
     * Deletes the given document.
     *
     * @param index       the target index
     * @param id          the ID to use
     * @param routing     the routing to use
     * @param primaryTerm the primaryTerm to use for optimistic locking during the delete
     * @param seqNo       the seqNo to use for optimistic locking during the delete
     * @return the response of the call
     * @throws OptimisticLockException in case of an optimistic locking error (wrong version provided)
     */
    public JSONObject delete(String index, String id, String routing, Long primaryTerm, Long seqNo)
            throws OptimisticLockException {
        return performDelete().routing(routing)
                              .primaryTerm(primaryTerm)
                              .seqNo(seqNo)
                              .tryExecute(index + "/_doc/" + id)
                              .response();
    }

    protected HttpEntity handleNotFoundAsResponse(ResponseException e) {
        if (e.getResponse().getStatusLine().getStatusCode() == 404) {
            return e.getResponse().getEntity();
        } else {
            return null;
        }
    }

    /**
     * Deletes all documents matched by the given query.
     *
     * @param alias   the alias which determines the indices to search in
     * @param routing the routing to use
     * @param query   the query to execute
     * @return the response of the call
     */
    public JSONObject deleteByQuery(String alias, @Nullable String routing, JSONObject query) {
        return performPost().routing(routing).data(query).execute(alias + API_DELETE_BY_QUERY).response();
    }

    /**
     * Executes a search.
     *
     * @param alias   the alias which determines the indices to search in
     * @param routing the routing to use
     * @param from    the number of items to skip
     * @param size    the maximal result length
     * @param query   the query to execute
     * @return the response of the call
     */
    public JSONObject search(String alias, @Nullable String routing, int from, int size, JSONObject query) {
        return performGet().routing(routing)
                           .withParam("size", size)
                           .withParam("from", from)
                           .data(query)
                           .execute(alias + API_SEARCH)
                           .response();
    }

    /**
     * Executes a async reindex request.
     *
     * @param ed           the current entitydescriptor that should be reindexd
     * @param newIndexName the name of the index in which the documents shoulds be reindex
     * @param onSuccess    is called if the request is successfully finished
     * @param onFailure    is called if a exception occurs while performing the request
     */
    public void reindex(EntityDescriptor ed,
                        String newIndexName,
                        Consumer<Response> onSuccess,
                        Consumer<Exception> onFailure) {
        performPost().data(new JSONObject().fluentPut("source",
                                                      new JSONObject().fluentPut(PARAM_INDEX,
                                                                                 elastic.determineAlias(ed)))
                                           .fluentPut("dest", new JSONObject().fluentPut(PARAM_INDEX, newIndexName)))
                     .executeAsync(API_REINDEX, onSuccess, onFailure);
    }

    /**
     * Adds an alias to a given index.
     *
     * @param indexName the name of the index which should be aliased
     * @param alias     the alias to apply
     * @return the response of the call
     */
    public JSONObject addAlias(String indexName, String alias) {
        return performPut().execute("/" + indexName + API_ALIAS + "/" + alias).response();
    }

    /**
     * Returns the names of the indexed which are aliased with the given alias.
     *
     * @param alias the given alias
     * @return a list of indexes which are aliased with the given alias
     */
    public boolean aliasExists(String alias) {
        try {
            return restClient.performRequest(new Request("HEAD", API_ALIAS + "/" + alias))
                             .getStatusLine()
                             .getStatusCode() == 200;
        } catch (ResponseException e) {
            throw Exceptions.handle()
                            .to(Elastic.LOG)
                            .error(e)
                            .withSystemErrorMessage("An error occurred when checking for alias '%s': %s (%s)", alias)
                            .handle();
        } catch (IOException e) {
            throw Exceptions.handle()
                            .to(Elastic.LOG)
                            .error(e)
                            .withSystemErrorMessage("An IO error occurred when checking for index '%s': %s (%s)", alias)
                            .handle();
        }
    }

    /**
     * Returns all indices which hold the {@link Elastic#ACTIVE_ALIAS} for the given {@link EntityDescriptor}.
     *
     * @param ed the entity descriptor to check
     * @return a list of all indices which hold the {@link Elastic#ACTIVE_ALIAS} for the given {@link EntityDescriptor}
     */
    public List<String> getIndicesForAlias(EntityDescriptor ed) {
        List<String> indexNames = new ArrayList<>();
        performGet().execute(API_ALIAS + "/" + elastic.determineAlias(ed))
                    .response()
                    .forEach((indexName, info) -> indexNames.add(indexName));
        return indexNames;
    }

    /**
     * Performs an atomic move operation where the alias, which marks the currently active index (see {@link Elastic#ACTIVE_ALIAS}),
     * for the given descriptor is transferred to the given destination.
     *
     * @param ed          the entity descriptor
     * @param destination the index that should be marked with the given alias
     * @return the reponse of the call
     */
    public JSONObject moveActiveAlias(EntityDescriptor ed, String destination) {
        String alias = elastic.determineAlias(ed);

        if (!indexExists(alias)) {
            throw Exceptions.handle()
                            .withSystemErrorMessage("There exists no index which holds the alias '%s'", alias)
                            .handle();
        }

        if (!indexExists(destination)) {
            throw Exceptions.handle()
                            .withSystemErrorMessage("There exists no index with name '%s'", destination)
                            .handle();
        }

        List<String> indices = elastic.getLowLevelClient().getIndicesForAlias(ed);
        if (indices.size() > 1) {
            throw Exceptions.handle()
                            .withSystemErrorMessage(
                                    "More than one index is referenced by alias '%s'. Cannot move alias to '%'",
                                    alias,
                                    destination)
                            .handle();
        }

        JSONObject remove = new JSONObject().fluentPut(PARAM_INDEX, indices.get(0)).fluentPut("alias", alias);
        JSONObject add = new JSONObject().fluentPut(PARAM_INDEX, destination).fluentPut("alias", alias);
        JSONArray actions = new JSONArray().fluentAdd(new JSONObject().fluentPut("remove", remove))
                                           .fluentAdd(new JSONObject().fluentPut("add", add));

        return performPost().data(new JSONObject().fluentPut("actions", actions)).execute(API_ALIASES).response();
    }

    /**
     * Creates a scroll search.
     *
     * @param alias        the alias which determines the indices to search in
     * @param routing      the routing to use
     * @param from         the number of items to skip
     * @param sizePerShard the maximal number of results per shard
     * @param ttlSeconds   the ttl of the scroll cursor in seconds
     * @param query        the query to execute
     * @return the response of the call
     */
    public JSONObject createScroll(String alias,
                                   String routing,
                                   int from,
                                   int sizePerShard,
                                   int ttlSeconds,
                                   JSONObject query) {
        return performGet().routing(routing)
                           .withParam("size", sizePerShard)
                           .withParam("from", from)
                           .withParam("scroll", ttlSeconds + "s")
                           .data(query)
                           .execute(alias + API_SEARCH)
                           .response();
    }

    /**
     * Continues a scroll query.
     *
     * @param ttlSeconds the ttl of the scroll cursor in seconds
     * @param scrollId   the id of the scroll cursor
     * @return the response of the call
     */
    public JSONObject continueScroll(int ttlSeconds, String scrollId) {
        return performGet().data(new JSONObject().fluentPut("scroll", ttlSeconds + "s")
                                                 .fluentPut("scroll_id", scrollId))
                           .execute("/_search/scroll")
                           .response();
    }

    /**
     * Closes a scroll query.
     *
     * @param scrollId the id of the scroll cursor
     * @return the response of the call
     */
    public JSONObject closeScroll(String scrollId) {
        return performDelete().data(new JSONObject().fluentPut("scroll_id", scrollId))
                              .execute("/_search/scroll")
                              .response();
    }

    /**
     * Determines if a given query has at least one result.
     *
     * @param alias   the alias which determines the indices to search in
     * @param routing the routing to use
     * @param query   the query to execute
     * @return the response of the call
     */
    public JSONObject exists(String alias, String routing, JSONObject query) {
        return performGet().routing(routing)
                           .withParam("size", 0)
                           .withParam("terminate_after", 1)
                           .data(query)
                           .execute(alias + API_SEARCH)
                           .response();
    }

    /**
     * Determines the number of hits for a given query.
     *
     * @param alias   the alias which determines the indices to search in
     * @param routing the routing to use
     * @param query   the query to execute
     * @return the response of the call
     */
    public JSONObject count(String alias, String routing, JSONObject query) {
        return performGet().routing(routing).data(query).execute(alias + "/_count").response();
    }

    /**
     * Executes a list of bulk statements.
     *
     * @param bulkData the statements to execute.
     * @return the response of the call
     * @see BulkContext
     */
    @SuppressWarnings("squid:S1612")
    @Explain("Due to method overloading the compiler cannot deduce which method to pick")
    public JSONObject bulk(List<JSONObject> bulkData) {
        return performPost().rawData(bulkData.stream().map(obj -> obj.toJSONString()).collect(Collectors.joining("\n"))
                                     + "\n").execute("_bulk").response();
    }

    /**
     * Creates the given index.
     *
     * @param index            the name of the index
     * @param numberOfShards   the number of shards to use
     * @param numberOfReplicas the number of replicas per shard
     * @return the response of the call
     */
    public JSONObject createIndex(String index, int numberOfShards, int numberOfReplicas) {
        JSONObject indexObj = new JSONObject().fluentPut("number_of_shards", numberOfShards)
                                              .fluentPut("number_of_replicas", numberOfReplicas);
        JSONObject settingsObj = new JSONObject().fluentPut(PARAM_INDEX, indexObj);
        JSONObject input = new JSONObject().fluentPut("settings", settingsObj);
        return performPut().data(input).execute(index).response();
    }

    /**
     * Creates the given mapping.
     *
     * @param index the name of the index
     * @param data  the mapping to create
     * @return the response of the call
     */
    public JSONObject putMapping(String index, JSONObject data) {
        return performPut().data(data).execute(index + "/_mapping").response();
    }

    /**
     * Determines if the given index exists.
     *
     * @param index the name of the index
     * @return <tt>true</tt> if the index exists, <tt>false</tt> otherwise
     */
    public boolean indexExists(String index) {
        try {
            return restClient.performRequest(new Request("HEAD", index)).getStatusLine().getStatusCode() == 200;
        } catch (ResponseException e) {
            throw Exceptions.handle()
                            .to(Elastic.LOG)
                            .error(e)
                            .withSystemErrorMessage("An error occurred when checking for index '%s': %s (%s)", index)
                            .handle();
        } catch (IOException e) {
            throw Exceptions.handle()
                            .to(Elastic.LOG)
                            .error(e)
                            .withSystemErrorMessage("An IO error occurred when checking for index '%s': %s (%s)", index)
                            .handle();
        }
    }

    /**
     * Allows to explicitly refresh an index, making all operations performed since the last refresh available for search.
     *
     * @param index the index which should be refreshed
     */
    public void refresh(String index) {
        performPost().execute(index + "/_refresh").response();
    }
}
