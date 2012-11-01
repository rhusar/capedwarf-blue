/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.capedwarf.datastore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.google.appengine.api.datastore.DatastoreAttributes;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceConfig;
import com.google.appengine.api.datastore.DeleteContext;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Index;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyRange;
import com.google.appengine.api.datastore.PreGetContext;
import com.google.appengine.api.datastore.PutContext;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.datastore.TransactionOptions;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.jboss.capedwarf.common.threads.DirectFuture;

/**
 * JBoss DatastoreService impl.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 * @author <a href="mailto:marko.luksa@gmail.com">Marko Luksa</a>
 */
public class JBossDatastoreService extends AbstractDatastoreService implements DatastoreService {
    public JBossDatastoreService() {
        this(null);
    }

    public JBossDatastoreService(DatastoreServiceConfig config) {
        super(new DatastoreServiceImpl(config));
    }

    protected <T> Future<T> wrap(final Transaction transaction, final Callable<T> callable, final Runnable pre, final Function<T, Void> post) {
        return DirectFuture.create(new Callable<T>() {
            public T call() throws Exception {
                if (pre != null) {
                    pre.run();
                }
                final T result = callable.call();
                if (post != null) {
                    post.apply(result);
                }
                return result;
            }
        });
    }

    protected <T> T unwrap(Future<T> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public Entity get(Key key) throws EntityNotFoundException {
        return get(getCurrentTransaction(null), key);
    }

    public Entity get(Transaction transaction, Key key) throws EntityNotFoundException {
        final Entity result = unwrap(doGet(transaction, key));
        if (result == null) {
            throw new EntityNotFoundException(key);
        }
        return result;
    }

    public Map<Key, Entity> get(Iterable<Key> keys) {
        return get(getCurrentTransaction(null), keys);
    }

    public Map<Key, Entity> get(final Transaction transaction, final Iterable<Key> keys) {
        final Map<Key, Entity> map = new LinkedHashMap<Key, Entity>();
        final PreGetContext preGetContext = DatastoreCallbacks.createPreGetContext(transaction, Lists.newArrayList(keys), map);
        final Function<Key, Void> pre = new Function<Key, Void>() {
            public Void apply(Key input) {
                getDatastoreCallbacks().executePreGetCallbacks(preGetContext);
                return null;
            }
        };
        final List<Entity> results = new ArrayList<Entity>();
        final Function<Map.Entry<Key, Entity>, Void> post = new Function<Map.Entry<Key, Entity>, Void>() {
            public Void apply(Map.Entry<Key, Entity> input) {
                getDatastoreCallbacks().executePostLoadCallbacks(DatastoreCallbacks.createPostLoadContext(transaction, results));
                return null;
            }
        };
        for (Key key : keys) {
            pre.apply(key);
        }
        for (Key key : keys) {
            Entity previous = map.get(key);
            if (previous == null) {
                final Entity entity = getDelegate().get(transaction, key);
                if (entity != null) {
                    map.put(key, entity);
                    previous = entity;
                }
            }
            if (previous != null) {
                results.add(previous);
            }
        }
        for (Map.Entry<Key, Entity> entry : map.entrySet()) {
            post.apply(entry);
        }
        return map;
    }

    public Key put(Entity entity) {
        return put(getCurrentTransaction(null), entity);
    }

    public Key put(Transaction transaction, Entity entity) {
        return unwrap(doPut(transaction, entity));
    }

    public List<Key> put(Iterable<Entity> entities) {
        return put(getCurrentTransaction(null), entities);
    }

    public List<Key> put(Transaction transaction, Iterable<Entity> entities) {
        final PutContext preContext = DatastoreCallbacks.createPutContext(transaction, Lists.newArrayList(entities));
        final Function<Entity, Void> pre = new Function<Entity, Void>() {
            public Void apply(Entity input) {
                getDatastoreCallbacks().executePrePutCallbacks(preContext);
                return null;
            }
        };
        final PutContext postContext = DatastoreCallbacks.createPutContext(transaction, Lists.newArrayList(entities));
        final Function<Entity, Void> post = new Function<Entity, Void>() {
            public Void apply(Entity input) {
                getDatastoreCallbacks().executePostPutCallbacks(postContext);
                return null;
            }
        };
        for (Entity entity : entities) {
            pre.apply(entity);
        }
        final List<Key> keys = new ArrayList<Key>();
        for (Entity entity : entities) {
            keys.add(getDelegate().put(transaction, entity));
        }
        for (Entity entity : entities) {
            post.apply(entity);
        }
        return keys;
    }

    public void delete(Key... keys) {
        delete(getCurrentTransaction(null), keys);
    }

    public void delete(Transaction transaction, Key... keys) {
        delete(transaction, Arrays.asList(keys));
    }

    public void delete(Iterable<Key> keys) {
        delete(getCurrentTransaction(null), keys);
    }

    public void delete(final Transaction transaction, final Iterable<Key> keys) {
        final DeleteContext preContext = DatastoreCallbacks.createDeleteContext(transaction, Lists.newArrayList(keys));
        final Function<Key, Void> pre = new Function<Key, Void>() {
            public Void apply(Key input) {
                getDatastoreCallbacks().executePreDeleteCallbacks(preContext);
                return null;
            }
        };
        final DeleteContext postContext = DatastoreCallbacks.createDeleteContext(transaction, Lists.newArrayList(keys));
        final Function<Key, Void> post = new Function<Key, Void>() {
            public Void apply(Key input) {
                getDatastoreCallbacks().executePostDeleteCallbacks(postContext);
                return null;
            }
        };
        for (Key key : keys) {
            pre.apply(key);
        }
        for (Key key : keys) {
            getDelegate().delete(transaction, key);
        }
        for (Key key : keys) {
            post.apply(key);
        }
    }

    public Transaction beginTransaction() {
        return beginTransaction(TransactionOptions.Builder.withDefaults());
    }

    public Transaction beginTransaction(final TransactionOptions transactionOptions) {
        return getDelegate().beginTransaction(transactionOptions);
    }

    public KeyRange allocateIds(String kind, long num) {
        return allocateIds(null, kind, num);
    }

    public KeyRange allocateIds(Key key, String s, long l) {
        return getDelegate().allocateIds(key, s, l);
    }

    public KeyRangeState allocateIdRange(KeyRange keys) {
        return getDelegate().allocateIdRange(keys);
    }

    public DatastoreAttributes getDatastoreAttributes() {
        return getDelegate().getDatastoreAttributes();
    }

    public Map<Index, Index.IndexState> getIndexes() {
        return getDelegate().getIndexes();
    }

    /**
     * Testing only!
     */
    public void clearCache() {
        getDelegate().clearCache();
    }
}
