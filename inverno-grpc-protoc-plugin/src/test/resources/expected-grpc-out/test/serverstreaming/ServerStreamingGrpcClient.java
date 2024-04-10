package test.serverstreaming;

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
 * This is a test service with a server streaming method.
 * </p>
 */
@Bean
public final class ServerStreamingGrpcClient {

	public static final GrpcServiceName SERVICE_NAME = GrpcServiceName.of("test", "ServerStreaming");
	
	private final HttpClient httpClient;
	private final GrpcClient grpcClient;
	
	public ServerStreamingGrpcClient(HttpClient httpClient, GrpcClient grpcClient) {
		this.httpClient = httpClient;
		this.grpcClient = grpcClient;
	}

	public <A extends ExchangeContext> ServerStreamingGrpcClient.Stub<A> createStub(String host, int port) {
		return new ServerStreamingGrpcClient.StubImpl<>(this.httpClient.<A>endpoint(host, port).build(), true);
	}
	
	public <A extends ExchangeContext> ServerStreamingGrpcClient.Stub<A> createStub(InetSocketAddress remoteAddress) {
		return new ServerStreamingGrpcClient.StubImpl<>(this.httpClient.<A>endpoint(remoteAddress).build(), true);
	}
	
	public <A extends ExchangeContext> ServerStreamingGrpcClient.Stub<A> createStub(Endpoint<A> endpoint) {
		return new ServerStreamingGrpcClient.StubImpl<>(endpoint, false);
	}
	
	private final class StubImpl<A extends ExchangeContext> implements ServerStreamingGrpcClient.Stub<A> {

		private final Endpoint<A> endpoint;
		private final boolean shutdownEndpoint;
		private final Consumer<GrpcOutboundRequestMetadata> metadataConfigurer;
		
		public StubImpl(Endpoint<A> endpoint, boolean shutdownEndpoint) {
			this.endpoint = endpoint;
			this.shutdownEndpoint = shutdownEndpoint;
			this.metadataConfigurer = null;
		}
		
		private StubImpl(ServerStreamingGrpcClient.StubImpl<A> parent, Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
			this.endpoint = parent.endpoint;
			this.shutdownEndpoint = false;
			this.metadataConfigurer = metadataConfigurer;
		}

		@Override
		public ServerStreamingGrpcClient.Stub<A> withMetadata(Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
			return new ServerStreamingGrpcClient.StubImpl<>(this, metadataConfigurer);
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
					GrpcExchange.ServerStreaming<A, test.serverstreaming.ServerStreamingRequest, test.serverstreaming.ServerStreamingResponse> grpcExchange = ServerStreamingGrpcClient.this.grpcClient.serverStreaming(exchange, SERVICE_NAME, "callServerStreaming", test.serverstreaming.ServerStreamingRequest.getDefaultInstance(), test.serverstreaming.ServerStreamingResponse.getDefaultInstance());
					grpcExchange.request().metadata(this.metadataConfigurer);
					return grpcExchange;
				});
		}
	}
	
	/**
	 * <p>
	 * This is a test service with a server streaming method.
	 * </p>
	 *
	 * @param <A> the exchange context type
	 */
	public interface Stub<A extends ExchangeContext> extends GrpcClient.Stub<A, ServerStreamingGrpcClient.Stub<A>> {
		
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
		 * @param <A>     the context type
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
		 * @param <A>     the context type
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
}
