package test.outer;

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
 * 
 */

public final class OuterServiceGrpcClient {

	public static final GrpcServiceName SERVICE_NAME = GrpcServiceName.of("test", "OuterService");

	private OuterServiceGrpcClient() {}

    public static abstract class Http {

		private final HttpClient httpClient;
		private final GrpcClient grpcClient;

		public Http(HttpClient httpClient, GrpcClient grpcClient) {
			this.httpClient = httpClient;
			this.grpcClient = grpcClient;
		}

		public <A extends ExchangeContext> OuterServiceGrpcClient.HttpClientStub<A> createStub(String host, int port) {
			return new OuterServiceGrpcClient.Http.StubImpl<>(this.httpClient.<A>endpoint(host, port).build(), true);
		}

		public <A extends ExchangeContext> OuterServiceGrpcClient.HttpClientStub<A> createStub(InetSocketAddress remoteAddress) {
			return new OuterServiceGrpcClient.Http.StubImpl<>(this.httpClient.<A>endpoint(remoteAddress).build(), true);
		}

		public <A extends ExchangeContext> OuterServiceGrpcClient.HttpClientStub<A> createStub(Endpoint<A> endpoint) {
			return new OuterServiceGrpcClient.Http.StubImpl<>(endpoint, false);
		}

		private final class StubImpl<A extends ExchangeContext> implements OuterServiceGrpcClient.HttpClientStub<A> {

			private final Endpoint<A> endpoint;
			private final boolean shutdownEndpoint;
			private final Consumer<GrpcOutboundRequestMetadata> metadataConfigurer;

			public StubImpl(Endpoint<A> endpoint, boolean shutdownEndpoint) {
				this.endpoint = endpoint;
				this.shutdownEndpoint = shutdownEndpoint;
				this.metadataConfigurer = null;
			}

			private StubImpl(OuterServiceGrpcClient.Http.StubImpl<A> parent, Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
				this.endpoint = parent.endpoint;
				this.shutdownEndpoint = false;
				this.metadataConfigurer = metadataConfigurer;
			}

			@Override
			public OuterServiceGrpcClient.HttpClientStub<A> withMetadata(Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
				return new OuterServiceGrpcClient.Http.StubImpl<>(this, metadataConfigurer);
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
			public Mono<GrpcExchange.Unary<A, test.outer.OuterClass.OuterRequest, test.outer.OuterClass.OuterResponse>> call(A context) {
				return this.endpoint.exchange(context)
					.map(exchange -> {
						GrpcExchange.Unary<A, test.outer.OuterClass.OuterRequest, test.outer.OuterClass.OuterResponse> grpcExchange = OuterServiceGrpcClient.Http.this.grpcClient.unary(exchange, SERVICE_NAME, "call", test.outer.OuterClass.OuterRequest.getDefaultInstance(), test.outer.OuterClass.OuterResponse.getDefaultInstance());
						grpcExchange.request().metadata(this.metadataConfigurer);
						return grpcExchange;
					});
			}
		}
	}

	/**
	 * 
	 *
	 * @param <A> the exchange context type
	 */
	public interface HttpClientStub<A extends ExchangeContext> extends GrpcClient.CloseableStub<A, OuterServiceGrpcClient.HttpClientStub<A>> {
		
		/**
		 * 
		 * 
		 * @return a mono emitting the unary exchange
		 */
		default Mono<GrpcExchange.Unary<A, test.outer.OuterClass.OuterRequest, test.outer.OuterClass.OuterResponse>> call() {
			return this.call((A)null);
		}

		/**
		 * 
		 * 
		 * @param context the context
		 * 
		 * @return a mono emitting the unary exchange
		 */
		Mono<GrpcExchange.Unary<A, test.outer.OuterClass.OuterRequest, test.outer.OuterClass.OuterResponse>> call(A context);

		/**
		 * 
		 * 
		 * @param request the client request
		 * 
		 * @return the server response
		 */
		default Mono<test.outer.OuterClass.OuterResponse> call(test.outer.OuterClass.OuterRequest request) {
			return this.call(request, null);
		}

		/**
		 * 
		 * 
		 * @param request the client request
		 * @param context the context
		 * 
		 * @return the server response
		 */
		default Mono<test.outer.OuterClass.OuterResponse> call(test.outer.OuterClass.OuterRequest request, A context) {
			return this.call(context)
				.flatMap(grpcExchange -> {
					grpcExchange.request().value(request);
					return grpcExchange.response().flatMap(GrpcResponse.Unary::value);
				});
		}
	}

