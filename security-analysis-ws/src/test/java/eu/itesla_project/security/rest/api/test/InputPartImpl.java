/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.security.rest.api.test;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Objects;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.util.GenericType;

/**
*
* @author Giovanni Ferrari <giovanni.ferrari at techrain.it>
*/
public class InputPartImpl implements InputPart {

    private final Object body;

    private MediaType mediaType;

    private MultivaluedMap<String, String> headers;

    InputPartImpl(Object body, MediaType mediaType) {
        this.body = Objects.requireNonNull(body);
        this.mediaType = Objects.requireNonNull(mediaType);
    }

    InputPartImpl(Object body, MediaType mediaType, MultivaluedMap<String, String> headers) {
        this.body = Objects.requireNonNull(body);
        this.mediaType = Objects.requireNonNull(mediaType);
        this.headers = headers;
    }

    public MultivaluedMap<String, String> getHeaders() {
        return headers;
    }

    public String getBodyAsString() throws IOException {
        return body.toString();
    }

    @Override
    public <T> T getBody(Class<T> type, Type genericType) throws IOException {
        return (T) body;
    }

    public <T> T getBody(GenericType<T> type) throws IOException {
        return (T) body;
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public boolean isContentTypeFromMessage() {
        return false;
    }

    @Override
    public void setMediaType(MediaType arg0) {
        this.mediaType = Objects.requireNonNull(mediaType);
    }
}
