/*
 * Copyright (c) 2008, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package org.glassfish.grizzly.filterchain;

import org.glassfish.grizzly.IOEvent;
import java.io.IOException;

/**
 * Executes appropriate {@link Filter} processing method to process occurred
 * {@link IOEvent}.
 */
public interface FilterExecutor {

    NextAction execute(Filter filter, FilterChainContext context)
            throws IOException;

    int defaultStartIdx(FilterChainContext context);

    int defaultEndIdx(FilterChainContext context);

    int getNextFilter(FilterChainContext context);

    int getPreviousFilter(FilterChainContext context);

    void initIndexes(FilterChainContext context);

    boolean hasNextFilter(FilterChainContext context, int idx);

    boolean hasPreviousFilter(FilterChainContext context, int idx);

    boolean isUpstream();

    boolean isDownstream();
}
