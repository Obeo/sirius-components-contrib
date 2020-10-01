/*******************************************************************************
 * Copyright (c) 2019, 2020 Obeo.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
package org.eclipse.sirius.web.spring.graphql.ws.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import org.eclipse.sirius.web.spring.graphql.api.GraphQLConstants;
import org.eclipse.sirius.web.spring.graphql.controllers.GraphQLPayload;
import org.eclipse.sirius.web.spring.graphql.ws.SubscriptionEntry;
import org.eclipse.sirius.web.spring.graphql.ws.dto.input.StartMessage;
import org.eclipse.sirius.web.spring.graphql.ws.dto.output.CompleteMessage;
import org.eclipse.sirius.web.spring.graphql.ws.dto.output.DataMessage;
import org.eclipse.sirius.web.spring.graphql.ws.dto.output.ErrorMessage;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.WebSocketSession;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLContext;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

/**
 * This class will handle all the start messages sent by the consumers of the Web Socket API. It will executes the
 * GraphQL payload provided. If that payload contains a query or a mutation, it will return the result directly. If,
 * otherwise, it contains a subscription then it creates a new GraphQL subscription and registers it in the subscription
 * entries of the Web Socket session.
 *
 * @author sbegaudeau
 */
public class StartMessageHandler implements IWebSocketMessageHandler {

    /** Used to separate the session id from the operation id in the creation of the subscription id. */
    private static final String SEPARATOR = "#"; //$NON-NLS-1$

    private Logger logger = LoggerFactory.getLogger(StartMessageHandler.class);

    private final WebSocketSession session;

    private final GraphQL graphQL;

    private final ObjectMapper objectMapper;

    private final Map<WebSocketSession, List<SubscriptionEntry>> sessions2entries;

    public StartMessageHandler(WebSocketSession session, GraphQL graphQL, ObjectMapper objectMapper, Map<WebSocketSession, List<SubscriptionEntry>> sessions2entries) {
        this.session = Objects.requireNonNull(session);
        this.graphQL = Objects.requireNonNull(graphQL);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.sessions2entries = Objects.requireNonNull(sessions2entries);
    }

    public void handle(StartMessage startMessage) {
        String id = startMessage.getId();
        GraphQLPayload graphQLPayload = startMessage.getPayload();

        String query = graphQLPayload.getQuery();
        Map<String, Object> variables = Optional.ofNullable(graphQLPayload.getVariables()).orElse(Map.of());
        String operationName = graphQLPayload.getOperationName();

        // @formatter:off
        GraphQLContext graphQLContext = GraphQLContext.newContext()
                .of(GraphQLConstants.SUBSCRIPTION_ID, this.session.getId() + SEPARATOR + id)
                .of(GraphQLConstants.PRINCIPAL, this.session.getPrincipal())
                .build();

        ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                .query(query)
                .variables(variables)
                .operationName(operationName)
                .context(graphQLContext)
                .build();
        // @formatter:on

        ExecutionResult executionResult = this.graphQL.execute(executionInput);
        if (executionResult.getData() instanceof Publisher<?>) {
            Publisher<ExecutionResult> publisher = executionResult.getData();

            this.subscribe(id, publisher);
        } else {
            this.send(this.objectMapper, this.session, new DataMessage(id, executionResult.toSpecification()), this.logger);
        }
    }

    private void subscribe(String id, Publisher<ExecutionResult> publisher) {
        Consumer<ExecutionResult> consumer = result -> this.send(this.objectMapper, this.session, new DataMessage(id, result.toSpecification()), this.logger);
        Consumer<Throwable> onErrorConsumer = error -> {
            this.send(this.objectMapper, this.session, new ErrorMessage(id, null), this.logger);
        };
        Runnable onCompleteConsumer = () -> this.send(this.objectMapper, this.session, new CompleteMessage(id), this.logger);

        // @formatter:off
        Disposable subscription = Flux.from(publisher)
                .subscribe(consumer, onErrorConsumer, onCompleteConsumer);
        // @formatter:on

        SubscriptionEntry entry = new SubscriptionEntry(id, subscription);

        List<SubscriptionEntry> subscriptionEntries = this.sessions2entries.getOrDefault(this.session, new ArrayList<>());
        subscriptionEntries.add(entry);
        this.sessions2entries.put(this.session, subscriptionEntries);

    }

}
