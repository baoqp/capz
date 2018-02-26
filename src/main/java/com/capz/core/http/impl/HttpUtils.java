package com.capz.core.http.impl;

import com.capz.core.http.HttpServerRequest;
import io.netty.handler.codec.compression.ZlibWrapper;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http2.Http2Headers;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;


public final class HttpUtils {

    private HttpUtils() {
    }

    private static int indexOfSlash(CharSequence str, int start) {
        for (int i = start; i < str.length(); i++) {
            if (str.charAt(i) == '/') {
                return i;
            }
        }

        return -1;
    }

    private static boolean matches(CharSequence path, int start, String what) {
        return matches(path, start, what, false);
    }

    private static boolean matches(CharSequence path, int start, String what, boolean exact) {
        if (exact) {
            if (path.length() - start != what.length()) {
                return false;
            }
        }

        if (path.length() - start >= what.length()) {
            for (int i = 0; i < what.length(); i++) {
                if (path.charAt(start + i) != what.charAt(i)) {
                    return false;
                }
            }
            return true;
        }

        return false;
    }

    /**
     * Removed dots as per <a href="http://tools.ietf.org/html/rfc3986#section-5.2.4>rfc3986</a>.
     * <p>
     * There are 2 extra transformations that are not part of the spec but kept for backwards compatibility:
     * <p>
     * double slash // will be converted to single slash and the path will always start with slash.
     *
     * @param path raw path
     * @return normalized path
     */
    public static String removeDots(CharSequence path) {

        if (path == null) {
            return null;
        }

        final StringBuilder obuf = new StringBuilder(path.length());

        int i = 0;
        while (i < path.length()) {
            // remove dots as described in
            // http://tools.ietf.org/html/rfc3986#section-5.2.4
            if (matches(path, i, "./")) {
                i += 2;
            } else if (matches(path, i, "../")) {
                i += 3;
            } else if (matches(path, i, "/./")) {
                // preserve last slash
                i += 2;
            } else if (matches(path, i, "/.", true)) {
                path = "/";
                i = 0;
            } else if (matches(path, i, "/../")) {
                // preserve last slash
                i += 3;
                int pos = obuf.lastIndexOf("/");
                if (pos != -1) {
                    obuf.delete(pos, obuf.length());
                }
            } else if (matches(path, i, "/..", true)) {
                path = "/";
                i = 0;
                int pos = obuf.lastIndexOf("/");
                if (pos != -1) {
                    obuf.delete(pos, obuf.length());
                }
            } else if (matches(path, i, ".", true) || matches(path, i, "..", true)) {
                break;
            } else {
                if (path.charAt(i) == '/') {
                    i++;
                    // Not standard!!!
                    // but common // -> /
                    if (obuf.length() == 0 || obuf.charAt(obuf.length() - 1) != '/') {
                        obuf.append('/');
                    }
                }
                int pos = indexOfSlash(path, i);
                if (pos != -1) {
                    obuf.append(path, i, pos);
                    i = pos;
                } else {
                    obuf.append(path, i, path.length());
                    break;
                }
            }
        }

        return obuf.toString();
    }

    /**
     * Resolve an URI reference as per <a href="http://tools.ietf.org/html/rfc3986#section-5.2.4>rfc3986</a>
     */
    public static URI resolveURIReference(String base, String ref) throws URISyntaxException {
        return resolveURIReference(URI.create(base), ref);
    }

    /**
     * Resolve an URI reference as per <a href="http://tools.ietf.org/html/rfc3986#section-5.2.4>rfc3986</a>
     */
    public static URI resolveURIReference(URI base, String ref) throws URISyntaxException {
        URI _ref = URI.create(ref);
        String scheme;
        String authority;
        String path;
        String query;
        if (_ref.getScheme() != null) {
            scheme = _ref.getScheme();
            authority = _ref.getAuthority();
            path = removeDots(_ref.getPath());
            query = _ref.getRawQuery();
        } else {
            if (_ref.getAuthority() != null) {
                authority = _ref.getAuthority();
                path = _ref.getPath();
                query = _ref.getRawQuery();
            } else {
                if (_ref.getPath().length() == 0) {
                    path = base.getPath();
                    if (_ref.getRawQuery() != null) {
                        query = _ref.getRawQuery();
                    } else {
                        query = base.getRawQuery();
                    }
                } else {
                    if (_ref.getPath().startsWith("/")) {
                        path = removeDots(_ref.getPath());
                    } else {
                        // Merge paths
                        String mergedPath;
                        String basePath = base.getPath();
                        if (base.getAuthority() != null && basePath.length() == 0) {
                            mergedPath = "/" + _ref.getPath();
                        } else {
                            int index = basePath.lastIndexOf('/');
                            if (index > -1) {
                                mergedPath = basePath.substring(0, index + 1) + _ref.getPath();
                            } else {
                                mergedPath = _ref.getPath();
                            }
                        }
                        path = removeDots(mergedPath);
                    }
                    query = _ref.getRawQuery();
                }
                authority = base.getAuthority();
            }
            scheme = base.getScheme();
        }
        return new URI(scheme, authority, path, query, _ref.getFragment());
    }

