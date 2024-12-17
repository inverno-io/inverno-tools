package test.hello;

import io.inverno.mod.discovery.ServiceID;
import io.inverno.mod.grpc.base.GrpcOutboundRequestMetadata;
import io.inverno.mod.grpc.base.GrpcServiceName;
import io.inverno.mod.grpc.client.GrpcClient;
import io.inverno.mod.grpc.client.GrpcExchange;
import io.inverno.mod.grpc.client.GrpcResponse;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.client.Endpoint;
import io.inverno.mod.http.client.HttpClient;
import io.inverno.mod.web.client.WebClient;
import java.net.InetSocketAddress;
import java.util.function.Consumer;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * <p>
 * This is a hello service.
 * </p>
 * 
 * <p>
 * It is called Greeter and has 4 methods:
 * </p>
 * 
 * <ul>
 *   <li>SayHelloUnary</li>
 *   <li>SayHelloClientStreaming</li>
 *   <li>SayHelloServerStreaming</li>
 *   <li>SayHelloBidirectionalStreaming</li>
 * </ul>
 */

public final class GreeterGrpcClient {

	public static final GrpcServiceName SERVICE_NAME = GrpcServiceName.of("test", "Greeter");

	private GreeterGrpcClient() {}

    public static abstract class Http {

		private final HttpClient httpClient;
		private final GrpcClient grpcClient;

		public Http(HttpClient httpClient, GrpcClient grpcClient) {
			this.httpClient = httpClient;
			this.grpcClient = grpcClient;
		}

		public <A extends ExchangeContext> GreeterGrpcClient.HttpClientStub<A> createStub(String host, int port) {
			return new GreeterGrpcClient.Http.StubImpl<>(this.httpClient.<A>endpoint(host, port).build(), true);
		}

		public <A extends ExchangeContext> GreeterGrpcClient.HttpClientStub<A> createStub(InetSocketAddress remoteAddress) {
			return new GreeterGrpcClient.Http.StubImpl<>(this.httpClient.<A>endpoint(remoteAddress).build(), true);
		}

		public <A extends ExchangeContext> GreeterGrpcClient.HttpClientStub<A> createStub(Endpoint<A> endpoint) {
			return new GreeterGrpcClient.Http.StubImpl<>(endpoint, false);
		}

		private final class StubImpl<A extends ExchangeContext> implements GreeterGrpcClient.HttpClientStub<A> {

			private final Endpoint<A> endpoint;
			private final boolean shutdownEndpoint;
			private final Consumer<GrpcOutboundRequestMetadata> metadataConfigurer;

			public StubImpl(Endpoint<A> endpoint, boolean shutdownEndpoint) {
				this.endpoint = endpoint;
				this.shutdownEndpoint = shutdownEndpoint;
				this.metadataConfigurer = null;
			}

			private StubImpl(GreeterGrpcClient.Http.StubImpl<A> parent, Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
				this.endpoint = parent.endpoint;
				this.shutdownEndpoint = false;
				this.metadataConfigurer = metadataConfigurer;
			}

			@Override
			public GreeterGrpcClient.HttpClientStub<A> withMetadata(Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
				return new GreeterGrpcClient.Http.StubImpl<>(this, metadataConfigurer);
			}

			@Override
			public Mono<Void> shutdown() {
				return this.shutdownEndpoint ? this.endpoint.shutdown() : Mono.empty();
			}

			@Override
			public Mono<Void> shutdownGracefully() {
				return this.shutdownEndpoint ? this.endpoint.shutdownGracefully() : Mono.empty();
			}
			
