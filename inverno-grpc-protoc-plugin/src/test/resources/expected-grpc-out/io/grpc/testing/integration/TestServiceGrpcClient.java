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
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * A simple service to test the various types of RPCs and experiment with
 * performance with various types of payload.
 */

public final class TestServiceGrpcClient {

	public static final GrpcServiceName SERVICE_NAME = GrpcServiceName.of("grpc.testing", "TestService");

	private TestServiceGrpcClient() {}

    public static abstract class Http {

		private final HttpClient httpClient;
		private final GrpcClient grpcClient;

		public Http(HttpClient httpClient, GrpcClient grpcClient) {
			this.httpClient = httpClient;
			this.grpcClient = grpcClient;
		}

		public <A extends ExchangeContext> TestServiceGrpcClient.HttpClientStub<A> createStub(String host, int port) {
			return new TestServiceGrpcClient.Http.StubImpl<>(this.httpClient.<A>endpoint(host, port).build(), true);
		}

		public <A extends ExchangeContext> TestServiceGrpcClient.HttpClientStub<A> createStub(InetSocketAddress remoteAddress) {
			return new TestServiceGrpcClient.Http.StubImpl<>(this.httpClient.<A>endpoint(remoteAddress).build(), true);
		}

		public <A extends ExchangeContext> TestServiceGrpcClient.HttpClientStub<A> createStub(Endpoint<A> endpoint) {
			return new TestServiceGrpcClient.Http.StubImpl<>(endpoint, false);
		}

		private final class StubImpl<A extends ExchangeContext> implements TestServiceGrpcClient.HttpClientStub<A> {

			private final Endpoint<A> endpoint;
			private final boolean shutdownEndpoint;
			private final Consumer<GrpcOutboundRequestMetadata> metadataConfigurer;

			public StubImpl(Endpoint<A> endpoint, boolean shutdownEndpoint) {
				this.endpoint = endpoint;
				this.shutdownEndpoint = shutdownEndpoint;
				this.metadataConfigurer = null;
			}

			private StubImpl(TestServiceGrpcClient.Http.StubImpl<A> parent, Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
				this.endpoint = parent.endpoint;
				this.shutdownEndpoint = false;
				this.metadataConfigurer = metadataConfigurer;
			}

			@Override
			public TestServiceGrpcClient.HttpClientStub<A> withMetadata(Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
				return new TestServiceGrpcClient.Http.StubImpl<>(this, metadataConfigurer);
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
			public Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.EmptyProtos.Empty, io.grpc.testing.integration.EmptyProtos.Empty>> emptyCall(A context) {
				return this.endpoint.exchange(context)
					.map(exchange -> {
						GrpcExchange.Unary<A, io.grpc.testing.integration.EmptyProtos.Empty, io.grpc.testing.integration.EmptyProtos.Empty> grpcExchange = TestServiceGrpcClient.Http.this.grpcClient.unary(exchange, SERVICE_NAME, "EmptyCall", io.grpc.testing.integration.EmptyProtos.Empty.getDefaultInstance(), io.grpc.testing.integration.EmptyProtos.Empty.getDefaultInstance());
						grpcExchange.request().metadata(this.metadataConfigurer);
						return grpcExchange;
					});
			}
			@Override
			public Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.SimpleRequest, io.grpc.testing.integration.Messages.SimpleResponse>> unaryCall(A context) {
				return this.endpoint.exchange(context)
					.map(exchange -> {
						GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.SimpleRequest, io.grpc.testing.integration.Messages.SimpleResponse> grpcExchange = TestServiceGrpcClient.Http.this.grpcClient.unary(exchange, SERVICE_NAME, "UnaryCall", io.grpc.testing.integration.Messages.SimpleRequest.getDefaultInstance(), io.grpc.testing.integration.Messages.SimpleResponse.getDefaultInstance());
						grpcExchange.request().metadata(this.metadataConfigurer);
						return grpcExchange;
					});
			}
			@Override
			public Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.SimpleRequest, io.grpc.testing.integration.Messages.SimpleResponse>> cacheableUnaryCall(A context) {
				return this.endpoint.exchange(context)
					.map(exchange -> {
						GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.SimpleRequest, io.grpc.testing.integration.Messages.SimpleResponse> grpcExchange = TestServiceGrpcClient.Http.this.grpcClient.unary(exchange, SERVICE_NAME, "CacheableUnaryCall", io.grpc.testing.integration.Messages.SimpleRequest.getDefaultInstance(), io.grpc.testing.integration.Messages.SimpleResponse.getDefaultInstance());
						grpcExchange.request().metadata(this.metadataConfigurer);
						return grpcExchange;
					});
			}
			@Override
			public Mono<GrpcExchange.ServerStreaming<A, io.grpc.testing.integration.Messages.StreamingOutputCallRequest, io.grpc.testing.integration.Messages.StreamingOutputCallResponse>> streamingOutputCall(A context) {
				return this.endpoint.exchange(context)
					.map(exchange -> {
						GrpcExchange.ServerStreaming<A, io.grpc.testing.integration.Messages.StreamingOutputCallRequest, io.grpc.testing.integration.Messages.StreamingOutputCallResponse> grpcExchange = TestServiceGrpcClient.Http.this.grpcClient.serverStreaming(exchange, SERVICE_NAME, "StreamingOutputCall", io.grpc.testing.integration.Messages.StreamingOutputCallRequest.getDefaultInstance(), io.grpc.testing.integration.Messages.StreamingOutputCallResponse.getDefaultInstance());
						grpcExchange.request().metadata(this.metadataConfigurer);
						return grpcExchange;
					});
			}
			@Override
			public Mono<GrpcExchange.ClientStreaming<A, io.grpc.testing.integration.Messages.StreamingInputCallRequest, io.grpc.testing.integration.Messages.StreamingInputCallResponse>> streamingInputCall(A context) {
				return this.endpoint.exchange(context)
					.map(exchange -> {
						GrpcExchange.ClientStreaming<A, io.grpc.testing.integration.Messages.StreamingInputCallRequest, io.grpc.testing.integration.Messages.StreamingInputCallResponse> grpcExchange = TestServiceGrpcClient.Http.this.grpcClient.clientStreaming(exchange, SERVICE_NAME, "StreamingInputCall", io.grpc.testing.integration.Messages.StreamingInputCallRequest.getDefaultInstance(), io.grpc.testing.integration.Messages.StreamingInputCallResponse.getDefaultInstance());
						grpcExchange.request().metadata(this.metadataConfigurer);
						return grpcExchange;
					});
			}
			@Override
			public Mono<GrpcExchange.BidirectionalStreaming<A, io.grpc.testing.integration.Messages.StreamingOutputCallRequest, io.grpc.testing.integration.Messages.StreamingOutputCallResponse>> fullDuplexCall(A context) {
				return this.endpoint.exchange(context)
					.map(exchange -> {
						GrpcExchange.BidirectionalStreaming<A, io.grpc.testing.integration.Messages.StreamingOutputCallRequest, io.grpc.testing.integration.Messages.StreamingOutputCallResponse> grpcExchange = TestServiceGrpcClient.Http.this.grpcClient.bidirectionalStreaming(exchange, SERVICE_NAME, "FullDuplexCall", io.grpc.testing.integration.Messages.StreamingOutputCallRequest.getDefaultInstance(), io.grpc.testing.integration.Messages.StreamingOutputCallResponse.getDefaultInstance());
						grpcExchange.request().metadata(this.metadataConfigurer);
						return grpcExchange;
					});
			}
			@Override
			public Mono<GrpcExchange.BidirectionalStreaming<A, io.grpc.testing.integration.Messages.StreamingOutputCallRequest, io.grpc.testing.integration.Messages.StreamingOutputCallResponse>> halfDuplexCall(A context) {
				return this.endpoint.exchange(context)
					.map(exchange -> {
						GrpcExchange.BidirectionalStreaming<A, io.grpc.testing.integration.Messages.StreamingOutputCallRequest, io.grpc.testing.integration.Messages.StreamingOutputCallResponse> grpcExchange = TestServiceGrpcClient.Http.this.grpcClient.bidirectionalStreaming(exchange, SERVICE_NAME, "HalfDuplexCall", io.grpc.testing.integration.Messages.StreamingOutputCallRequest.getDefaultInstance(), io.grpc.testing.integration.Messages.StreamingOutputCallResponse.getDefaultInstance());
						grpcExchange.request().metadata(this.metadataConfigurer);
						return grpcExchange;
					});
			}
			@Override
			public Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.EmptyProtos.Empty, io.grpc.testing.integration.EmptyProtos.Empty>> unimplementedCall(A context) {
				return this.endpoint.exchange(context)
					.map(exchange -> {
						GrpcExchange.Unary<A, io.grpc.testing.integration.EmptyProtos.Empty, io.grpc.testing.integration.EmptyProtos.Empty> grpcExchange = TestServiceGrpcClient.Http.this.grpcClient.unary(exchange, SERVICE_NAME, "UnimplementedCall", io.grpc.testing.integration.EmptyProtos.Empty.getDefaultInstance(), io.grpc.testing.integration.EmptyProtos.Empty.getDefaultInstance());
						grpcExchange.request().metadata(this.metadataConfigurer);
						return grpcExchange;
					});
			}
		}
	}

