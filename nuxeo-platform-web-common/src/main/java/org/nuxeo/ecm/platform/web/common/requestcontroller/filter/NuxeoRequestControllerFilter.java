/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.web.common.requestcontroller.filter;

import java.io.IOException;
import java.security.Principal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.web.common.ServletHelper;
import org.nuxeo.ecm.platform.web.common.exceptionhandling.ExceptionHelper;
import org.nuxeo.ecm.platform.web.common.requestcontroller.service.RequestControllerManager;
import org.nuxeo.ecm.platform.web.common.requestcontroller.service.RequestFilterConfig;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Filter to handle Transactions and Requests synchronization. This filter is
 * useful when accessing web resources that are not protected by Seam Filter.
 * This is the case for specific Servlets, WebEngine, XML-RPC connector ...
 *
 * @author tiry
 */
public class NuxeoRequestControllerFilter implements Filter {

    protected static final String SESSION_LOCK_KEY = "NuxeoSessionLockKey";

    protected static final String SYNCED_REQUEST_FLAG = "NuxeoSessionAlreadySync";

    // FIXME: typo in constant name.
    protected static final int LOCK_TIMOUT_S = 120;

    public static final DateFormat HTTP_EXPIRES_DATE_FORMAT = httpExpiresDateFormat();

    protected static RequestControllerManager rcm;

    private static final Log log = LogFactory.getLog(NuxeoRequestControllerFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        doInitIfNeeded();
    }

    private static void doInitIfNeeded() {
        if (rcm == null) {
            if (Framework.getRuntime() != null) {
                rcm = Framework.getLocalService(RequestControllerManager.class);

                if (rcm == null) {
                    log.error("Unable to get RequestControllerManager service");
                    // throw new ServletException(
                    // "RequestControllerManager can not be found");
                }
                log.debug("Staring NuxeoRequestController filter");
            } else {
                log.debug("Postpone filter init since Runtime is not yet available");
            }
        }
    }

    public static String doFormatLogMessage(HttpServletRequest request,
            String info) {
        String remoteHost = RemoteHostGuessExtractor.getRemoteHost(request);
        Principal principal = request.getUserPrincipal();
        String principalName = principal != null ? principal.getName() : "none";
        String uri = request.getRequestURI();
        HttpSession session = request.getSession(false);
        String sessionId = session != null ? session.getId() : "none";
        String threadName = Thread.currentThread().getName();
        return "remote=" + remoteHost + ",principal=" + principalName + ",uri="
                + uri + ",session=" + sessionId + ",thread=" + threadName
                + ",info=" + info;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if (log.isDebugEnabled()) {
            log.debug(doFormatLogMessage(httpRequest,
                    "Entering NuxeoRequestController filter"));
        }

        doInitIfNeeded();

        RequestFilterConfig config = rcm.getConfigForRequest(httpRequest);

        boolean useSync = config.needSynchronization();
        boolean useTx = config.needTransaction();

        // Add cache header if needed
        if (httpRequest.getMethod().equals("GET")) {
            boolean isCached = config.isCached();
            if (isCached) {
                addCacheHeader(httpResponse, config.isPrivate(),
                        config.getCacheTime());
            }
        }

        if (!useSync && !useTx) {
            if (log.isDebugEnabled()) {
                log.debug(doFormatLogMessage(httpRequest,
                        "Existing NuxeoRequestController filter: nothing to be done"));
            }

            try {
                chain.doFilter(request, response);
            } catch (ServletException e) {
                Throwable unwrappedError = ExceptionHelper.unwrapException(e);
                if (ExceptionHelper.isClientAbortError(unwrappedError)) {
                    log.warn("Client disconnected: "
                            + unwrappedError.getMessage());
                } else {
                    throw e;
                }
            }
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug(doFormatLogMessage(httpRequest,
                    "Handling request with tx=" + useTx + " and sync="
                            + useSync));
        }

        boolean sessionSynched = false;
        if (useSync) {
            sessionSynched = simpleSyncOnSession(httpRequest);
        }
        boolean txStarted = false;
        try {
            if (useTx) {
                txStarted = ServletHelper.startTransaction(httpRequest);
                if (txStarted) {
                    response = new BufferingHttpServletResponse(httpResponse);
                }
            }
            chain.doFilter(request, response);
        } catch (Exception e) {
            log.error(
                    doFormatLogMessage(httpRequest,
                            "Unhandled error was caught by the Filter"), e);
            if (txStarted) {
                if (log.isDebugEnabled()) {
                    log.debug(doFormatLogMessage(httpRequest,
                            "Marking transaction for RollBack"));
                }
                TransactionHelper.setTransactionRollbackOnly();
            }
            throw new ServletException(e);
        } finally {
            if (txStarted) {
                try {
                    TransactionHelper.commitOrRollbackTransaction();
                } finally {
                    ((BufferingHttpServletResponse) response).stopBuffering();
                }
            }
            if (sessionSynched) {
                simpleReleaseSyncOnSession(httpRequest);
            }
            if (log.isDebugEnabled()) {
                log.debug(doFormatLogMessage(httpRequest,
                        "Exiting NuxeoRequestController filter"));
            }
        }
    }

