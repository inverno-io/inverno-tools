package io.grpc.testing.integration;

import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.grpc.base.GrpcServiceName;
import io.inverno.mod.grpc.server.GrpcExchange;
import io.inverno.mod.grpc.server.GrpcServer;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.web.server.WebRouter;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * A simple service to test the various types of RPCs and experiment with
 * performance with various types of payload.
 *
 * @param <A> the exchange context type
 */
public abstract class TestServiceGrpcRoutesConfigurer<A extends ExchangeContext> implements WebRouter.Configurer<A> {

	public static final GrpcServiceName SERVICE_NAME = GrpcServiceName.of("grpc.testing", "TestService");
	
	private final GrpcServer grpcServer;
	
	public TestServiceGrpcRoutesConfigurer(GrpcServer grpcServer) {
		this.grpcServer = grpcServer;
	}
	
	@Override
	public final void configure(WebRouter<A> routes) {
		routes
			.route()
				.path(SERVICE_NAME.methodPath("EmptyCall"))
				.method(Method.POST)
				.consume(MediaTypes.APPLICATION_GRPC)
				.consume(MediaTypes.APPLICATION_GRPC_PROTO)
				.handler(this.grpcServer.unary(
					io.grpc.testing.integration.EmptyProtos.Empty.getDefaultInstance(), 
					io.grpc.testing.integration.EmptyProtos.Empty.getDefaultInstance(), 
					(GrpcExchange.Unary<A, io.grpc.testing.integration.EmptyProtos.Empty, io.grpc.testing.integration.EmptyProtos.Empty> grpcExchange) -> this.emptyCall(grpcExchange)
				))
			.route()
				.path(SERVICE_NAME.methodPath("UnaryCall"))
				.method(Method.POST)
				.consume(MediaTypes.APPLICATION_GRPC)
				.consume(MediaTypes.APPLICATION_GRPC_PROTO)
				.handler(this.grpcServer.unary(
					io.grpc.testing.integration.Messages.SimpleRequest.getDefaultInstance(), 
					io.grpc.testing.integration.Messages.SimpleResponse.getDefaultInstance(), 
					(GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.SimpleRequest, io.grpc.testing.integration.Messages.SimpleResponse> grpcExchange) -> this.unaryCall(grpcExchange)
				))
			.route()
				.path(SERVICE_NAME.methodPath("CacheableUnaryCall"))
				.method(Method.POST)
				.consume(MediaTypes.APPLICATION_GRPC)
				.consume(MediaTypes.APPLICATION_GRPC_PROTO)
				.handler(this.grpcServer.unary(
					io.grpc.testing.integration.Messages.SimpleRequest.getDefaultInstance(), 
					io.grpc.testing.integration.Messages.SimpleResponse.getDefaultInstance(), 
					(GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.SimpleRequest, io.grpc.testing.integration.Messages.SimpleResponse> grpcExchange) -> this.cacheableUnaryCall(grpcExchange)
				))
			.route()
				.path(SERVICE_NAME.methodPath("StreamingOutputCall"))
				.method(Method.POST)
				.consume(MediaTypes.APPLICATION_GRPC)
				.consume(MediaTypes.APPLICATION_GRPC_PROTO)
				.handler(this.grpcServer.serverStreaming(
					io.grpc.testing.integration.Messages.StreamingOutputCallRequest.getDefaultInstance(), 
					io.grpc.testing.integration.Messages.StreamingOutputCallResponse.getDefaultInstance(), 
					(GrpcExchange.ServerStreaming<A, io.grpc.testing.integration.Messages.StreamingOutputCallRequest, io.grpc.testing.integration.Messages.StreamingOutputCallResponse> grpcExchange) -> this.streamingOutputCall(grpcExchange)
				))
			.route()
				.path(SERVICE_NAME.methodPath("StreamingInputCall"))
				.method(Method.POST)
				.consume(MediaTypes.APPLICATION_GRPC)
				.consume(MediaTypes.APPLICATION_GRPC_PROTO)
				.handler(this.grpcServer.clientStreaming(
					io.grpc.testing.integration.Messages.StreamingInputCallRequest.getDefaultInstance(), 
					io.grpc.testing.integration.Messages.StreamingInputCallResponse.getDefaultInstance(), 
					(GrpcExchange.ClientStreaming<A, io.grpc.testing.integration.Messages.StreamingInputCallRequest, io.grpc.testing.integration.Messages.StreamingInputCallResponse> grpcExchange) -> this.streamingInputCall(grpcExchange)
				))
			.route()
				.path(SERVICE_NAME.methodPath("FullDuplexCall"))
				.method(Method.POST)
				.consume(MediaTypes.APPLICATION_GRPC)
				.consume(MediaTypes.APPLICATION_GRPC_PROTO)
				.handler(this.grpcServer.bidirectionalStreaming(
					io.grpc.testing.integration.Messages.StreamingOutputCallRequest.getDefaultInstance(), 
					io.grpc.testing.integration.Messages.StreamingOutputCallResponse.getDefaultInstance(), 
					(GrpcExchange.BidirectionalStreaming<A, io.grpc.testing.integration.Messages.StreamingOutputCallRequest, io.grpc.testing.integration.Messages.StreamingOutputCallResponse> grpcExchange) -> this.fullDuplexCall(grpcExchange)
				))
			.route()
				.path(SERVICE_NAME.methodPath("HalfDuplexCall"))
				.method(Method.POST)
				.consume(MediaTypes.APPLICATION_GRPC)
				.consume(MediaTypes.APPLICATION_GRPC_PROTO)
				.handler(this.grpcServer.bidirectionalStreaming(
					io.grpc.testing.integration.Messages.StreamingOutputCallRequest.getDefaultInstance(), 
					io.grpc.testing.integration.Messages.StreamingOutputCallResponse.getDefaultInstance(), 
					(GrpcExchange.BidirectionalStreaming<A, io.grpc.testing.integration.Messages.StreamingOutputCallRequest, io.grpc.testing.integration.Messages.StreamingOutputCallResponse> grpcExchange) -> this.halfDuplexCall(grpcExchange)
				))
			.route()
				.path(SERVICE_NAME.methodPath("UnimplementedCall"))
				.method(Method.POST)
				.consume(MediaTypes.APPLICATION_GRPC)
				.consume(MediaTypes.APPLICATION_GRPC_PROTO)
				.handler(this.grpcServer.unary(
					io.grpc.testing.integration.EmptyProtos.Empty.getDefaultInstance(), 
					io.grpc.testing.integration.EmptyProtos.Empty.getDefaultInstance(), 
					(GrpcExchange.Unary<A, io.grpc.testing.integration.EmptyProtos.Empty, io.grpc.testing.integration.EmptyProtos.Empty> grpcExchange) -> this.unimplementedCall(grpcExchange)
				));
	}
	
