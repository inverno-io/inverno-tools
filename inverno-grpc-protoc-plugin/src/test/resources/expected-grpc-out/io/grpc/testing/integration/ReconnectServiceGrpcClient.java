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
 * A service used to control reconnect server.
 */

public final class ReconnectServiceGrpcClient {

	public static final GrpcServiceName SERVICE_NAME = GrpcServiceName.of("grpc.testing", "ReconnectService");

	private ReconnectServiceGrpcClient() {}

    public static abstract class Http {

		private final HttpClient httpClient;
		private final GrpcClient grpcClient;

		public Http(HttpClient httpClient, GrpcClient grpcClient) {
			this.httpClient = httpClient;
			this.grpcClient = grpcClient;
		}

		public <A extends ExchangeContext> ReconnectServiceGrpcClient.HttpClientStub<A> createStub(String host, int port) {
			return new ReconnectServiceGrpcClient.Http.StubImpl<>(this.httpClient.<A>endpoint(host, port).build(), true);
		}

		public <A extends ExchangeContext> ReconnectServiceGrpcClient.HttpClientStub<A> createStub(InetSocketAddress remoteAddress) {
			return new ReconnectServiceGrpcClient.Http.StubImpl<>(this.httpClient.<A>endpoint(remoteAddress).build(), true);
		}

		public <A extends ExchangeContext> ReconnectServiceGrpcClient.HttpClientStub<A> createStub(Endpoint<A> endpoint) {
			return new ReconnectServiceGrpcClient.Http.StubImpl<>(endpoint, false);
		}

		private final class StubImpl<A extends ExchangeContext> implements ReconnectServiceGrpcClient.HttpClientStub<A> {

			private final Endpoint<A> endpoint;
			private final boolean shutdownEndpoint;
			private final Consumer<GrpcOutboundRequestMetadata> metadataConfigurer;

			public StubImpl(Endpoint<A> endpoint, boolean shutdownEndpoint) {
				this.endpoint = endpoint;
				this.shutdownEndpoint = shutdownEndpoint;
				this.metadataConfigurer = null;
			}

			private StubImpl(ReconnectServiceGrpcClient.Http.StubImpl<A> parent, Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
				this.endpoint = parent.endpoint;
				this.shutdownEndpoint = false;
				this.metadataConfigurer = metadataConfigurer;
			}

			@Override
			public ReconnectServiceGrpcClient.HttpClientStub<A> withMetadata(Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
				return new ReconnectServiceGrpcClient.Http.StubImpl<>(this, metadataConfigurer);
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
			public Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.ReconnectParams, io.grpc.testing.integration.EmptyProtos.Empty>> start(A context) {
				return this.endpoint.exchange(context)
					.map(exchange -> {
						GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.ReconnectParams, io.grpc.testing.integration.EmptyProtos.Empty> grpcExchange = ReconnectServiceGrpcClient.Http.this.grpcClient.unary(exchange, SERVICE_NAME, "Start", io.grpc.testing.integration.Messages.ReconnectParams.getDefaultInstance(), io.grpc.testing.integration.EmptyProtos.Empty.getDefaultInstance());
						grpcExchange.request().metadata(this.metadataConfigurer);
						return grpcExchange;
					});
			}
			@Override
			public Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.EmptyProtos.Empty, io.grpc.testing.integration.Messages.ReconnectInfo>> stop(A context) {
				return this.endpoint.exchange(context)
					.map(exchange -> {
						GrpcExchange.Unary<A, io.grpc.testing.integration.EmptyProtos.Empty, io.grpc.testing.integration.Messages.ReconnectInfo> grpcExchange = ReconnectServiceGrpcClient.Http.this.grpcClient.unary(exchange, SERVICE_NAME, "Stop", io.grpc.testing.integration.EmptyProtos.Empty.getDefaultInstance(), io.grpc.testing.integration.Messages.ReconnectInfo.getDefaultInstance());
						grpcExchange.request().metadata(this.metadataConfigurer);
						return grpcExchange;
					});
			}
		}
	}

	/**
	 * A service used to control reconnect server.
	 *
	 * @param <A> the exchange context type
	 */
	public interface HttpClientStub<A extends ExchangeContext> extends GrpcClient.CloseableStub<A, ReconnectServiceGrpcClient.HttpClientStub<A>> {
		
		/**
		 * One empty request followed by one empty response.
		 * 
		 * @return a mono emitting the unary exchange
		 */
		default Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.ReconnectParams, io.grpc.testing.integration.EmptyProtos.Empty>> start() {
			return this.start((A)null);
		}

