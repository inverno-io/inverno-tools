package test.clientstreaming;

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
 * This is a test service with a client streaming method.
 * </p>
 */

public final class ClientStreamingGrpcClient {

	public static final GrpcServiceName SERVICE_NAME = GrpcServiceName.of("test", "ClientStreaming");

	private ClientStreamingGrpcClient() {}

    public static abstract class Http {

		private final HttpClient httpClient;
		private final GrpcClient grpcClient;

		public Http(HttpClient httpClient, GrpcClient grpcClient) {
			this.httpClient = httpClient;
			this.grpcClient = grpcClient;
		}

		public <A extends ExchangeContext> ClientStreamingGrpcClient.HttpClientStub<A> createStub(String host, int port) {
			return new ClientStreamingGrpcClient.Http.StubImpl<>(this.httpClient.<A>endpoint(host, port).build(), true);
		}

		public <A extends ExchangeContext> ClientStreamingGrpcClient.HttpClientStub<A> createStub(InetSocketAddress remoteAddress) {
			return new ClientStreamingGrpcClient.Http.StubImpl<>(this.httpClient.<A>endpoint(remoteAddress).build(), true);
		}

		public <A extends ExchangeContext> ClientStreamingGrpcClient.HttpClientStub<A> createStub(Endpoint<A> endpoint) {
			return new ClientStreamingGrpcClient.Http.StubImpl<>(endpoint, false);
		}

		private final class StubImpl<A extends ExchangeContext> implements ClientStreamingGrpcClient.HttpClientStub<A> {

			private final Endpoint<A> endpoint;
			private final boolean shutdownEndpoint;
			private final Consumer<GrpcOutboundRequestMetadata> metadataConfigurer;

			public StubImpl(Endpoint<A> endpoint, boolean shutdownEndpoint) {
				this.endpoint = endpoint;
				this.shutdownEndpoint = shutdownEndpoint;
				this.metadataConfigurer = null;
			}

			private StubImpl(ClientStreamingGrpcClient.Http.StubImpl<A> parent, Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
				this.endpoint = parent.endpoint;
				this.shutdownEndpoint = false;
				this.metadataConfigurer = metadataConfigurer;
			}

			@Override
			public ClientStreamingGrpcClient.HttpClientStub<A> withMetadata(Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
				return new ClientStreamingGrpcClient.Http.StubImpl<>(this, metadataConfigurer);
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
			public Mono<GrpcExchange.ClientStreaming<A, test.clientstreaming.ClientStreamingRequest, test.clientstreaming.ClientStreamingResponse>> callClientStreaming(A context) {
				return this.endpoint.exchange(context)
					.map(exchange -> {
						GrpcExchange.ClientStreaming<A, test.clientstreaming.ClientStreamingRequest, test.clientstreaming.ClientStreamingResponse> grpcExchange = ClientStreamingGrpcClient.Http.this.grpcClient.clientStreaming(exchange, SERVICE_NAME, "callClientStreaming", test.clientstreaming.ClientStreamingRequest.getDefaultInstance(), test.clientstreaming.ClientStreamingResponse.getDefaultInstance());
						grpcExchange.request().metadata(this.metadataConfigurer);
						return grpcExchange;
					});
			}
		}
	}

	/**
	 * <p>
	 * This is a test service with a client streaming method.
	 * </p>
	 *
	 * @param <A> the exchange context type
	 */
	public interface HttpClientStub<A extends ExchangeContext> extends GrpcClient.CloseableStub<A, ClientStreamingGrpcClient.HttpClientStub<A>> {
		
		/**
		 * <p>
		 * Calls client streaming method.
		 * </p>
		 * 
		 * @return a mono emitting the client streaming exchange
		 */
		default Mono<GrpcExchange.ClientStreaming<A, test.clientstreaming.ClientStreamingRequest, test.clientstreaming.ClientStreamingResponse>> callClientStreaming() {
			return this.callClientStreaming((A)null);
		}

		/**
		 * <p>
		 * Calls client streaming method.
		 * </p>
		 * 
		 * @param context the context
		 * 
		 * @return a mono emitting the client streaming exchange
		 */
		Mono<GrpcExchange.ClientStreaming<A, test.clientstreaming.ClientStreamingRequest, test.clientstreaming.ClientStreamingResponse>> callClientStreaming(A context);

		/**
		 * <p>
		 * Calls client streaming method.
		 * </p>
		 * 
		 * @param request the client request publisher
		 * 
		 * @return the server response
		 */
		default Mono<test.clientstreaming.ClientStreamingResponse> callClientStreaming(Publisher<test.clientstreaming.ClientStreamingRequest> request) {
			return this.callClientStreaming(request, null);
		}

		/**
		 * <p>
		 * Calls client streaming method.
		 * </p>
		 * 
		 * @param request the client request publisher
		 * @param context the context
		 * 
		 * @return the server response
		 */
		default Mono<test.clientstreaming.ClientStreamingResponse> callClientStreaming(Publisher<test.clientstreaming.ClientStreamingRequest> request, A context) {
			return this.callClientStreaming(context)
				.flatMap(grpcExchange -> {
					grpcExchange.request().stream(request);
					return grpcExchange.response().flatMap(GrpcResponse.Unary::value);
				});
		}
	}

