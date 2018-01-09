package eu.techrain.histodbserver;

import java.security.Principal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import org.springframework.web.context.request.WebRequest;

public class WebRequestMock implements WebRequest {
    
    private final Map<String, Object> attributes = new HashMap();
    private final Map<String, String[]> parameters = new HashMap();
    
    
    public void setParameter(String name, String[] values){
        parameters.put(name,  values);
    }
    
    @Override
    public Object getAttribute(String name, int scope) {
        return attributes.get(name);
    }

    @Override
    public void setAttribute(String name, Object value, int scope) {
        attributes.put(name, value);
    }

    @Override
    public void removeAttribute(String name, int scope) {
        attributes.remove(name);
    }

    @Override
    public String[] getAttributeNames(int scope) {
        return (String[]) attributes.keySet().toArray();
    }

    @Override
    public void registerDestructionCallback(String name, Runnable callback, int scope) {
    }

    @Override
    public Object resolveReference(String key) {
        return null;
    }

    @Override
    public String getSessionId() {
        return null;
    }

    @Override
    public Object getSessionMutex() {
        return null;
    }

    @Override
    public String getHeader(String headerName) {
        return null;
    }

    @Override
    public String[] getHeaderValues(String headerName) {
        return null;
    }

    @Override
    public Iterator<String> getHeaderNames() {
        return null;
    }

    @Override
    public String getParameter(String paramName) {
        return parameters.get(paramName) != null && parameters.get(paramName).length > 0 ? parameters.get(paramName)[0] : null;
    }

    @Override
    public String[] getParameterValues(String paramName) {
        return parameters.get(paramName);
    }

    @Override
    public Iterator<String> getParameterNames() {
        return parameters.keySet().iterator();
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return parameters;
    }

    @Override
    public Locale getLocale() {
        return null;
    }

    @Override
    public String getContextPath() {
        return null;
    }

    @Override
    public String getRemoteUser() {
        return null;
    }

    @Override
    public Principal getUserPrincipal() {
        return null;
    }

    @Override
    public boolean isUserInRole(String role) {
        return false;
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public boolean checkNotModified(long lastModifiedTimestamp) {
        return false;
    }

    @Override
    public boolean checkNotModified(String etag) {
        return false;
    }

    @Override
    public boolean checkNotModified(String etag, long lastModifiedTimestamp) {
        return false;
    }

    @Override
    public String getDescription(boolean includeClientInfo) {
        return null;
    }

}
