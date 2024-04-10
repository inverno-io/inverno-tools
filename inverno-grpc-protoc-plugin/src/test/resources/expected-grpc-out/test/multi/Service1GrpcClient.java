package test.multi;

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
public final class Service1GrpcClient {

	public static final GrpcServiceName SERVICE_NAME = GrpcServiceName.of("test", "Service1");
	
	private final HttpClient httpClient;
	private final GrpcClient grpcClient;
	
	public Service1GrpcClient(HttpClient httpClient, GrpcClient grpcClient) {
		this.httpClient = httpClient;
		this.grpcClient = grpcClient;
	}

	public <A extends ExchangeContext> Service1GrpcClient.Stub<A> createStub(String host, int port) {
		return new Service1GrpcClient.StubImpl<>(this.httpClient.<A>endpoint(host, port).build(), true);
	}
	
	public <A extends ExchangeContext> Service1GrpcClient.Stub<A> createStub(InetSocketAddress remoteAddress) {
		return new Service1GrpcClient.StubImpl<>(this.httpClient.<A>endpoint(remoteAddress).build(), true);
	}
	
	public <A extends ExchangeContext> Service1GrpcClient.Stub<A> createStub(Endpoint<A> endpoint) {
		return new Service1GrpcClient.StubImpl<>(endpoint, false);
	}
	
	private final class StubImpl<A extends ExchangeContext> implements Service1GrpcClient.Stub<A> {

		private final Endpoint<A> endpoint;
		private final boolean shutdownEndpoint;
		private final Consumer<GrpcOutboundRequestMetadata> metadataConfigurer;
		
		public StubImpl(Endpoint<A> endpoint, boolean shutdownEndpoint) {
			this.endpoint = endpoint;
			this.shutdownEndpoint = shutdownEndpoint;
			this.metadataConfigurer = null;
		}
		
		private StubImpl(Service1GrpcClient.StubImpl<A> parent, Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
			this.endpoint = parent.endpoint;
			this.shutdownEndpoint = false;
			this.metadataConfigurer = metadataConfigurer;
		}

		@Override
		public Service1GrpcClient.Stub<A> withMetadata(Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
			return new Service1GrpcClient.StubImpl<>(this, metadataConfigurer);
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
		public Mono<GrpcExchange.Unary<A, test.multi.Svc1Request, test.multi.Svc1Response>> call(A context) {
			return this.endpoint.exchange(context)
				.map(exchange -> {
					GrpcExchange.Unary<A, test.multi.Svc1Request, test.multi.Svc1Response> grpcExchange = Service1GrpcClient.this.grpcClient.unary(exchange, SERVICE_NAME, "call", test.multi.Svc1Request.getDefaultInstance(), test.multi.Svc1Response.getDefaultInstance());
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
	public interface Stub<A extends ExchangeContext> extends GrpcClient.Stub<A, Service1GrpcClient.Stub<A>> {
		
		/**
		 * 
		 * 
		 * @return a mono emitting the unary exchange
		 */
		default Mono<GrpcExchange.Unary<A, test.multi.Svc1Request, test.multi.Svc1Response>> call() {
			return this.call((A)null);
		}

		/**
		 * 
		 * 
		 * @param <A>     the context type
		 * @param context the context
		 * 
		 * @return a mono emitting the unary exchange
		 */
		Mono<GrpcExchange.Unary<A, test.multi.Svc1Request, test.multi.Svc1Response>> call(A context);

		/**
		 * 
		 * 
		 * @param request the client request
		 * 
		 * @return the server response
		 */
		default Mono<test.multi.Svc1Response> call(test.multi.Svc1Request request) {
			return this.call(request, null);
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
		default Mono<test.multi.Svc1Response> call(test.multi.Svc1Request request, A context) {
			return this.call(context)
				.flatMap(grpcExchange -> {
					grpcExchange.request().value(request);
					return grpcExchange.response().flatMap(GrpcResponse.Unary::value);
				});
		}
	}
}
