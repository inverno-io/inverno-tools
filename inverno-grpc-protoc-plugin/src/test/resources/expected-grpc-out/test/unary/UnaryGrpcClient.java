package test.unary;

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
import reactor.core.publisher.Mono;

/**
 * <p>
 * This is a test service with a unary method.
 * </p>
 */

public final class UnaryGrpcClient {

	public static final GrpcServiceName SERVICE_NAME = GrpcServiceName.of("test", "Unary");

	private UnaryGrpcClient() {}

    public static abstract class Http {

		private final HttpClient httpClient;
		private final GrpcClient grpcClient;

		public Http(HttpClient httpClient, GrpcClient grpcClient) {
			this.httpClient = httpClient;
			this.grpcClient = grpcClient;
		}

		public <A extends ExchangeContext> UnaryGrpcClient.HttpClientStub<A> createStub(String host, int port) {
			return new UnaryGrpcClient.Http.StubImpl<>(this.httpClient.<A>endpoint(host, port).build(), true);
		}

		public <A extends ExchangeContext> UnaryGrpcClient.HttpClientStub<A> createStub(InetSocketAddress remoteAddress) {
			return new UnaryGrpcClient.Http.StubImpl<>(this.httpClient.<A>endpoint(remoteAddress).build(), true);
		}

		public <A extends ExchangeContext> UnaryGrpcClient.HttpClientStub<A> createStub(Endpoint<A> endpoint) {
			return new UnaryGrpcClient.Http.StubImpl<>(endpoint, false);
		}

		private final class StubImpl<A extends ExchangeContext> implements UnaryGrpcClient.HttpClientStub<A> {

			private final Endpoint<A> endpoint;
			private final boolean shutdownEndpoint;
			private final Consumer<GrpcOutboundRequestMetadata> metadataConfigurer;

			public StubImpl(Endpoint<A> endpoint, boolean shutdownEndpoint) {
				this.endpoint = endpoint;
				this.shutdownEndpoint = shutdownEndpoint;
				this.metadataConfigurer = null;
			}

			private StubImpl(UnaryGrpcClient.Http.StubImpl<A> parent, Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
				this.endpoint = parent.endpoint;
				this.shutdownEndpoint = false;
				this.metadataConfigurer = metadataConfigurer;
			}

			@Override
			public UnaryGrpcClient.HttpClientStub<A> withMetadata(Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
				return new UnaryGrpcClient.Http.StubImpl<>(this, metadataConfigurer);
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
			public Mono<GrpcExchange.Unary<A, test.unary.UnaryRequest, test.unary.UnaryResponse>> callUnary(A context) {
				return this.endpoint.exchange(context)
					.map(exchange -> {
						GrpcExchange.Unary<A, test.unary.UnaryRequest, test.unary.UnaryResponse> grpcExchange = UnaryGrpcClient.Http.this.grpcClient.unary(exchange, SERVICE_NAME, "callUnary", test.unary.UnaryRequest.getDefaultInstance(), test.unary.UnaryResponse.getDefaultInstance());
						grpcExchange.request().metadata(this.metadataConfigurer);
						return grpcExchange;
					});
			}
		}
	}

	/**
	 * <p>
	 * This is a test service with a unary method.
	 * </p>
	 *
	 * @param <A> the exchange context type
	 */
	public interface HttpClientStub<A extends ExchangeContext> extends GrpcClient.CloseableStub<A, UnaryGrpcClient.HttpClientStub<A>> {
		
		/**
		 * <p>
		 * Calls unary method.
		 * </p>
		 * 
		 * @return a mono emitting the unary exchange
		 */
		default Mono<GrpcExchange.Unary<A, test.unary.UnaryRequest, test.unary.UnaryResponse>> callUnary() {
			return this.callUnary((A)null);
		}

		/**
		 * <p>
		 * Calls unary method.
		 * </p>
		 * 
		 * @param context the context
		 * 
		 * @return a mono emitting the unary exchange
		 */
		Mono<GrpcExchange.Unary<A, test.unary.UnaryRequest, test.unary.UnaryResponse>> callUnary(A context);

		/**
		 * <p>
		 * Calls unary method.
		 * </p>
		 * 
		 * @param request the client request
		 * 
		 * @return the server response
		 */
		default Mono<test.unary.UnaryResponse> callUnary(test.unary.UnaryRequest request) {
			return this.callUnary(request, null);
		}

