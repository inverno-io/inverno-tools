package test.bidirectionalstreaming;

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
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * <p>
 * This is a test service with a bidirectional streaming method.
 * </p>
 */

public final class BidirectionalStreamingGrpcClient {

	public static final GrpcServiceName SERVICE_NAME = GrpcServiceName.of("test", "BidirectionalStreaming");

	private BidirectionalStreamingGrpcClient() {}

    public static abstract class Http {

		private final HttpClient httpClient;
		private final GrpcClient grpcClient;

		public Http(HttpClient httpClient, GrpcClient grpcClient) {
			this.httpClient = httpClient;
			this.grpcClient = grpcClient;
		}

		public <A extends ExchangeContext> BidirectionalStreamingGrpcClient.HttpClientStub<A> createStub(String host, int port) {
			return new BidirectionalStreamingGrpcClient.Http.StubImpl<>(this.httpClient.<A>endpoint(host, port).build(), true);
		}

		public <A extends ExchangeContext> BidirectionalStreamingGrpcClient.HttpClientStub<A> createStub(InetSocketAddress remoteAddress) {
			return new BidirectionalStreamingGrpcClient.Http.StubImpl<>(this.httpClient.<A>endpoint(remoteAddress).build(), true);
		}

		public <A extends ExchangeContext> BidirectionalStreamingGrpcClient.HttpClientStub<A> createStub(Endpoint<A> endpoint) {
			return new BidirectionalStreamingGrpcClient.Http.StubImpl<>(endpoint, false);
		}

		private final class StubImpl<A extends ExchangeContext> implements BidirectionalStreamingGrpcClient.HttpClientStub<A> {

			private final Endpoint<A> endpoint;
			private final boolean shutdownEndpoint;
			private final Consumer<GrpcOutboundRequestMetadata> metadataConfigurer;

			public StubImpl(Endpoint<A> endpoint, boolean shutdownEndpoint) {
				this.endpoint = endpoint;
				this.shutdownEndpoint = shutdownEndpoint;
				this.metadataConfigurer = null;
			}

			private StubImpl(BidirectionalStreamingGrpcClient.Http.StubImpl<A> parent, Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
				this.endpoint = parent.endpoint;
				this.shutdownEndpoint = false;
				this.metadataConfigurer = metadataConfigurer;
			}

