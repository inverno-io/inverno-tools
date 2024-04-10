package test.bidirectionalstreaming;

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
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * <p>
 * This is a test service with a bidirectional streaming method.
 * </p>
 */
@Bean
public final class BidirectionalStreamingGrpcClient {

	public static final GrpcServiceName SERVICE_NAME = GrpcServiceName.of("test", "BidirectionalStreaming");
	
	private final HttpClient httpClient;
	private final GrpcClient grpcClient;
	
	public BidirectionalStreamingGrpcClient(HttpClient httpClient, GrpcClient grpcClient) {
		this.httpClient = httpClient;
		this.grpcClient = grpcClient;
	}

	public <A extends ExchangeContext> BidirectionalStreamingGrpcClient.Stub<A> createStub(String host, int port) {
		return new BidirectionalStreamingGrpcClient.StubImpl<>(this.httpClient.<A>endpoint(host, port).build(), true);
	}
	
	public <A extends ExchangeContext> BidirectionalStreamingGrpcClient.Stub<A> createStub(InetSocketAddress remoteAddress) {
		return new BidirectionalStreamingGrpcClient.StubImpl<>(this.httpClient.<A>endpoint(remoteAddress).build(), true);
	}
	
	public <A extends ExchangeContext> BidirectionalStreamingGrpcClient.Stub<A> createStub(Endpoint<A> endpoint) {
		return new BidirectionalStreamingGrpcClient.StubImpl<>(endpoint, false);
	}
	
	private final class StubImpl<A extends ExchangeContext> implements BidirectionalStreamingGrpcClient.Stub<A> {

		private final Endpoint<A> endpoint;
		private final boolean shutdownEndpoint;
		private final Consumer<GrpcOutboundRequestMetadata> metadataConfigurer;
		
		public StubImpl(Endpoint<A> endpoint, boolean shutdownEndpoint) {
			this.endpoint = endpoint;
			this.shutdownEndpoint = shutdownEndpoint;
			this.metadataConfigurer = null;
		}
		
		private StubImpl(BidirectionalStreamingGrpcClient.StubImpl<A> parent, Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
			this.endpoint = parent.endpoint;
			this.shutdownEndpoint = false;
			this.metadataConfigurer = metadataConfigurer;
		}

		@Override
		public BidirectionalStreamingGrpcClient.Stub<A> withMetadata(Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
			return new BidirectionalStreamingGrpcClient.StubImpl<>(this, metadataConfigurer);
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
		public Mono<GrpcExchange.BidirectionalStreaming<A, test.bidirectionalstreaming.BidirectionalStreamingRequest, test.bidirectionalstreaming.BidirectionalStreamingResponse>> callBidirectionalStreaming(A context) {
			return this.endpoint.exchange(context)
				.map(exchange -> {
					GrpcExchange.BidirectionalStreaming<A, test.bidirectionalstreaming.BidirectionalStreamingRequest, test.bidirectionalstreaming.BidirectionalStreamingResponse> grpcExchange = BidirectionalStreamingGrpcClient.this.grpcClient.bidirectionalStreaming(exchange, SERVICE_NAME, "callBidirectionalStreaming", test.bidirectionalstreaming.BidirectionalStreamingRequest.getDefaultInstance(), test.bidirectionalstreaming.BidirectionalStreamingResponse.getDefaultInstance());
					grpcExchange.request().metadata(this.metadataConfigurer);
					return grpcExchange;
				});
		}
	}
	
	/**
	 * <p>
	 * This is a test service with a bidirectional streaming method.
	 * </p>
	 *
	 * @param <A> the exchange context type
	 */
	public interface Stub<A extends ExchangeContext> extends GrpcClient.Stub<A, BidirectionalStreamingGrpcClient.Stub<A>> {
		
		/**
		 * <p>
		 * Calls bidirectional streaming method.
		 * </p>
		 * 
		 * @return a mono emitting the bidirectional streaming exchange
		 */
		default Mono<GrpcExchange.BidirectionalStreaming<A, test.bidirectionalstreaming.BidirectionalStreamingRequest, test.bidirectionalstreaming.BidirectionalStreamingResponse>> callBidirectionalStreaming() {
			return this.callBidirectionalStreaming((A)null);
		}

		/**
		 * <p>
		 * Calls bidirectional streaming method.
		 * </p>
		 * 
		 * @param <A>     the context type
		 * @param context the context
		 * 
		 * @return a mono emitting the bidirectional streaming exchange
		 */
		Mono<GrpcExchange.BidirectionalStreaming<A, test.bidirectionalstreaming.BidirectionalStreamingRequest, test.bidirectionalstreaming.BidirectionalStreamingResponse>> callBidirectionalStreaming(A context);

		/**
		 * <p>
		 * Calls bidirectional streaming method.
		 * </p>
		 * 
		 * @param request the client request publisher
		 * 
		 * @return the server response publisher
		 */
		default Publisher<test.bidirectionalstreaming.BidirectionalStreamingResponse> callBidirectionalStreaming(Publisher<test.bidirectionalstreaming.BidirectionalStreamingRequest> request) {
			return this.callBidirectionalStreaming(request, null);
		}

		/**
		 * <p>
		 * Calls bidirectional streaming method.
		 * </p>
		 * 
		 * @param <A>     the context type
		 * @param request the client request publisher
		 * @param context the context
		 * 
		 * @return the server response publisher
		 */
		default Publisher<test.bidirectionalstreaming.BidirectionalStreamingResponse> callBidirectionalStreaming(Publisher<test.bidirectionalstreaming.BidirectionalStreamingRequest> request, A context) {
			return this.callBidirectionalStreaming(context)
				.flatMapMany(grpcExchange -> {
					grpcExchange.request().stream(request);
					return grpcExchange.response().flatMapMany(GrpcResponse.Streaming::stream);
				});
		}
	}
}