	/**
	 * A simple service to test the various types of RPCs and experiment with
	 * performance with various types of payload.
	 *
	 * @param <A> the exchange context type
	 */
	public interface HttpClientStub<A extends ExchangeContext> extends GrpcClient.CloseableStub<A, TestServiceGrpcClient.HttpClientStub<A>> {
		
		/**
		 * One empty request followed by one empty response.
		 * 
		 * @return a mono emitting the unary exchange
		 */
		default Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.EmptyProtos.Empty, io.grpc.testing.integration.EmptyProtos.Empty>> emptyCall() {
			return this.emptyCall((A)null);
		}

		/**
		 * One empty request followed by one empty response.
		 * 
		 * @param context the context
		 * 
		 * @return a mono emitting the unary exchange
		 */
		Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.EmptyProtos.Empty, io.grpc.testing.integration.EmptyProtos.Empty>> emptyCall(A context);

		/**
		 * One empty request followed by one empty response.
		 * 
		 * @param request the client request
		 * 
		 * @return the server response
		 */
		default Mono<io.grpc.testing.integration.EmptyProtos.Empty> emptyCall(io.grpc.testing.integration.EmptyProtos.Empty request) {
			return this.emptyCall(request, null);
		}

		/**
		 * One empty request followed by one empty response.
		 * 
		 * @param request the client request
		 * @param context the context
		 * 
		 * @return the server response
		 */
		default Mono<io.grpc.testing.integration.EmptyProtos.Empty> emptyCall(io.grpc.testing.integration.EmptyProtos.Empty request, A context) {
			return this.emptyCall(context)
				.flatMap(grpcExchange -> {
					grpcExchange.request().value(request);
					return grpcExchange.response().flatMap(GrpcResponse.Unary::value);
				});
		}
		/**
		 * One request followed by one response.
		 * 
		 * @return a mono emitting the unary exchange
		 */
		default Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.SimpleRequest, io.grpc.testing.integration.Messages.SimpleResponse>> unaryCall() {
			return this.unaryCall((A)null);
		}

		/**
		 * One request followed by one response.
		 * 
		 * @param context the context
		 * 
		 * @return a mono emitting the unary exchange
		 */
		Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.SimpleRequest, io.grpc.testing.integration.Messages.SimpleResponse>> unaryCall(A context);

		/**
		 * One request followed by one response.
		 * 
		 * @param request the client request
		 * 
		 * @return the server response
		 */
		default Mono<io.grpc.testing.integration.Messages.SimpleResponse> unaryCall(io.grpc.testing.integration.Messages.SimpleRequest request) {
			return this.unaryCall(request, null);
		}

		/**
		 * One request followed by one response.
		 * 
		 * @param request the client request
		 * @param context the context
		 * 
		 * @return the server response
		 */
		default Mono<io.grpc.testing.integration.Messages.SimpleResponse> unaryCall(io.grpc.testing.integration.Messages.SimpleRequest request, A context) {
			return this.unaryCall(context)
				.flatMap(grpcExchange -> {
					grpcExchange.request().value(request);
					return grpcExchange.response().flatMap(GrpcResponse.Unary::value);
				});
		}
		/**
		 * One request followed by one response. Response has cache control
		 * headers set such that a caching HTTP proxy (such as GFE) can
		 * satisfy subsequent requests.
		 * 
		 * @return a mono emitting the unary exchange
		 */
		default Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.SimpleRequest, io.grpc.testing.integration.Messages.SimpleResponse>> cacheableUnaryCall() {
			return this.cacheableUnaryCall((A)null);
		}

		/**
		 * One request followed by one response. Response has cache control
		 * headers set such that a caching HTTP proxy (such as GFE) can
		 * satisfy subsequent requests.
		 * 
		 * @param context the context
		 * 
		 * @return a mono emitting the unary exchange
		 */
		Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.SimpleRequest, io.grpc.testing.integration.Messages.SimpleResponse>> cacheableUnaryCall(A context);

		/**
		 * One request followed by one response. Response has cache control
		 * headers set such that a caching HTTP proxy (such as GFE) can
		 * satisfy subsequent requests.
		 * 
		 * @param request the client request
		 * 
		 * @return the server response
		 */
		default Mono<io.grpc.testing.integration.Messages.SimpleResponse> cacheableUnaryCall(io.grpc.testing.integration.Messages.SimpleRequest request) {
			return this.cacheableUnaryCall(request, null);
		}

