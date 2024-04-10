package io.grpc.testing.integration;

import io.inverno.core.annotation.Bean;
import io.inverno.mod.grpc.base.GrpcOutboundRequestMetadata;
import io.inverno.mod.grpc.base.GrpcServiceName;
import io.inverno.mod.grpc.client.GrpcClient;
import io.inverno.mod.grpc.client.GrpcExchange;
import io.inverno.mod.grpc.client.GrpcResponse;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.client.Endpoint;
import io.inverno.mod.http.client.HttpClient;
import java.net.InetSocketAddress;
import java.util.function.Consumer;
import reactor.core.publisher.Mono;

/**
 * A service used to control reconnect server.
 */
@Bean
public final class ReconnectServiceGrpcClient {

	public static final GrpcServiceName SERVICE_NAME = GrpcServiceName.of("grpc.testing", "ReconnectService");
	
	private final HttpClient httpClient;
	private final GrpcClient grpcClient;
	
	public ReconnectServiceGrpcClient(HttpClient httpClient, GrpcClient grpcClient) {
		this.httpClient = httpClient;
		this.grpcClient = grpcClient;
	}

	public <A extends ExchangeContext> ReconnectServiceGrpcClient.Stub<A> createStub(String host, int port) {
		return new ReconnectServiceGrpcClient.StubImpl<>(this.httpClient.<A>endpoint(host, port).build(), true);
	}
	
	public <A extends ExchangeContext> ReconnectServiceGrpcClient.Stub<A> createStub(InetSocketAddress remoteAddress) {
		return new ReconnectServiceGrpcClient.StubImpl<>(this.httpClient.<A>endpoint(remoteAddress).build(), true);
	}
	
	public <A extends ExchangeContext> ReconnectServiceGrpcClient.Stub<A> createStub(Endpoint<A> endpoint) {
		return new ReconnectServiceGrpcClient.StubImpl<>(endpoint, false);
	}
	
	private final class StubImpl<A extends ExchangeContext> implements ReconnectServiceGrpcClient.Stub<A> {

		private final Endpoint<A> endpoint;
		private final boolean shutdownEndpoint;
		private final Consumer<GrpcOutboundRequestMetadata> metadataConfigurer;
		
		public StubImpl(Endpoint<A> endpoint, boolean shutdownEndpoint) {
			this.endpoint = endpoint;
			this.shutdownEndpoint = shutdownEndpoint;
			this.metadataConfigurer = null;
		}
		
		private StubImpl(ReconnectServiceGrpcClient.StubImpl<A> parent, Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
			this.endpoint = parent.endpoint;
			this.shutdownEndpoint = false;
			this.metadataConfigurer = metadataConfigurer;
		}

		@Override
		public ReconnectServiceGrpcClient.Stub<A> withMetadata(Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
			return new ReconnectServiceGrpcClient.StubImpl<>(this, metadataConfigurer);
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
					GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.ReconnectParams, io.grpc.testing.integration.EmptyProtos.Empty> grpcExchange = ReconnectServiceGrpcClient.this.grpcClient.unary(exchange, SERVICE_NAME, "Start", io.grpc.testing.integration.Messages.ReconnectParams.getDefaultInstance(), io.grpc.testing.integration.EmptyProtos.Empty.getDefaultInstance());
					grpcExchange.request().metadata(this.metadataConfigurer);
					return grpcExchange;
				});
		}
		@Override
		public Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.EmptyProtos.Empty, io.grpc.testing.integration.Messages.ReconnectInfo>> stop(A context) {
			return this.endpoint.exchange(context)
				.map(exchange -> {
					GrpcExchange.Unary<A, io.grpc.testing.integration.EmptyProtos.Empty, io.grpc.testing.integration.Messages.ReconnectInfo> grpcExchange = ReconnectServiceGrpcClient.this.grpcClient.unary(exchange, SERVICE_NAME, "Stop", io.grpc.testing.integration.EmptyProtos.Empty.getDefaultInstance(), io.grpc.testing.integration.Messages.ReconnectInfo.getDefaultInstance());
					grpcExchange.request().metadata(this.metadataConfigurer);
					return grpcExchange;
				});
		}
	}
	
	/**
	 * A service used to control reconnect server.
	 *
	 * @param <A> the exchange context type
	 */
	public interface Stub<A extends ExchangeContext> extends GrpcClient.Stub<A, ReconnectServiceGrpcClient.Stub<A>> {
		
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
		 * @param <A>     the context type
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
		 * @param <A>     the context type
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
		 * @param <A>     the context type
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
		 * @param <A>     the context type
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
}