		/**
		 * One empty request followed by one empty response.
		 * 
		 * @param context the context
		 * 
		 * @return a mono emitting the unary exchange
		 */
		Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.ReconnectParams, io.grpc.testing.integration.EmptyProtos.Empty>> start(A context);

		/**
		 * One empty request followed by one empty response.
		 * 
		 * @param request the client request
		 * 
		 * @return the server response
		 */
		default Mono<io.grpc.testing.integration.EmptyProtos.Empty> start(io.grpc.testing.integration.Messages.ReconnectParams request) {
			return this.start(request, null);
		}

		/**
		 * One empty request followed by one empty response.
		 * 
		 * @param request the client request
		 * @param context the context
		 * 
		 * @return the server response
		 */
		default Mono<io.grpc.testing.integration.EmptyProtos.Empty> start(io.grpc.testing.integration.Messages.ReconnectParams request, A context) {
			return this.start(context)
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
		default Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.EmptyProtos.Empty, io.grpc.testing.integration.Messages.ReconnectInfo>> stop() {
			return this.stop((A)null);
		}

		/**
		 * One request followed by one response.
		 * 
		 * @param context the context
		 * 
		 * @return a mono emitting the unary exchange
		 */
		Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.EmptyProtos.Empty, io.grpc.testing.integration.Messages.ReconnectInfo>> stop(A context);

		/**
		 * One request followed by one response.
		 * 
		 * @param request the client request
		 * 
		 * @return the server response
		 */
		default Mono<io.grpc.testing.integration.Messages.ReconnectInfo> stop(io.grpc.testing.integration.EmptyProtos.Empty request) {
			return this.stop(request, null);
		}

		/**
		 * One request followed by one response.
		 * 
		 * @param request the client request
		 * @param context the context
		 * 
		 * @return the server response
		 */
		default Mono<io.grpc.testing.integration.Messages.ReconnectInfo> stop(io.grpc.testing.integration.EmptyProtos.Empty request, A context) {
			return this.stop(context)
				.flatMap(grpcExchange -> {
					grpcExchange.request().value(request);
					return grpcExchange.response().flatMap(GrpcResponse.Unary::value);
				});
		}
	}

    public static abstract class Web<A extends ExchangeContext> implements ReconnectServiceGrpcClient.WebClientStub<A> {

		private final ServiceID serviceID;
		private final WebClient<? extends A> webClient;
		private final GrpcClient grpcClient;

		private final Web<A>.StubImpl stub;

		public Web(ServiceID serviceID, WebClient<? extends A> webClient, GrpcClient grpcClient) {
			this.serviceID = serviceID;
			this.webClient = webClient;
			this.grpcClient = grpcClient;

			this.stub = new ReconnectServiceGrpcClient.Web.StubImpl(null);
		}

		@Override
		public ReconnectServiceGrpcClient.WebClientStub<A> withMetadata(Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
			return new ReconnectServiceGrpcClient.Web<A>.StubImpl(metadataConfigurer);
		}
		
		@Override
		public Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.ReconnectParams, io.grpc.testing.integration.EmptyProtos.Empty>> start(Consumer<A> contextConfigurer) {
			return this.stub.start(contextConfigurer);
		}
		@Override
		public Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.EmptyProtos.Empty, io.grpc.testing.integration.Messages.ReconnectInfo>> stop(Consumer<A> contextConfigurer) {
			return this.stub.stop(contextConfigurer);
		}

		private final class StubImpl implements ReconnectServiceGrpcClient.WebClientStub<A> {

			private final Consumer<GrpcOutboundRequestMetadata> metadataConfigurer;

			public StubImpl(Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
				this.metadataConfigurer = metadataConfigurer;
			}

			@Override
			public ReconnectServiceGrpcClient.WebClientStub<A> withMetadata(Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
				return new ReconnectServiceGrpcClient.Web<A>.StubImpl(metadataConfigurer);
			}
			
