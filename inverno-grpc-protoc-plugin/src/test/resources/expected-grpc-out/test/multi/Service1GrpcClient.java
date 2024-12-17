package test.multi;

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
import reactor.core.publisher.Mono;

/**
 * 
 */

public final class Service1GrpcClient {

	public static final GrpcServiceName SERVICE_NAME = GrpcServiceName.of("test", "Service1");

	private Service1GrpcClient() {}

    public static abstract class Http {

		private final HttpClient httpClient;
		private final GrpcClient grpcClient;

		public Http(HttpClient httpClient, GrpcClient grpcClient) {
			this.httpClient = httpClient;
			this.grpcClient = grpcClient;
		}

		public <A extends ExchangeContext> Service1GrpcClient.HttpClientStub<A> createStub(String host, int port) {
			return new Service1GrpcClient.Http.StubImpl<>(this.httpClient.<A>endpoint(host, port).build(), true);
		}

		public <A extends ExchangeContext> Service1GrpcClient.HttpClientStub<A> createStub(InetSocketAddress remoteAddress) {
			return new Service1GrpcClient.Http.StubImpl<>(this.httpClient.<A>endpoint(remoteAddress).build(), true);
		}

		public <A extends ExchangeContext> Service1GrpcClient.HttpClientStub<A> createStub(Endpoint<A> endpoint) {
			return new Service1GrpcClient.Http.StubImpl<>(endpoint, false);
		}

		private final class StubImpl<A extends ExchangeContext> implements Service1GrpcClient.HttpClientStub<A> {

			private final Endpoint<A> endpoint;
			private final boolean shutdownEndpoint;
			private final Consumer<GrpcOutboundRequestMetadata> metadataConfigurer;

			public StubImpl(Endpoint<A> endpoint, boolean shutdownEndpoint) {
				this.endpoint = endpoint;
				this.shutdownEndpoint = shutdownEndpoint;
				this.metadataConfigurer = null;
			}

			private StubImpl(Service1GrpcClient.Http.StubImpl<A> parent, Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
				this.endpoint = parent.endpoint;
				this.shutdownEndpoint = false;
				this.metadataConfigurer = metadataConfigurer;
			}

			@Override
			public Service1GrpcClient.HttpClientStub<A> withMetadata(Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
				return new Service1GrpcClient.Http.StubImpl<>(this, metadataConfigurer);
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
						GrpcExchange.Unary<A, test.multi.Svc1Request, test.multi.Svc1Response> grpcExchange = Service1GrpcClient.Http.this.grpcClient.unary(exchange, SERVICE_NAME, "call", test.multi.Svc1Request.getDefaultInstance(), test.multi.Svc1Response.getDefaultInstance());
						grpcExchange.request().metadata(this.metadataConfigurer);
						return grpcExchange;
					});
			}
		}
	}

	/**
	 * 
	 *
	 * @param <A> the exchange context type
	 */
	public interface HttpClientStub<A extends ExchangeContext> extends GrpcClient.CloseableStub<A, Service1GrpcClient.HttpClientStub<A>> {
		
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

    public static abstract class Web<A extends ExchangeContext> implements Service1GrpcClient.WebClientStub<A> {

		private final ServiceID serviceID;
		private final WebClient<? extends A> webClient;
		private final GrpcClient grpcClient;

		private final Web<A>.StubImpl stub;

		public Web(ServiceID serviceID, WebClient<? extends A> webClient, GrpcClient grpcClient) {
			this.serviceID = serviceID;
			this.webClient = webClient;
			this.grpcClient = grpcClient;

			this.stub = new Service1GrpcClient.Web.StubImpl(null);
		}

		@Override
		public Service1GrpcClient.WebClientStub<A> withMetadata(Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
			return new Service1GrpcClient.Web<A>.StubImpl(metadataConfigurer);
		}
		
		@Override
		public Mono<GrpcExchange.Unary<A, test.multi.Svc1Request, test.multi.Svc1Response>> call(Consumer<A> contextConfigurer) {
			return this.stub.call(contextConfigurer);
		}

		private final class StubImpl implements Service1GrpcClient.WebClientStub<A> {

			private final Consumer<GrpcOutboundRequestMetadata> metadataConfigurer;

			public StubImpl(Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
				this.metadataConfigurer = metadataConfigurer;
			}

			@Override
			public Service1GrpcClient.WebClientStub<A> withMetadata(Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
				return new Service1GrpcClient.Web<A>.StubImpl(metadataConfigurer);
			}
			
			@Override
			public Mono<GrpcExchange.Unary<A, test.multi.Svc1Request, test.multi.Svc1Response>> call(Consumer<A> contextConfigurer) {
				return Web.this.webClient.exchange(Web.this.serviceID.getURI())
					.map(exchange -> {
						if(contextConfigurer != null) {
							contextConfigurer.accept(exchange.context());
						}
						GrpcExchange.Unary<A, test.multi.Svc1Request, test.multi.Svc1Response> grpcExchange = (GrpcExchange.Unary<A, test.multi.Svc1Request, test.multi.Svc1Response>)Web.this.grpcClient.unary(exchange, SERVICE_NAME, "call", test.multi.Svc1Request.getDefaultInstance(), test.multi.Svc1Response.getDefaultInstance());
						grpcExchange.request().metadata(this.metadataConfigurer);
						return grpcExchange;
					});
			}
		}
	}

	/**
	 * 
	 *
	 * @param <A> the exchange context type
	 */
	public interface WebClientStub<A extends ExchangeContext> extends GrpcClient.Stub<A, Service1GrpcClient.WebClientStub<A>> {
		
		/**
		 * 
		 *
		 * @return a mono emitting the unary exchange
		 */
		default Mono<GrpcExchange.Unary<A, test.multi.Svc1Request, test.multi.Svc1Response>> call() {
			return this.call((Consumer<A>)null);
		}

		/**
		 * 
		 *
		 * @param contextConfigurer the context configurer
		 *
		 * @return a mono emitting the unary exchange
		 */
		Mono<GrpcExchange.Unary<A, test.multi.Svc1Request, test.multi.Svc1Response>> call(Consumer<A> contextConfigurer);

		/**
		 * 
		 *
		 * @param request the client request
		 *
		 * @return the server response
		 */
		default Mono<test.multi.Svc1Response> call(test.multi.Svc1Request request) {
			return this.call(request, (Consumer<A>)null);
		}

		/**
		 * 
		 *
		 * @param request           the client request
		 * @param contextConfigurer the context configurer
		 *
		 * @return the server response
		 */
		default Mono<test.multi.Svc1Response> call(test.multi.Svc1Request request, Consumer<A> contextConfigurer) {
			return this.call(contextConfigurer)
				.flatMap(grpcExchange -> {
					grpcExchange.request().value(request);
					return grpcExchange.response().flatMap(GrpcResponse.Unary::value);
				});
		}
	}
}
