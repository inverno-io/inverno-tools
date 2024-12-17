package io.grpc.testing.integration;

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
 * A simple service NOT implemented at servers so clients can test for
 * that case.
 */

public final class UnimplementedServiceGrpcClient {

	public static final GrpcServiceName SERVICE_NAME = GrpcServiceName.of("grpc.testing", "UnimplementedService");

	private UnimplementedServiceGrpcClient() {}

    public static abstract class Http {

		private final HttpClient httpClient;
		private final GrpcClient grpcClient;

		public Http(HttpClient httpClient, GrpcClient grpcClient) {
			this.httpClient = httpClient;
			this.grpcClient = grpcClient;
		}

		public <A extends ExchangeContext> UnimplementedServiceGrpcClient.HttpClientStub<A> createStub(String host, int port) {
			return new UnimplementedServiceGrpcClient.Http.StubImpl<>(this.httpClient.<A>endpoint(host, port).build(), true);
		}

		public <A extends ExchangeContext> UnimplementedServiceGrpcClient.HttpClientStub<A> createStub(InetSocketAddress remoteAddress) {
			return new UnimplementedServiceGrpcClient.Http.StubImpl<>(this.httpClient.<A>endpoint(remoteAddress).build(), true);
		}

		public <A extends ExchangeContext> UnimplementedServiceGrpcClient.HttpClientStub<A> createStub(Endpoint<A> endpoint) {
			return new UnimplementedServiceGrpcClient.Http.StubImpl<>(endpoint, false);
		}

		private final class StubImpl<A extends ExchangeContext> implements UnimplementedServiceGrpcClient.HttpClientStub<A> {

			private final Endpoint<A> endpoint;
			private final boolean shutdownEndpoint;
			private final Consumer<GrpcOutboundRequestMetadata> metadataConfigurer;

			public StubImpl(Endpoint<A> endpoint, boolean shutdownEndpoint) {
				this.endpoint = endpoint;
				this.shutdownEndpoint = shutdownEndpoint;
				this.metadataConfigurer = null;
			}

			private StubImpl(UnimplementedServiceGrpcClient.Http.StubImpl<A> parent, Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
				this.endpoint = parent.endpoint;
				this.shutdownEndpoint = false;
				this.metadataConfigurer = metadataConfigurer;
			}

			@Override
			public UnimplementedServiceGrpcClient.HttpClientStub<A> withMetadata(Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
				return new UnimplementedServiceGrpcClient.Http.StubImpl<>(this, metadataConfigurer);
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
			public Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.EmptyProtos.Empty, io.grpc.testing.integration.EmptyProtos.Empty>> unimplementedCall(A context) {
				return this.endpoint.exchange(context)
					.map(exchange -> {
						GrpcExchange.Unary<A, io.grpc.testing.integration.EmptyProtos.Empty, io.grpc.testing.integration.EmptyProtos.Empty> grpcExchange = UnimplementedServiceGrpcClient.Http.this.grpcClient.unary(exchange, SERVICE_NAME, "UnimplementedCall", io.grpc.testing.integration.EmptyProtos.Empty.getDefaultInstance(), io.grpc.testing.integration.EmptyProtos.Empty.getDefaultInstance());
						grpcExchange.request().metadata(this.metadataConfigurer);
						return grpcExchange;
					});
			}
		}
	}

	/**
	 * A simple service NOT implemented at servers so clients can test for
	 * that case.
	 *
	 * @param <A> the exchange context type
	 */
	public interface HttpClientStub<A extends ExchangeContext> extends GrpcClient.CloseableStub<A, UnimplementedServiceGrpcClient.HttpClientStub<A>> {
		
		/**
		 * One empty request followed by one empty response.
		 * 
		 * @return a mono emitting the unary exchange
		 */
		default Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.EmptyProtos.Empty, io.grpc.testing.integration.EmptyProtos.Empty>> unimplementedCall() {
			return this.unimplementedCall((A)null);
		}

		/**
		 * One empty request followed by one empty response.
		 * 
		 * @param context the context
		 * 
		 * @return a mono emitting the unary exchange
		 */
		Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.EmptyProtos.Empty, io.grpc.testing.integration.EmptyProtos.Empty>> unimplementedCall(A context);

		/**
		 * One empty request followed by one empty response.
		 * 
		 * @param request the client request
		 * 
		 * @return the server response
		 */
		default Mono<io.grpc.testing.integration.EmptyProtos.Empty> unimplementedCall(io.grpc.testing.integration.EmptyProtos.Empty request) {
			return this.unimplementedCall(request, null);
		}

		/**
		 * One empty request followed by one empty response.
		 * 
		 * @param request the client request
		 * @param context the context
		 * 
		 * @return the server response
		 */
		default Mono<io.grpc.testing.integration.EmptyProtos.Empty> unimplementedCall(io.grpc.testing.integration.EmptyProtos.Empty request, A context) {
			return this.unimplementedCall(context)
				.flatMap(grpcExchange -> {
					grpcExchange.request().value(request);
					return grpcExchange.response().flatMap(GrpcResponse.Unary::value);
				});
		}
	}