			@Override
			public Mono<GrpcExchange.Unary<A, test.hello.HelloRequest, test.hello.HelloResponse>> sayHelloUnary(A context) {
				return this.endpoint.exchange(context)
					.map(exchange -> {
						GrpcExchange.Unary<A, test.hello.HelloRequest, test.hello.HelloResponse> grpcExchange = GreeterGrpcClient.Http.this.grpcClient.unary(exchange, SERVICE_NAME, "SayHelloUnary", test.hello.HelloRequest.getDefaultInstance(), test.hello.HelloResponse.getDefaultInstance());
						grpcExchange.request().metadata(this.metadataConfigurer);
						return grpcExchange;
					});
			}
			@Override
			public Mono<GrpcExchange.ClientStreaming<A, test.hello.HelloRequest, test.hello.HelloResponse>> sayHelloClientStreaming(A context) {
				return this.endpoint.exchange(context)
					.map(exchange -> {
						GrpcExchange.ClientStreaming<A, test.hello.HelloRequest, test.hello.HelloResponse> grpcExchange = GreeterGrpcClient.Http.this.grpcClient.clientStreaming(exchange, SERVICE_NAME, "SayHelloClientStreaming", test.hello.HelloRequest.getDefaultInstance(), test.hello.HelloResponse.getDefaultInstance());
						grpcExchange.request().metadata(this.metadataConfigurer);
						return grpcExchange;
					});
			}
			@Override
			public Mono<GrpcExchange.ServerStreaming<A, test.hello.HelloRequest, test.hello.HelloResponse>> sayHelloServerStreaming(A context) {
				return this.endpoint.exchange(context)
					.map(exchange -> {
						GrpcExchange.ServerStreaming<A, test.hello.HelloRequest, test.hello.HelloResponse> grpcExchange = GreeterGrpcClient.Http.this.grpcClient.serverStreaming(exchange, SERVICE_NAME, "SayHelloServerStreaming", test.hello.HelloRequest.getDefaultInstance(), test.hello.HelloResponse.getDefaultInstance());
						grpcExchange.request().metadata(this.metadataConfigurer);
						return grpcExchange;
					});
			}
			@Override
			public Mono<GrpcExchange.BidirectionalStreaming<A, test.hello.HelloRequest, test.hello.HelloResponse>> sayHelloBidirectionalStreaming(A context) {
				return this.endpoint.exchange(context)
					.map(exchange -> {
						GrpcExchange.BidirectionalStreaming<A, test.hello.HelloRequest, test.hello.HelloResponse> grpcExchange = GreeterGrpcClient.Http.this.grpcClient.bidirectionalStreaming(exchange, SERVICE_NAME, "SayHelloBidirectionalStreaming", test.hello.HelloRequest.getDefaultInstance(), test.hello.HelloResponse.getDefaultInstance());
						grpcExchange.request().metadata(this.metadataConfigurer);
						return grpcExchange;
					});
			}
		}
	}

	/**
	 * <p>
	 * This is a hello service.
	 * </p>
	 * 
	 * <p>
	 * It is called Greeter and has 4 methods:
	 * </p>
	 * 
	 * <ul>
	 *   <li>SayHelloUnary</li>
	 *   <li>SayHelloClientStreaming</li>
	 *   <li>SayHelloServerStreaming</li>
	 *   <li>SayHelloBidirectionalStreaming</li>
	 * </ul>
	 *
	 * @param <A> the exchange context type
	 */
	public interface HttpClientStub<A extends ExchangeContext> extends GrpcClient.CloseableStub<A, GreeterGrpcClient.HttpClientStub<A>> {
		
		/**
		 * <p>
		 * Unary hello request.
		 * </p>
		 * 
		 * @return a mono emitting the unary exchange
		 */
		default Mono<GrpcExchange.Unary<A, test.hello.HelloRequest, test.hello.HelloResponse>> sayHelloUnary() {
			return this.sayHelloUnary((A)null);
		}

		/**
		 * <p>
		 * Unary hello request.
		 * </p>
		 * 
		 * @param context the context
		 * 
		 * @return a mono emitting the unary exchange
		 */
		Mono<GrpcExchange.Unary<A, test.hello.HelloRequest, test.hello.HelloResponse>> sayHelloUnary(A context);

