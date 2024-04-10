package io.grpc.testing.integration;

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
 * A simple service to test the various types of RPCs and experiment with
 * performance with various types of payload.
 */
@Bean
public final class TestServiceGrpcClient {

	public static final GrpcServiceName SERVICE_NAME = GrpcServiceName.of("grpc.testing", "TestService");
	
	private final HttpClient httpClient;
	private final GrpcClient grpcClient;
	
	public TestServiceGrpcClient(HttpClient httpClient, GrpcClient grpcClient) {
		this.httpClient = httpClient;
		this.grpcClient = grpcClient;
	}

	public <A extends ExchangeContext> TestServiceGrpcClient.Stub<A> createStub(String host, int port) {
		return new TestServiceGrpcClient.StubImpl<>(this.httpClient.<A>endpoint(host, port).build(), true);
	}
	
	public <A extends ExchangeContext> TestServiceGrpcClient.Stub<A> createStub(InetSocketAddress remoteAddress) {
		return new TestServiceGrpcClient.StubImpl<>(this.httpClient.<A>endpoint(remoteAddress).build(), true);
	}
	
	public <A extends ExchangeContext> TestServiceGrpcClient.Stub<A> createStub(Endpoint<A> endpoint) {
		return new TestServiceGrpcClient.StubImpl<>(endpoint, false);
	}
	
	private final class StubImpl<A extends ExchangeContext> implements TestServiceGrpcClient.Stub<A> {

		private final Endpoint<A> endpoint;
		private final boolean shutdownEndpoint;
		private final Consumer<GrpcOutboundRequestMetadata> metadataConfigurer;
		
		public StubImpl(Endpoint<A> endpoint, boolean shutdownEndpoint) {
			this.endpoint = endpoint;
			this.shutdownEndpoint = shutdownEndpoint;
			this.metadataConfigurer = null;
		}
		
		private StubImpl(TestServiceGrpcClient.StubImpl<A> parent, Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
			this.endpoint = parent.endpoint;
			this.shutdownEndpoint = false;
			this.metadataConfigurer = metadataConfigurer;
		}