    public static abstract class Web<A extends ExchangeContext> implements ClientStreamingGrpcClient.WebClientStub<A> {

		private final ServiceID serviceID;
		private final WebClient<? extends A> webClient;
		private final GrpcClient grpcClient;

		private final Web<A>.StubImpl stub;

		public Web(ServiceID serviceID, WebClient<? extends A> webClient, GrpcClient grpcClient) {
			this.serviceID = serviceID;
			this.webClient = webClient;
			this.grpcClient = grpcClient;

			this.stub = new ClientStreamingGrpcClient.Web.StubImpl(null);
		}

		@Override
		public ClientStreamingGrpcClient.WebClientStub<A> withMetadata(Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
			return new ClientStreamingGrpcClient.Web<A>.StubImpl(metadataConfigurer);
		}
		
		@Override
		public Mono<GrpcExchange.ClientStreaming<A, test.clientstreaming.ClientStreamingRequest, test.clientstreaming.ClientStreamingResponse>> callClientStreaming(Consumer<A> contextConfigurer) {
			return this.stub.callClientStreaming(contextConfigurer);
        }

		private final class StubImpl implements ClientStreamingGrpcClient.WebClientStub<A> {

			private final Consumer<GrpcOutboundRequestMetadata> metadataConfigurer;

			public StubImpl(Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
				this.metadataConfigurer = metadataConfigurer;
			}

			@Override
			public ClientStreamingGrpcClient.WebClientStub<A> withMetadata(Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
				return new ClientStreamingGrpcClient.Web<A>.StubImpl(metadataConfigurer);
			}
			
			@Override
			public Mono<GrpcExchange.ClientStreaming<A, test.clientstreaming.ClientStreamingRequest, test.clientstreaming.ClientStreamingResponse>> callClientStreaming(Consumer<A> contextConfigurer) {
				return Web.this.webClient.exchange(Web.this.serviceID.getURI())
					.map(exchange -> {
						if(contextConfigurer != null) {
							contextConfigurer.accept(exchange.context());
						}
						GrpcExchange.ClientStreaming<A, test.clientstreaming.ClientStreamingRequest, test.clientstreaming.ClientStreamingResponse> grpcExchange = (GrpcExchange.ClientStreaming<A, test.clientstreaming.ClientStreamingRequest, test.clientstreaming.ClientStreamingResponse>)Web.this.grpcClient.clientStreaming(exchange, SERVICE_NAME, "callClientStreaming", test.clientstreaming.ClientStreamingRequest.getDefaultInstance(), test.clientstreaming.ClientStreamingResponse.getDefaultInstance());
						grpcExchange.request().metadata(this.metadataConfigurer);
						return grpcExchange;
					});
			}
		}
	}

	/**
	 * <p>
	 * This is a test service with a client streaming method.
	 * </p>
	 *
	 * @param <A> the exchange context type
	 */
	public interface WebClientStub<A extends ExchangeContext> extends GrpcClient.Stub<A, ClientStreamingGrpcClient.WebClientStub<A>> {
		
		/**
		 * <p>
		 * Calls client streaming method.
		 * </p>
		 *
		 * @return a mono emitting the client streaming exchange
		 */
		default Mono<GrpcExchange.ClientStreaming<A, test.clientstreaming.ClientStreamingRequest, test.clientstreaming.ClientStreamingResponse>> callClientStreaming() {
			return this.callClientStreaming((Consumer<A>)null);
		}

		/**
		 * <p>
		 * Calls client streaming method.
		 * </p>
		 *
		 * @param contextConfigurer the context configurer
		 *
		 * @return a mono emitting the client streaming exchange
		 */
		Mono<GrpcExchange.ClientStreaming<A, test.clientstreaming.ClientStreamingRequest, test.clientstreaming.ClientStreamingResponse>> callClientStreaming(Consumer<A> contextConfigurer);

		/**
		 * <p>
		 * Calls client streaming method.
		 * </p>
		 *
		 * @param request the client request publisher
		 *
		 * @return the server response
		 */
		default Mono<test.clientstreaming.ClientStreamingResponse> callClientStreaming(Publisher<test.clientstreaming.ClientStreamingRequest> request) {
			return this.callClientStreaming(request, (Consumer<A>)null);
		}

		/**
		 * <p>
		 * Calls client streaming method.
		 * </p>
		 *
		 * @param request the client request publisher
		 * @param contextConfigurer the context configurer
		 *
		 * @return the server response
		 */
		default Mono<test.clientstreaming.ClientStreamingResponse> callClientStreaming(Publisher<test.clientstreaming.ClientStreamingRequest> request, Consumer<A> contextConfigurer) {
			return this.callClientStreaming(contextConfigurer)
				.flatMap(grpcExchange -> {
					grpcExchange.request().stream(request);
					return grpcExchange.response().flatMap(GrpcResponse.Unary::value);
				});
		}
	}
}