		/**
		 * <p>
		 * Unary hello request.
		 * </p>
		 * 
		 * @param request the client request
		 * 
		 * @return the server response
		 */
		default Mono<test.hello.HelloResponse> sayHelloUnary(test.hello.HelloRequest request) {
			return this.sayHelloUnary(request, null);
		}

		/**
		 * <p>
		 * Unary hello request.
		 * </p>
		 * 
		 * @param request the client request
		 * @param context the context
		 * 
		 * @return the server response
		 */
		default Mono<test.hello.HelloResponse> sayHelloUnary(test.hello.HelloRequest request, A context) {
			return this.sayHelloUnary(context)
				.flatMap(grpcExchange -> {
					grpcExchange.request().value(request);
					return grpcExchange.response().flatMap(GrpcResponse.Unary::value);
				});
		}
		/**
		 * <p>
		 * Client Streaming hello request.
		 * </p>
		 * 
		 * @return a mono emitting the client streaming exchange
		 */
		default Mono<GrpcExchange.ClientStreaming<A, test.hello.HelloRequest, test.hello.HelloResponse>> sayHelloClientStreaming() {
			return this.sayHelloClientStreaming((A)null);
		}

		/**
		 * <p>
		 * Client Streaming hello request.
		 * </p>
		 * 
		 * @param context the context
		 * 
		 * @return a mono emitting the client streaming exchange
		 */
		Mono<GrpcExchange.ClientStreaming<A, test.hello.HelloRequest, test.hello.HelloResponse>> sayHelloClientStreaming(A context);

		/**
		 * <p>
		 * Client Streaming hello request.
		 * </p>
		 * 
		 * @param request the client request publisher
		 * 
		 * @return the server response
		 */
		default Mono<test.hello.HelloResponse> sayHelloClientStreaming(Publisher<test.hello.HelloRequest> request) {
			return this.sayHelloClientStreaming(request, null);
		}

		/**
		 * <p>
		 * Client Streaming hello request.
		 * </p>
		 * 
		 * @param request the client request publisher
		 * @param context the context
		 * 
		 * @return the server response
		 */
		default Mono<test.hello.HelloResponse> sayHelloClientStreaming(Publisher<test.hello.HelloRequest> request, A context) {
			return this.sayHelloClientStreaming(context)
				.flatMap(grpcExchange -> {
					grpcExchange.request().stream(request);
					return grpcExchange.response().flatMap(GrpcResponse.Unary::value);
				});
		}
		/**
		 * <p>
		 * Server Streaming hello request.
		 * </p>
		 * 
		 * @return a mono emitting the server streaming exchange
		 */
		default Mono<GrpcExchange.ServerStreaming<A, test.hello.HelloRequest, test.hello.HelloResponse>> sayHelloServerStreaming() {
			return this.sayHelloServerStreaming((A)null);
		}

		/**
		 * <p>
		 * Server Streaming hello request.
		 * </p>
		 * 
		 * @param context the context
		 * 
		 * @return a mono emitting the server streaming exchange
		 */
		Mono<GrpcExchange.ServerStreaming<A, test.hello.HelloRequest, test.hello.HelloResponse>> sayHelloServerStreaming(A context);

		/**
		 * <p>
		 * Server Streaming hello request.
		 * </p>
		 * 
		 * @param request the client request
		 * 
		 * @return the server response publisher
		 */
		default Publisher<test.hello.HelloResponse> sayHelloServerStreaming(test.hello.HelloRequest request) {
			return this.sayHelloServerStreaming(request, null);
		}

		/**
		 * <p>
		 * Server Streaming hello request.
		 * </p>
		 * 
		 * @param request the client request
		 * @param context the context
		 * 
		 * @return the server response publisher
		 */
		default Publisher<test.hello.HelloResponse> sayHelloServerStreaming(test.hello.HelloRequest request, A context) {
			return this.sayHelloServerStreaming(context)
				.flatMapMany(grpcExchange -> {
					grpcExchange.request().value(request);
					return grpcExchange.response().flatMapMany(GrpcResponse.Streaming::stream);
				});
		}
		/**
		 * <p>
		 * Birirectional Streaming hello request.
		 * </p>
		 * 
		 * @return a mono emitting the bidirectional streaming exchange
		 */
		default Mono<GrpcExchange.BidirectionalStreaming<A, test.hello.HelloRequest, test.hello.HelloResponse>> sayHelloBidirectionalStreaming() {
			return this.sayHelloBidirectionalStreaming((A)null);
		}