		@Override
		public TestServiceGrpcClient.Stub<A> withMetadata(Consumer<GrpcOutboundRequestMetadata> metadataConfigurer) {
			return new TestServiceGrpcClient.StubImpl<>(this, metadataConfigurer);
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
					GrpcExchange.Unary<A, io.grpc.testing.integration.EmptyProtos.Empty, io.grpc.testing.integration.EmptyProtos.Empty> grpcExchange = TestServiceGrpcClient.this.grpcClient.unary(exchange, SERVICE_NAME, "EmptyCall", io.grpc.testing.integration.EmptyProtos.Empty.getDefaultInstance(), io.grpc.testing.integration.EmptyProtos.Empty.getDefaultInstance());
					grpcExchange.request().metadata(this.metadataConfigurer);
					return grpcExchange;
				});
		}
		@Override
		public Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.SimpleRequest, io.grpc.testing.integration.Messages.SimpleResponse>> unaryCall(A context) {
			return this.endpoint.exchange(context)
				.map(exchange -> {
					GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.SimpleRequest, io.grpc.testing.integration.Messages.SimpleResponse> grpcExchange = TestServiceGrpcClient.this.grpcClient.unary(exchange, SERVICE_NAME, "UnaryCall", io.grpc.testing.integration.Messages.SimpleRequest.getDefaultInstance(), io.grpc.testing.integration.Messages.SimpleResponse.getDefaultInstance());
					grpcExchange.request().metadata(this.metadataConfigurer);
					return grpcExchange;
				});
		}
		@Override
		public Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.SimpleRequest, io.grpc.testing.integration.Messages.SimpleResponse>> cacheableUnaryCall(A context) {
			return this.endpoint.exchange(context)
				.map(exchange -> {
					GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.SimpleRequest, io.grpc.testing.integration.Messages.SimpleResponse> grpcExchange = TestServiceGrpcClient.this.grpcClient.unary(exchange, SERVICE_NAME, "CacheableUnaryCall", io.grpc.testing.integration.Messages.SimpleRequest.getDefaultInstance(), io.grpc.testing.integration.Messages.SimpleResponse.getDefaultInstance());
					grpcExchange.request().metadata(this.metadataConfigurer);
					return grpcExchange;
				});
		}
		@Override
		public Mono<GrpcExchange.ServerStreaming<A, io.grpc.testing.integration.Messages.StreamingOutputCallRequest, io.grpc.testing.integration.Messages.StreamingOutputCallResponse>> streamingOutputCall(A context) {
			return this.endpoint.exchange(context)
				.map(exchange -> {
					GrpcExchange.ServerStreaming<A, io.grpc.testing.integration.Messages.StreamingOutputCallRequest, io.grpc.testing.integration.Messages.StreamingOutputCallResponse> grpcExchange = TestServiceGrpcClient.this.grpcClient.serverStreaming(exchange, SERVICE_NAME, "StreamingOutputCall", io.grpc.testing.integration.Messages.StreamingOutputCallRequest.getDefaultInstance(), io.grpc.testing.integration.Messages.StreamingOutputCallResponse.getDefaultInstance());
					grpcExchange.request().metadata(this.metadataConfigurer);
					return grpcExchange;
				});
		}
		@Override
		public Mono<GrpcExchange.ClientStreaming<A, io.grpc.testing.integration.Messages.StreamingInputCallRequest, io.grpc.testing.integration.Messages.StreamingInputCallResponse>> streamingInputCall(A context) {
			return this.endpoint.exchange(context)
				.map(exchange -> {
					GrpcExchange.ClientStreaming<A, io.grpc.testing.integration.Messages.StreamingInputCallRequest, io.grpc.testing.integration.Messages.StreamingInputCallResponse> grpcExchange = TestServiceGrpcClient.this.grpcClient.clientStreaming(exchange, SERVICE_NAME, "StreamingInputCall", io.grpc.testing.integration.Messages.StreamingInputCallRequest.getDefaultInstance(), io.grpc.testing.integration.Messages.StreamingInputCallResponse.getDefaultInstance());
					grpcExchange.request().metadata(this.metadataConfigurer);
					return grpcExchange;
				});
		}
		@Override
		public Mono<GrpcExchange.BidirectionalStreaming<A, io.grpc.testing.integration.Messages.StreamingOutputCallRequest, io.grpc.testing.integration.Messages.StreamingOutputCallResponse>> fullDuplexCall(A context) {
			return this.endpoint.exchange(context)
				.map(exchange -> {
					GrpcExchange.BidirectionalStreaming<A, io.grpc.testing.integration.Messages.StreamingOutputCallRequest, io.grpc.testing.integration.Messages.StreamingOutputCallResponse> grpcExchange = TestServiceGrpcClient.this.grpcClient.bidirectionalStreaming(exchange, SERVICE_NAME, "FullDuplexCall", io.grpc.testing.integration.Messages.StreamingOutputCallRequest.getDefaultInstance(), io.grpc.testing.integration.Messages.StreamingOutputCallResponse.getDefaultInstance());
					grpcExchange.request().metadata(this.metadataConfigurer);
					return grpcExchange;
				});
		}
		@Override
		public Mono<GrpcExchange.BidirectionalStreaming<A, io.grpc.testing.integration.Messages.StreamingOutputCallRequest, io.grpc.testing.integration.Messages.StreamingOutputCallResponse>> halfDuplexCall(A context) {
			return this.endpoint.exchange(context)
				.map(exchange -> {
					GrpcExchange.BidirectionalStreaming<A, io.grpc.testing.integration.Messages.StreamingOutputCallRequest, io.grpc.testing.integration.Messages.StreamingOutputCallResponse> grpcExchange = TestServiceGrpcClient.this.grpcClient.bidirectionalStreaming(exchange, SERVICE_NAME, "HalfDuplexCall", io.grpc.testing.integration.Messages.StreamingOutputCallRequest.getDefaultInstance(), io.grpc.testing.integration.Messages.StreamingOutputCallResponse.getDefaultInstance());
					grpcExchange.request().metadata(this.metadataConfigurer);
					return grpcExchange;
				});
		}
		@Override
		public Mono<GrpcExchange.Unary<A, io.grpc.testing.integration.EmptyProtos.Empty, io.grpc.testing.integration.EmptyProtos.Empty>> unimplementedCall(A context) {
			return this.endpoint.exchange(context)
				.map(exchange -> {
					GrpcExchange.Unary<A, io.grpc.testing.integration.EmptyProtos.Empty, io.grpc.testing.integration.EmptyProtos.Empty> grpcExchange = TestServiceGrpcClient.this.grpcClient.unary(exchange, SERVICE_NAME, "UnimplementedCall", io.grpc.testing.integration.EmptyProtos.Empty.getDefaultInstance(), io.grpc.testing.integration.EmptyProtos.Empty.getDefaultInstance());
					grpcExchange.request().metadata(this.metadataConfigurer);
					return grpcExchange;
				});
		}
	}
	
	/**
	 * A simple service to test the various types of RPCs and experiment with
	 * performance with various types of payload.
	 *
	 * @param <A> the exchange context type
	 */
	public interface Stub<A extends ExchangeContext> extends GrpcClient.Stub<A, TestServiceGrpcClient.Stub<A>> {
		
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
		 * @param <A>     the context type
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
		 * @param <A>     the context type
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
		 * @param <A>     the context type
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
		 * @param <A>     the context type
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
		 * @param <A>     the context type
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
		 * @param <A>     the context type
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
		 * @param <A>     the context type
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
		 * @param <A>     the context type
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
		 * @param <A>     the context type
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
		 * @param <A>     the context type
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
		 * @param <A>     the context type
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
		 * @param <A>     the context type
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
		 * @param <A>     the context type
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
		 * @param <A>     the context type
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
		 * @param <A>     the context type
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
		 * @param <A>     the context type
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
}