		/**
		 * One request followed by one response. Response has cache control
		 * headers set such that a caching HTTP proxy (such as GFE) can
		 * satisfy subsequent requests.
		 * 
		 * @param request the client request
		 * @param context the context
		 * 
		 * @return the server response
		 */
		default Mono<io.grpc.testing.integration.Messages.SimpleResponse> cacheableUnaryCall(io.grpc.testing.integration.Messages.SimpleRequest request, A context) {
			return this.cacheableUnaryCall(context)
				.flatMap(grpcExchange -> {
					grpcExchange.request().value(request);
					return grpcExchange.response().flatMap(GrpcResponse.Unary::value);
				});
		}
		/**
		 * One request followed by a sequence of responses (streamed download).
		 * The server returns the payload with client desired type and sizes.
		 * 
		 * @return a mono emitting the server streaming exchange
		 */
		default Mono<GrpcExchange.ServerStreaming<A, io.grpc.testing.integration.Messages.StreamingOutputCallRequest, io.grpc.testing.integration.Messages.StreamingOutputCallResponse>> streamingOutputCall() {
			return this.streamingOutputCall((A)null);
		}

		/**
		 * One request followed by a sequence of responses (streamed download).
		 * The server returns the payload with client desired type and sizes.
		 * 
		 * @param context the context
		 * 
		 * @return a mono emitting the server streaming exchange
		 */
		Mono<GrpcExchange.ServerStreaming<A, io.grpc.testing.integration.Messages.StreamingOutputCallRequest, io.grpc.testing.integration.Messages.StreamingOutputCallResponse>> streamingOutputCall(A context);

		/**
		 * One request followed by a sequence of responses (streamed download).
		 * The server returns the payload with client desired type and sizes.
		 * 
		 * @param request the client request
		 * 
		 * @return the server response publisher
		 */
		default Publisher<io.grpc.testing.integration.Messages.StreamingOutputCallResponse> streamingOutputCall(io.grpc.testing.integration.Messages.StreamingOutputCallRequest request) {
			return this.streamingOutputCall(request, null);
		}

		/**
		 * One request followed by a sequence of responses (streamed download).
		 * The server returns the payload with client desired type and sizes.
		 * 
		 * @param request the client request
		 * @param context the context
		 * 
		 * @return the server response publisher
		 */
		default Publisher<io.grpc.testing.integration.Messages.StreamingOutputCallResponse> streamingOutputCall(io.grpc.testing.integration.Messages.StreamingOutputCallRequest request, A context) {
			return this.streamingOutputCall(context)
				.flatMapMany(grpcExchange -> {
					grpcExchange.request().value(request);
					return grpcExchange.response().flatMapMany(GrpcResponse.Streaming::stream);
				});
		}
		/**
		 * A sequence of requests followed by one response (streamed upload).
		 * The server returns the aggregated size of client payload as the result.
		 * 
		 * @return a mono emitting the client streaming exchange
		 */
		default Mono<GrpcExchange.ClientStreaming<A, io.grpc.testing.integration.Messages.StreamingInputCallRequest, io.grpc.testing.integration.Messages.StreamingInputCallResponse>> streamingInputCall() {
			return this.streamingInputCall((A)null);
		}

		/**
		 * A sequence of requests followed by one response (streamed upload).
		 * The server returns the aggregated size of client payload as the result.
		 * 
		 * @param context the context
		 * 
		 * @return a mono emitting the client streaming exchange
		 */
		Mono<GrpcExchange.ClientStreaming<A, io.grpc.testing.integration.Messages.StreamingInputCallRequest, io.grpc.testing.integration.Messages.StreamingInputCallResponse>> streamingInputCall(A context);

		/**
		 * A sequence of requests followed by one response (streamed upload).
		 * The server returns the aggregated size of client payload as the result.
		 * 
		 * @param request the client request publisher
		 * 
		 * @return the server response
		 */
		default Mono<io.grpc.testing.integration.Messages.StreamingInputCallResponse> streamingInputCall(Publisher<io.grpc.testing.integration.Messages.StreamingInputCallRequest> request) {
			return this.streamingInputCall(request, null);
		}

		/**
		 * A sequence of requests followed by one response (streamed upload).
		 * The server returns the aggregated size of client payload as the result.
		 * 
		 * @param request the client request publisher
		 * @param context the context
		 * 
		 * @return the server response
		 */
		default Mono<io.grpc.testing.integration.Messages.StreamingInputCallResponse> streamingInputCall(Publisher<io.grpc.testing.integration.Messages.StreamingInputCallRequest> request, A context) {
			return this.streamingInputCall(context)
				.flatMap(grpcExchange -> {
					grpcExchange.request().stream(request);
					return grpcExchange.response().flatMap(GrpcResponse.Unary::value);
				});
		}
		/**
		 * A sequence of requests with each request served by the server immediately.
		 * As one request could lead to multiple responses, this interface
		 * demonstrates the idea of full duplexing.
		 * 
		 * @return a mono emitting the bidirectional streaming exchange
		 */
		default Mono<GrpcExchange.BidirectionalStreaming<A, io.grpc.testing.integration.Messages.StreamingOutputCallRequest, io.grpc.testing.integration.Messages.StreamingOutputCallResponse>> fullDuplexCall() {
			return this.fullDuplexCall((A)null);
		}

		/**
		 * A sequence of requests with each request served by the server immediately.
		 * As one request could lead to multiple responses, this interface
		 * demonstrates the idea of full duplexing.
		 * 
		 * @param context the context
		 * 
		 * @return a mono emitting the bidirectional streaming exchange
		 */
		Mono<GrpcExchange.BidirectionalStreaming<A, io.grpc.testing.integration.Messages.StreamingOutputCallRequest, io.grpc.testing.integration.Messages.StreamingOutputCallResponse>> fullDuplexCall(A context);

		/**
		 * A sequence of requests with each request served by the server immediately.
		 * As one request could lead to multiple responses, this interface
		 * demonstrates the idea of full duplexing.
		 * 
		 * @param request the client request publisher
		 * 
		 * @return the server response publisher
		 */
		default Publisher<io.grpc.testing.integration.Messages.StreamingOutputCallResponse> fullDuplexCall(Publisher<io.grpc.testing.integration.Messages.StreamingOutputCallRequest> request) {
			return this.fullDuplexCall(request, null);
		}