		/**
		 * <p>
		 * Birirectional Streaming hello request.
		 * </p>
		 * 
		 * @param context the context
		 * 
		 * @return a mono emitting the bidirectional streaming exchange
		 */
		Mono<GrpcExchange.BidirectionalStreaming<A, test.hello.HelloRequest, test.hello.HelloResponse>> sayHelloBidirectionalStreaming(A context);

		/**
		 * <p>
		 * Birirectional Streaming hello request.
		 * </p>
		 * 
		 * @param request the client request publisher
		 * 
		 * @return the server response publisher
		 */
		default Publisher<test.hello.HelloResponse> sayHelloBidirectionalStreaming(Publisher<test.hello.HelloRequest> request) {
			return this.sayHelloBidirectionalStreaming(request, null);
		}

		/**
		 * <p>
		 * Birirectional Streaming hello request.
		 * </p>
		 * 
		 * @param request the client request publisher
		 * @param context the context
		 * 
		 * @return the server response publisher
		 */
		default Publisher<test.hello.HelloResponse> sayHelloBidirectionalStreaming(Publisher<test.hello.HelloRequest> request, A context) {
			return this.sayHelloBidirectionalStreaming(context)
				.flatMapMany(grpcExchange -> {
					grpcExchange.request().stream(request);
					return grpcExchange.response().flatMapMany(GrpcResponse.Streaming::stream);
				});
		}
	}

    public static abstract class Web<A extends ExchangeContext> implements GreeterGrpcClient.WebClientStub<A> {

		private final ServiceID serviceID;
		private final WebClient<? extends A> webClient;
		private final GrpcClient grpcClient;

		private final Web<A>.StubImpl stub;

		public Web(ServiceID serviceID, WebClient<? extends A> webClient, GrpcClient grpcClient) {
			this.serviceID = serviceID;
			this.webClient = webClient;
			this.grpcClient = grpcClient;

			this.stub = new GreeterGrpcClient.Web.StubImpl(null);
		}

		@Override
		public GreeterGrpcClient.WebClientStub<A> withMetadata(Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
			return new GreeterGrpcClient.Web<A>.StubImpl(metadataConfigurer);
		}
		
		@Override
		public Mono<GrpcExchange.Unary<A, test.hello.HelloRequest, test.hello.HelloResponse>> sayHelloUnary(Consumer<A> contextConfigurer) {
			return this.stub.sayHelloUnary(contextConfigurer);
		}
		@Override
		public Mono<GrpcExchange.ClientStreaming<A, test.hello.HelloRequest, test.hello.HelloResponse>> sayHelloClientStreaming(Consumer<A> contextConfigurer) {
			return this.stub.sayHelloClientStreaming(contextConfigurer);
        }
		@Override
		public Mono<GrpcExchange.ServerStreaming<A, test.hello.HelloRequest, test.hello.HelloResponse>> sayHelloServerStreaming(Consumer<A> contextConfigurer) {
			return this.stub.sayHelloServerStreaming(contextConfigurer);
		}
		@Override
		public Mono<GrpcExchange.BidirectionalStreaming<A, test.hello.HelloRequest, test.hello.HelloResponse>> sayHelloBidirectionalStreaming(Consumer<A> contextConfigurer) {
			return this.stub.sayHelloBidirectionalStreaming(contextConfigurer);
        }

		private final class StubImpl implements GreeterGrpcClient.WebClientStub<A> {

			private final Consumer<GrpcOutboundRequestMetadata> metadataConfigurer;

			public StubImpl(Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
				this.metadataConfigurer = metadataConfigurer;
			}

