/*
 * Copyright 2015-2016 Red Hat, Inc, and individual contributors.
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
package org.jboss.hal.client.configuration;

import java.util.List;
import java.util.Map;
import javax.inject.Inject;

import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.ProxyPlace;
import org.jboss.hal.core.finder.Finder;
import org.jboss.hal.core.finder.FinderPath;
import org.jboss.hal.core.mvp.ApplicationPresenter;
import org.jboss.hal.core.mvp.HasPresenter;
import org.jboss.hal.core.mvp.PatternFlyView;
import org.jboss.hal.dmr.dispatch.Dispatcher;
import org.jboss.hal.dmr.model.Composite;
import org.jboss.hal.dmr.model.CompositeResult;
import org.jboss.hal.dmr.model.NamedNode;
import org.jboss.hal.dmr.model.Operation;
import org.jboss.hal.dmr.model.OperationFactory;
import org.jboss.hal.dmr.model.ResourceAddress;
import org.jboss.hal.meta.AddressTemplate;
import org.jboss.hal.meta.StatementContext;
import org.jboss.hal.resources.Names;
import org.jboss.hal.resources.Resources;
import org.jboss.hal.spi.Message;
import org.jboss.hal.spi.MessageEvent;
import org.jboss.hal.spi.Requires;

import static org.jboss.hal.dmr.ModelDescriptionConstants.CHILD_TYPE;
import static org.jboss.hal.dmr.ModelDescriptionConstants.CONFIGURATION;
import static org.jboss.hal.dmr.ModelDescriptionConstants.READ_CHILDREN_RESOURCES_OPERATION;
import static org.jboss.hal.dmr.ModelNodeHelper.asNamedNodes;
import static org.jboss.hal.meta.token.NameTokens.PATH;

/**
 * @author Harald Pehl
 */
public class PathsPresenter extends
        ApplicationPresenter<PathsPresenter.MyView, PathsPresenter.MyProxy> {

    // @formatter:off
    @ProxyCodeSplit
    @NameToken(PATH)
    @Requires(ROOT_ADDRESS)
    public interface MyProxy extends ProxyPlace<PathsPresenter> {}

    public interface MyView extends PatternFlyView, HasPresenter<PathsPresenter> {
        void update(List<NamedNode> paths);
    }
    // @formatter:on


    static final String ROOT_ADDRESS = "/path=*";
    static final AddressTemplate ROOT_TEMPLATE = AddressTemplate.of(ROOT_ADDRESS);

    private final Dispatcher dispatcher;
    private final Resources resources;
    private final StatementContext statementContext;
    private final OperationFactory operationFactory;

    @Inject
    public PathsPresenter(final EventBus eventBus,
            final MyView view,
            final MyProxy proxy,
            final Finder finder,
            final Resources resources,
            final Dispatcher dispatcher,
            final StatementContext statementContext,
            final OperationFactory operationFactory) {
        super(eventBus, view, proxy, finder);
        this.resources = resources;
        this.dispatcher = dispatcher;
        this.statementContext = statementContext;
        this.operationFactory = operationFactory;
    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
    }

    @Override
    protected void onReset() {
        super.onReset();
        loadPaths();
    }

    @Override
    protected FinderPath finderPath() {
        return new FinderPath()
                .append(CONFIGURATION, Names.PATHS.toLowerCase(), Names.CONFIGURATION, Names.PATHS);
    }

    void loadPaths() {
        Operation operation = new Operation.Builder(READ_CHILDREN_RESOURCES_OPERATION, ResourceAddress.ROOT)
                .param(CHILD_TYPE, "path")
                .build();
        dispatcher.execute(operation, result -> getView().update(asNamedNodes(result.asPropertyList())));
    }

    void savePath(final String name, final Map<String, Object> changedValues) {
        Composite composite = operationFactory
                .fromChangeSet(ROOT_TEMPLATE.resolve(statementContext, name), changedValues);
        dispatcher.execute(composite, (CompositeResult result) -> {
            MessageEvent.fire(getEventBus(),
                    Message.success(resources.messages().modifyResourceSuccess(Names.PATH, name)));
            loadPaths();
        });
    }
}