		/**
		 * A sequence of requests with each request served by the server immediately.
		 * As one request could lead to multiple responses, this interface
		 * demonstrates the idea of full duplexing.
		 * 
		 * @param request the client request publisher
		 * @param context the context
		 * 
		 * @return the server response publisher
		 */
		default Publisher<io.grpc.testing.integration.Messages.StreamingOutputCallResponse> fullDuplexCall(Publisher<io.grpc.testing.integration.Messages.StreamingOutputCallRequest> request, A context) {
			return this.fullDuplexCall(context)
				.flatMapMany(grpcExchange -> {
					grpcExchange.request().stream(request);
					return grpcExchange.response().flatMapMany(GrpcResponse.Streaming::stream);
				});
		}
		/**
		 * A sequence of requests followed by a sequence of responses.
		 * The server buffers all the client requests and then serves them in order. A
		 * stream of responses are returned to the client when the server starts with
		 * first request.
		 * 
		 * @return a mono emitting the bidirectional streaming exchange
		 */
		default Mono<GrpcExchange.BidirectionalStreaming<A, io.grpc.testing.integration.Messages.StreamingOutputCallRequest, io.grpc.testing.integration.Messages.StreamingOutputCallResponse>> halfDuplexCall() {
			return this.halfDuplexCall((A)null);
		}

		/**
		 * A sequence of requests followed by a sequence of responses.
		 * The server buffers all the client requests and then serves them in order. A
		 * stream of responses are returned to the client when the server starts with
		 * first request.
		 * 
		 * @param context the context
		 * 
		 * @return a mono emitting the bidirectional streaming exchange
		 */
		Mono<GrpcExchange.BidirectionalStreaming<A, io.grpc.testing.integration.Messages.StreamingOutputCallRequest, io.grpc.testing.integration.Messages.StreamingOutputCallResponse>> halfDuplexCall(A context);

		/**
		 * A sequence of requests followed by a sequence of responses.
		 * The server buffers all the client requests and then serves them in order. A
		 * stream of responses are returned to the client when the server starts with
		 * first request.
		 * 
		 * @param request the client request publisher
		 * 
		 * @return the server response publisher
		 */
		default Publisher<io.grpc.testing.integration.Messages.StreamingOutputCallResponse> halfDuplexCall(Publisher<io.grpc.testing.integration.Messages.StreamingOutputCallRequest> request) {
			return this.halfDuplexCall(request, null);
		}

		/**
		 * A sequence of requests followed by a sequence of responses.
		 * The server buffers all the client requests and then serves them in order. A
		 * stream of responses are returned to the client when the server starts with
		 * first request.
		 * 
		 * @param request the client request publisher
		 * @param context the context
		 * 
		 * @return the server response publisher
		 */
		default Publisher<io.grpc.testing.integration.Messages.StreamingOutputCallResponse> halfDuplexCall(Publisher<io.grpc.testing.integration.Messages.StreamingOutputCallRequest> request, A context) {
			return this.halfDuplexCall(context)
				.flatMapMany(grpcExchange -> {
					grpcExchange.request().stream(request);
					return grpcExchange.response().flatMapMany(GrpcResponse.Streaming::stream);
				});
		}
		/**
		 * The test server will not implement this method. It will be used
		 * to test the behavior when clients call unimplemented methods.
		 * 
		 * @return a mono emitting the unary exchange
		 */
		default Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.EmptyProtos.Empty, io.grpc.testing.integration.EmptyProtos.Empty>> unimplementedCall() {
			return this.unimplementedCall((A)null);
		}

		/**
		 * The test server will not implement this method. It will be used
		 * to test the behavior when clients call unimplemented methods.
		 * 
		 * @param context the context
		 * 
		 * @return a mono emitting the unary exchange
		 */
		Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.EmptyProtos.Empty, io.grpc.testing.integration.EmptyProtos.Empty>> unimplementedCall(A context);

		/**
		 * The test server will not implement this method. It will be used
		 * to test the behavior when clients call unimplemented methods.
		 * 
		 * @param request the client request
		 * 
		 * @return the server response
		 */
		default Mono<io.grpc.testing.integration.EmptyProtos.Empty> unimplementedCall(io.grpc.testing.integration.EmptyProtos.Empty request) {
			return this.unimplementedCall(request, null);
		}

		/**
		 * The test server will not implement this method. It will be used
		 * to test the behavior when clients call unimplemented methods.
		 * 
		 * @param request the client request
		 * @param context the context
		 * 
		 * @return the server response
		 */
		default Mono<io.grpc.testing.integration.EmptyProtos.Empty> unimplementedCall(io.grpc.testing.integration.EmptyProtos.Empty request, A context) {
			return this.unimplementedCall(context)
				.flatMap(grpcExchange -> {
					grpcExchange.request().value(request);
					return grpcExchange.response().flatMap(GrpcResponse.Unary::value);
				});
		}
	}

    public static abstract class Web<A extends ExchangeContext> implements TestServiceGrpcClient.WebClientStub<A> {

		private final ServiceID serviceID;
		private final WebClient<? extends A> webClient;
		private final GrpcClient grpcClient;

		private final Web<A>.StubImpl stub;

		public Web(ServiceID serviceID, WebClient<? extends A> webClient, GrpcClient grpcClient) {
			this.serviceID = serviceID;
			this.webClient = webClient;
			this.grpcClient = grpcClient;

			this.stub = new TestServiceGrpcClient.Web.StubImpl(null);
		}

		@Override
		public TestServiceGrpcClient.WebClientStub<A> withMetadata(Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
			return new TestServiceGrpcClient.Web<A>.StubImpl(metadataConfigurer);
		}
		
		@Override
		public Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.EmptyProtos.Empty, io.grpc.testing.integration.EmptyProtos.Empty>> emptyCall(Consumer<A> contextConfigurer) {
			return this.stub.emptyCall(contextConfigurer);
		}
		@Override
		public Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.SimpleRequest, io.grpc.testing.integration.Messages.SimpleResponse>> unaryCall(Consumer<A> contextConfigurer) {
			return this.stub.unaryCall(contextConfigurer);
		}
		@Override
		public Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.SimpleRequest, io.grpc.testing.integration.Messages.SimpleResponse>> cacheableUnaryCall(Consumer<A> contextConfigurer) {
			return this.stub.cacheableUnaryCall(contextConfigurer);
		}
		@Override
		public Mono<GrpcExchange.ServerStreaming<A, io.grpc.testing.integration.Messages.StreamingOutputCallRequest, io.grpc.testing.integration.Messages.StreamingOutputCallResponse>> streamingOutputCall(Consumer<A> contextConfigurer) {
			return this.stub.streamingOutputCall(contextConfigurer);
		}
		@Override
		public Mono<GrpcExchange.ClientStreaming<A, io.grpc.testing.integration.Messages.StreamingInputCallRequest, io.grpc.testing.integration.Messages.StreamingInputCallResponse>> streamingInputCall(Consumer<A> contextConfigurer) {
			return this.stub.streamingInputCall(contextConfigurer);
        }
		@Override
		public Mono<GrpcExchange.BidirectionalStreaming<A, io.grpc.testing.integration.Messages.StreamingOutputCallRequest, io.grpc.testing.integration.Messages.StreamingOutputCallResponse>> fullDuplexCall(Consumer<A> contextConfigurer) {
			return this.stub.fullDuplexCall(contextConfigurer);
        }
		@Override
		public Mono<GrpcExchange.BidirectionalStreaming<A, io.grpc.testing.integration.Messages.StreamingOutputCallRequest, io.grpc.testing.integration.Messages.StreamingOutputCallResponse>> halfDuplexCall(Consumer<A> contextConfigurer) {
			return this.stub.halfDuplexCall(contextConfigurer);
        }
		@Override
		public Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.EmptyProtos.Empty, io.grpc.testing.integration.EmptyProtos.Empty>> unimplementedCall(Consumer<A> contextConfigurer) {
			return this.stub.unimplementedCall(contextConfigurer);
		}