			@Override
			public GreeterGrpcClient.WebClientStub<A> withMetadata(Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
				return new GreeterGrpcClient.Web<A>.StubImpl(metadataConfigurer);
			}
			
			@Override
			public Mono<GrpcExchange.Unary<A, test.hello.HelloRequest, test.hello.HelloResponse>> sayHelloUnary(Consumer<A> contextConfigurer) {
				return Web.this.webClient.exchange(Web.this.serviceID.getURI())
					.map(exchange -> {
						if(contextConfigurer != null) {
							contextConfigurer.accept(exchange.context());
						}
						GrpcExchange.Unary<A, test.hello.HelloRequest, test.hello.HelloResponse> grpcExchange = (GrpcExchange.Unary<A, test.hello.HelloRequest, test.hello.HelloResponse>)Web.this.grpcClient.unary(exchange, SERVICE_NAME, "SayHelloUnary", test.hello.HelloRequest.getDefaultInstance(), test.hello.HelloResponse.getDefaultInstance());
						grpcExchange.request().metadata(this.metadataConfigurer);
						return grpcExchange;
					});
			}
			@Override
			public Mono<GrpcExchange.ClientStreaming<A, test.hello.HelloRequest, test.hello.HelloResponse>> sayHelloClientStreaming(Consumer<A> contextConfigurer) {
				return Web.this.webClient.exchange(Web.this.serviceID.getURI())
					.map(exchange -> {
						if(contextConfigurer != null) {
							contextConfigurer.accept(exchange.context());
						}
						GrpcExchange.ClientStreaming<A, test.hello.HelloRequest, test.hello.HelloResponse> grpcExchange = (GrpcExchange.ClientStreaming<A, test.hello.HelloRequest, test.hello.HelloResponse>)Web.this.grpcClient.clientStreaming(exchange, SERVICE_NAME, "SayHelloClientStreaming", test.hello.HelloRequest.getDefaultInstance(), test.hello.HelloResponse.getDefaultInstance());
						grpcExchange.request().metadata(this.metadataConfigurer);
						return grpcExchange;
					});
			}
			@Override
			public Mono<GrpcExchange.ServerStreaming<A, test.hello.HelloRequest, test.hello.HelloResponse>> sayHelloServerStreaming(Consumer<A> contextConfigurer) {
				return Web.this.webClient.exchange(Web.this.serviceID.getURI())
					.map(exchange -> {
						if(contextConfigurer != null) {
							contextConfigurer.accept(exchange.context());
						}
						GrpcExchange.ServerStreaming<A, test.hello.HelloRequest, test.hello.HelloResponse> grpcExchange = (GrpcExchange.ServerStreaming<A, test.hello.HelloRequest, test.hello.HelloResponse>)Web.this.grpcClient.serverStreaming(exchange, SERVICE_NAME, "SayHelloServerStreaming", test.hello.HelloRequest.getDefaultInstance(), test.hello.HelloResponse.getDefaultInstance());
						grpcExchange.request().metadata(this.metadataConfigurer);
						return grpcExchange;
					});
			}
			@Override
			public Mono<GrpcExchange.BidirectionalStreaming<A, test.hello.HelloRequest, test.hello.HelloResponse>> sayHelloBidirectionalStreaming(Consumer<A> contextConfigurer) {
				return Web.this.webClient.exchange(Web.this.serviceID.getURI())
						.map(exchange -> {
							if(contextConfigurer != null) {
								contextConfigurer.accept(exchange.context());
							}
						GrpcExchange.BidirectionalStreaming<A, test.hello.HelloRequest, test.hello.HelloResponse> grpcExchange = (GrpcExchange.BidirectionalStreaming<A, test.hello.HelloRequest, test.hello.HelloResponse>)Web.this.grpcClient.bidirectionalStreaming(exchange, SERVICE_NAME, "SayHelloBidirectionalStreaming", test.hello.HelloRequest.getDefaultInstance(), test.hello.HelloResponse.getDefaultInstance());
						grpcExchange.request().metadata(this.metadataConfigurer);
						return grpcExchange;
					});
			}
		}
	}

