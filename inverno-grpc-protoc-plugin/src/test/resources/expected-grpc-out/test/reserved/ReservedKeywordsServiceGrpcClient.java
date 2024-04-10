package test.reserved;

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
 * 
 */
@Bean
public final class ReservedKeywordsServiceGrpcClient {

	public static final GrpcServiceName SERVICE_NAME = GrpcServiceName.of("test", "ReservedKeywordsService");
	
	private final HttpClient httpClient;
	private final GrpcClient grpcClient;
	
	public ReservedKeywordsServiceGrpcClient(HttpClient httpClient, GrpcClient grpcClient) {
		this.httpClient = httpClient;
		this.grpcClient = grpcClient;
	}

	public <A extends ExchangeContext> ReservedKeywordsServiceGrpcClient.Stub<A> createStub(String host, int port) {
		return new ReservedKeywordsServiceGrpcClient.StubImpl<>(this.httpClient.<A>endpoint(host, port).build(), true);
	}
	
	public <A extends ExchangeContext> ReservedKeywordsServiceGrpcClient.Stub<A> createStub(InetSocketAddress remoteAddress) {
		return new ReservedKeywordsServiceGrpcClient.StubImpl<>(this.httpClient.<A>endpoint(remoteAddress).build(), true);
	}
	
	public <A extends ExchangeContext> ReservedKeywordsServiceGrpcClient.Stub<A> createStub(Endpoint<A> endpoint) {
		return new ReservedKeywordsServiceGrpcClient.StubImpl<>(endpoint, false);
	}
	
	private final class StubImpl<A extends ExchangeContext> implements ReservedKeywordsServiceGrpcClient.Stub<A> {

		private final Endpoint<A> endpoint;
		private final boolean shutdownEndpoint;
		private final Consumer<GrpcOutboundRequestMetadata> metadataConfigurer;
		
		public StubImpl(Endpoint<A> endpoint, boolean shutdownEndpoint) {
			this.endpoint = endpoint;
			this.shutdownEndpoint = shutdownEndpoint;
			this.metadataConfigurer = null;
		}
		
		private StubImpl(ReservedKeywordsServiceGrpcClient.StubImpl<A> parent, Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
			this.endpoint = parent.endpoint;
			this.shutdownEndpoint = false;
			this.metadataConfigurer = metadataConfigurer;
		}

		@Override
		public ReservedKeywordsServiceGrpcClient.Stub<A> withMetadata(Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
			return new ReservedKeywordsServiceGrpcClient.StubImpl<>(this, metadataConfigurer);
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
		public Mono<GrpcExchange.Unary<A, test.reserved.ReservedKeywordsRequest, test.reserved.ReservedKeywordsResponse>> new_(A context) {
			return this.endpoint.exchange(context)
				.map(exchange -> {
					GrpcExchange.Unary<A, test.reserved.ReservedKeywordsRequest, test.reserved.ReservedKeywordsResponse> grpcExchange = ReservedKeywordsServiceGrpcClient.this.grpcClient.unary(exchange, SERVICE_NAME, "new", test.reserved.ReservedKeywordsRequest.getDefaultInstance(), test.reserved.ReservedKeywordsResponse.getDefaultInstance());
					grpcExchange.request().metadata(this.metadataConfigurer);
					return grpcExchange;
				});
		}
	}
	
	/**
	 * 
	 *
	 * @param <A> the exchange context type
	 */
	public interface Stub<A extends ExchangeContext> extends GrpcClient.Stub<A, ReservedKeywordsServiceGrpcClient.Stub<A>> {
		
		/**
		 * 
		 * 
		 * @return a mono emitting the unary exchange
		 */
		default Mono<GrpcExchange.Unary<A, test.reserved.ReservedKeywordsRequest, test.reserved.ReservedKeywordsResponse>> new_() {
			return this.new_((A)null);
		}

		/**
		 * 
		 * 
		 * @param <A>     the context type
		 * @param context the context
		 * 
		 * @return a mono emitting the unary exchange
		 */
		Mono<GrpcExchange.Unary<A, test.reserved.ReservedKeywordsRequest, test.reserved.ReservedKeywordsResponse>> new_(A context);

		/**
		 * 
		 * 
		 * @param request the client request
		 * 
		 * @return the server response
		 */
		default Mono<test.reserved.ReservedKeywordsResponse> new_(test.reserved.ReservedKeywordsRequest request) {
			return this.new_(request, null);
		}

		/**
		 * 
		 * 
		 * @param <A>     the context type
		 * @param request the client request
		 * @param context the context
		 * 
		 * @return the server response
		 */
		default Mono<test.reserved.ReservedKeywordsResponse> new_(test.reserved.ReservedKeywordsRequest request, A context) {
			return this.new_(context)
				.flatMap(grpcExchange -> {
					grpcExchange.request().value(request);
					return grpcExchange.response().flatMap(GrpcResponse.Unary::value);
				});
		}
	}
}
