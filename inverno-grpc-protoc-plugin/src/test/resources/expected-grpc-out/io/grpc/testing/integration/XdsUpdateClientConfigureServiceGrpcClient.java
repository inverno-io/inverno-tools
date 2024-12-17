package io.grpc.testing.integration;

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
 * A service to dynamically update the configuration of an xDS test client.
 */

public final class XdsUpdateClientConfigureServiceGrpcClient {

	public static final GrpcServiceName SERVICE_NAME = GrpcServiceName.of("grpc.testing", "XdsUpdateClientConfigureService");

	private XdsUpdateClientConfigureServiceGrpcClient() {}

    public static abstract class Http {

		private final HttpClient httpClient;
		private final GrpcClient grpcClient;

		public Http(HttpClient httpClient, GrpcClient grpcClient) {
			this.httpClient = httpClient;
			this.grpcClient = grpcClient;
		}

		public <A extends ExchangeContext> XdsUpdateClientConfigureServiceGrpcClient.HttpClientStub<A> createStub(String host, int port) {
			return new XdsUpdateClientConfigureServiceGrpcClient.Http.StubImpl<>(this.httpClient.<A>endpoint(host, port).build(), true);
		}

		public <A extends ExchangeContext> XdsUpdateClientConfigureServiceGrpcClient.HttpClientStub<A> createStub(InetSocketAddress remoteAddress) {
			return new XdsUpdateClientConfigureServiceGrpcClient.Http.StubImpl<>(this.httpClient.<A>endpoint(remoteAddress).build(), true);
		}

		public <A extends ExchangeContext> XdsUpdateClientConfigureServiceGrpcClient.HttpClientStub<A> createStub(Endpoint<A> endpoint) {
			return new XdsUpdateClientConfigureServiceGrpcClient.Http.StubImpl<>(endpoint, false);
		}

		private final class StubImpl<A extends ExchangeContext> implements XdsUpdateClientConfigureServiceGrpcClient.HttpClientStub<A> {

			private final Endpoint<A> endpoint;
			private final boolean shutdownEndpoint;
			private final Consumer<GrpcOutboundRequestMetadata> metadataConfigurer;

			public StubImpl(Endpoint<A> endpoint, boolean shutdownEndpoint) {
				this.endpoint = endpoint;
				this.shutdownEndpoint = shutdownEndpoint;
				this.metadataConfigurer = null;
			}

			private StubImpl(XdsUpdateClientConfigureServiceGrpcClient.Http.StubImpl<A> parent, Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
				this.endpoint = parent.endpoint;
				this.shutdownEndpoint = false;
				this.metadataConfigurer = metadataConfigurer;
			}

			@Override
			public XdsUpdateClientConfigureServiceGrpcClient.HttpClientStub<A> withMetadata(Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
				return new XdsUpdateClientConfigureServiceGrpcClient.Http.StubImpl<>(this, metadataConfigurer);
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
			public Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.ClientConfigureRequest, io.grpc.testing.integration.Messages.ClientConfigureResponse>> configure(A context) {
				return this.endpoint.exchange(context)
					.map(exchange -> {
						GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.ClientConfigureRequest, io.grpc.testing.integration.Messages.ClientConfigureResponse> grpcExchange = XdsUpdateClientConfigureServiceGrpcClient.Http.this.grpcClient.unary(exchange, SERVICE_NAME, "Configure", io.grpc.testing.integration.Messages.ClientConfigureRequest.getDefaultInstance(), io.grpc.testing.integration.Messages.ClientConfigureResponse.getDefaultInstance());
						grpcExchange.request().metadata(this.metadataConfigurer);
						return grpcExchange;
					});
			}
		}
	}

	/**
	 * A service to dynamically update the configuration of an xDS test client.
	 *
	 * @param <A> the exchange context type
	 */
	public interface HttpClientStub<A extends ExchangeContext> extends GrpcClient.CloseableStub<A, XdsUpdateClientConfigureServiceGrpcClient.HttpClientStub<A>> {
		
		/**
		 * One empty request followed by one empty response.
		 * 
		 * @return a mono emitting the unary exchange
		 */
		default Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.ClientConfigureRequest, io.grpc.testing.integration.Messages.ClientConfigureResponse>> configure() {
			return this.configure((A)null);
		}

		/**
		 * One empty request followed by one empty response.
		 * 
		 * @param context the context
		 * 
		 * @return a mono emitting the unary exchange
		 */
		Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.ClientConfigureRequest, io.grpc.testing.integration.Messages.ClientConfigureResponse>> configure(A context);

		/**
		 * One empty request followed by one empty response.
		 * 
		 * @param request the client request
		 * 
		 * @return the server response
		 */
		default Mono<io.grpc.testing.integration.Messages.ClientConfigureResponse> configure(io.grpc.testing.integration.Messages.ClientConfigureRequest request) {
			return this.configure(request, null);
		}