	/**
	 * <p>
	 * This is a hello service.
	 * </p>
	 * 
	 * <p>
	 * It is called Greeter and has 4 methods:
	 * </p>
	 * 
	 * <ul>
	 *   <li>SayHelloUnary</li>
	 *   <li>SayHelloClientStreaming</li>
	 *   <li>SayHelloServerStreaming</li>
	 *   <li>SayHelloBidirectionalStreaming</li>
	 * </ul>
	 *
	 * @param <A> the exchange context type
	 */
	public interface WebClientStub<A extends ExchangeContext> extends GrpcClient.Stub<A, GreeterGrpcClient.WebClientStub<A>> {
		
		/**
		 * <p>
		 * Unary hello request.
		 * </p>
		 *
		 * @return a mono emitting the unary exchange
		 */
		default Mono<GrpcExchange.Unary<A, test.hello.HelloRequest, test.hello.HelloResponse>> sayHelloUnary() {
			return this.sayHelloUnary((Consumer<A>)null);
		}

		/**
		 * <p>
		 * Unary hello request.
		 * </p>
		 *
		 * @param contextConfigurer the context configurer
		 *
		 * @return a mono emitting the unary exchange
		 */
		Mono<GrpcExchange.Unary<A, test.hello.HelloRequest, test.hello.HelloResponse>> sayHelloUnary(Consumer<A> contextConfigurer);

		/**
		 * <p>
		 * Unary hello request.
		 * </p>
		 *
		 * @param request the client request
		 *
		 * @return the server response
		 */
		default Mono<test.hello.HelloResponse> sayHelloUnary(test.hello.HelloRequest request) {
			return this.sayHelloUnary(request, (Consumer<A>)null);
		}

		/**
		 * <p>
		 * Unary hello request.
		 * </p>
		 *
		 * @param request           the client request
		 * @param contextConfigurer the context configurer
		 *
		 * @return the server response
		 */
		default Mono<test.hello.HelloResponse> sayHelloUnary(test.hello.HelloRequest request, Consumer<A> contextConfigurer) {
			return this.sayHelloUnary(contextConfigurer)
				.flatMap(grpcExchange -> {
					grpcExchange.request().value(request);
					return grpcExchange.response().flatMap(GrpcResponse.Unary::value);
				});
		}
		/**
		 * <p>
		 * Client Streaming hello request.
		 * </p>
		 *
		 * @return a mono emitting the client streaming exchange
		 */
		default Mono<GrpcExchange.ClientStreaming<A, test.hello.HelloRequest, test.hello.HelloResponse>> sayHelloClientStreaming() {
			return this.sayHelloClientStreaming((Consumer<A>)null);
		}

		/**
		 * <p>
		 * Client Streaming hello request.
		 * </p>
		 *
		 * @param contextConfigurer the context configurer
		 *
		 * @return a mono emitting the client streaming exchange
		 */
		Mono<GrpcExchange.ClientStreaming<A, test.hello.HelloRequest, test.hello.HelloResponse>> sayHelloClientStreaming(Consumer<A> contextConfigurer);

		/**
		 * <p>
		 * Client Streaming hello request.
		 * </p>
		 *
		 * @param request the client request publisher
		 *
		 * @return the server response
		 */
		default Mono<test.hello.HelloResponse> sayHelloClientStreaming(Publisher<test.hello.HelloRequest> request) {
			return this.sayHelloClientStreaming(request, (Consumer<A>)null);
		}

