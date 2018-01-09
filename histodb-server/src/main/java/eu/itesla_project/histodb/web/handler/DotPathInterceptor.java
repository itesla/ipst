/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.histodb.web.handler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
*
* @author Giovanni Ferrari <giovanni.ferrari@techrain.it>
*/
@Component
public class DotPathInterceptor implements HandlerInterceptor {

    @Override
    public void afterCompletion(HttpServletRequest req, HttpServletResponse res, Object handler, Exception exc)
            throws Exception {
    }

    @Override
    public void postHandle(HttpServletRequest req, HttpServletResponse res, Object handler, ModelAndView mv)
            throws Exception {
    }

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws Exception {
        String uri = req.getRequestURI();
        String lastPath = uri.substring(uri.lastIndexOf("/"));
        if (lastPath.indexOf(".") > 0) {
            String format = lastPath.substring(lastPath.indexOf(".") + 1);
            req.setAttribute("format", format);
        }
        return true;
    }

}
