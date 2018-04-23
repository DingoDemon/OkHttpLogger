# OkHttpLogger

recomposed from Logging Interceptor https://github.com/square/okhttp/tree/master/okhttp-logging-interceptor

you will get direct information for log when you use okhttp/retrofit。

#### how to use :

```java
MyHttpLoggerInterceptor interceptor = new MyHttpLoggerInterceptor();
OkHttpClient client = new OkHttpClient.Builder()
  .addInterceptor(interceptor)
  .build();
```


## Download

```
compile 'Dingo.Demon:OkhttpLogger:1.0'
```
