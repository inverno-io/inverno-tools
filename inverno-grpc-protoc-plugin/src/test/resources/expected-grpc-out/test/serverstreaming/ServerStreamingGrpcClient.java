package test.serverstreaming;

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
 * This is a test service with a server streaming method.
 * </p>
 */

public final class ServerStreamingGrpcClient {

	public static final GrpcServiceName SERVICE_NAME = GrpcServiceName.of("test", "ServerStreaming");

	private ServerStreamingGrpcClient() {}

    public static abstract class Http {

		private final HttpClient httpClient;
		private final GrpcClient grpcClient;

		public Http(HttpClient httpClient, GrpcClient grpcClient) {
			this.httpClient = httpClient;
			this.grpcClient = grpcClient;
		}

		public <A extends ExchangeContext> ServerStreamingGrpcClient.HttpClientStub<A> createStub(String host, int port) {
			return new ServerStreamingGrpcClient.Http.StubImpl<>(this.httpClient.<A>endpoint(host, port).build(), true);
		}

		public <A extends ExchangeContext> ServerStreamingGrpcClient.HttpClientStub<A> createStub(InetSocketAddress remoteAddress) {
			return new ServerStreamingGrpcClient.Http.StubImpl<>(this.httpClient.<A>endpoint(remoteAddress).build(), true);
		}

		public <A extends ExchangeContext> ServerStreamingGrpcClient.HttpClientStub<A> createStub(Endpoint<A> endpoint) {
			return new ServerStreamingGrpcClient.Http.StubImpl<>(endpoint, false);
		}

		private final class StubImpl<A extends ExchangeContext> implements ServerStreamingGrpcClient.HttpClientStub<A> {

			private final Endpoint<A> endpoint;
			private final boolean shutdownEndpoint;
			private final Consumer<GrpcOutboundRequestMetadata> metadataConfigurer;

			public StubImpl(Endpoint<A> endpoint, boolean shutdownEndpoint) {
				this.endpoint = endpoint;
				this.shutdownEndpoint = shutdownEndpoint;
				this.metadataConfigurer = null;
			}

			private StubImpl(ServerStreamingGrpcClient.Http.StubImpl<A> parent, Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
				this.endpoint = parent.endpoint;
				this.shutdownEndpoint = false;
				this.metadataConfigurer = metadataConfigurer;
			}

			@Override
			public ServerStreamingGrpcClient.HttpClientStub<A> withMetadata(Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
				return new ServerStreamingGrpcClient.Http.StubImpl<>(this, metadataConfigurer);
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
			public Mono<GrpcExchange.ServerStreaming<A, test.serverstreaming.ServerStreamingRequest, test.serverstreaming.ServerStreamingResponse>> callServerStreaming(A context) {
				return this.endpoint.exchange(context)
					.map(exchange -> {
						GrpcExchange.ServerStreaming<A, test.serverstreaming.ServerStreamingRequest, test.serverstreaming.ServerStreamingResponse> grpcExchange = ServerStreamingGrpcClient.Http.this.grpcClient.serverStreaming(exchange, SERVICE_NAME, "callServerStreaming", test.serverstreaming.ServerStreamingRequest.getDefaultInstance(), test.serverstreaming.ServerStreamingResponse.getDefaultInstance());
						grpcExchange.request().metadata(this.metadataConfigurer);
						return grpcExchange;
					});
			}
		}
	}

	/**
	 * <p>
	 * This is a test service with a server streaming method.
	 * </p>
	 *
	 * @param <A> the exchange context type
	 */
	public interface HttpClientStub<A extends ExchangeContext> extends GrpcClient.CloseableStub<A, ServerStreamingGrpcClient.HttpClientStub<A>> {
		
		/**
		 * <p>
		 * Calls server streaming method.
		 * </p>
		 * 
		 * @return a mono emitting the server streaming exchange
		 */
		default Mono<GrpcExchange.ServerStreaming<A, test.serverstreaming.ServerStreamingRequest, test.serverstreaming.ServerStreamingResponse>> callServerStreaming() {
			return this.callServerStreaming((A)null);
		}

		/**
		 * <p>
		 * Calls server streaming method.
		 * </p>
		 * 
		 * @param context the context
		 * 
		 * @return a mono emitting the server streaming exchange
		 */
		Mono<GrpcExchange.ServerStreaming<A, test.serverstreaming.ServerStreamingRequest, test.serverstreaming.ServerStreamingResponse>> callServerStreaming(A context);

		/**
		 * <p>
		 * Calls server streaming method.
		 * </p>
		 * 
		 * @param request the client request
		 * 
		 * @return the server response publisher
		 */
		default Publisher<test.serverstreaming.ServerStreamingResponse> callServerStreaming(test.serverstreaming.ServerStreamingRequest request) {
			return this.callServerStreaming(request, null);
		}

		/**
		 * <p>
		 * Calls server streaming method.
		 * </p>
		 * 
		 * @param request the client request
		 * @param context the context
		 * 
		 * @return the server response publisher
		 */
		default Publisher<test.serverstreaming.ServerStreamingResponse> callServerStreaming(test.serverstreaming.ServerStreamingRequest request, A context) {
			return this.callServerStreaming(context)
				.flatMapMany(grpcExchange -> {
					grpcExchange.request().value(request);
					return grpcExchange.response().flatMapMany(GrpcResponse.Streaming::stream);
				});
		}
	}