		private final class StubImpl implements TestServiceGrpcClient.WebClientStub<A> {

			private final Consumer<GrpcOutboundRequestMetadata> metadataConfigurer;

			public StubImpl(Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
				this.metadataConfigurer = metadataConfigurer;
			}

			@Override
			public TestServiceGrpcClient.WebClientStub<A> withMetadata(Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
				return new TestServiceGrpcClient.Web<A>.StubImpl(metadataConfigurer);
			}
			
			@Override
			public Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.EmptyProtos.Empty, io.grpc.testing.integration.EmptyProtos.Empty>> emptyCall(Consumer<A> contextConfigurer) {
				return Web.this.webClient.exchange(Web.this.serviceID.getURI())
					.map(exchange -> {
						if(contextConfigurer != null) {
							contextConfigurer.accept(exchange.context());
						}
						GrpcExchange.Unary<A, io.grpc.testing.integration.EmptyProtos.Empty, io.grpc.testing.integration.EmptyProtos.Empty> grpcExchange = (GrpcExchange.Unary<A, io.grpc.testing.integration.EmptyProtos.Empty, io.grpc.testing.integration.EmptyProtos.Empty>)Web.this.grpcClient.unary(exchange, SERVICE_NAME, "EmptyCall", io.grpc.testing.integration.EmptyProtos.Empty.getDefaultInstance(), io.grpc.testing.integration.EmptyProtos.Empty.getDefaultInstance());
						grpcExchange.request().metadata(this.metadataConfigurer);
						return grpcExchange;
					});
			}
			@Override
			public Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.SimpleRequest, io.grpc.testing.integration.Messages.SimpleResponse>> unaryCall(Consumer<A> contextConfigurer) {
				return Web.this.webClient.exchange(Web.this.serviceID.getURI())
					.map(exchange -> {
						if(contextConfigurer != null) {
							contextConfigurer.accept(exchange.context());
						}
						GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.SimpleRequest, io.grpc.testing.integration.Messages.SimpleResponse> grpcExchange = (GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.SimpleRequest, io.grpc.testing.integration.Messages.SimpleResponse>)Web.this.grpcClient.unary(exchange, SERVICE_NAME, "UnaryCall", io.grpc.testing.integration.Messages.SimpleRequest.getDefaultInstance(), io.grpc.testing.integration.Messages.SimpleResponse.getDefaultInstance());
						grpcExchange.request().metadata(this.metadataConfigurer);
						return grpcExchange;
					});
			}
			@Override
			public Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.SimpleRequest, io.grpc.testing.integration.Messages.SimpleResponse>> cacheableUnaryCall(Consumer<A> contextConfigurer) {
				return Web.this.webClient.exchange(Web.this.serviceID.getURI())
					.map(exchange -> {
						if(contextConfigurer != null) {
							contextConfigurer.accept(exchange.context());
						}
						GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.SimpleRequest, io.grpc.testing.integration.Messages.SimpleResponse> grpcExchange = (GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.SimpleRequest, io.grpc.testing.integration.Messages.SimpleResponse>)Web.this.grpcClient.unary(exchange, SERVICE_NAME, "CacheableUnaryCall", io.grpc.testing.integration.Messages.SimpleRequest.getDefaultInstance(), io.grpc.testing.integration.Messages.SimpleResponse.getDefaultInstance());
						grpcExchange.request().metadata(this.metadataConfigurer);
						return grpcExchange;
					});
			}
			@Override
			public Mono<GrpcExchange.ServerStreaming<A, io.grpc.testing.integration.Messages.StreamingOutputCallRequest, io.grpc.testing.integration.Messages.StreamingOutputCallResponse>> streamingOutputCall(Consumer<A> contextConfigurer) {
				return Web.this.webClient.exchange(Web.this.serviceID.getURI())
					.map(exchange -> {
						if(contextConfigurer != null) {
							contextConfigurer.accept(exchange.context());
						}
						GrpcExchange.ServerStreaming<A, io.grpc.testing.integration.Messages.StreamingOutputCallRequest, io.grpc.testing.integration.Messages.StreamingOutputCallResponse> grpcExchange = (GrpcExchange.ServerStreaming<A, io.grpc.testing.integration.Messages.StreamingOutputCallRequest, io.grpc.testing.integration.Messages.StreamingOutputCallResponse>)Web.this.grpcClient.serverStreaming(exchange, SERVICE_NAME, "StreamingOutputCall", io.grpc.testing.integration.Messages.StreamingOutputCallRequest.getDefaultInstance(), io.grpc.testing.integration.Messages.StreamingOutputCallResponse.getDefaultInstance());
						grpcExchange.request().metadata(this.metadataConfigurer);
						return grpcExchange;
					});
			}
			@Override
			public Mono<GrpcExchange.ClientStreaming<A, io.grpc.testing.integration.Messages.StreamingInputCallRequest, io.grpc.testing.integration.Messages.StreamingInputCallResponse>> streamingInputCall(Consumer<A> contextConfigurer) {
				return Web.this.webClient.exchange(Web.this.serviceID.getURI())
					.map(exchange -> {
						if(contextConfigurer != null) {
							contextConfigurer.accept(exchange.context());
						}
						GrpcExchange.ClientStreaming<A, io.grpc.testing.integration.Messages.StreamingInputCallRequest, io.grpc.testing.integration.Messages.StreamingInputCallResponse> grpcExchange = (GrpcExchange.ClientStreaming<A, io.grpc.testing.integration.Messages.StreamingInputCallRequest, io.grpc.testing.integration.Messages.StreamingInputCallResponse>)Web.this.grpcClient.clientStreaming(exchange, SERVICE_NAME, "StreamingInputCall", io.grpc.testing.integration.Messages.StreamingInputCallRequest.getDefaultInstance(), io.grpc.testing.integration.Messages.StreamingInputCallResponse.getDefaultInstance());
						grpcExchange.request().metadata(this.metadataConfigurer);
						return grpcExchange;
					});
			}
			@Override
			public Mono<GrpcExchange.BidirectionalStreaming<A, io.grpc.testing.integration.Messages.StreamingOutputCallRequest, io.grpc.testing.integration.Messages.StreamingOutputCallResponse>> fullDuplexCall(Consumer<A> contextConfigurer) {
				return Web.this.webClient.exchange(Web.this.serviceID.getURI())
						.map(exchange -> {
							if(contextConfigurer != null) {
								contextConfigurer.accept(exchange.context());
							}
						GrpcExchange.BidirectionalStreaming<A, io.grpc.testing.integration.Messages.StreamingOutputCallRequest, io.grpc.testing.integration.Messages.StreamingOutputCallResponse> grpcExchange = (GrpcExchange.BidirectionalStreaming<A, io.grpc.testing.integration.Messages.StreamingOutputCallRequest, io.grpc.testing.integration.Messages.StreamingOutputCallResponse>)Web.this.grpcClient.bidirectionalStreaming(exchange, SERVICE_NAME, "FullDuplexCall", io.grpc.testing.integration.Messages.StreamingOutputCallRequest.getDefaultInstance(), io.grpc.testing.integration.Messages.StreamingOutputCallResponse.getDefaultInstance());
						grpcExchange.request().metadata(this.metadataConfigurer);
						return grpcExchange;
					});
			}
			@Override
			public Mono<GrpcExchange.BidirectionalStreaming<A, io.grpc.testing.integration.Messages.StreamingOutputCallRequest, io.grpc.testing.integration.Messages.StreamingOutputCallResponse>> halfDuplexCall(Consumer<A> contextConfigurer) {
				return Web.this.webClient.exchange(Web.this.serviceID.getURI())
						.map(exchange -> {
							if(contextConfigurer != null) {
								contextConfigurer.accept(exchange.context());
							}
						GrpcExchange.BidirectionalStreaming<A, io.grpc.testing.integration.Messages.StreamingOutputCallRequest, io.grpc.testing.integration.Messages.StreamingOutputCallResponse> grpcExchange = (GrpcExchange.BidirectionalStreaming<A, io.grpc.testing.integration.Messages.StreamingOutputCallRequest, io.grpc.testing.integration.Messages.StreamingOutputCallResponse>)Web.this.grpcClient.bidirectionalStreaming(exchange, SERVICE_NAME, "HalfDuplexCall", io.grpc.testing.integration.Messages.StreamingOutputCallRequest.getDefaultInstance(), io.grpc.testing.integration.Messages.StreamingOutputCallResponse.getDefaultInstance());
						grpcExchange.request().metadata(this.metadataConfigurer);
						return grpcExchange;
					});
			}
			@Override
			public Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.EmptyProtos.Empty, io.grpc.testing.integration.EmptyProtos.Empty>> unimplementedCall(Consumer<A> contextConfigurer) {
				return Web.this.webClient.exchange(Web.this.serviceID.getURI())
					.map(exchange -> {
						if(contextConfigurer != null) {
							contextConfigurer.accept(exchange.context());
						}
						GrpcExchange.Unary<A, io.grpc.testing.integration.EmptyProtos.Empty, io.grpc.testing.integration.EmptyProtos.Empty> grpcExchange = (GrpcExchange.Unary<A, io.grpc.testing.integration.EmptyProtos.Empty, io.grpc.testing.integration.EmptyProtos.Empty>)Web.this.grpcClient.unary(exchange, SERVICE_NAME, "UnimplementedCall", io.grpc.testing.integration.EmptyProtos.Empty.getDefaultInstance(), io.grpc.testing.integration.EmptyProtos.Empty.getDefaultInstance());
						grpcExchange.request().metadata(this.metadataConfigurer);
						return grpcExchange;
					});
			}
		}
	}