    /**
     * Extract the path out of the uri.
     */
    static String parsePath(String uri) {
        int i;
        if (uri.charAt(0) == '/') {
            i = 0;
        } else {
            i = uri.indexOf("://");
            if (i == -1) {
                i = 0;
            } else {
                i = uri.indexOf('/', i + 3);
                if (i == -1) {
                    // contains no /
                    return "/";
                }
            }
        }

        int queryStart = uri.indexOf('?', i);
        if (queryStart == -1) {
            queryStart = uri.length();
        }
        return uri.substring(i, queryStart);
    }

    /**
     * Extract the query out of a uri or returns {@code null} if no query was found.
     */
    static String parseQuery(String uri) {
        int i = uri.indexOf('?');
        if (i == -1) {
            return null;
        } else {
            return uri.substring(i + 1, uri.length());
        }
    }

    static String absoluteURI(String serverOrigin, HttpServerRequest req) throws URISyntaxException {
        String absoluteURI;
        URI uri = new URI(req.uri());
        String scheme = uri.getScheme();
        if (scheme != null && (scheme.equals("http") || scheme.equals("https"))) {
            absoluteURI = uri.toString();
        } else {
            String host = req.host();
            if (host != null) {
                absoluteURI = req.scheme() + "://" + host + uri;
            } else {
                // Fall back to the server origin
                absoluteURI = serverOrigin + uri;
            }
        }
        return absoluteURI;
    }

    static Map<String, List<String>> params(String uri) {
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(uri);
        return queryStringDecoder.parameters();

    }




    private static class CustomCompressor extends HttpContentCompressor {
        @Override
        public ZlibWrapper determineWrapper(String acceptEncoding) {
            return super.determineWrapper(acceptEncoding);
        }
    }

    private static final CustomCompressor compressor = new CustomCompressor();

    static String determineContentEncoding(Http2Headers headers) {
        String acceptEncoding = headers.get(HttpHeaderNames.ACCEPT_ENCODING) != null ? headers.get(HttpHeaderNames.ACCEPT_ENCODING).toString() : null;
        if (acceptEncoding != null) {
            ZlibWrapper wrapper = compressor.determineWrapper(acceptEncoding);
            if (wrapper != null) {
                switch (wrapper) {
                    case GZIP:
                        return "gzip";
                    case ZLIB:
                        return "deflate";
                }
            }
        }
        return null;
    }

    static HttpMethod toNettyHttpMethod(com.capz.core.http.HttpMethod method, String rawMethod) {
        switch (method) {
            case CONNECT: {
                return HttpMethod.CONNECT;
            }
            case GET: {
                return HttpMethod.GET;
            }
            case PUT: {
                return HttpMethod.PUT;
            }
            case POST: {
                return HttpMethod.POST;
            }
            case DELETE: {
                return HttpMethod.DELETE;
            }
            case HEAD: {
                return HttpMethod.HEAD;
            }
            case OPTIONS: {
                return HttpMethod.OPTIONS;
            }
            case TRACE: {
                return HttpMethod.TRACE;
            }
            case PATCH: {
                return HttpMethod.PATCH;
            }
            default: {
                return HttpMethod.valueOf(rawMethod);
            }
        }
    }


    static com.capz.core.http.HttpMethod toVertxMethod(String method) {
        try {
            return com.capz.core.http.HttpMethod.valueOf(method);
        } catch (IllegalArgumentException e) {
            return com.capz.core.http.HttpMethod.OTHER;
        }
    }
}
