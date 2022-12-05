# WebClient-with-CompletableFuture

This Spring boot app is created to demonstrate/reproduce the issue described at https://github.com/reactor/reactor-netty/issues/2582

See [Makefile](Makefile) to build, run and test the App

## Config
The app can be configured from [.env](/.env) or from [src\main\resources\application.properties](src\main\resources\application.properties)

## Details

- At `HttpBinWebClientConfig`, we configure and create a `WebClient` using `HttpClient.create()`
- `HttpBinClient` uses the above `WebClient` to send a GET request to `httpbin.org`
- ReproducerController exposes two GET endpoints
  - sendGet
  - sendAsyncGet (uses CompletableFuture)

## Issue

After the application starts, When HttpBinClient is invoked for the first time,
- `reactor-http-nio-*` threads are created
- a new [http] client pool is created
- a new pooled channel is created

If HttpBinClient is invoked from a CompletableFuture, the `reactor-http-nio-*` threads inherit the classLoaders of `ForkJoinPool.commonPool-worker-*` thread.

```js
12:47.760 TRACE 21416 --- [http-nio-8080-exec-1] org.springframework.web.servlet.DispatcherServlet : GET "/test/sendAsyncGet", parameters={}, headers={masked} in DispatcherServlet 'dispatcherServlet'
12:47.783 INFO  21416 --- [http-nio-8080-exec-1] com.test.ReproducerController : TCCL is TomcatEmbeddedWebappClassLoader
context: ROOT
delegate: true
----------> Parent Classloader:
org.springframework.boot.loader.LaunchedURLClassLoader@4501b7af

12:47.784 INFO  21416 --- [http-nio-8080-exec-1] com.test.ReproducerController : CL is org.springframework.boot.loader.LaunchedURLClassLoader@4501b7af

12:47.789 INFO  21416 --- [ForkJoinPool.commonPool-worker-3] com.test.ReproducerController : TCCL is jdk.internal.loader.ClassLoaders$AppClassLoader@30946e09
12:47.789 INFO  21416 --- [ForkJoinPool.commonPool-worker-3] com.test.ReproducerController : CL is org.springframework.boot.loader.LaunchedURLClassLoader@4501b7af

12:49.643 DEBUG 21416 --- [ForkJoinPool.commonPool-worker-3] reactor.netty.resources.PooledConnectionProvider : Creating a new [http] client pool [PoolFactory{evictionInterval=PT0S, leasingStrategy=fifo, maxConnections=500, maxIdleTime=-1, maxLifeTime=-1, metricsEnabled=false, pendingAcquireMaxCount=1000, pendingAcquireTimeout=45000}] for [httpbin.org:443]
12:50.628 DEBUG 21416 --- [reactor-http-nio-2] reactor.netty.resources.PooledConnectionProvider : [d01091ce] Created a new pooled channel, now: 0 active connections, 0 inactive connections and 0 pending acquire requests.

12:50.691 DEBUG 21416 --- [reactor-http-nio-2] reactor.netty.tcp.SslProvider : [d01091ce] SSL enabled using engine sun.security.ssl.SSLEngineImpl@389631ac and SNI httpbin.org:443
12:50.738 DEBUG 21416 --- [reactor-http-nio-2] reactor.netty.transport.TransportConfig : [d01091ce] Initialized pipeline DefaultChannelPipeline{(reactor.left.sslHandler = io.netty.handler.ssl.SslHandler), (reactor.left.sslReader = reactor.netty.tcp.SslProvider$SslReadHandler), (reactor.left.httpCodec = io.netty.handler.codec.http.HttpClientCodec), (reactor.right.reactiveBridge = reactor.netty.channel.ChannelOperationsHandler)}
12:50.892 DEBUG 21416 --- [reactor-http-nio-2] reactor.netty.transport.TransportConnector : [d01091ce] Connecting to [httpbin.org/3.215.37.86:443].
...
12:51.682 TRACE 21416 --- [reactor-http-nio-2] org.springframework.http.codec.json.Jackson2JsonDecoder : [740231ef] [d01091ce-1, L:/192.168.178.25:51797 - R:httpbin.org/3.215.37.86:443] Decoded [{"args":{},"headers":{"Accept":"*/*","Content-Type":"application/json","Host":"httpbin.org","User-Agent":"ReactorNetty/1.0.25","X-Amzn-Trace-Id":"Root=1-638e34a3-455720994ab1288f6268b3f1"},"origin":"88.64.153.179, 163.116.179.55","url":"https://httpbin.org/get"}]
12:51.683 INFO  21416 --- [reactor-http-nio-2] com.test.HttpBinClient : TCCL is jdk.internal.loader.ClassLoaders$AppClassLoader@30946e09
12:51.684 INFO  21416 --- [reactor-http-nio-2] com.test.HttpBinClient : CL is org.springframework.boot.loader.LaunchedURLClassLoader@4501b7af
12:51.685 INFO  21416 --- [reactor-http-nio-2] com.test.HttpBinClient : In Thread reactor-http-nio-2
12:51.687 ERROR 21416 --- [reactor-http-nio-2] com.test.HttpBinClient : !!!! Error !!!!
java.lang.ClassNotFoundException: com.sun.xml.bind.v2.ContextFactory
        at java.base/jdk.internal.loader.BuiltinClassLoader.loadClass(BuiltinClassLoader.java:581)
        at java.base/jdk.internal.loader.ClassLoaders$AppClassLoader.loadClass(ClassLoaders.java:178)
        at java.base/java.lang.ClassLoader.loadClass(ClassLoader.java:522)
        at com.test.HttpBinClient.reproduceError(HttpBinClient.java:40)
```

