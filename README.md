# OkHttpLogger

recomposed from Logging Interceptor https://github.com/square/okhttp/tree/master/okhttp-logging-interceptor

you will get direct information in logcat when you use okhttp/retrofit。

#### how to use :

```java
MyHttpLoggerInterceptor interceptor = new MyHttpLoggerInterceptor();
OkHttpClient client = new OkHttpClient.Builder()
  .addInterceptor(interceptor)
  .build();
```


## Download

```
compile 'Dingo.Demon:lib:1.0'
```
