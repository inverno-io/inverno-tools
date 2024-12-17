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
 * A service used to obtain stats for verifying LB behavior.
 */

public final class LoadBalancerStatsServiceGrpcClient {

	public static final GrpcServiceName SERVICE_NAME = GrpcServiceName.of("grpc.testing", "LoadBalancerStatsService");

	private LoadBalancerStatsServiceGrpcClient() {}

    public static abstract class Http {

		private final HttpClient httpClient;
		private final GrpcClient grpcClient;

		public Http(HttpClient httpClient, GrpcClient grpcClient) {
			this.httpClient = httpClient;
			this.grpcClient = grpcClient;
		}

		public <A extends ExchangeContext> LoadBalancerStatsServiceGrpcClient.HttpClientStub<A> createStub(String host, int port) {
			return new LoadBalancerStatsServiceGrpcClient.Http.StubImpl<>(this.httpClient.<A>endpoint(host, port).build(), true);
		}

		public <A extends ExchangeContext> LoadBalancerStatsServiceGrpcClient.HttpClientStub<A> createStub(InetSocketAddress remoteAddress) {
			return new LoadBalancerStatsServiceGrpcClient.Http.StubImpl<>(this.httpClient.<A>endpoint(remoteAddress).build(), true);
		}

		public <A extends ExchangeContext> LoadBalancerStatsServiceGrpcClient.HttpClientStub<A> createStub(Endpoint<A> endpoint) {
			return new LoadBalancerStatsServiceGrpcClient.Http.StubImpl<>(endpoint, false);
		}

		private final class StubImpl<A extends ExchangeContext> implements LoadBalancerStatsServiceGrpcClient.HttpClientStub<A> {

			private final Endpoint<A> endpoint;
			private final boolean shutdownEndpoint;
			private final Consumer<GrpcOutboundRequestMetadata> metadataConfigurer;

			public StubImpl(Endpoint<A> endpoint, boolean shutdownEndpoint) {
				this.endpoint = endpoint;
				this.shutdownEndpoint = shutdownEndpoint;
				this.metadataConfigurer = null;
			}

			private StubImpl(LoadBalancerStatsServiceGrpcClient.Http.StubImpl<A> parent, Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
				this.endpoint = parent.endpoint;
				this.shutdownEndpoint = false;
				this.metadataConfigurer = metadataConfigurer;
			}

			@Override
			public LoadBalancerStatsServiceGrpcClient.HttpClientStub<A> withMetadata(Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
				return new LoadBalancerStatsServiceGrpcClient.Http.StubImpl<>(this, metadataConfigurer);
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
			public Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.LoadBalancerStatsRequest, io.grpc.testing.integration.Messages.LoadBalancerStatsResponse>> getClientStats(A context) {
				return this.endpoint.exchange(context)
					.map(exchange -> {
						GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.LoadBalancerStatsRequest, io.grpc.testing.integration.Messages.LoadBalancerStatsResponse> grpcExchange = LoadBalancerStatsServiceGrpcClient.Http.this.grpcClient.unary(exchange, SERVICE_NAME, "GetClientStats", io.grpc.testing.integration.Messages.LoadBalancerStatsRequest.getDefaultInstance(), io.grpc.testing.integration.Messages.LoadBalancerStatsResponse.getDefaultInstance());
						grpcExchange.request().metadata(this.metadataConfigurer);
						return grpcExchange;
					});
			}
			@Override
			public Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.LoadBalancerAccumulatedStatsRequest, io.grpc.testing.integration.Messages.LoadBalancerAccumulatedStatsResponse>> getClientAccumulatedStats(A context) {
				return this.endpoint.exchange(context)
					.map(exchange -> {
						GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.LoadBalancerAccumulatedStatsRequest, io.grpc.testing.integration.Messages.LoadBalancerAccumulatedStatsResponse> grpcExchange = LoadBalancerStatsServiceGrpcClient.Http.this.grpcClient.unary(exchange, SERVICE_NAME, "GetClientAccumulatedStats", io.grpc.testing.integration.Messages.LoadBalancerAccumulatedStatsRequest.getDefaultInstance(), io.grpc.testing.integration.Messages.LoadBalancerAccumulatedStatsResponse.getDefaultInstance());
						grpcExchange.request().metadata(this.metadataConfigurer);
						return grpcExchange;
					});
			}
		}
	}