If HttpBinClient is invoked directly (without CompletableFuture), the `reactor-http-nio-*` threads inherit the classLoaders of thread `http-nio-8080-exec-*`

```js
17:09.291 TRACE 12208 --- [http-nio-8080-exec-1] org.springframework.web.servlet.DispatcherServlet : GET "/test/sendGet", parameters={}, headers={masked} in DispatcherServlet 'dispatcherServlet'
17:09.320 INFO  12208 --- [http-nio-8080-exec-1] com.test.ReproducerController : TCCL is TomcatEmbeddedWebappClassLoader
  context: ROOT
  delegate: true
----------> Parent Classloader:
org.springframework.boot.loader.LaunchedURLClassLoader@4501b7af

17:09.322 INFO  12208 --- [http-nio-8080-exec-1] com.test.ReproducerController : CL is org.springframework.boot.loader.LaunchedURLClassLoader@4501b7af

17:11.134 DEBUG 12208 --- [http-nio-8080-exec-1] reactor.netty.resources.PooledConnectionProvider : Creating a new [http] client pool [PoolFactory{evictionInterval=PT0S, leasingStrategy=fifo, maxConnections=500, maxIdleTime=-1, maxLifeTime=-1, metricsEnabled=false, pendingAcquireMaxCount=1000, pendingAcquireTimeout=45000}] for [httpbin.org:443]
17:12.039 DEBUG 12208 --- [reactor-http-nio-2] reactor.netty.resources.PooledConnectionProvider : [17b7b2bc] Created a new pooled channel, now: 0 active connections, 0 inactive connections and 0 pending acquire requests.

17:12.074 DEBUG 12208 --- [reactor-http-nio-2] reactor.netty.tcp.SslProvider : [17b7b2bc] SSL enabled using engine sun.security.ssl.SSLEngineImpl@4a3043dd and SNI httpbin.org:443
17:12.128 DEBUG 12208 --- [reactor-http-nio-2] reactor.netty.transport.TransportConfig : [17b7b2bc] Initialized pipeline DefaultChannelPipeline{(reactor.left.sslHandler = io.netty.handler.ssl.SslHandler), (reactor.left.sslReader = reactor.netty.tcp.SslProvider$SslReadHandler), (reactor.left.httpCodec = io.netty.handler.codec.http.HttpClientCodec), (reactor.right.reactiveBridge = reactor.netty.channel.ChannelOperationsHandler)}
17:12.249 DEBUG 12208 --- [reactor-http-nio-2] reactor.netty.transport.TransportConnector : [17b7b2bc] Connecting to [httpbin.org/23.20.129.27:443].
...
17:13.004 TRACE 12208 --- [reactor-http-nio-2] org.springframework.http.codec.json.Jackson2JsonDecoder : [3a6407af] [17b7b2bc-1, L:/192.168.178.25:51929 - R:httpbin.org/23.20.129.27:443] Decoded [{"args":{},"headers":{"Accept":"*/*","Content-Type":"application/json","Host":"httpbin.org","User-Agent":"ReactorNetty/1.0.25","X-Amzn-Trace-Id":"Root=1-638e35a8-0b288c562d05a5c355b04020"},"origin":"88.64.153.179, 163.116.179.55","url":"https://httpbin.org/get"}]
17:13.005 INFO  12208 --- [reactor-http-nio-2] com.test.HttpBinClient : TCCL is TomcatEmbeddedWebappClassLoader
  context: ROOT
  delegate: true
----------> Parent Classloader:
org.springframework.boot.loader.LaunchedURLClassLoader@4501b7af

17:13.007 INFO  12208 --- [reactor-http-nio-2] com.test.HttpBinClient : CL is org.springframework.boot.loader.LaunchedURLClassLoader@4501b7af
17:13.007 INFO  12208 --- [reactor-http-nio-2] com.test.HttpBinClient : In Thread reactor-http-nio-2
17:13.010 INFO  12208 --- [reactor-http-nio-2] com.test.HttpBinClient : !!!! Success !!!! loaded class class com.sun.xml.bind.v2.ContextFactory
```

## RootCause? java.util.concurrent.ForkJoinPool.DefaultForkJoinWorkerThreadFactory

```
java -version
openjdk version "11.0.11" 2021-04-20
OpenJDK Runtime Environment AdoptOpenJDK-11.0.11+9 (build 11.0.11+9)
OpenJDK 64-Bit Server VM AdoptOpenJDK-11.0.11+9 (build 11.0.11+9, mixed mode)
```

```java
    /**
     * Default ForkJoinWorkerThreadFactory implementation; creates a
     * new ForkJoinWorkerThread using the system class loader as the
     * thread context class loader.
     */
    private static final class DefaultForkJoinWorkerThreadFactory
        implements ForkJoinWorkerThreadFactory {
        private static final AccessControlContext ACC = contextWithPermissions(
            new RuntimePermission("getClassLoader"),
            new RuntimePermission("setContextClassLoader"));

        public final ForkJoinWorkerThread newThread(ForkJoinPool pool) {
            return AccessController.doPrivileged(
                new PrivilegedAction<>() {
                    public ForkJoinWorkerThread run() {
                        return new ForkJoinWorkerThread(
                            pool, ClassLoader.getSystemClassLoader()); }},
                ACC);
        }
    }
```