    public static abstract class Web<A extends ExchangeContext> implements OuterServiceGrpcClient.WebClientStub<A> {

		private final ServiceID serviceID;
		private final WebClient<? extends A> webClient;
		private final GrpcClient grpcClient;

		private final Web<A>.StubImpl stub;

		public Web(ServiceID serviceID, WebClient<? extends A> webClient, GrpcClient grpcClient) {
			this.serviceID = serviceID;
			this.webClient = webClient;
			this.grpcClient = grpcClient;

			this.stub = new OuterServiceGrpcClient.Web.StubImpl(null);
		}

		@Override
		public OuterServiceGrpcClient.WebClientStub<A> withMetadata(Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
			return new OuterServiceGrpcClient.Web<A>.StubImpl(metadataConfigurer);
		}
		
		@Override
		public Mono<GrpcExchange.Unary<A, test.outer.OuterClass.OuterRequest, test.outer.OuterClass.OuterResponse>> call(Consumer<A> contextConfigurer) {
			return this.stub.call(contextConfigurer);
		}

		private final class StubImpl implements OuterServiceGrpcClient.WebClientStub<A> {

			private final Consumer<GrpcOutboundRequestMetadata> metadataConfigurer;

			public StubImpl(Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
				this.metadataConfigurer = metadataConfigurer;
			}

			@Override
			public OuterServiceGrpcClient.WebClientStub<A> withMetadata(Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
				return new OuterServiceGrpcClient.Web<A>.StubImpl(metadataConfigurer);
			}
			
			@Override
			public Mono<GrpcExchange.Unary<A, test.outer.OuterClass.OuterRequest, test.outer.OuterClass.OuterResponse>> call(Consumer<A> contextConfigurer) {
				return Web.this.webClient.exchange(Web.this.serviceID.getURI())
					.map(exchange -> {
						if(contextConfigurer != null) {
							contextConfigurer.accept(exchange.context());
						}
						GrpcExchange.Unary<A, test.outer.OuterClass.OuterRequest, test.outer.OuterClass.OuterResponse> grpcExchange = (GrpcExchange.Unary<A, test.outer.OuterClass.OuterRequest, test.outer.OuterClass.OuterResponse>)Web.this.grpcClient.unary(exchange, SERVICE_NAME, "call", test.outer.OuterClass.OuterRequest.getDefaultInstance(), test.outer.OuterClass.OuterResponse.getDefaultInstance());
						grpcExchange.request().metadata(this.metadataConfigurer);
						return grpcExchange;
					});
			}
		}
	}

	/**
	 * 
	 *
	 * @param <A> the exchange context type
	 */
	public interface WebClientStub<A extends ExchangeContext> extends GrpcClient.Stub<A, OuterServiceGrpcClient.WebClientStub<A>> {
		
		/**
		 * 
		 *
		 * @return a mono emitting the unary exchange
		 */
		default Mono<GrpcExchange.Unary<A, test.outer.OuterClass.OuterRequest, test.outer.OuterClass.OuterResponse>> call() {
			return this.call((Consumer<A>)null);
		}

		/**
		 * 
		 *
		 * @param contextConfigurer the context configurer
		 *
		 * @return a mono emitting the unary exchange
		 */
		Mono<GrpcExchange.Unary<A, test.outer.OuterClass.OuterRequest, test.outer.OuterClass.OuterResponse>> call(Consumer<A> contextConfigurer);

		/**
		 * 
		 *
		 * @param request the client request
		 *
		 * @return the server response
		 */
		default Mono<test.outer.OuterClass.OuterResponse> call(test.outer.OuterClass.OuterRequest request) {
			return this.call(request, (Consumer<A>)null);
		}

		/**
		 * 
		 *
		 * @param request           the client request
		 * @param contextConfigurer the context configurer
		 *
		 * @return the server response
		 */
		default Mono<test.outer.OuterClass.OuterResponse> call(test.outer.OuterClass.OuterRequest request, Consumer<A> contextConfigurer) {
			return this.call(contextConfigurer)
				.flatMap(grpcExchange -> {
					grpcExchange.request().value(request);
					return grpcExchange.response().flatMap(GrpcResponse.Unary::value);
				});
		}
	}
}