		/**
		 * <p>
		 * Calls unary method.
		 * </p>
		 * 
		 * @param request the client request
		 * @param context the context
		 * 
		 * @return the server response
		 */
		default Mono<test.unary.UnaryResponse> callUnary(test.unary.UnaryRequest request, A context) {
			return this.callUnary(context)
				.flatMap(grpcExchange -> {
					grpcExchange.request().value(request);
					return grpcExchange.response().flatMap(GrpcResponse.Unary::value);
				});
		}
	}

    public static abstract class Web<A extends ExchangeContext> implements UnaryGrpcClient.WebClientStub<A> {

		private final ServiceID serviceID;
		private final WebClient<? extends A> webClient;
		private final GrpcClient grpcClient;

		private final Web<A>.StubImpl stub;

		public Web(ServiceID serviceID, WebClient<? extends A> webClient, GrpcClient grpcClient) {
			this.serviceID = serviceID;
			this.webClient = webClient;
			this.grpcClient = grpcClient;

			this.stub = new UnaryGrpcClient.Web.StubImpl(null);
		}

		@Override
		public UnaryGrpcClient.WebClientStub<A> withMetadata(Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
			return new UnaryGrpcClient.Web<A>.StubImpl(metadataConfigurer);
		}
		
		@Override
		public Mono<GrpcExchange.Unary<A, test.unary.UnaryRequest, test.unary.UnaryResponse>> callUnary(Consumer<A> contextConfigurer) {
			return this.stub.callUnary(contextConfigurer);
		}

		private final class StubImpl implements UnaryGrpcClient.WebClientStub<A> {

			private final Consumer<GrpcOutboundRequestMetadata> metadataConfigurer;

			public StubImpl(Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
				this.metadataConfigurer = metadataConfigurer;
			}

			@Override
			public UnaryGrpcClient.WebClientStub<A> withMetadata(Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
				return new UnaryGrpcClient.Web<A>.StubImpl(metadataConfigurer);
			}
			
			@Override
			public Mono<GrpcExchange.Unary<A, test.unary.UnaryRequest, test.unary.UnaryResponse>> callUnary(Consumer<A> contextConfigurer) {
				return Web.this.webClient.exchange(Web.this.serviceID.getURI())
					.map(exchange -> {
						if(contextConfigurer != null) {
							contextConfigurer.accept(exchange.context());
						}
						GrpcExchange.Unary<A, test.unary.UnaryRequest, test.unary.UnaryResponse> grpcExchange = (GrpcExchange.Unary<A, test.unary.UnaryRequest, test.unary.UnaryResponse>)Web.this.grpcClient.unary(exchange, SERVICE_NAME, "callUnary", test.unary.UnaryRequest.getDefaultInstance(), test.unary.UnaryResponse.getDefaultInstance());
						grpcExchange.request().metadata(this.metadataConfigurer);
						return grpcExchange;
					});
			}
		}
	}

	/**
	 * <p>
	 * This is a test service with a unary method.
	 * </p>
	 *
	 * @param <A> the exchange context type
	 */
	public interface WebClientStub<A extends ExchangeContext> extends GrpcClient.Stub<A, UnaryGrpcClient.WebClientStub<A>> {
		
		/**
		 * <p>
		 * Calls unary method.
		 * </p>
		 *
		 * @return a mono emitting the unary exchange
		 */
		default Mono<GrpcExchange.Unary<A, test.unary.UnaryRequest, test.unary.UnaryResponse>> callUnary() {
			return this.callUnary((Consumer<A>)null);
		}

		/**
		 * <p>
		 * Calls unary method.
		 * </p>
		 *
		 * @param contextConfigurer the context configurer
		 *
		 * @return a mono emitting the unary exchange
		 */
		Mono<GrpcExchange.Unary<A, test.unary.UnaryRequest, test.unary.UnaryResponse>> callUnary(Consumer<A> contextConfigurer);

		/**
		 * <p>
		 * Calls unary method.
		 * </p>
		 *
		 * @param request the client request
		 *
		 * @return the server response
		 */
		default Mono<test.unary.UnaryResponse> callUnary(test.unary.UnaryRequest request) {
			return this.callUnary(request, (Consumer<A>)null);
		}

		/**
		 * <p>
		 * Calls unary method.
		 * </p>
		 *
		 * @param request           the client request
		 * @param contextConfigurer the context configurer
		 *
		 * @return the server response
		 */
		default Mono<test.unary.UnaryResponse> callUnary(test.unary.UnaryRequest request, Consumer<A> contextConfigurer) {
			return this.callUnary(contextConfigurer)
				.flatMap(grpcExchange -> {
					grpcExchange.request().value(request);
					return grpcExchange.response().flatMap(GrpcResponse.Unary::value);
				});
		}
	}
}