	/**
	 * A service used to obtain stats for verifying LB behavior.
	 *
	 * @param <A> the exchange context type
	 */
	public interface HttpClientStub<A extends ExchangeContext> extends GrpcClient.CloseableStub<A, LoadBalancerStatsServiceGrpcClient.HttpClientStub<A>> {
		
		/**
		 * One empty request followed by one empty response.
		 * 
		 * @return a mono emitting the unary exchange
		 */
		default Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.LoadBalancerStatsRequest, io.grpc.testing.integration.Messages.LoadBalancerStatsResponse>> getClientStats() {
			return this.getClientStats((A)null);
		}

		/**
		 * One empty request followed by one empty response.
		 * 
		 * @param context the context
		 * 
		 * @return a mono emitting the unary exchange
		 */
		Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.LoadBalancerStatsRequest, io.grpc.testing.integration.Messages.LoadBalancerStatsResponse>> getClientStats(A context);

		/**
		 * One empty request followed by one empty response.
		 * 
		 * @param request the client request
		 * 
		 * @return the server response
		 */
		default Mono<io.grpc.testing.integration.Messages.LoadBalancerStatsResponse> getClientStats(io.grpc.testing.integration.Messages.LoadBalancerStatsRequest request) {
			return this.getClientStats(request, null);
		}

		/**
		 * One empty request followed by one empty response.
		 * 
		 * @param request the client request
		 * @param context the context
		 * 
		 * @return the server response
		 */
		default Mono<io.grpc.testing.integration.Messages.LoadBalancerStatsResponse> getClientStats(io.grpc.testing.integration.Messages.LoadBalancerStatsRequest request, A context) {
			return this.getClientStats(context)
				.flatMap(grpcExchange -> {
					grpcExchange.request().value(request);
					return grpcExchange.response().flatMap(GrpcResponse.Unary::value);
				});
		}
		/**
		 * One request followed by one response.
		 * 
		 * @return a mono emitting the unary exchange
		 */
		default Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.LoadBalancerAccumulatedStatsRequest, io.grpc.testing.integration.Messages.LoadBalancerAccumulatedStatsResponse>> getClientAccumulatedStats() {
			return this.getClientAccumulatedStats((A)null);
		}

		/**
		 * One request followed by one response.
		 * 
		 * @param context the context
		 * 
		 * @return a mono emitting the unary exchange
		 */
		Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.LoadBalancerAccumulatedStatsRequest, io.grpc.testing.integration.Messages.LoadBalancerAccumulatedStatsResponse>> getClientAccumulatedStats(A context);

		/**
		 * One request followed by one response.
		 * 
		 * @param request the client request
		 * 
		 * @return the server response
		 */
		default Mono<io.grpc.testing.integration.Messages.LoadBalancerAccumulatedStatsResponse> getClientAccumulatedStats(io.grpc.testing.integration.Messages.LoadBalancerAccumulatedStatsRequest request) {
			return this.getClientAccumulatedStats(request, null);
		}

		/**
		 * One request followed by one response.
		 * 
		 * @param request the client request
		 * @param context the context
		 * 
		 * @return the server response
		 */
		default Mono<io.grpc.testing.integration.Messages.LoadBalancerAccumulatedStatsResponse> getClientAccumulatedStats(io.grpc.testing.integration.Messages.LoadBalancerAccumulatedStatsRequest request, A context) {
			return this.getClientAccumulatedStats(context)
				.flatMap(grpcExchange -> {
					grpcExchange.request().value(request);
					return grpcExchange.response().flatMap(GrpcResponse.Unary::value);
				});
		}
	}

    public static abstract class Web<A extends ExchangeContext> implements LoadBalancerStatsServiceGrpcClient.WebClientStub<A> {