	/**
	 * A simple service to test the various types of RPCs and experiment with
	 * performance with various types of payload.
	 *
	 * @param <A> the exchange context type
	 */
	public interface WebClientStub<A extends ExchangeContext> extends GrpcClient.Stub<A, TestServiceGrpcClient.WebClientStub<A>> {
		
		/**
		 * One empty request followed by one empty response.
		 *
		 * @return a mono emitting the unary exchange
		 */
		default Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.EmptyProtos.Empty, io.grpc.testing.integration.EmptyProtos.Empty>> emptyCall() {
			return this.emptyCall((Consumer<A>)null);
		}

		/**
		 * One empty request followed by one empty response.
		 *
		 * @param contextConfigurer the context configurer
		 *
		 * @return a mono emitting the unary exchange
		 */
		Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.EmptyProtos.Empty, io.grpc.testing.integration.EmptyProtos.Empty>> emptyCall(Consumer<A> contextConfigurer);

		/**
		 * One empty request followed by one empty response.
		 *
		 * @param request the client request
		 *
		 * @return the server response
		 */
		default Mono<io.grpc.testing.integration.EmptyProtos.Empty> emptyCall(io.grpc.testing.integration.EmptyProtos.Empty request) {
			return this.emptyCall(request, (Consumer<A>)null);
		}

		/**
		 * One empty request followed by one empty response.
		 *
		 * @param request           the client request
		 * @param contextConfigurer the context configurer
		 *
		 * @return the server response
		 */
		default Mono<io.grpc.testing.integration.EmptyProtos.Empty> emptyCall(io.grpc.testing.integration.EmptyProtos.Empty request, Consumer<A> contextConfigurer) {
			return this.emptyCall(contextConfigurer)
				.flatMap(grpcExchange -> {
					grpcExchange.request().value(request);
					return grpcExchange.response().flatMap(GrpcResponse.Unary::value);
				});
		}
		/**
		 * One request followed by one response.
		 *
		 * @return a mono emitting the unary exchange
		 */
		default Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.SimpleRequest, io.grpc.testing.integration.Messages.SimpleResponse>> unaryCall() {
			return this.unaryCall((Consumer<A>)null);
		}

		/**
		 * One request followed by one response.
		 *
		 * @param contextConfigurer the context configurer
		 *
		 * @return a mono emitting the unary exchange
		 */
		Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.SimpleRequest, io.grpc.testing.integration.Messages.SimpleResponse>> unaryCall(Consumer<A> contextConfigurer);

		/**
		 * One request followed by one response.
		 *
		 * @param request the client request
		 *
		 * @return the server response
		 */
		default Mono<io.grpc.testing.integration.Messages.SimpleResponse> unaryCall(io.grpc.testing.integration.Messages.SimpleRequest request) {
			return this.unaryCall(request, (Consumer<A>)null);
		}

