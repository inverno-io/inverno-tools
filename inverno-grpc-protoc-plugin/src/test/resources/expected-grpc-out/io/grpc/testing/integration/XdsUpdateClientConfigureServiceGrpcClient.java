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
 * A service to dynamically update the configuration of an xDS test client.
 */
@Bean
public final class XdsUpdateClientConfigureServiceGrpcClient {

	public static final GrpcServiceName SERVICE_NAME = GrpcServiceName.of("grpc.testing", "XdsUpdateClientConfigureService");
	
	private final HttpClient httpClient;
	private final GrpcClient grpcClient;
	
	public XdsUpdateClientConfigureServiceGrpcClient(HttpClient httpClient, GrpcClient grpcClient) {
		this.httpClient = httpClient;
		this.grpcClient = grpcClient;
	}

	public <A extends ExchangeContext> XdsUpdateClientConfigureServiceGrpcClient.Stub<A> createStub(String host, int port) {
		return new XdsUpdateClientConfigureServiceGrpcClient.StubImpl<>(this.httpClient.<A>endpoint(host, port).build(), true);
	}
	
	public <A extends ExchangeContext> XdsUpdateClientConfigureServiceGrpcClient.Stub<A> createStub(InetSocketAddress remoteAddress) {
		return new XdsUpdateClientConfigureServiceGrpcClient.StubImpl<>(this.httpClient.<A>endpoint(remoteAddress).build(), true);
	}
	
	public <A extends ExchangeContext> XdsUpdateClientConfigureServiceGrpcClient.Stub<A> createStub(Endpoint<A> endpoint) {
		return new XdsUpdateClientConfigureServiceGrpcClient.StubImpl<>(endpoint, false);
	}
	
	private final class StubImpl<A extends ExchangeContext> implements XdsUpdateClientConfigureServiceGrpcClient.Stub<A> {

		private final Endpoint<A> endpoint;
		private final boolean shutdownEndpoint;
		private final Consumer<GrpcOutboundRequestMetadata> metadataConfigurer;
		
		public StubImpl(Endpoint<A> endpoint, boolean shutdownEndpoint) {
			this.endpoint = endpoint;
			this.shutdownEndpoint = shutdownEndpoint;
			this.metadataConfigurer = null;
		}
		
		private StubImpl(XdsUpdateClientConfigureServiceGrpcClient.StubImpl<A> parent, Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
			this.endpoint = parent.endpoint;
			this.shutdownEndpoint = false;
			this.metadataConfigurer = metadataConfigurer;
		}

		@Override
		public XdsUpdateClientConfigureServiceGrpcClient.Stub<A> withMetadata(Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
			return new XdsUpdateClientConfigureServiceGrpcClient.StubImpl<>(this, metadataConfigurer);
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
		public Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.ClientConfigureRequest, io.grpc.testing.integration.Messages.ClientConfigureResponse>> configure(A context) {
			return this.endpoint.exchange(context)
				.map(exchange -> {
					GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.ClientConfigureRequest, io.grpc.testing.integration.Messages.ClientConfigureResponse> grpcExchange = XdsUpdateClientConfigureServiceGrpcClient.this.grpcClient.unary(exchange, SERVICE_NAME, "Configure", io.grpc.testing.integration.Messages.ClientConfigureRequest.getDefaultInstance(), io.grpc.testing.integration.Messages.ClientConfigureResponse.getDefaultInstance());
					grpcExchange.request().metadata(this.metadataConfigurer);
					return grpcExchange;
				});
		}
	}
	
	/**
	 * A service to dynamically update the configuration of an xDS test client.
	 *
	 * @param <A> the exchange context type
	 */
	public interface Stub<A extends ExchangeContext> extends GrpcClient.Stub<A, XdsUpdateClientConfigureServiceGrpcClient.Stub<A>> {
		
		/**
		 * One empty request followed by one empty response.
		 * 
		 * @return a mono emitting the unary exchange
		 */
		default Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.ClientConfigureRequest, io.grpc.testing.integration.Messages.ClientConfigureResponse>> configure() {
			return this.configure((A)null);
		}

		/**
		 * One empty request followed by one empty response.
		 * 
		 * @param <A>     the context type
		 * @param context the context
		 * 
		 * @return a mono emitting the unary exchange
		 */
		Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.ClientConfigureRequest, io.grpc.testing.integration.Messages.ClientConfigureResponse>> configure(A context);

		/**
		 * One empty request followed by one empty response.
		 * 
		 * @param request the client request
		 * 
		 * @return the server response
		 */
		default Mono<io.grpc.testing.integration.Messages.ClientConfigureResponse> configure(io.grpc.testing.integration.Messages.ClientConfigureRequest request) {
			return this.configure(request, null);
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
		default Mono<io.grpc.testing.integration.Messages.ClientConfigureResponse> configure(io.grpc.testing.integration.Messages.ClientConfigureRequest request, A context) {
			return this.configure(context)
				.flatMap(grpcExchange -> {
					grpcExchange.request().value(request);
					return grpcExchange.response().flatMap(GrpcResponse.Unary::value);
				});
		}
	}
}
