package logger.dingo.com.okhttplogger;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

import okhttp3.Connection;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.http.HttpHeaders;
import okhttp3.internal.platform.Platform;
import okio.Buffer;

import static android.util.Log.INFO;

public class MyHttpLoggerInterceptor implements Interceptor {
    static final Charset UTF_8 = Charset.forName("UTF-8");
    private volatile Level level;
    private Logger logger;


    public MyHttpLoggerInterceptor() {
        this(Level.NORMAL, Logger.DEFAULT);
    }

    public MyHttpLoggerInterceptor(Level level, Logger logger) {
        this.level = level;
        this.logger = logger;
    }

    public MyHttpLoggerInterceptor setLevel(Level level) {
        if (level == null) throw new NullPointerException("level == null. Use Level.NONE instead.");
        this.level = level;
        return this;
    }


    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        if (this.level == Level.NONE) {
            return chain.proceed(request);
        } else {
            this.logRequest(request, chain.connection());
            long current = System.nanoTime();

            Response response;
            try {
                response = chain.proceed(request);
            } catch (Exception exception) {
                logger.log("<-- HTTP FAILED: " + exception);
                throw exception;
            }

            long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - current);
            return this.logResponse(response, duration);
        }
    }


    private void logRequest(Request request, Connection connection) {
        boolean onlyHeader = this.level == Level.HEADERS;
        boolean logHeaders = this.level == Level.HEADERS || this.level == Level.NORMAL;
        RequestBody requestBody = request.body();
        boolean hasBody = requestBody != null;
        Protocol protocol = connection != null ? connection.protocol() : Protocol.HTTP_1_1;
        StringBuilder stringBuilder = new StringBuilder();

        try {
            String info = "--> " + request.method() + ' ' + request.url() + ' ' + protocol + " \n";
            stringBuilder.append(info);
            if (logHeaders) {
                Headers headers = request.headers();

                for (int i = 0; i < headers.size(); i++) {
                    stringBuilder.append("\t<Headers：========" + headers.name(i) + ": " + headers.value(i) + "========> \n");
                }
            }
            stringBuilder.append(" ");
            if (!onlyHeader && hasBody) {
                if (onlyTextMessage(requestBody.contentType())) {
                    this.appendBodyInfo(request, stringBuilder);
                } else {
                    stringBuilder.append("\tbody: maybe [file part]  , ignored! \n");
                }
            }
        } catch (Exception e) {
            logger.log("<-- HTTP FAILED: " + e);
        } finally {
            stringBuilder.append("\n --> END " + request.method());
            logger.log(stringBuilder.toString());
        }

    }

    private static boolean onlyTextMessage(MediaType mediaType) {
        if (mediaType == null) {
            return false;
        } else if (mediaType.type() != null && mediaType.type().equals("text")) {
            return true;
        } else {
            String s = mediaType.subtype();
            if (s != null) {
                s = s.toLowerCase();
                if (s.contains("x-www-form-urlencoded") || s.contains("json") || s.contains("xml") || s.contains("html")) {
                    return true;
                }
            }

            return false;
        }
    }


    private Response logResponse(Response response, long ms) {
        ResponseBody responseBody = response.body();
        boolean onlyHeader = this.level == Level.HEADERS;
        boolean logHeaders = this.level == Level.HEADERS || this.level == Level.NORMAL;
        boolean logBody = this.level == Level.BODY || this.level == Level.NORMAL;
        boolean fail = false;
        StringBuilder stringBuilder = new StringBuilder();

        try {
            stringBuilder.append("<-- " + response.code() + ' ' + response.message() + ' ' + response.request().url() + " (" + ms + "ms） \n");
            if (!logHeaders && !logBody) {
                return response;
            }

            if (logHeaders) {
                Headers headers = response.headers();
                for (int i = 0; i < headers.size(); i++) {
                    stringBuilder.append("\t<Headers========" + headers.name(i) + ": " + headers.value(i) + "========> \n");
                }
            }

            stringBuilder.append(" ");
            if (onlyHeader || !HttpHeaders.hasBody(response)) {
                return response;
            }

            if (!onlyTextMessage(responseBody.contentType())) {
                stringBuilder.append("\tbody: maybe [file part] , too large too print , ignored!");
                return response;
            }

            String bodyString = responseBody.string();
            stringBuilder.append("\t<body~~~~~~~~~: "  + bodyString + "~~~~~~~~>");
        } catch (Exception exception) {
            stringBuilder.append("\n <-- END HTTP");
            logger.log(stringBuilder.toString());
            fail = true;
            return response;
        } finally {
            if (!fail) {
                stringBuilder.append("\n <-- END HTTP");
                logger.log(stringBuilder.toString());
            }

        }

        return response;
    }


    private void appendBodyInfo(Request request, StringBuilder builder) {
        try {
            Request request1 = request.newBuilder().build();
            Buffer buffer = new Buffer();
            request1.body().writeTo(buffer);
            Charset c = UTF_8;
            MediaType mediaType = request1.body().contentType();
            if (mediaType != null) {
                c = mediaType.charset(UTF_8);
            }

            builder.append("\t<body~~~~~~~~~: " + buffer.readString(c) + "~~~~~~~~>");
        } catch (Exception exception) {
            exception.printStackTrace();
        }

    }


    public interface Logger {
        void log(String message);

        /**
         * A {@link Logger} defaults output appropriate for the current platform.
         */
        Logger DEFAULT = new Logger() {
            @Override
            public void log(String message) {
                Platform.get().log(INFO, message, null);
            }
        };
    }


    public  enum Level {
        NONE,
        HEADERS,
        BODY,
        NORMAL;

        private Level() {

        }
    }
}