    public static abstract class Web<A extends ExchangeContext> implements ServerStreamingGrpcClient.WebClientStub<A> {

		private final ServiceID serviceID;
		private final WebClient<? extends A> webClient;
		private final GrpcClient grpcClient;

		private final Web<A>.StubImpl stub;

		public Web(ServiceID serviceID, WebClient<? extends A> webClient, GrpcClient grpcClient) {
			this.serviceID = serviceID;
			this.webClient = webClient;
			this.grpcClient = grpcClient;

			this.stub = new ServerStreamingGrpcClient.Web.StubImpl(null);
		}

		@Override
		public ServerStreamingGrpcClient.WebClientStub<A> withMetadata(Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
			return new ServerStreamingGrpcClient.Web<A>.StubImpl(metadataConfigurer);
		}
		
		@Override
		public Mono<GrpcExchange.ServerStreaming<A, test.serverstreaming.ServerStreamingRequest, test.serverstreaming.ServerStreamingResponse>> callServerStreaming(Consumer<A> contextConfigurer) {
			return this.stub.callServerStreaming(contextConfigurer);
		}

		private final class StubImpl implements ServerStreamingGrpcClient.WebClientStub<A> {

			private final Consumer<GrpcOutboundRequestMetadata> metadataConfigurer;

			public StubImpl(Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
				this.metadataConfigurer = metadataConfigurer;
			}

			@Override
			public ServerStreamingGrpcClient.WebClientStub<A> withMetadata(Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
				return new ServerStreamingGrpcClient.Web<A>.StubImpl(metadataConfigurer);
			}
			
			@Override
			public Mono<GrpcExchange.ServerStreaming<A, test.serverstreaming.ServerStreamingRequest, test.serverstreaming.ServerStreamingResponse>> callServerStreaming(Consumer<A> contextConfigurer) {
				return Web.this.webClient.exchange(Web.this.serviceID.getURI())
					.map(exchange -> {
						if(contextConfigurer != null) {
							contextConfigurer.accept(exchange.context());
						}
						GrpcExchange.ServerStreaming<A, test.serverstreaming.ServerStreamingRequest, test.serverstreaming.ServerStreamingResponse> grpcExchange = (GrpcExchange.ServerStreaming<A, test.serverstreaming.ServerStreamingRequest, test.serverstreaming.ServerStreamingResponse>)Web.this.grpcClient.serverStreaming(exchange, SERVICE_NAME, "callServerStreaming", test.serverstreaming.ServerStreamingRequest.getDefaultInstance(), test.serverstreaming.ServerStreamingResponse.getDefaultInstance());
						grpcExchange.request().metadata(this.metadataConfigurer);
						return grpcExchange;
					});
			}
		}
	}

	/**
	 * <p>
	 * This is a test service with a server streaming method.
	 * </p>
	 *
	 * @param <A> the exchange context type
	 */
	public interface WebClientStub<A extends ExchangeContext> extends GrpcClient.Stub<A, ServerStreamingGrpcClient.WebClientStub<A>> {
		
		/**
		 * <p>
		 * Calls server streaming method.
		 * </p>
		 *
		 * @return a mono emitting the server streaming exchange
		 */
		default Mono<GrpcExchange.ServerStreaming<A, test.serverstreaming.ServerStreamingRequest, test.serverstreaming.ServerStreamingResponse>> callServerStreaming() {
			return this.callServerStreaming((Consumer<A>)null);
		}

		/**
		 * <p>
		 * Calls server streaming method.
		 * </p>
		 *
		 * @param contextConfigurer the context configurer
		 *
		 * @return a mono emitting the server streaming exchange
		 */
		Mono<GrpcExchange.ServerStreaming<A, test.serverstreaming.ServerStreamingRequest, test.serverstreaming.ServerStreamingResponse>> callServerStreaming(Consumer<A> contextConfigurer);

		/**
		 * <p>
		 * Calls server streaming method.
		 * </p>
		 *
		 * @param request the client request
		 *
		 * @return the server response publisher
		 */
		default Publisher<test.serverstreaming.ServerStreamingResponse> callServerStreaming(test.serverstreaming.ServerStreamingRequest request) {
			return this.callServerStreaming(request, (Consumer<A>)null);
		}

		/**
		 * <p>
		 * Calls server streaming method.
		 * </p>
		 *
		 * @param request the client request
		 * @param contextConfigurer the context configurer
		 *
		 * @return the server response publisher
		 */
		default Publisher<test.serverstreaming.ServerStreamingResponse> callServerStreaming(test.serverstreaming.ServerStreamingRequest request, Consumer<A> contextConfigurer) {
			return this.callServerStreaming(contextConfigurer)
				.flatMapMany(grpcExchange -> {
					grpcExchange.request().value(request);
					return grpcExchange.response().flatMapMany(GrpcResponse.Streaming::stream);
				});
		}
	}
}