		/**
		 * One empty request followed by one empty response.
		 * 
		 * @param request the client request
		 * @param context the context
		 * 
		 * @return the server response
		 */
		default Mono<io.grpc.testing.integration.Messages.ClientConfigureResponse> configure(io.grpc.testing.integration.Messages.ClientConfigureRequest request, A context) {
			return this.configure(context)
				.flatMap(grpcExchange -> {
					grpcExchange.request().value(request);
					return grpcExchange.response().flatMap(GrpcResponse.Unary::value);
				});
		}
	}

    public static abstract class Web<A extends ExchangeContext> implements XdsUpdateClientConfigureServiceGrpcClient.WebClientStub<A> {

		private final ServiceID serviceID;
		private final WebClient<? extends A> webClient;
		private final GrpcClient grpcClient;

		private final Web<A>.StubImpl stub;

		public Web(ServiceID serviceID, WebClient<? extends A> webClient, GrpcClient grpcClient) {
			this.serviceID = serviceID;
			this.webClient = webClient;
			this.grpcClient = grpcClient;

			this.stub = new XdsUpdateClientConfigureServiceGrpcClient.Web.StubImpl(null);
		}

		@Override
		public XdsUpdateClientConfigureServiceGrpcClient.WebClientStub<A> withMetadata(Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
			return new XdsUpdateClientConfigureServiceGrpcClient.Web<A>.StubImpl(metadataConfigurer);
		}
		
		@Override
		public Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.ClientConfigureRequest, io.grpc.testing.integration.Messages.ClientConfigureResponse>> configure(Consumer<A> contextConfigurer) {
			return this.stub.configure(contextConfigurer);
		}

		private final class StubImpl implements XdsUpdateClientConfigureServiceGrpcClient.WebClientStub<A> {

			private final Consumer<GrpcOutboundRequestMetadata> metadataConfigurer;

			public StubImpl(Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
				this.metadataConfigurer = metadataConfigurer;
			}

			@Override
			public XdsUpdateClientConfigureServiceGrpcClient.WebClientStub<A> withMetadata(Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
				return new XdsUpdateClientConfigureServiceGrpcClient.Web<A>.StubImpl(metadataConfigurer);
			}
			
			@Override
			public Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.ClientConfigureRequest, io.grpc.testing.integration.Messages.ClientConfigureResponse>> configure(Consumer<A> contextConfigurer) {
				return Web.this.webClient.exchange(Web.this.serviceID.getURI())
					.map(exchange -> {
						if(contextConfigurer != null) {
							contextConfigurer.accept(exchange.context());
						}
						GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.ClientConfigureRequest, io.grpc.testing.integration.Messages.ClientConfigureResponse> grpcExchange = (GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.ClientConfigureRequest, io.grpc.testing.integration.Messages.ClientConfigureResponse>)Web.this.grpcClient.unary(exchange, SERVICE_NAME, "Configure", io.grpc.testing.integration.Messages.ClientConfigureRequest.getDefaultInstance(), io.grpc.testing.integration.Messages.ClientConfigureResponse.getDefaultInstance());
						grpcExchange.request().metadata(this.metadataConfigurer);
						return grpcExchange;
					});
			}
		}
	}

	/**
	 * A service to dynamically update the configuration of an xDS test client.
	 *
	 * @param <A> the exchange context type
	 */
	public interface WebClientStub<A extends ExchangeContext> extends GrpcClient.Stub<A, XdsUpdateClientConfigureServiceGrpcClient.WebClientStub<A>> {
		
		/**
		 * One empty request followed by one empty response.
		 *
		 * @return a mono emitting the unary exchange
		 */
		default Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.ClientConfigureRequest, io.grpc.testing.integration.Messages.ClientConfigureResponse>> configure() {
			return this.configure((Consumer<A>)null);
		}

		/**
		 * One empty request followed by one empty response.
		 *
		 * @param contextConfigurer the context configurer
		 *
		 * @return a mono emitting the unary exchange
		 */
		Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.ClientConfigureRequest, io.grpc.testing.integration.Messages.ClientConfigureResponse>> configure(Consumer<A> contextConfigurer);

		/**
		 * One empty request followed by one empty response.
		 *
		 * @param request the client request
		 *
		 * @return the server response
		 */
		default Mono<io.grpc.testing.integration.Messages.ClientConfigureResponse> configure(io.grpc.testing.integration.Messages.ClientConfigureRequest request) {
			return this.configure(request, (Consumer<A>)null);
		}

		/**
		 * One empty request followed by one empty response.
		 *
		 * @param request           the client request
		 * @param contextConfigurer the context configurer
		 *
		 * @return the server response
		 */
		default Mono<io.grpc.testing.integration.Messages.ClientConfigureResponse> configure(io.grpc.testing.integration.Messages.ClientConfigureRequest request, Consumer<A> contextConfigurer) {
			return this.configure(contextConfigurer)
				.flatMap(grpcExchange -> {
					grpcExchange.request().value(request);
					return grpcExchange.response().flatMap(GrpcResponse.Unary::value);
				});
		}
	}
}