		private final ServiceID serviceID;
		private final WebClient<? extends A> webClient;
		private final GrpcClient grpcClient;

		private final Web<A>.StubImpl stub;

		public Web(ServiceID serviceID, WebClient<? extends A> webClient, GrpcClient grpcClient) {
			this.serviceID = serviceID;
			this.webClient = webClient;
			this.grpcClient = grpcClient;

			this.stub = new LoadBalancerStatsServiceGrpcClient.Web.StubImpl(null);
		}

		@Override
		public LoadBalancerStatsServiceGrpcClient.WebClientStub<A> withMetadata(Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
			return new LoadBalancerStatsServiceGrpcClient.Web<A>.StubImpl(metadataConfigurer);
		}
		
		@Override
		public Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.LoadBalancerStatsRequest, io.grpc.testing.integration.Messages.LoadBalancerStatsResponse>> getClientStats(Consumer<A> contextConfigurer) {
			return this.stub.getClientStats(contextConfigurer);
		}
		@Override
		public Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.LoadBalancerAccumulatedStatsRequest, io.grpc.testing.integration.Messages.LoadBalancerAccumulatedStatsResponse>> getClientAccumulatedStats(Consumer<A> contextConfigurer) {
			return this.stub.getClientAccumulatedStats(contextConfigurer);
		}

		private final class StubImpl implements LoadBalancerStatsServiceGrpcClient.WebClientStub<A> {

			private final Consumer<GrpcOutboundRequestMetadata> metadataConfigurer;

			public StubImpl(Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
				this.metadataConfigurer = metadataConfigurer;
			}

			@Override
			public LoadBalancerStatsServiceGrpcClient.WebClientStub<A> withMetadata(Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
				return new LoadBalancerStatsServiceGrpcClient.Web<A>.StubImpl(metadataConfigurer);
			}
			
			@Override
			public Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.LoadBalancerStatsRequest, io.grpc.testing.integration.Messages.LoadBalancerStatsResponse>> getClientStats(Consumer<A> contextConfigurer) {
				return Web.this.webClient.exchange(Web.this.serviceID.getURI())
					.map(exchange -> {
						if(contextConfigurer != null) {
							contextConfigurer.accept(exchange.context());
						}
						GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.LoadBalancerStatsRequest, io.grpc.testing.integration.Messages.LoadBalancerStatsResponse> grpcExchange = (GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.LoadBalancerStatsRequest, io.grpc.testing.integration.Messages.LoadBalancerStatsResponse>)Web.this.grpcClient.unary(exchange, SERVICE_NAME, "GetClientStats", io.grpc.testing.integration.Messages.LoadBalancerStatsRequest.getDefaultInstance(), io.grpc.testing.integration.Messages.LoadBalancerStatsResponse.getDefaultInstance());
						grpcExchange.request().metadata(this.metadataConfigurer);
						return grpcExchange;
					});
			}
			@Override
			public Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.LoadBalancerAccumulatedStatsRequest, io.grpc.testing.integration.Messages.LoadBalancerAccumulatedStatsResponse>> getClientAccumulatedStats(Consumer<A> contextConfigurer) {
				return Web.this.webClient.exchange(Web.this.serviceID.getURI())
					.map(exchange -> {
						if(contextConfigurer != null) {
							contextConfigurer.accept(exchange.context());
						}
						GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.LoadBalancerAccumulatedStatsRequest, io.grpc.testing.integration.Messages.LoadBalancerAccumulatedStatsResponse> grpcExchange = (GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.LoadBalancerAccumulatedStatsRequest, io.grpc.testing.integration.Messages.LoadBalancerAccumulatedStatsResponse>)Web.this.grpcClient.unary(exchange, SERVICE_NAME, "GetClientAccumulatedStats", io.grpc.testing.integration.Messages.LoadBalancerAccumulatedStatsRequest.getDefaultInstance(), io.grpc.testing.integration.Messages.LoadBalancerAccumulatedStatsResponse.getDefaultInstance());
						grpcExchange.request().metadata(this.metadataConfigurer);
						return grpcExchange;
					});
			}
		}
	}