    public static abstract class Web<A extends ExchangeContext> implements UnimplementedServiceGrpcClient.WebClientStub<A> {

		private final ServiceID serviceID;
		private final WebClient<? extends A> webClient;
		private final GrpcClient grpcClient;

		private final Web<A>.StubImpl stub;

		public Web(ServiceID serviceID, WebClient<? extends A> webClient, GrpcClient grpcClient) {
			this.serviceID = serviceID;
			this.webClient = webClient;
			this.grpcClient = grpcClient;

			this.stub = new UnimplementedServiceGrpcClient.Web.StubImpl(null);
		}

		@Override
		public UnimplementedServiceGrpcClient.WebClientStub<A> withMetadata(Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
			return new UnimplementedServiceGrpcClient.Web<A>.StubImpl(metadataConfigurer);
		}
		
		@Override
		public Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.EmptyProtos.Empty, io.grpc.testing.integration.EmptyProtos.Empty>> unimplementedCall(Consumer<A> contextConfigurer) {
			return this.stub.unimplementedCall(contextConfigurer);
		}

		private final class StubImpl implements UnimplementedServiceGrpcClient.WebClientStub<A> {

			private final Consumer<GrpcOutboundRequestMetadata> metadataConfigurer;

			public StubImpl(Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
				this.metadataConfigurer = metadataConfigurer;
			}

			@Override
			public UnimplementedServiceGrpcClient.WebClientStub<A> withMetadata(Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
				return new UnimplementedServiceGrpcClient.Web<A>.StubImpl(metadataConfigurer);
			}
			
			@Override
			public Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.EmptyProtos.Empty, io.grpc.testing.integration.EmptyProtos.Empty>> unimplementedCall(Consumer<A> contextConfigurer) {
				return Web.this.webClient.exchange(Web.this.serviceID.getURI())
					.map(exchange -> {
						if(contextConfigurer != null) {
							contextConfigurer.accept(exchange.context());
						}
						GrpcExchange.Unary<A, io.grpc.testing.integration.EmptyProtos.Empty, io.grpc.testing.integration.EmptyProtos.Empty> grpcExchange = (GrpcExchange.Unary<A, io.grpc.testing.integration.EmptyProtos.Empty, io.grpc.testing.integration.EmptyProtos.Empty>)Web.this.grpcClient.unary(exchange, SERVICE_NAME, "UnimplementedCall", io.grpc.testing.integration.EmptyProtos.Empty.getDefaultInstance(), io.grpc.testing.integration.EmptyProtos.Empty.getDefaultInstance());
						grpcExchange.request().metadata(this.metadataConfigurer);
						return grpcExchange;
					});
			}
		}
	}

	/**
	 * A simple service NOT implemented at servers so clients can test for
	 * that case.
	 *
	 * @param <A> the exchange context type
	 */
	public interface WebClientStub<A extends ExchangeContext> extends GrpcClient.Stub<A, UnimplementedServiceGrpcClient.WebClientStub<A>> {
		
		/**
		 * One empty request followed by one empty response.
		 *
		 * @return a mono emitting the unary exchange
		 */
		default Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.EmptyProtos.Empty, io.grpc.testing.integration.EmptyProtos.Empty>> unimplementedCall() {
			return this.unimplementedCall((Consumer<A>)null);
		}

		/**
		 * One empty request followed by one empty response.
		 *
		 * @param contextConfigurer the context configurer
		 *
		 * @return a mono emitting the unary exchange
		 */
		Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.EmptyProtos.Empty, io.grpc.testing.integration.EmptyProtos.Empty>> unimplementedCall(Consumer<A> contextConfigurer);

		/**
		 * One empty request followed by one empty response.
		 *
		 * @param request the client request
		 *
		 * @return the server response
		 */
		default Mono<io.grpc.testing.integration.EmptyProtos.Empty> unimplementedCall(io.grpc.testing.integration.EmptyProtos.Empty request) {
			return this.unimplementedCall(request, (Consumer<A>)null);
		}

		/**
		 * One empty request followed by one empty response.
		 *
		 * @param request           the client request
		 * @param contextConfigurer the context configurer
		 *
		 * @return the server response
		 */
		default Mono<io.grpc.testing.integration.EmptyProtos.Empty> unimplementedCall(io.grpc.testing.integration.EmptyProtos.Empty request, Consumer<A> contextConfigurer) {
			return this.unimplementedCall(contextConfigurer)
				.flatMap(grpcExchange -> {
					grpcExchange.request().value(request);
					return grpcExchange.response().flatMap(GrpcResponse.Unary::value);
				});
		}
	}
}