			@Override
			public BidirectionalStreamingGrpcClient.HttpClientStub<A> withMetadata(Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
				return new BidirectionalStreamingGrpcClient.Http.StubImpl<>(this, metadataConfigurer);
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
						GrpcExchange.BidirectionalStreaming<A, test.bidirectionalstreaming.BidirectionalStreamingRequest, test.bidirectionalstreaming.BidirectionalStreamingResponse> grpcExchange = BidirectionalStreamingGrpcClient.Http.this.grpcClient.bidirectionalStreaming(exchange, SERVICE_NAME, "callBidirectionalStreaming", test.bidirectionalstreaming.BidirectionalStreamingRequest.getDefaultInstance(), test.bidirectionalstreaming.BidirectionalStreamingResponse.getDefaultInstance());
						grpcExchange.request().metadata(this.metadataConfigurer);
						return grpcExchange;
					});
			}
		}
	}

	/**
	 * <p>
	 * This is a test service with a bidirectional streaming method.
	 * </p>
	 *
	 * @param <A> the exchange context type
	 */
	public interface HttpClientStub<A extends ExchangeContext> extends GrpcClient.CloseableStub<A, BidirectionalStreamingGrpcClient.HttpClientStub<A>> {
		
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

    public static abstract class Web<A extends ExchangeContext> implements BidirectionalStreamingGrpcClient.WebClientStub<A> {

		private final ServiceID serviceID;
		private final WebClient<? extends A> webClient;
		private final GrpcClient grpcClient;

		private final Web<A>.StubImpl stub;

		public Web(ServiceID serviceID, WebClient<? extends A> webClient, GrpcClient grpcClient) {
			this.serviceID = serviceID;
			this.webClient = webClient;
			this.grpcClient = grpcClient;

			this.stub = new BidirectionalStreamingGrpcClient.Web.StubImpl(null);
		}

		@Override
		public BidirectionalStreamingGrpcClient.WebClientStub<A> withMetadata(Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
			return new BidirectionalStreamingGrpcClient.Web<A>.StubImpl(metadataConfigurer);
		}
		
		@Override
		public Mono<GrpcExchange.BidirectionalStreaming<A, test.bidirectionalstreaming.BidirectionalStreamingRequest, test.bidirectionalstreaming.BidirectionalStreamingResponse>> callBidirectionalStreaming(Consumer<A> contextConfigurer) {
			return this.stub.callBidirectionalStreaming(contextConfigurer);
        }

		private final class StubImpl implements BidirectionalStreamingGrpcClient.WebClientStub<A> {

			private final Consumer<GrpcOutboundRequestMetadata> metadataConfigurer;

			public StubImpl(Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
				this.metadataConfigurer = metadataConfigurer;
			}

			@Override
			public BidirectionalStreamingGrpcClient.WebClientStub<A> withMetadata(Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
				return new BidirectionalStreamingGrpcClient.Web<A>.StubImpl(metadataConfigurer);
			}
			
			@Override
			public Mono<GrpcExchange.BidirectionalStreaming<A, test.bidirectionalstreaming.BidirectionalStreamingRequest, test.bidirectionalstreaming.BidirectionalStreamingResponse>> callBidirectionalStreaming(Consumer<A> contextConfigurer) {
				return Web.this.webClient.exchange(Web.this.serviceID.getURI())
						.map(exchange -> {
							if(contextConfigurer != null) {
								contextConfigurer.accept(exchange.context());
							}
						GrpcExchange.BidirectionalStreaming<A, test.bidirectionalstreaming.BidirectionalStreamingRequest, test.bidirectionalstreaming.BidirectionalStreamingResponse> grpcExchange = (GrpcExchange.BidirectionalStreaming<A, test.bidirectionalstreaming.BidirectionalStreamingRequest, test.bidirectionalstreaming.BidirectionalStreamingResponse>)Web.this.grpcClient.bidirectionalStreaming(exchange, SERVICE_NAME, "callBidirectionalStreaming", test.bidirectionalstreaming.BidirectionalStreamingRequest.getDefaultInstance(), test.bidirectionalstreaming.BidirectionalStreamingResponse.getDefaultInstance());
						grpcExchange.request().metadata(this.metadataConfigurer);
						return grpcExchange;
					});
			}
		}
	}

	/**
	 * <p>
	 * This is a test service with a bidirectional streaming method.
	 * </p>
	 *
	 * @param <A> the exchange context type
	 */
	public interface WebClientStub<A extends ExchangeContext> extends GrpcClient.Stub<A, BidirectionalStreamingGrpcClient.WebClientStub<A>> {
		
		/**
		 * <p>
		 * Calls bidirectional streaming method.
		 * </p>
		 *
		 * @return a mono emitting the bidirectional streaming exchange
		 */
		default Mono<GrpcExchange.BidirectionalStreaming<A, test.bidirectionalstreaming.BidirectionalStreamingRequest, test.bidirectionalstreaming.BidirectionalStreamingResponse>> callBidirectionalStreaming() {
			return this.callBidirectionalStreaming((Consumer<A>)null);
		}

		/**
		 * <p>
		 * Calls bidirectional streaming method.
		 * </p>
		 *
		 * @param contextConfigurer the context configurer
		 *
		 * @return a mono emitting the bidirectional streaming exchange
		 */
		Mono<GrpcExchange.BidirectionalStreaming<A, test.bidirectionalstreaming.BidirectionalStreamingRequest, test.bidirectionalstreaming.BidirectionalStreamingResponse>> callBidirectionalStreaming(Consumer<A> contextConfigurer);

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
			return this.callBidirectionalStreaming(request, (Consumer<A>)null);
		}

		/**
		 * <p>
		 * Calls bidirectional streaming method.
		 * </p>
		 *
		 * @param request           the client request publisher
		 * @param contextConfigurer the context configurer
		 *
		 * @return the server response publisher
		 */
		default Publisher<test.bidirectionalstreaming.BidirectionalStreamingResponse> callBidirectionalStreaming(Publisher<test.bidirectionalstreaming.BidirectionalStreamingRequest> request, Consumer<A> contextConfigurer) {
			return this.callBidirectionalStreaming(contextConfigurer)
				.flatMapMany(grpcExchange -> {
					grpcExchange.request().stream(request);
					return grpcExchange.response().flatMapMany(GrpcResponse.Streaming::stream);
				});
		}
	}
}