	/**
	 * A service used to obtain stats for verifying LB behavior.
	 *
	 * @param <A> the exchange context type
	 */
	public interface WebClientStub<A extends ExchangeContext> extends GrpcClient.Stub<A, LoadBalancerStatsServiceGrpcClient.WebClientStub<A>> {
		
		/**
		 * One empty request followed by one empty response.
		 *
		 * @return a mono emitting the unary exchange
		 */
		default Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.LoadBalancerStatsRequest, io.grpc.testing.integration.Messages.LoadBalancerStatsResponse>> getClientStats() {
			return this.getClientStats((Consumer<A>)null);
		}

		/**
		 * One empty request followed by one empty response.
		 *
		 * @param contextConfigurer the context configurer
		 *
		 * @return a mono emitting the unary exchange
		 */
		Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.LoadBalancerStatsRequest, io.grpc.testing.integration.Messages.LoadBalancerStatsResponse>> getClientStats(Consumer<A> contextConfigurer);

		/**
		 * One empty request followed by one empty response.
		 *
		 * @param request the client request
		 *
		 * @return the server response
		 */
		default Mono<io.grpc.testing.integration.Messages.LoadBalancerStatsResponse> getClientStats(io.grpc.testing.integration.Messages.LoadBalancerStatsRequest request) {
			return this.getClientStats(request, (Consumer<A>)null);
		}

		/**
		 * One empty request followed by one empty response.
		 *
		 * @param request           the client request
		 * @param contextConfigurer the context configurer
		 *
		 * @return the server response
		 */
		default Mono<io.grpc.testing.integration.Messages.LoadBalancerStatsResponse> getClientStats(io.grpc.testing.integration.Messages.LoadBalancerStatsRequest request, Consumer<A> contextConfigurer) {
			return this.getClientStats(contextConfigurer)
				.flatMap(grpcExchange -> {
					grpcExchange.request().value(request);
					return grpcExchange.response().flatMap(GrpcResponse.Unary::value);
				});
		}
		/**
		 * One request followed by one response.
		 *
		 * @return a mono emitting the unary exchange
		 */
		default Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.LoadBalancerAccumulatedStatsRequest, io.grpc.testing.integration.Messages.LoadBalancerAccumulatedStatsResponse>> getClientAccumulatedStats() {
			return this.getClientAccumulatedStats((Consumer<A>)null);
		}

		/**
		 * One request followed by one response.
		 *
		 * @param contextConfigurer the context configurer
		 *
		 * @return a mono emitting the unary exchange
		 */
		Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.LoadBalancerAccumulatedStatsRequest, io.grpc.testing.integration.Messages.LoadBalancerAccumulatedStatsResponse>> getClientAccumulatedStats(Consumer<A> contextConfigurer);

		/**
		 * One request followed by one response.
		 *
		 * @param request the client request
		 *
		 * @return the server response
		 */
		default Mono<io.grpc.testing.integration.Messages.LoadBalancerAccumulatedStatsResponse> getClientAccumulatedStats(io.grpc.testing.integration.Messages.LoadBalancerAccumulatedStatsRequest request) {
			return this.getClientAccumulatedStats(request, (Consumer<A>)null);
		}

		/**
		 * One request followed by one response.
		 *
		 * @param request           the client request
		 * @param contextConfigurer the context configurer
		 *
		 * @return the server response
		 */
		default Mono<io.grpc.testing.integration.Messages.LoadBalancerAccumulatedStatsResponse> getClientAccumulatedStats(io.grpc.testing.integration.Messages.LoadBalancerAccumulatedStatsRequest request, Consumer<A> contextConfigurer) {
			return this.getClientAccumulatedStats(contextConfigurer)
				.flatMap(grpcExchange -> {
					grpcExchange.request().value(request);
					return grpcExchange.response().flatMap(GrpcResponse.Unary::value);
				});
		}
	}
}