		/**
		 * One request followed by one response.
		 *
		 * @param request           the client request
		 * @param contextConfigurer the context configurer
		 *
		 * @return the server response
		 */
		default Mono<io.grpc.testing.integration.Messages.SimpleResponse> unaryCall(io.grpc.testing.integration.Messages.SimpleRequest request, Consumer<A> contextConfigurer) {
			return this.unaryCall(contextConfigurer)
				.flatMap(grpcExchange -> {
					grpcExchange.request().value(request);
					return grpcExchange.response().flatMap(GrpcResponse.Unary::value);
				});
		}
		/**
		 * One request followed by one response. Response has cache control
		 * headers set such that a caching HTTP proxy (such as GFE) can
		 * satisfy subsequent requests.
		 *
		 * @return a mono emitting the unary exchange
		 */
		default Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.SimpleRequest, io.grpc.testing.integration.Messages.SimpleResponse>> cacheableUnaryCall() {
			return this.cacheableUnaryCall((Consumer<A>)null);
		}

		/**
		 * One request followed by one response. Response has cache control
		 * headers set such that a caching HTTP proxy (such as GFE) can
		 * satisfy subsequent requests.
		 *
		 * @param contextConfigurer the context configurer
		 *
		 * @return a mono emitting the unary exchange
		 */
		Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.SimpleRequest, io.grpc.testing.integration.Messages.SimpleResponse>> cacheableUnaryCall(Consumer<A> contextConfigurer);

		/**
		 * One request followed by one response. Response has cache control
		 * headers set such that a caching HTTP proxy (such as GFE) can
		 * satisfy subsequent requests.
		 *
		 * @param request the client request
		 *
		 * @return the server response
		 */
		default Mono<io.grpc.testing.integration.Messages.SimpleResponse> cacheableUnaryCall(io.grpc.testing.integration.Messages.SimpleRequest request) {
			return this.cacheableUnaryCall(request, (Consumer<A>)null);
		}

		/**
		 * One request followed by one response. Response has cache control
		 * headers set such that a caching HTTP proxy (such as GFE) can
		 * satisfy subsequent requests.
		 *
		 * @param request           the client request
		 * @param contextConfigurer the context configurer
		 *
		 * @return the server response
		 */
		default Mono<io.grpc.testing.integration.Messages.SimpleResponse> cacheableUnaryCall(io.grpc.testing.integration.Messages.SimpleRequest request, Consumer<A> contextConfigurer) {
			return this.cacheableUnaryCall(contextConfigurer)
				.flatMap(grpcExchange -> {
					grpcExchange.request().value(request);
					return grpcExchange.response().flatMap(GrpcResponse.Unary::value);
				});
		}
		/**
		 * One request followed by a sequence of responses (streamed download).
		 * The server returns the payload with client desired type and sizes.
		 *
		 * @return a mono emitting the server streaming exchange
		 */
		default Mono<GrpcExchange.ServerStreaming<A, io.grpc.testing.integration.Messages.StreamingOutputCallRequest, io.grpc.testing.integration.Messages.StreamingOutputCallResponse>> streamingOutputCall() {
			return this.streamingOutputCall((Consumer<A>)null);
		}

		/**
		 * One request followed by a sequence of responses (streamed download).
		 * The server returns the payload with client desired type and sizes.
		 *
		 * @param contextConfigurer the context configurer
		 *
		 * @return a mono emitting the server streaming exchange
		 */
		Mono<GrpcExchange.ServerStreaming<A, io.grpc.testing.integration.Messages.StreamingOutputCallRequest, io.grpc.testing.integration.Messages.StreamingOutputCallResponse>> streamingOutputCall(Consumer<A> contextConfigurer);

		/**
		 * One request followed by a sequence of responses (streamed download).
		 * The server returns the payload with client desired type and sizes.
		 *
		 * @param request the client request
		 *
		 * @return the server response publisher
		 */
		default Publisher<io.grpc.testing.integration.Messages.StreamingOutputCallResponse> streamingOutputCall(io.grpc.testing.integration.Messages.StreamingOutputCallRequest request) {
			return this.streamingOutputCall(request, (Consumer<A>)null);
		}

		/**
		 * One request followed by a sequence of responses (streamed download).
		 * The server returns the payload with client desired type and sizes.
		 *
		 * @param request the client request
		 * @param contextConfigurer the context configurer
		 *
		 * @return the server response publisher
		 */
		default Publisher<io.grpc.testing.integration.Messages.StreamingOutputCallResponse> streamingOutputCall(io.grpc.testing.integration.Messages.StreamingOutputCallRequest request, Consumer<A> contextConfigurer) {
			return this.streamingOutputCall(contextConfigurer)
				.flatMapMany(grpcExchange -> {
					grpcExchange.request().value(request);
					return grpcExchange.response().flatMapMany(GrpcResponse.Streaming::stream);
				});
		}
		/**
		 * A sequence of requests followed by one response (streamed upload).
		 * The server returns the aggregated size of client payload as the result.
		 *
		 * @return a mono emitting the client streaming exchange
		 */
		default Mono<GrpcExchange.ClientStreaming<A, io.grpc.testing.integration.Messages.StreamingInputCallRequest, io.grpc.testing.integration.Messages.StreamingInputCallResponse>> streamingInputCall() {
			return this.streamingInputCall((Consumer<A>)null);
		}

		/**
		 * A sequence of requests followed by one response (streamed upload).
		 * The server returns the aggregated size of client payload as the result.
		 *
		 * @param contextConfigurer the context configurer
		 *
		 * @return a mono emitting the client streaming exchange
		 */
		Mono<GrpcExchange.ClientStreaming<A, io.grpc.testing.integration.Messages.StreamingInputCallRequest, io.grpc.testing.integration.Messages.StreamingInputCallResponse>> streamingInputCall(Consumer<A> contextConfigurer);

		/**
		 * A sequence of requests followed by one response (streamed upload).
		 * The server returns the aggregated size of client payload as the result.
		 *
		 * @param request the client request publisher
		 *
		 * @return the server response
		 */
		default Mono<io.grpc.testing.integration.Messages.StreamingInputCallResponse> streamingInputCall(Publisher<io.grpc.testing.integration.Messages.StreamingInputCallRequest> request) {
			return this.streamingInputCall(request, (Consumer<A>)null);
		}

		/**
		 * A sequence of requests followed by one response (streamed upload).
		 * The server returns the aggregated size of client payload as the result.
		 *
		 * @param request the client request publisher
		 * @param contextConfigurer the context configurer
		 *
		 * @return the server response
		 */
		default Mono<io.grpc.testing.integration.Messages.StreamingInputCallResponse> streamingInputCall(Publisher<io.grpc.testing.integration.Messages.StreamingInputCallRequest> request, Consumer<A> contextConfigurer) {
			return this.streamingInputCall(contextConfigurer)
				.flatMap(grpcExchange -> {
					grpcExchange.request().stream(request);
					return grpcExchange.response().flatMap(GrpcResponse.Unary::value);
				});
		}
		/**
		 * A sequence of requests with each request served by the server immediately.
		 * As one request could lead to multiple responses, this interface
		 * demonstrates the idea of full duplexing.
		 *
		 * @return a mono emitting the bidirectional streaming exchange
		 */
		default Mono<GrpcExchange.BidirectionalStreaming<A, io.grpc.testing.integration.Messages.StreamingOutputCallRequest, io.grpc.testing.integration.Messages.StreamingOutputCallResponse>> fullDuplexCall() {
			return this.fullDuplexCall((Consumer<A>)null);
		}