		/**
		 * <p>
		 * Client Streaming hello request.
		 * </p>
		 *
		 * @param request the client request publisher
		 * @param contextConfigurer the context configurer
		 *
		 * @return the server response
		 */
		default Mono<test.hello.HelloResponse> sayHelloClientStreaming(Publisher<test.hello.HelloRequest> request, Consumer<A> contextConfigurer) {
			return this.sayHelloClientStreaming(contextConfigurer)
				.flatMap(grpcExchange -> {
					grpcExchange.request().stream(request);
					return grpcExchange.response().flatMap(GrpcResponse.Unary::value);
				});
		}
		/**
		 * <p>
		 * Server Streaming hello request.
		 * </p>
		 *
		 * @return a mono emitting the server streaming exchange
		 */
		default Mono<GrpcExchange.ServerStreaming<A, test.hello.HelloRequest, test.hello.HelloResponse>> sayHelloServerStreaming() {
			return this.sayHelloServerStreaming((Consumer<A>)null);
		}

		/**
		 * <p>
		 * Server Streaming hello request.
		 * </p>
		 *
		 * @param contextConfigurer the context configurer
		 *
		 * @return a mono emitting the server streaming exchange
		 */
		Mono<GrpcExchange.ServerStreaming<A, test.hello.HelloRequest, test.hello.HelloResponse>> sayHelloServerStreaming(Consumer<A> contextConfigurer);

		/**
		 * <p>
		 * Server Streaming hello request.
		 * </p>
		 *
		 * @param request the client request
		 *
		 * @return the server response publisher
		 */
		default Publisher<test.hello.HelloResponse> sayHelloServerStreaming(test.hello.HelloRequest request) {
			return this.sayHelloServerStreaming(request, (Consumer<A>)null);
		}

		/**
		 * <p>
		 * Server Streaming hello request.
		 * </p>
		 *
		 * @param request the client request
		 * @param contextConfigurer the context configurer
		 *
		 * @return the server response publisher
		 */
		default Publisher<test.hello.HelloResponse> sayHelloServerStreaming(test.hello.HelloRequest request, Consumer<A> contextConfigurer) {
			return this.sayHelloServerStreaming(contextConfigurer)
				.flatMapMany(grpcExchange -> {
					grpcExchange.request().value(request);
					return grpcExchange.response().flatMapMany(GrpcResponse.Streaming::stream);
				});
		}
		/**
		 * <p>
		 * Birirectional Streaming hello request.
		 * </p>
		 *
		 * @return a mono emitting the bidirectional streaming exchange
		 */
		default Mono<GrpcExchange.BidirectionalStreaming<A, test.hello.HelloRequest, test.hello.HelloResponse>> sayHelloBidirectionalStreaming() {
			return this.sayHelloBidirectionalStreaming((Consumer<A>)null);
		}

		/**
		 * <p>
		 * Birirectional Streaming hello request.
		 * </p>
		 *
		 * @param contextConfigurer the context configurer
		 *
		 * @return a mono emitting the bidirectional streaming exchange
		 */
		Mono<GrpcExchange.BidirectionalStreaming<A, test.hello.HelloRequest, test.hello.HelloResponse>> sayHelloBidirectionalStreaming(Consumer<A> contextConfigurer);

		/**
		 * <p>
		 * Birirectional Streaming hello request.
		 * </p>
		 *
		 * @param request the client request publisher
		 *
		 * @return the server response publisher
		 */
		default Publisher<test.hello.HelloResponse> sayHelloBidirectionalStreaming(Publisher<test.hello.HelloRequest> request) {
			return this.sayHelloBidirectionalStreaming(request, (Consumer<A>)null);
		}

		/**
		 * <p>
		 * Birirectional Streaming hello request.
		 * </p>
		 *
		 * @param request           the client request publisher
		 * @param contextConfigurer the context configurer
		 *
		 * @return the server response publisher
		 */
		default Publisher<test.hello.HelloResponse> sayHelloBidirectionalStreaming(Publisher<test.hello.HelloRequest> request, Consumer<A> contextConfigurer) {
			return this.sayHelloBidirectionalStreaming(contextConfigurer)
				.flatMapMany(grpcExchange -> {
					grpcExchange.request().stream(request);
					return grpcExchange.response().flatMapMany(GrpcResponse.Streaming::stream);
				});
		}
	}
}
