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
 * A service to remotely control health status of an xDS test server.
 */
@Bean
public final class XdsUpdateHealthServiceGrpcClient {

	public static final GrpcServiceName SERVICE_NAME = GrpcServiceName.of("grpc.testing", "XdsUpdateHealthService");
	
	private final HttpClient httpClient;
	private final GrpcClient grpcClient;
	
	public XdsUpdateHealthServiceGrpcClient(HttpClient httpClient, GrpcClient grpcClient) {
		this.httpClient = httpClient;
		this.grpcClient = grpcClient;
	}

	public <A extends ExchangeContext> XdsUpdateHealthServiceGrpcClient.Stub<A> createStub(String host, int port) {
		return new XdsUpdateHealthServiceGrpcClient.StubImpl<>(this.httpClient.<A>endpoint(host, port).build(), true);
	}
	
	public <A extends ExchangeContext> XdsUpdateHealthServiceGrpcClient.Stub<A> createStub(InetSocketAddress remoteAddress) {
		return new XdsUpdateHealthServiceGrpcClient.StubImpl<>(this.httpClient.<A>endpoint(remoteAddress).build(), true);
	}
	
	public <A extends ExchangeContext> XdsUpdateHealthServiceGrpcClient.Stub<A> createStub(Endpoint<A> endpoint) {
		return new XdsUpdateHealthServiceGrpcClient.StubImpl<>(endpoint, false);
	}
	
	private final class StubImpl<A extends ExchangeContext> implements XdsUpdateHealthServiceGrpcClient.Stub<A> {

		private final Endpoint<A> endpoint;
		private final boolean shutdownEndpoint;
		private final Consumer<GrpcOutboundRequestMetadata> metadataConfigurer;
		
		public StubImpl(Endpoint<A> endpoint, boolean shutdownEndpoint) {
			this.endpoint = endpoint;
			this.shutdownEndpoint = shutdownEndpoint;
			this.metadataConfigurer = null;
		}
		
		private StubImpl(XdsUpdateHealthServiceGrpcClient.StubImpl<A> parent, Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
			this.endpoint = parent.endpoint;
			this.shutdownEndpoint = false;
			this.metadataConfigurer = metadataConfigurer;
		}

		@Override
		public XdsUpdateHealthServiceGrpcClient.Stub<A> withMetadata(Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
			return new XdsUpdateHealthServiceGrpcClient.StubImpl<>(this, metadataConfigurer);
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
		public Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.EmptyProtos.Empty, io.grpc.testing.integration.EmptyProtos.Empty>> setServing(A context) {
			return this.endpoint.exchange(context)
				.map(exchange -> {
					GrpcExchange.Unary<A, io.grpc.testing.integration.EmptyProtos.Empty, io.grpc.testing.integration.EmptyProtos.Empty> grpcExchange = XdsUpdateHealthServiceGrpcClient.this.grpcClient.unary(exchange, SERVICE_NAME, "SetServing", io.grpc.testing.integration.EmptyProtos.Empty.getDefaultInstance(), io.grpc.testing.integration.EmptyProtos.Empty.getDefaultInstance());
					grpcExchange.request().metadata(this.metadataConfigurer);
					return grpcExchange;
				});
		}
		@Override
		public Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.EmptyProtos.Empty, io.grpc.testing.integration.EmptyProtos.Empty>> setNotServing(A context) {
			return this.endpoint.exchange(context)
				.map(exchange -> {
					GrpcExchange.Unary<A, io.grpc.testing.integration.EmptyProtos.Empty, io.grpc.testing.integration.EmptyProtos.Empty> grpcExchange = XdsUpdateHealthServiceGrpcClient.this.grpcClient.unary(exchange, SERVICE_NAME, "SetNotServing", io.grpc.testing.integration.EmptyProtos.Empty.getDefaultInstance(), io.grpc.testing.integration.EmptyProtos.Empty.getDefaultInstance());
					grpcExchange.request().metadata(this.metadataConfigurer);
					return grpcExchange;
				});
		}
	}
	
	/**
	 * A service to remotely control health status of an xDS test server.
	 *
	 * @param <A> the exchange context type
	 */
	public interface Stub<A extends ExchangeContext> extends GrpcClient.Stub<A, XdsUpdateHealthServiceGrpcClient.Stub<A>> {
		
		/**
		 * One empty request followed by one empty response.
		 * 
		 * @return a mono emitting the unary exchange
		 */
		default Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.EmptyProtos.Empty, io.grpc.testing.integration.EmptyProtos.Empty>> setServing() {
			return this.setServing((A)null);
		}

		/**
		 * One empty request followed by one empty response.
		 * 
		 * @param <A>     the context type
		 * @param context the context
		 * 
		 * @return a mono emitting the unary exchange
		 */
		Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.EmptyProtos.Empty, io.grpc.testing.integration.EmptyProtos.Empty>> setServing(A context);

		/**
		 * One empty request followed by one empty response.
		 * 
		 * @param request the client request
		 * 
		 * @return the server response
		 */
		default Mono<io.grpc.testing.integration.EmptyProtos.Empty> setServing(io.grpc.testing.integration.EmptyProtos.Empty request) {
			return this.setServing(request, null);
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
		default Mono<io.grpc.testing.integration.EmptyProtos.Empty> setServing(io.grpc.testing.integration.EmptyProtos.Empty request, A context) {
			return this.setServing(context)
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
		default Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.EmptyProtos.Empty, io.grpc.testing.integration.EmptyProtos.Empty>> setNotServing() {
			return this.setNotServing((A)null);
		}

		/**
		 * One request followed by one response.
		 * 
		 * @param <A>     the context type
		 * @param context the context
		 * 
		 * @return a mono emitting the unary exchange
		 */
		Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.EmptyProtos.Empty, io.grpc.testing.integration.EmptyProtos.Empty>> setNotServing(A context);

		/**
		 * One request followed by one response.
		 * 
		 * @param request the client request
		 * 
		 * @return the server response
		 */
		default Mono<io.grpc.testing.integration.EmptyProtos.Empty> setNotServing(io.grpc.testing.integration.EmptyProtos.Empty request) {
			return this.setNotServing(request, null);
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
		default Mono<io.grpc.testing.integration.EmptyProtos.Empty> setNotServing(io.grpc.testing.integration.EmptyProtos.Empty request, A context) {
			return this.setNotServing(context)
				.flatMap(grpcExchange -> {
					grpcExchange.request().value(request);
					return grpcExchange.response().flatMap(GrpcResponse.Unary::value);
				});
		}
	}
}