			@Override
			public Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.ReconnectParams, io.grpc.testing.integration.EmptyProtos.Empty>> start(Consumer<A> contextConfigurer) {
				return Web.this.webClient.exchange(Web.this.serviceID.getURI())
					.map(exchange -> {
						if(contextConfigurer != null) {
							contextConfigurer.accept(exchange.context());
						}
						GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.ReconnectParams, io.grpc.testing.integration.EmptyProtos.Empty> grpcExchange = (GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.ReconnectParams, io.grpc.testing.integration.EmptyProtos.Empty>)Web.this.grpcClient.unary(exchange, SERVICE_NAME, "Start", io.grpc.testing.integration.Messages.ReconnectParams.getDefaultInstance(), io.grpc.testing.integration.EmptyProtos.Empty.getDefaultInstance());
						grpcExchange.request().metadata(this.metadataConfigurer);
						return grpcExchange;
					});
			}
			@Override
			public Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.EmptyProtos.Empty, io.grpc.testing.integration.Messages.ReconnectInfo>> stop(Consumer<A> contextConfigurer) {
				return Web.this.webClient.exchange(Web.this.serviceID.getURI())
					.map(exchange -> {
						if(contextConfigurer != null) {
							contextConfigurer.accept(exchange.context());
						}
						GrpcExchange.Unary<A, io.grpc.testing.integration.EmptyProtos.Empty, io.grpc.testing.integration.Messages.ReconnectInfo> grpcExchange = (GrpcExchange.Unary<A, io.grpc.testing.integration.EmptyProtos.Empty, io.grpc.testing.integration.Messages.ReconnectInfo>)Web.this.grpcClient.unary(exchange, SERVICE_NAME, "Stop", io.grpc.testing.integration.EmptyProtos.Empty.getDefaultInstance(), io.grpc.testing.integration.Messages.ReconnectInfo.getDefaultInstance());
						grpcExchange.request().metadata(this.metadataConfigurer);
						return grpcExchange;
					});
			}
		}
	}

	/**
	 * A service used to control reconnect server.
	 *
	 * @param <A> the exchange context type
	 */
	public interface WebClientStub<A extends ExchangeContext> extends GrpcClient.Stub<A, ReconnectServiceGrpcClient.WebClientStub<A>> {
		
		/**
		 * One empty request followed by one empty response.
		 *
		 * @return a mono emitting the unary exchange
		 */
		default Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.ReconnectParams, io.grpc.testing.integration.EmptyProtos.Empty>> start() {
			return this.start((Consumer<A>)null);
		}

		/**
		 * One empty request followed by one empty response.
		 *
		 * @param contextConfigurer the context configurer
		 *
		 * @return a mono emitting the unary exchange
		 */
		Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.ReconnectParams, io.grpc.testing.integration.EmptyProtos.Empty>> start(Consumer<A> contextConfigurer);

		/**
		 * One empty request followed by one empty response.
		 *
		 * @param request the client request
		 *
		 * @return the server response
		 */
		default Mono<io.grpc.testing.integration.EmptyProtos.Empty> start(io.grpc.testing.integration.Messages.ReconnectParams request) {
			return this.start(request, (Consumer<A>)null);
		}

		/**
		 * One empty request followed by one empty response.
		 *
		 * @param request           the client request
		 * @param contextConfigurer the context configurer
		 *
		 * @return the server response
		 */
		default Mono<io.grpc.testing.integration.EmptyProtos.Empty> start(io.grpc.testing.integration.Messages.ReconnectParams request, Consumer<A> contextConfigurer) {
			return this.start(contextConfigurer)
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
		default Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.EmptyProtos.Empty, io.grpc.testing.integration.Messages.ReconnectInfo>> stop() {
			return this.stop((Consumer<A>)null);
		}

		/**
		 * One request followed by one response.
		 *
		 * @param contextConfigurer the context configurer
		 *
		 * @return a mono emitting the unary exchange
		 */
		Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.EmptyProtos.Empty, io.grpc.testing.integration.Messages.ReconnectInfo>> stop(Consumer<A> contextConfigurer);

		/**
		 * One request followed by one response.
		 *
		 * @param request the client request
		 *
		 * @return the server response
		 */
		default Mono<io.grpc.testing.integration.Messages.ReconnectInfo> stop(io.grpc.testing.integration.EmptyProtos.Empty request) {
			return this.stop(request, (Consumer<A>)null);
		}

		/**
		 * One request followed by one response.
		 *
		 * @param request           the client request
		 * @param contextConfigurer the context configurer
		 *
		 * @return the server response
		 */
		default Mono<io.grpc.testing.integration.Messages.ReconnectInfo> stop(io.grpc.testing.integration.EmptyProtos.Empty request, Consumer<A> contextConfigurer) {
			return this.stop(contextConfigurer)
				.flatMap(grpcExchange -> {
					grpcExchange.request().value(request);
					return grpcExchange.response().flatMap(GrpcResponse.Unary::value);
				});
		}
	}
}