	/**
	 * One empty request followed by one empty response.
	 * 
	 * @param exchange the unary exchange
	 */
	public void emptyCall(GrpcExchange.Unary<A, io.grpc.testing.integration.EmptyProtos.Empty, io.grpc.testing.integration.EmptyProtos.Empty> grpcExchange) {
		grpcExchange.response().value(grpcExchange.request().value().flatMap(this::emptyCall));
	}

	/**
	 * One empty request followed by one empty response.
	 * 
	 * @param request the client request
	 * 
	 * @return the server response
	 */
	public Mono<io.grpc.testing.integration.EmptyProtos.Empty> emptyCall(io.grpc.testing.integration.EmptyProtos.Empty request) {
		throw new UnsupportedOperationException();
	}

	/**
	 * One request followed by one response.
	 * 
	 * @param exchange the unary exchange
	 */
	public void unaryCall(GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.SimpleRequest, io.grpc.testing.integration.Messages.SimpleResponse> grpcExchange) {
		grpcExchange.response().value(grpcExchange.request().value().flatMap(this::unaryCall));
	}

	/**
	 * One request followed by one response.
	 * 
	 * @param request the client request
	 * 
	 * @return the server response
	 */
	public Mono<io.grpc.testing.integration.Messages.SimpleResponse> unaryCall(io.grpc.testing.integration.Messages.SimpleRequest request) {
		throw new UnsupportedOperationException();
	}

	/**
	 * One request followed by one response. Response has cache control
	 * headers set such that a caching HTTP proxy (such as GFE) can
	 * satisfy subsequent requests.
	 * 
	 * @param exchange the unary exchange
	 */
	public void cacheableUnaryCall(GrpcExchange.Unary<A, io.grpc.testing.integration.Messages.SimpleRequest, io.grpc.testing.integration.Messages.SimpleResponse> grpcExchange) {
		grpcExchange.response().value(grpcExchange.request().value().flatMap(this::cacheableUnaryCall));
	}

	/**
	 * One request followed by one response. Response has cache control
	 * headers set such that a caching HTTP proxy (such as GFE) can
	 * satisfy subsequent requests.
	 * 
	 * @param request the client request
	 * 
	 * @return the server response
	 */
	public Mono<io.grpc.testing.integration.Messages.SimpleResponse> cacheableUnaryCall(io.grpc.testing.integration.Messages.SimpleRequest request) {
		throw new UnsupportedOperationException();
	}

