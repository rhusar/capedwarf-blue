/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.test.capedwarf.datastore.test;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.PropertyProjection;
import com.google.appengine.api.datastore.Query;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Datastore querying optimizations tests.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 * @author <a href="mailto:marko.luksa@gmail.com">Marko Luksa</a>
 */
@RunWith(Arquillian.class)
public class QueryOptimizationsTestCase extends QueryTest {

    @Test
    public void testDummy() throws Exception {
        // remote this
    }

    @Test
    public void testKeysOnly() throws Exception {
        Entity john = createEntity("Person", 1)
                .withProperty("name", "John")
                .withProperty("surname", "Doe")
                .store();

        Query query = new Query("Person").setKeysOnly();

        PreparedQuery preparedQuery = service.prepare(query);

        Entity entity = preparedQuery.asSingleEntity();
        assertEquals(john.getKey(), entity.getKey());
        assertNull(entity.getProperty("name"));
        assertNull(entity.getProperty("surname"));
    }

    @Test
    public void testProjections() throws Exception {
        Entity e = createEntity("Product", 1)
                .withProperty("price", 123L)
                .withProperty("percent", 0.123)
                .withProperty("x", -0.321)
                .withProperty("diff", -5L)
                .withProperty("weight", 10L)
                .store();

        Query query = new Query("Product")
                .addProjection(new PropertyProjection("price", Long.class))
                .addProjection(new PropertyProjection("percent", Double.class))
                .addProjection(new PropertyProjection("x", Double.class))
                .addProjection(new PropertyProjection("diff", Long.class));

        PreparedQuery preparedQuery = service.prepare(query);
        Entity result = preparedQuery.asSingleEntity();
        assertEquals(e.getKey(), result.getKey());
        assertEquals(e.getProperty("price"), result.getProperty("price"));
        assertEquals(e.getProperty("percent"), result.getProperty("percent"));
        assertEquals(e.getProperty("x"), result.getProperty("x"));
        assertEquals(e.getProperty("diff"), result.getProperty("diff"));
        assertNull(result.getProperty("weight"));
    }
}
