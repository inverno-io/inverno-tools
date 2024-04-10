package test.unary;

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
 * <p>
 * This is a test service with a unary method.
 * </p>
 */
@Bean
public final class UnaryGrpcClient {

	public static final GrpcServiceName SERVICE_NAME = GrpcServiceName.of("test", "Unary");
	
	private final HttpClient httpClient;
	private final GrpcClient grpcClient;
	
	public UnaryGrpcClient(HttpClient httpClient, GrpcClient grpcClient) {
		this.httpClient = httpClient;
		this.grpcClient = grpcClient;
	}

	public <A extends ExchangeContext> UnaryGrpcClient.Stub<A> createStub(String host, int port) {
		return new UnaryGrpcClient.StubImpl<>(this.httpClient.<A>endpoint(host, port).build(), true);
	}
	
	public <A extends ExchangeContext> UnaryGrpcClient.Stub<A> createStub(InetSocketAddress remoteAddress) {
		return new UnaryGrpcClient.StubImpl<>(this.httpClient.<A>endpoint(remoteAddress).build(), true);
	}
	
	public <A extends ExchangeContext> UnaryGrpcClient.Stub<A> createStub(Endpoint<A> endpoint) {
		return new UnaryGrpcClient.StubImpl<>(endpoint, false);
	}
	
	private final class StubImpl<A extends ExchangeContext> implements UnaryGrpcClient.Stub<A> {

		private final Endpoint<A> endpoint;
		private final boolean shutdownEndpoint;
		private final Consumer<GrpcOutboundRequestMetadata> metadataConfigurer;
		
		public StubImpl(Endpoint<A> endpoint, boolean shutdownEndpoint) {
			this.endpoint = endpoint;
			this.shutdownEndpoint = shutdownEndpoint;
			this.metadataConfigurer = null;
		}
		
		private StubImpl(UnaryGrpcClient.StubImpl<A> parent, Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
			this.endpoint = parent.endpoint;
			this.shutdownEndpoint = false;
			this.metadataConfigurer = metadataConfigurer;
		}

		@Override
		public UnaryGrpcClient.Stub<A> withMetadata(Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
			return new UnaryGrpcClient.StubImpl<>(this, metadataConfigurer);
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
					GrpcExchange.Unary<A, test.unary.UnaryRequest, test.unary.UnaryResponse> grpcExchange = UnaryGrpcClient.this.grpcClient.unary(exchange, SERVICE_NAME, "callUnary", test.unary.UnaryRequest.getDefaultInstance(), test.unary.UnaryResponse.getDefaultInstance());
					grpcExchange.request().metadata(this.metadataConfigurer);
					return grpcExchange;
				});
		}
	}
	
	/**
	 * <p>
	 * This is a test service with a unary method.
	 * </p>
	 *
	 * @param <A> the exchange context type
	 */
	public interface Stub<A extends ExchangeContext> extends GrpcClient.Stub<A, UnaryGrpcClient.Stub<A>> {
		
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
		 * @param <A>     the context type
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
		 * @param <A>     the context type
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
}