	/**
	 * One request followed by a sequence of responses (streamed download).
	 * The server returns the payload with client desired type and sizes.
	 * 
	 * @param exchange the server streaming exchange
	 */
	public void streamingOutputCall(GrpcExchange.ServerStreaming<A, io.grpc.testing.integration.Messages.StreamingOutputCallRequest, io.grpc.testing.integration.Messages.StreamingOutputCallResponse> grpcExchange) {
		grpcExchange.response().stream(grpcExchange.request().value().flatMapMany(this::streamingOutputCall));
	}

	/**
	 * One request followed by a sequence of responses (streamed download).
	 * The server returns the payload with client desired type and sizes.
	 * 
	 * @param request the client request
	 * 
	 * @return the server response publisher
	 */
	public Publisher<io.grpc.testing.integration.Messages.StreamingOutputCallResponse> streamingOutputCall(io.grpc.testing.integration.Messages.StreamingOutputCallRequest request) {
		throw new UnsupportedOperationException();
	}

	/**
	 * A sequence of requests followed by one response (streamed upload).
	 * The server returns the aggregated size of client payload as the result.
	 * 
	 * @param exchange the client streaming exchange
	 */
	public void streamingInputCall(GrpcExchange.ClientStreaming<A, io.grpc.testing.integration.Messages.StreamingInputCallRequest, io.grpc.testing.integration.Messages.StreamingInputCallResponse> grpcExchange) {
		grpcExchange.response().value(this.streamingInputCall(grpcExchange.request().stream()));
	}

	/**
	 * A sequence of requests followed by one response (streamed upload).
	 * The server returns the aggregated size of client payload as the result.
	 * 
	 * @param request the client request publisher
	 * 
	 * @return the server response
	 */
	public Mono<io.grpc.testing.integration.Messages.StreamingInputCallResponse> streamingInputCall(Publisher<io.grpc.testing.integration.Messages.StreamingInputCallRequest> request) {
		throw new UnsupportedOperationException();
	}

	/**
	 * A sequence of requests with each request served by the server immediately.
	 * As one request could lead to multiple responses, this interface
	 * demonstrates the idea of full duplexing.
	 * 
	 * @param exchange the bidirectional streaming exchange
	 */
	public void fullDuplexCall(GrpcExchange.BidirectionalStreaming<A, io.grpc.testing.integration.Messages.StreamingOutputCallRequest, io.grpc.testing.integration.Messages.StreamingOutputCallResponse> grpcExchange) {
		grpcExchange.response().stream(this.fullDuplexCall(grpcExchange.request().stream()));
	}

	/**
	 * A sequence of requests with each request served by the server immediately.
	 * As one request could lead to multiple responses, this interface
	 * demonstrates the idea of full duplexing.
	 * 
	 * @param request the client request publisher
	 * 
	 * @return the server response publisher
	 */
	public Publisher<io.grpc.testing.integration.Messages.StreamingOutputCallResponse> fullDuplexCall(Publisher<io.grpc.testing.integration.Messages.StreamingOutputCallRequest> request) {
		throw new UnsupportedOperationException();
	}

	/**
	 * A sequence of requests followed by a sequence of responses.
	 * The server buffers all the client requests and then serves them in order. A
	 * stream of responses are returned to the client when the server starts with
	 * first request.
	 * 
	 * @param exchange the bidirectional streaming exchange
	 */
	public void halfDuplexCall(GrpcExchange.BidirectionalStreaming<A, io.grpc.testing.integration.Messages.StreamingOutputCallRequest, io.grpc.testing.integration.Messages.StreamingOutputCallResponse> grpcExchange) {
		grpcExchange.response().stream(this.halfDuplexCall(grpcExchange.request().stream()));
	}

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
	public Publisher<io.grpc.testing.integration.Messages.StreamingOutputCallResponse> halfDuplexCall(Publisher<io.grpc.testing.integration.Messages.StreamingOutputCallRequest> request) {
		throw new UnsupportedOperationException();
	}

	/**
	 * The test server will not implement this method. It will be used
	 * to test the behavior when clients call unimplemented methods.
	 * 
	 * @param exchange the unary exchange
	 */
	public void unimplementedCall(GrpcExchange.Unary<A, io.grpc.testing.integration.EmptyProtos.Empty, io.grpc.testing.integration.EmptyProtos.Empty> grpcExchange) {
		grpcExchange.response().value(grpcExchange.request().value().flatMap(this::unimplementedCall));
	}

	/**
	 * The test server will not implement this method. It will be used
	 * to test the behavior when clients call unimplemented methods.
	 * 
	 * @param request the client request
	 * 
	 * @return the server response
	 */
	public Mono<io.grpc.testing.integration.EmptyProtos.Empty> unimplementedCall(io.grpc.testing.integration.EmptyProtos.Empty request) {
		throw new UnsupportedOperationException();
	}

}
