/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.test.capedwarf.log.test.configuration;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.test.capedwarf.common.support.All;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:mluksa@redhat.com">Marko Luksa</a>
 */
@RunWith(Arquillian.class)
@Category(All.class)
public class CategoryLogLevelTest extends LoggingConfigurationTestBase {

    @Deployment
    public static Archive getDeployment() {
        return getDeploymentWithLoggingProperties(new StringAsset(
            ".level=WARNING\n" +
                ".com.google.appengine.tck.info.level=INFO"));
    }

    @Test
    public void testLogLevel() throws Exception {
        assertLogOnlyLogsMessagesAboveOrAtLevel(Logger.getLogger("com.google.appengine.tck.info.Test"), Level.INFO);
        assertLogOnlyLogsMessagesAboveOrAtLevel(Logger.getLogger("com.google.appengine.tck.other.Test"), Level.WARNING);
    }
}