		/**
		 * A sequence of requests with each request served by the server immediately.
		 * As one request could lead to multiple responses, this interface
		 * demonstrates the idea of full duplexing.
		 *
		 * @param contextConfigurer the context configurer
		 *
		 * @return a mono emitting the bidirectional streaming exchange
		 */
		Mono<GrpcExchange.BidirectionalStreaming<A, io.grpc.testing.integration.Messages.StreamingOutputCallRequest, io.grpc.testing.integration.Messages.StreamingOutputCallResponse>> fullDuplexCall(Consumer<A> contextConfigurer);

		/**
		 * A sequence of requests with each request served by the server immediately.
		 * As one request could lead to multiple responses, this interface
		 * demonstrates the idea of full duplexing.
		 *
		 * @param request the client request publisher
		 *
		 * @return the server response publisher
		 */
		default Publisher<io.grpc.testing.integration.Messages.StreamingOutputCallResponse> fullDuplexCall(Publisher<io.grpc.testing.integration.Messages.StreamingOutputCallRequest> request) {
			return this.fullDuplexCall(request, (Consumer<A>)null);
		}

		/**
		 * A sequence of requests with each request served by the server immediately.
		 * As one request could lead to multiple responses, this interface
		 * demonstrates the idea of full duplexing.
		 *
		 * @param request           the client request publisher
		 * @param contextConfigurer the context configurer
		 *
		 * @return the server response publisher
		 */
		default Publisher<io.grpc.testing.integration.Messages.StreamingOutputCallResponse> fullDuplexCall(Publisher<io.grpc.testing.integration.Messages.StreamingOutputCallRequest> request, Consumer<A> contextConfigurer) {
			return this.fullDuplexCall(contextConfigurer)
				.flatMapMany(grpcExchange -> {
					grpcExchange.request().stream(request);
					return grpcExchange.response().flatMapMany(GrpcResponse.Streaming::stream);
				});
		}
		/**
		 * A sequence of requests followed by a sequence of responses.
		 * The server buffers all the client requests and then serves them in order. A
		 * stream of responses are returned to the client when the server starts with
		 * first request.
		 *
		 * @return a mono emitting the bidirectional streaming exchange
		 */
		default Mono<GrpcExchange.BidirectionalStreaming<A, io.grpc.testing.integration.Messages.StreamingOutputCallRequest, io.grpc.testing.integration.Messages.StreamingOutputCallResponse>> halfDuplexCall() {
			return this.halfDuplexCall((Consumer<A>)null);
		}

		/**
		 * A sequence of requests followed by a sequence of responses.
		 * The server buffers all the client requests and then serves them in order. A
		 * stream of responses are returned to the client when the server starts with
		 * first request.
		 *
		 * @param contextConfigurer the context configurer
		 *
		 * @return a mono emitting the bidirectional streaming exchange
		 */
		Mono<GrpcExchange.BidirectionalStreaming<A, io.grpc.testing.integration.Messages.StreamingOutputCallRequest, io.grpc.testing.integration.Messages.StreamingOutputCallResponse>> halfDuplexCall(Consumer<A> contextConfigurer);

		/**
		 * A sequence of requests followed by a sequence of responses.
		 * The server buffers all the client requests and then serves them in order. A
		 * stream of responses are returned to the client when the server starts with
		 * first request.
		 *
		 * @param request the client request publisher
		 *
		 * @return the server response publisher
		 */
		default Publisher<io.grpc.testing.integration.Messages.StreamingOutputCallResponse> halfDuplexCall(Publisher<io.grpc.testing.integration.Messages.StreamingOutputCallRequest> request) {
			return this.halfDuplexCall(request, (Consumer<A>)null);
		}

		/**
		 * A sequence of requests followed by a sequence of responses.
		 * The server buffers all the client requests and then serves them in order. A
		 * stream of responses are returned to the client when the server starts with
		 * first request.
		 *
		 * @param request           the client request publisher
		 * @param contextConfigurer the context configurer
		 *
		 * @return the server response publisher
		 */
		default Publisher<io.grpc.testing.integration.Messages.StreamingOutputCallResponse> halfDuplexCall(Publisher<io.grpc.testing.integration.Messages.StreamingOutputCallRequest> request, Consumer<A> contextConfigurer) {
			return this.halfDuplexCall(contextConfigurer)
				.flatMapMany(grpcExchange -> {
					grpcExchange.request().stream(request);
					return grpcExchange.response().flatMapMany(GrpcResponse.Streaming::stream);
				});
		}
		/**
		 * The test server will not implement this method. It will be used
		 * to test the behavior when clients call unimplemented methods.
		 *
		 * @return a mono emitting the unary exchange
		 */
		default Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.EmptyProtos.Empty, io.grpc.testing.integration.EmptyProtos.Empty>> unimplementedCall() {
			return this.unimplementedCall((Consumer<A>)null);
		}

		/**
		 * The test server will not implement this method. It will be used
		 * to test the behavior when clients call unimplemented methods.
		 *
		 * @param contextConfigurer the context configurer
		 *
		 * @return a mono emitting the unary exchange
		 */
		Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.EmptyProtos.Empty, io.grpc.testing.integration.EmptyProtos.Empty>> unimplementedCall(Consumer<A> contextConfigurer);

		/**
		 * The test server will not implement this method. It will be used
		 * to test the behavior when clients call unimplemented methods.
		 *
		 * @param request the client request
		 *
		 * @return the server response
		 */
		default Mono<io.grpc.testing.integration.EmptyProtos.Empty> unimplementedCall(io.grpc.testing.integration.EmptyProtos.Empty request) {
			return this.unimplementedCall(request, (Consumer<A>)null);
		}

		/**
		 * The test server will not implement this method. It will be used
		 * to test the behavior when clients call unimplemented methods.
		 *
		 * @param request           the client request
		 * @param contextConfigurer the context configurer
		 *
		 * @return the server response
		 */
		default Mono<io.grpc.testing.integration.EmptyProtos.Empty> unimplementedCall(io.grpc.testing.integration.EmptyProtos.Empty request, Consumer<A> contextConfigurer) {
			return this.unimplementedCall(contextConfigurer)
				.flatMap(grpcExchange -> {
					grpcExchange.request().value(request);
					return grpcExchange.response().flatMap(GrpcResponse.Unary::value);
				});
		}
	}
}
