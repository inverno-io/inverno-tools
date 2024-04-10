package test.clientstreaming;

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
 * This is a test service with a client streaming method.
 * </p>
 */
@Bean
public final class ClientStreamingGrpcClient {

	public static final GrpcServiceName SERVICE_NAME = GrpcServiceName.of("test", "ClientStreaming");
	
	private final HttpClient httpClient;
	private final GrpcClient grpcClient;
	
	public ClientStreamingGrpcClient(HttpClient httpClient, GrpcClient grpcClient) {
		this.httpClient = httpClient;
		this.grpcClient = grpcClient;
	}

	public <A extends ExchangeContext> ClientStreamingGrpcClient.Stub<A> createStub(String host, int port) {
		return new ClientStreamingGrpcClient.StubImpl<>(this.httpClient.<A>endpoint(host, port).build(), true);
	}
	
	public <A extends ExchangeContext> ClientStreamingGrpcClient.Stub<A> createStub(InetSocketAddress remoteAddress) {
		return new ClientStreamingGrpcClient.StubImpl<>(this.httpClient.<A>endpoint(remoteAddress).build(), true);
	}
	
	public <A extends ExchangeContext> ClientStreamingGrpcClient.Stub<A> createStub(Endpoint<A> endpoint) {
		return new ClientStreamingGrpcClient.StubImpl<>(endpoint, false);
	}
	
	private final class StubImpl<A extends ExchangeContext> implements ClientStreamingGrpcClient.Stub<A> {

		private final Endpoint<A> endpoint;
		private final boolean shutdownEndpoint;
		private final Consumer<GrpcOutboundRequestMetadata> metadataConfigurer;
		
		public StubImpl(Endpoint<A> endpoint, boolean shutdownEndpoint) {
			this.endpoint = endpoint;
			this.shutdownEndpoint = shutdownEndpoint;
			this.metadataConfigurer = null;
		}
		
		private StubImpl(ClientStreamingGrpcClient.StubImpl<A> parent, Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
			this.endpoint = parent.endpoint;
			this.shutdownEndpoint = false;
			this.metadataConfigurer = metadataConfigurer;
		}

		@Override
		public ClientStreamingGrpcClient.Stub<A> withMetadata(Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
			return new ClientStreamingGrpcClient.StubImpl<>(this, metadataConfigurer);
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
					GrpcExchange.ClientStreaming<A, test.clientstreaming.ClientStreamingRequest, test.clientstreaming.ClientStreamingResponse> grpcExchange = ClientStreamingGrpcClient.this.grpcClient.clientStreaming(exchange, SERVICE_NAME, "callClientStreaming", test.clientstreaming.ClientStreamingRequest.getDefaultInstance(), test.clientstreaming.ClientStreamingResponse.getDefaultInstance());
					grpcExchange.request().metadata(this.metadataConfigurer);
					return grpcExchange;
				});
		}
	}
	
	/**
	 * <p>
	 * This is a test service with a client streaming method.
	 * </p>
	 *
	 * @param <A> the exchange context type
	 */
	public interface Stub<A extends ExchangeContext> extends GrpcClient.Stub<A, ClientStreamingGrpcClient.Stub<A>> {
		
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
		 * @param <A>     the context type
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
		 * @param <A>     the context type
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
}