    /**
     * Synchronizes the HttpSession.
     * <p>
     * Uses a {@link Lock} object in the HttpSession and locks it. If
     * HttpSession is not created, exits without locking anything.
     */
    public static boolean simpleSyncOnSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            if (log.isDebugEnabled()) {
                log.debug(doFormatLogMessage(request,
                        "HttpSession does not exist, this request won't be synched"));
            }
            return false;
        }

        if (log.isDebugEnabled()) {
            log.debug(doFormatLogMessage(request, "Trying to sync on session "));
        }

        if (request.getAttribute(SYNCED_REQUEST_FLAG) != null) {
            if (log.isWarnEnabled()) {
                log.warn(doFormatLogMessage(request,
                        "Request has already be synced, filter is reentrant, exiting without locking"));
            }
            return false;
        }

        Lock lock = (Lock) session.getAttribute(SESSION_LOCK_KEY);
        if (lock == null) {
            lock = new ReentrantLock();
            session.setAttribute(SESSION_LOCK_KEY, lock);
        }

        boolean locked = false;
        try {
            locked = lock.tryLock(LOCK_TIMOUT_S, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error(
                    doFormatLogMessage(request,
                            "Unable to acquire lock for Session sync"), e);
            return false;
        }

        if (locked) {
            request.setAttribute(SYNCED_REQUEST_FLAG, true);
            if (log.isDebugEnabled()) {
                log.debug(doFormatLogMessage(request,
                        "Request synced on session"));
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug(doFormatLogMessage(request, "Sync timeout"));
            }
        }

        return locked;
    }

    /**
     * Releases the {@link Lock} if present in the HttpSession.
     */
    public static boolean simpleReleaseSyncOnSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            if (log.isDebugEnabled()) {
                log.debug(doFormatLogMessage(request,
                        "No more HttpSession: can not unlock !, HttpSession must have been invalidated"));
            }
            return false;
        }
        log.debug("Trying to unlock on session " + session.getId()
                + " on Thread " + Thread.currentThread().getId());

        Lock lock = (Lock) session.getAttribute(SESSION_LOCK_KEY);
        if (lock == null) {
            log.error("Unable to find session lock, HttpSession may have been invalidated");
            return false;
        } else {
            try {
                lock.unlock();
            } catch (Throwable t) {
                log.debug("Unlock failed on request " + request.getRequestURI());
                return false;
            }
            if (request.getAttribute(SYNCED_REQUEST_FLAG) != null) {
                request.removeAttribute(SYNCED_REQUEST_FLAG);
            }
            if (log.isDebugEnabled()) {
                log.debug("session unlocked on Thread ");
            }
            return true;
        }
    }

    private static DateFormat httpExpiresDateFormat() {
        // formatted http Expires: Thu, 01 Dec 1994 16:00:00 GMT
        DateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z",
                Locale.US);
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        return df;
    }

    /**
     * Set cache parameters to httpResponse.
     */
    public static void addCacheHeader(HttpServletResponse httpResponse,
            Boolean isPrivate, String cacheTime) {
        if (isPrivate) {
            httpResponse.addHeader("Cache-Control", "private, max-age="
                    + cacheTime);
        } else {
            httpResponse.addHeader("Cache-Control", "public, max-age="
                    + cacheTime);
        }
        // Generating expires using current date and adding cache time.
        // we are using the format Expires: Thu, 01 Dec 1994 16:00:00 GMT
        Date date = new Date();
        long newDate = date.getTime() + new Long(cacheTime) * 1000;
        date.setTime(newDate);

        httpResponse.setHeader("Expires", HTTP_EXPIRES_DATE_FORMAT.format(date));
    }

    @Override
    public void destroy() {
        rcm = null;
    }

}
